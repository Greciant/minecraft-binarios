package com.awakenedmod.awakened.content.shadow;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ShadowZones {
   private static final List<ShadowZones.Zone> ZONES = new CopyOnWriteArrayList<>();
   private static long lastTick = Long.MIN_VALUE;
   private static final int DOT_INTERVAL = 10;
   private static final float DMG_DIM = 0.6F;
   private static final float DMG_DARK = 1.0F;
   private static final double SINK_DEPTH = 1.35;

   private ShadowZones() {
   }

   public static void open(
      ServerPlayer owner,
      ServerLevel level,
      Vec3 center,
      double radius,
      int ticks,
      float dmgPerSecond,
      boolean pull,
      boolean trap,
      boolean devourStyle,
      String abilityKey
   ) {
      ZONES.add(new ShadowZones.Zone(owner, level, center, radius, ticks, dmgPerSecond, pull, trap, devourStyle, abilityKey));
      if (devourStyle) {
         ShadowFx.devourOpen(level, center, radius);
         ShadowShapeEntity lake = new ShadowShapeEntity(AwakenedEntities.SHADOW_SHAPE.get(), level);
         lake.configure(ShadowShapeEntity.Shape.DISC, (float)radius, 788500, 252, ticks);
         lake.setPos(center.x, center.y + 0.05, center.z);
         level.addFreshEntity(lake);
      } else {
         ShadowFx.murk(level, center, (int)(16.0 + radius * 6.0), radius * 0.6);
      }
   }

   public static void tickAll(MinecraftServer server) {
      long now = server.overworld().getGameTime();
      if (now != lastTick) {
         lastTick = now;

         for (ShadowZones.Zone z : ZONES) {
            if (!z.tick()) {
               ZONES.remove(z);
            }
         }
      }
   }

   private static final class Zone {
      private final ServerPlayer owner;
      private final ServerLevel level;
      private final Vec3 center;
      private final double radius;
      private final float dmgPerSecond;
      private final boolean pull;
      private final boolean trap;
      private final boolean pit;
      private final String abilityKey;
      private int ticksLeft;
      private long age;
      private int dotTimer;
      private final Set<LivingEntity> sunk = new HashSet<>();

      private Zone(
         ServerPlayer owner,
         ServerLevel level,
         Vec3 center,
         double radius,
         int ticks,
         float dmgPerSecond,
         boolean pull,
         boolean trap,
         boolean pit,
         String abilityKey
      ) {
         this.owner = owner;
         this.level = level;
         this.center = center;
         this.radius = radius;
         this.ticksLeft = ticks;
         this.dmgPerSecond = dmgPerSecond;
         this.pull = pull;
         this.trap = trap;
         this.pit = pit;
         this.abilityKey = abilityKey;
      }

      private boolean tick() {
         if (this.ticksLeft-- <= 0) {
            for (LivingEntity e : this.sunk) {
               this.release(e);
            }

            this.sunk.clear();
            ShadowFx.murk(this.level, this.center, 30, this.radius * 0.6);
            return false;
         } else {
            this.age++;
            ShadowFx.devourTick(this.level, this.center, this.radius, this.age);
            boolean dealDot = ++this.dotTimer >= 10;
            if (dealDot) {
               this.dotTimer = 0;
            }

            float darkMul = ShadowDark.scaleAt(this.level, this.center, 0.6F, 1.0F);
            float perHit = this.dmgPerSecond * darkMul * 0.5F;
            boolean ownerValid = this.owner != null && this.owner.isAlive() && !this.owner.isRemoved();
            if (ownerValid) {
               perHit = MasteryScaling.scaledDamage(perHit, this.owner, this.abilityKey);
            } else {
               perHit = MasteryScaling.configScaledDamage(perHit, this.abilityKey);
            }

            double rSq = this.radius * this.radius;
            AABB box = new AABB(
               this.center.x - this.radius,
               this.center.y - 1.35 - 1.0,
               this.center.z - this.radius,
               this.center.x + this.radius,
               this.center.y + 2.0,
               this.center.z + this.radius
            );
            Set<LivingEntity> toRelease = new HashSet<>(this.sunk);

            for (LivingEntity e : this.level
               .getEntitiesOfClass(
                  LivingEntity.class,
                  box,
                  le -> le != this.owner
                        && le.isAlive()
                        && Harm.allowed(this.owner, le)
                        && (le.getX() - this.center.x) * (le.getX() - this.center.x) + (le.getZ() - this.center.z) * (le.getZ() - this.center.z) <= rSq
               )) {
               boolean sinkable = !(e instanceof Player);
               if (this.pull && sinkable) {
                  e.noPhysics = true;
                  this.sunk.add(e);
                  toRelease.remove(e);
                  Vec3 cur = e.getDeltaMovement();
                  double vy = Math.max(-0.5, Math.min(0.15, (this.center.y - 1.35 - e.getY()) * 0.5));
                  e.setDeltaMovement(cur.x * 0.5, vy, cur.z * 0.5);
                  e.hurtMarked = true;
                  e.resetFallDistance();
                  if (e instanceof Mob mob) {
                     mob.getNavigation().stop();
                  }

                  ShadowFx.devourDrag(this.level, e.position(), e.getBbWidth());
               }

               if (this.trap) {
                  Harm.effect(this.owner, e, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 250, false, false));
                  Harm.effect(this.owner, e, new MobEffectInstance(MobEffects.WEAKNESS, 10, 4, false, false));
                  Harm.effect(this.owner, e, new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 10, 9, false, false));
               }

               if (dealDot) {
                  if (ownerValid) {
                     boolean aliveBefore = e.isAlive();
                     Dmg.hurt(e, this.level, Dmg.by(this.owner, DamageTypes.MAGIC), perHit);
                     if (aliveBefore && !e.isAlive()) {
                        MasterySystem.award(this.owner, MasteryReason.KILL);
                        if (this.pit && Foes.defeatedBy(e, this.owner)) {
                           QuestSystem.feat(this.owner, "shadow_devour_kill");
                        }
                     }
                  } else {
                     Dmg.hurt(e, this.level, this.level.damageSources().magic(), perHit);
                  }
               }
            }

            for (LivingEntity e : toRelease) {
               this.release(e);
               this.sunk.remove(e);
            }

            return true;
         }
      }

      private void release(LivingEntity e) {
         if (e.isAlive() && !e.isRemoved()) {
            e.noPhysics = false;
            if (e.getY() < this.center.y) {
               e.setPos(e.getX(), this.center.y, e.getZ());
            }

            e.setDeltaMovement(0.0, 0.0, 0.0);
            e.resetFallDistance();
            e.hurtMarked = true;
         }
      }
   }
}
