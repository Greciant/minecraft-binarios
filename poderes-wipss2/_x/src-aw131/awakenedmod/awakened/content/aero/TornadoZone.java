package com.awakenedmod.awakened.content.aero;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class TornadoZone implements AeroAmbience.TickFx {
   private static final double DRAG_IN = 0.16;
   static final double INFLUENCE_RADIUS = 12.0;
   private static final double BELOW_REACH = 6.0;
   private static final double PULL_EDGE = 0.55;
   private static final double PULL_RIM = 0.12;
   private static final double FOOTING_POP = 0.12;
   private static final double SPIN = 0.08;
   private static final double LIFT = 0.13;
   private static final double LIFT_RATE = 0.2;
   private static final double LIFT_TARGET = 5.0;
   private static final int FLING_WINDOW = 15;
   private final ServerLevel level;
   private final Vec3 base;
   private final ServerPlayer caster;
   private final UUID casterId;
   private final double flingStrength;
   private final float tickDamage;
   private final float flingDamage;
   private final Map<UUID, Double> entryY = new HashMap<>();
   private final Set<UUID> flung = new HashSet<>();
   private final List<LivingEntity> flungMobs = new ArrayList<>();
   private final Consumer<TornadoZone> onEnd;
   private boolean ended;
   private int age;

   TornadoZone(ServerLevel level, Vec3 base, ServerPlayer caster, double flingStrength, float tickDamage, float flingDamage, Consumer<TornadoZone> onEnd) {
      this.level = level;
      this.base = base;
      this.caster = caster;
      this.casterId = caster.getUUID();
      this.flingStrength = flingStrength;
      this.tickDamage = tickDamage;
      this.flingDamage = flingDamage;
      this.onEnd = onEnd;
   }

   void end() {
      if (!this.ended) {
         this.ended = true;
         this.onEnd.accept(this);
      }
   }

   @Override
   public boolean tick() {
      if (this.ended) {
         return false;
      } else {
         this.age++;
         if (this.age > 100) {
            this.awardFlingCatch();
            this.end();
            return false;
         } else {
            this.sweepLiving();
            this.scatterLoose();
            AeroVfx.pullBand(this.level, this.base, 4.0, 12.0);
            return true;
         }
      }
   }

   private void awardFlingCatch() {
      List<LivingEntity> worthThrowing = this.flungMobs.stream().filter(e -> Foes.counts(this.caster, e)).toList();
      if (worthThrowing.size() >= 3) {
         MasterySystem.award(this.caster, MasteryReason.HIT, worthThrowing);
         QuestSystem.feat(this.caster, "aero_fling_group");
      }
   }

   private AABB bounds() {
      return new AABB(this.base.x - 12.0, this.base.y - 6.0, this.base.z - 12.0, this.base.x + 12.0, this.base.y + 12.0, this.base.z + 12.0);
   }

   private double horizDist(Entity e) {
      double dx = e.getX() - this.base.x;
      double dz = e.getZ() - this.base.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private boolean inFunnel(Entity e) {
      double dy = e.getY() - this.base.y;
      return this.horizDist(e) <= 4.0 && dy >= -2.0 && dy <= 12.0;
   }

   private boolean inInfluence(Entity e) {
      double dy = e.getY() - this.base.y;
      return this.horizDist(e) <= 12.0 && dy >= -6.0 && dy <= 12.0;
   }

   private void sweepLiving() {
      for (LivingEntity e : this.level
         .getEntitiesOfClass(LivingEntity.class, this.bounds(), le -> le.isAlive() && this.inInfluence(le) && Harm.allowed(this.caster, le))) {
         UUID id = e.getUUID();
         if (!this.flung.contains(id)) {
            if (!this.inFunnel(e)) {
               if (!id.equals(this.casterId)) {
                  this.pullInward(e);
               }
            } else {
               double startY = this.entryY.computeIfAbsent(id, k -> e.getY());
               boolean forceFling = this.age >= 85 || e.getY() - startY >= 5.0 || e.getY() - this.base.y >= 10.5;
               if (forceFling) {
                  this.flingOut(e, id);
               } else {
                  this.drag(e);
                  if (!id.equals(this.casterId) && this.age % 20 == 0) {
                     Dmg.hurt(e, this.level, Dmg.by(this.caster, DamageTypes.PLAYER_ATTACK), this.tickDamage);
                  }
               }
            }
         }
      }
   }

   private void pullInward(LivingEntity e) {
      double r = this.horizDist(e);
      if (!(r < 0.001)) {
         double t = Mth.clamp((12.0 - r) / 8.0, 0.0, 1.0);
         double pull = (0.12 + 0.43000000000000005 * t) * (1.0 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0));
         if (!(pull <= 0.0)) {
            Vec3 inward = new Vec3((this.base.x - e.getX()) / r, 0.0, (this.base.z - e.getZ()) / r);
            double up = 0.0;
            double depth = this.base.y - e.getY();
            if (depth > 0.5) {
               up = Mth.clamp(depth * 0.06, 0.06, 0.3) * t;
            } else if (e.onGround() && t > 0.5) {
               up = 0.12 * t;
            }

            e.push(inward.x * pull, up, inward.z * pull);
            e.hurtMarked = true;
         }
      }
   }

   private void drag(LivingEntity e) {
      double dx = this.base.x - e.getX();
      double dz = this.base.z - e.getZ();
      double horiz = Math.sqrt(dx * dx + dz * dz);
      Vec3 inward = horiz > 0.001 ? new Vec3(dx / horiz, 0.0, dz / horiz) : Vec3.ZERO;
      Vec3 tangent = new Vec3(-inward.z, 0.0, inward.x);
      double lift = Mth.clamp(0.2 - e.getDeltaMovement().y, 0.0, 0.13);
      Vec3 push = inward.scale(0.16).add(tangent.scale(0.08)).add(0.0, lift, 0.0);
      e.push(push.x, push.y, push.z);
      e.hurtMarked = true;
   }

   private void flingOut(LivingEntity e, UUID id) {
      double dx = e.getX() - this.base.x;
      double dz = e.getZ() - this.base.z;
      double horiz = Math.sqrt(dx * dx + dz * dz);
      Vec3 out = horiz > 0.001 ? new Vec3(dx / horiz, 0.0, dz / horiz) : new Vec3(1.0, 0.0, 0.0);
      e.push(out.x * this.flingStrength, this.flingStrength * 0.6, out.z * this.flingStrength);
      e.hurtMarked = true;
      this.flung.add(id);
      if (!id.equals(this.casterId)) {
         this.flungMobs.add(e);
         Dmg.hurt(e, this.level, Dmg.by(this.caster, DamageTypes.PLAYER_ATTACK), this.flingDamage);
         AeroVfx.flingBurst(this.level, e.position().add(0.0, (double)e.getBbHeight() * 0.5, 0.0));
      }
   }

   private void scatterLoose() {
      RandomSource rand = this.level.random;

      for (Entity e : this.level
         .getEntitiesOfClass(Entity.class, this.bounds(), le -> le.isAlive() && this.inFunnel(le) && (le instanceof ItemEntity || le instanceof Projectile))) {
         double dx = this.base.x - e.getX();
         double dz = this.base.z - e.getZ();
         double horiz = Math.sqrt(dx * dx + dz * dz);
         Vec3 inward = horiz > 0.001 ? new Vec3(dx / horiz, 0.0, dz / horiz) : Vec3.ZERO;
         Vec3 tangent = new Vec3(-inward.z, 0.0, inward.x);
         Vec3 jitter = new Vec3(rand.nextGaussian(), rand.nextGaussian() * 0.4, rand.nextGaussian()).scale(0.08);
         Vec3 push = inward.scale(0.11199999999999999).add(tangent.scale(0.128)).add(0.0, 0.052000000000000005, 0.0).add(jitter);
         e.setDeltaMovement(e.getDeltaMovement().add(push));
         e.hurtMarked = true;
      }
   }
}
