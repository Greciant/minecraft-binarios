package com.awakenedmod.awakened.content.aero;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class AirRemovalZone implements AeroAmbience.TickFx, AirRules.Zone {
   private static final int AIR_DRAIN_PER_TICK = 9;
   private static final int BOUNDARY_SLOW_AMPLIFIER = 6;
   private static final int BOUNDARY_SLOW_TICKS = 8;
   private static final double BOUNDARY_DRAG = 0.45;
   private final float tickDamagePerSecond;
   private final ServerLevel level;
   private final Vec3 center;
   private final ServerPlayer caster;
   private final Runnable onEnd;
   private final Set<UUID> suppressed = new HashSet<>();
   private boolean ended;
   private int age;

   AirRemovalZone(ServerLevel level, Vec3 center, ServerPlayer caster, float tickDamagePerSecond, Runnable onEnd) {
      this.level = level;
      this.center = center;
      this.caster = caster;
      this.tickDamagePerSecond = tickDamagePerSecond;
      this.onEnd = onEnd;
      AirRules.register(this);
   }

   @Override
   public boolean containsEntity(Entity entity) {
      return AirRules.withinCylinder(entity, this.level, this.center, 9.0, 5.0, 3.0);
   }

   public ServerPlayer responsible() {
      return this.caster;
   }

   void end() {
      if (!this.ended) {
         this.ended = true;
         AirRules.unregister(this);
         this.onEnd.run();
      }
   }

   @Override
   public boolean tick() {
      if (this.ended) {
         return false;
      } else {
         this.age++;
         if (this.age > 240) {
            this.end();
            return false;
         } else {
            this.boundaryTick();
            if (this.age % 20 == 0) {
               this.damageTick();
            } else {
               this.holdTick();
            }

            return true;
         }
      }
   }

   private AABB bounds() {
      double r = 9.0;
      return new AABB(this.center.x - r, this.center.y - 3.0, this.center.z - r, this.center.x + r, this.center.y + 5.0 + 0.5, this.center.z + r);
   }

   private AABB shellBounds() {
      double r = 10.5;
      return new AABB(this.center.x - r, this.center.y - 3.0 - 1.5, this.center.z - r, this.center.x + r, this.center.y + 5.0 + 1.5, this.center.z + r);
   }

   private boolean inBoundaryShell(Entity entity) {
      if (entity.level() != this.level) {
         return false;
      } else {
         Vec3 pos = entity.position();
         double dx = pos.x - this.center.x;
         double dz = pos.z - this.center.z;
         double horiz = Math.sqrt(dx * dx + dz * dz);
         double dy = pos.y - this.center.y;
         double shell = 1.5;
         boolean nearWall = Math.abs(horiz - 9.0) <= shell && dy >= -3.0 - shell && dy <= 5.0 + shell;
         boolean nearCap = horiz <= 9.0 + shell && (Math.abs(dy - 5.0) <= shell || Math.abs(dy + 3.0) <= shell);
         return nearWall || nearCap;
      }
   }

   private void boundaryTick() {
      for (LivingEntity e : this.level
         .getEntitiesOfClass(
            LivingEntity.class, this.shellBounds(), ex -> ex.isAlive() && ex != this.caster && this.inBoundaryShell(ex) && Harm.allowed(this.caster, ex)
         )) {
         Harm.effect(this.caster, e, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 6, false, false));
         e.setDeltaMovement(e.getDeltaMovement().scale(0.45));
         e.hurtMarked = true;
         AeroVfx.boundaryBuffet(this.level, e);
      }
   }

   private void damageTick() {
      List<LivingEntity> inside = this.level
         .getEntitiesOfClass(
            LivingEntity.class, this.bounds(), ex -> ex.isAlive() && ex != this.caster && this.containsEntity(ex) && Harm.allowed(this.caster, ex)
         );

      for (LivingEntity e : inside) {
         Dmg.hurt(e, this.level, Dmg.by(this.caster, DamageTypes.DROWN), this.tickDamagePerSecond);
         e.clearFire();
         drainAir(e);
         AeroVfx.suffocationGasp(this.level, e);
         if (Foes.counts(this.caster, e) && this.suppressed.add(e.getUUID())) {
            QuestSystem.feat(this.caster, "aero_suppress");
         }
      }

      MasterySystem.award(this.caster, MasteryReason.HIT, inside);
   }

   private void holdTick() {
      for (LivingEntity e : this.level.getEntitiesOfClass(LivingEntity.class, this.bounds(), ex -> ex.isAlive() && this.containsEntity(ex))) {
         if (e.isOnFire()) {
            e.clearFire();
         }

         if (e != this.caster && Harm.allowed(this.caster, e)) {
            drainAir(e);
            AeroVfx.suffocationWisp(this.level, e);
         }
      }
   }

   private static void drainAir(LivingEntity e) {
      e.setAirSupply(Math.max(0, e.getAirSupply() - 9));
   }
}
