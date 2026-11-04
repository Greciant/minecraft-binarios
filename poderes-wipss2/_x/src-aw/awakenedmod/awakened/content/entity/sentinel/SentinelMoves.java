package com.awakenedmod.awakened.content.entity.sentinel;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.ThreatMoves;
import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SentinelMoves {
   public static final SentinelMove OPTIC_BEAM = new SentinelMove() {
      @Override
      public byte action() {
         return 1;
      }

      @Override
      public int windup() {
         return 22;
      }

      @Override
      public int recover() {
         return 24;
      }

      @Override
      public double minRange() {
         return 6.0;
      }

      @Override
      public double maxRange() {
         return 28.0;
      }

      @Override
      public int weight() {
         return 12;
      }

      public void windupTick(SentinelEntity self, ServerLevel level, LivingEntity target, int tick) {
         Vec3 head = self.position().add(0.0, (double)self.getBbHeight() * 0.9, 0.0);
         Fx.burst(level, Fx.OPTIC_RED, head, 4 + tick / 4, 0.25, 0.01);
         if (tick == 1) {
            Fx.sound(level, self.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.4F, 0.5F);
         }
      }

      public void strike(SentinelEntity self, ServerLevel level, LivingEntity target) {
         self.setAction((byte)2);
         float dmg = self.scaleDamage(8.0F);
         SentinelMoves.coneBeam(self, level, target, this.maxRange(), 22.0, dmg, ParticleTypes.SONIC_BOOM);
         Fx.sound(level, self.position(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.5F, 1.2F);
      }
   };
   public static final SentinelMove SEISMIC_STOMP = new SentinelMove() {
      @Override
      public byte action() {
         return 3;
      }

      @Override
      public int windup() {
         return 16;
      }

      @Override
      public int recover() {
         return 18;
      }

      @Override
      public double minRange() {
         return 0.0;
      }

      @Override
      public double maxRange() {
         return 9.0;
      }

      @Override
      public int weight() {
         return 10;
      }

      @Override
      public boolean requiresGround() {
         return true;
      }

      public void windupTick(SentinelEntity self, ServerLevel level, LivingEntity target, int tick) {
         if (tick == 1) {
            Fx.sound(level, self.position(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.6F, 0.5F);
         }
      }

      public void strike(SentinelEntity self, ServerLevel level, LivingEntity target) {
         ThreatMoves.shockwave(level, self, 8.0, self.scaleDamage(7.0F), 1.4, 0.55);
      }
   };
   public static final SentinelMove GRAB_THROW = new SentinelMove() {
      @Override
      public byte action() {
         return 4;
      }

      @Override
      public int windup() {
         return 12;
      }

      @Override
      public int recover() {
         return 16;
      }

      @Override
      public double minRange() {
         return 0.0;
      }

      @Override
      public double maxRange() {
         return 6.0;
      }

      @Override
      public int weight() {
         return 9;
      }

      @Override
      public boolean requiresGround() {
         return true;
      }

      public void strike(SentinelEntity self, ServerLevel level, LivingEntity target) {
         if (!(self.distanceToSqr(target) > 49.0)) {
            Dmg.hurt(target, level, self.damageSources().mobAttack(self), self.scaleDamage(9.0F));
            Vec3 away = target.position().subtract(self.position());
            if (away.lengthSqr() < 1.0E-4) {
               away = self.getLookAngle();
            }

            away = away.normalize();
            target.setDeltaMovement(away.x * 1.8, 1.1, away.z * 1.8);
            target.hurtMarked = true;
            Fx.burst(level, ParticleTypes.EXPLOSION, target.position().add(0.0, 0.5, 0.0), 6, 0.4, 0.05);
            Fx.sound(level, self.position(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.4F, 0.7F);
         }
      }
   };
   public static final SentinelMove PALM_CANNON = new SentinelMove() {
      @Override
      public byte action() {
         return 5;
      }

      @Override
      public int windup() {
         return 12;
      }

      @Override
      public int recover() {
         return 14;
      }

      @Override
      public double minRange() {
         return 6.0;
      }

      @Override
      public double maxRange() {
         return 30.0;
      }

      @Override
      public int weight() {
         return 12;
      }

      public void windupTick(SentinelEntity self, ServerLevel level, LivingEntity target, int tick) {
         Vec3 palm = SentinelMoves.muzzle(self, target, 0.66, 1.2);
         Fx.burst(level, Fx.OPTIC_RED, palm, 3, 0.15, 0.01);
      }

      public void strike(SentinelEntity self, ServerLevel level, LivingEntity target) {
         int bolts = 3 + self.escalationPhase();
         SentinelMoves.fireBolts(
            self, level, target, new ItemStack(Items.FIRE_CHARGE), bolts, self.scaleDamage(4.0F), 2.0F, 2.0F, 0.35F, 1.8F, 0.66, 8.0, Fx.OPTIC_RED, 0, 0, 0.0
         );
         Fx.sound(level, self.position(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.4F, 0.7F);
      }
   };
   public static final List<SentinelMove> BASE_KIT = List.of(OPTIC_BEAM, SEISMIC_STOMP, GRAB_THROW, PALM_CANNON);

   private SentinelMoves() {
   }

   static Vec3 muzzle(SentinelEntity self, LivingEntity target, double heightFrac, double forward) {
      Vec3 from = self.position().add(0.0, (double)self.getBbHeight() * heightFrac, 0.0);
      Vec3 dir = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0).subtract(from);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = self.getLookAngle();
      }

      dir = dir.normalize();
      return from.add(dir.x * forward, dir.y * forward, dir.z * forward);
   }

   static void fireBolts(
      SentinelEntity self,
      ServerLevel level,
      LivingEntity target,
      ItemStack item,
      int count,
      float impact,
      float aoe,
      float radius,
      float knockback,
      float speed,
      double heightFrac,
      double spreadDeg,
      ParticleOptions flash,
      int igniteSeconds,
      int slowSeconds,
      double flameAura
   ) {
      Vec3 from = muzzle(self, target, heightFrac, 1.4);
      Vec3 base = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0).subtract(from);
      double first = (double)(-(count - 1)) / 2.0;

      for (int i = 0; i < count; i++) {
         KineticProjectile bolt = new KineticProjectile(AwakenedEntities.KINETIC_CHARGE.get(), self, level, item.copy());
         bolt.setPos(from.x, from.y, from.z);
         bolt.configure(impact, aoe, radius, knockback);
         if (igniteSeconds > 0) {
            bolt.setIgnite(igniteSeconds);
         }

         if (slowSeconds > 0) {
            bolt.setSlow(slowSeconds);
         }

         if (flameAura > 0.0) {
            bolt.setFlameAura(flameAura);
            bolt.setContactRadius(flameAura);
         }

         Vec3 aim = base.yRot((float)Math.toRadians((first + (double)i) * spreadDeg));
         bolt.shoot(aim.x, aim.y, aim.z, speed, 1.4F);
         Fx.burst(level, flash, bolt.position(), 8, 0.2, 0.04);
         level.addFreshEntity(bolt);
      }
   }

   static void coneBeam(SentinelEntity self, ServerLevel level, LivingEntity target, double range, double halfAngleDeg, float damage, ParticleOptions beam) {
      Vec3 eye = self.position().add(0.0, (double)self.getBbHeight() * 0.86, 0.0);
      Vec3 aim = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0).subtract(eye).normalize();

      for (double d = 1.0; d <= range; d += 0.8) {
         Vec3 p = eye.add(aim.scale(d));
         level.sendParticles(beam, p.x, p.y, p.z, 1, 0.06, 0.06, 0.06, 0.0);
      }

      double cos = Math.cos(Math.toRadians(halfAngleDeg));
      AABB area = new AABB(eye, eye).inflate(range);

      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e != self && SentinelEntity.isHostileTarget(e))) {
         Vec3 to = victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0).subtract(eye);
         if (!(to.lengthSqr() > range * range) && !(to.lengthSqr() < 1.0E-4) && to.normalize().dot(aim) >= cos) {
            Dmg.hurt(victim, level, self.damageSources().mobAttack(self), damage);
         }
      }
   }

   static void pull(SentinelEntity self, ServerLevel level, double radius, float damage, double strength, boolean slow, ParticleOptions fx) {
      Vec3 c = self.position().add(0.0, (double)self.getBbHeight() * 0.5, 0.0);

      for (LivingEntity victim : level.getEntitiesOfClass(
         LivingEntity.class, new AABB(c, c).inflate(radius), e -> e.isAlive() && SentinelEntity.isHostileTarget(e)
      )) {
         Vec3 to = c.subtract(victim.position());
         if (!(to.lengthSqr() > radius * radius)) {
            if (to.lengthSqr() > 1.0E-4) {
               to = to.normalize();
               victim.setDeltaMovement(to.x * strength, Math.max(0.1, to.y * strength), to.z * strength);
               victim.hurtMarked = true;
            }

            Dmg.hurt(victim, level, self.damageSources().mobAttack(self), damage);
            if (slow) {
               Harm.effect(self, victim, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, false));
            }

            Fx.burst(level, fx, victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0), 8, 0.3, 0.02);
         }
      }

      Fx.sound(level, self.position(), SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.HOSTILE, 1.4F, 0.5F);
   }

   static void radialBurst(
      SentinelEntity self, ServerLevel level, double radius, float damage, double horiz, double vert, ParticleOptions fx, int slowSeconds, int igniteSeconds
   ) {
      ThreatMoves.shockwave(level, self, radius, damage, horiz, vert);
      Vec3 c = self.position().add(0.0, 1.0, 0.0);
      Fx.sphereShell(level, fx, c, Math.min(radius, 4.0), 60);
      if (slowSeconds > 0 || igniteSeconds > 0) {
         for (LivingEntity victim : level.getEntitiesOfClass(
            LivingEntity.class, self.getBoundingBox().inflate(radius), e -> e.isAlive() && SentinelEntity.isHostileTarget(e)
         )) {
            if (slowSeconds > 0) {
               Harm.effect(self, victim, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowSeconds * 20, 1, false, false));
            }

            if (igniteSeconds > 0) {
               victim.igniteForSeconds((float)igniteSeconds);
            }
         }
      }
   }

   static void frontSlam(SentinelEntity self, ServerLevel level, float damage, ParticleOptions fx) {
      Vec3 fwd = self.getLookAngle();
      Vec3 hit = self.position().add(fwd.x * 3.0, (double)self.getBbHeight() * 0.4, fwd.z * 3.0);
      Fx.burst(level, fx, hit, 16, 1.0, 0.05);

      for (LivingEntity victim : level.getEntitiesOfClass(
         LivingEntity.class, new AABB(hit, hit).inflate(3.5), e -> e.isAlive() && SentinelEntity.isHostileTarget(e)
      )) {
         Dmg.hurt(victim, level, self.damageSources().mobAttack(self), damage);
         Vec3 push = victim.position().subtract(self.position()).normalize();
         victim.setDeltaMovement(push.x * 0.6, 0.3, push.z * 0.6);
         victim.hurtMarked = true;
      }

      Fx.sound(level, self.position(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.5F);
   }
}
