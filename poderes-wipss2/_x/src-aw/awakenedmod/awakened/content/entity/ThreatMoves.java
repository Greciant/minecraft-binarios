package com.awakenedmod.awakened.content.entity;

import com.awakenedmod.awakened.content.entity.boss.BossDefenses;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ThreatMoves {
   private ThreatMoves() {
   }

   public static boolean leapToward(Mob mob, LivingEntity target, double horiz, double up) {
      Vec3 flat = new Vec3(target.getX() - mob.getX(), 0.0, target.getZ() - mob.getZ());
      if (flat.lengthSqr() < 1.0E-4) {
         return false;
      } else {
         flat = flat.normalize();
         mob.setDeltaMovement(flat.x * horiz, up, flat.z * horiz);
         mob.hurtMarked = true;
         if (mob.level() instanceof ServerLevel level) {
            Fx.burst(level, ParticleTypes.EXPLOSION, mob.position().add(0.0, 0.2, 0.0), 6, 0.4, 0.05);
            Fx.sound(level, mob.position(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0F, 0.8F);
         }

         return true;
      }
   }

   public static void lungeForward(Mob mob, LivingEntity rangeRef, double factor, double maxBlocks) {
      double dist = Math.min(maxBlocks, (double)mob.distanceTo(rangeRef));
      float yaw = (float)Math.toRadians((double)(mob.getYRot() + 90.0F));
      Vec3 fwd = new Vec3(Math.cos((double)yaw), 0.0, Math.sin((double)yaw));
      Vec3 dm = mob.getDeltaMovement();
      mob.setDeltaMovement(fwd.x * factor * dist, dm.y, fwd.z * factor * dist);
      mob.hurtMarked = true;
   }

   public static Vec3 predictPos(LivingEntity target, Vec3 earlierPos, int elapsedTicks, float leadTicks) {
      if (elapsedTicks <= 0) {
         return target.position();
      } else {
         double vx = (target.getX() - earlierPos.x) / (double)elapsedTicks;
         double vz = (target.getZ() - earlierPos.z) / (double)elapsedTicks;
         return new Vec3(target.getX() + vx * (double)leadTicks, target.getY(), target.getZ() + vz * (double)leadTicks);
      }
   }

   public static int leapTo(Mob mob, Vec3 dest, double up, double maxHorizSpeed) {
      int flight = (int)Math.round(up / 0.045);
      Vec3 flat = new Vec3(dest.x - mob.getX(), 0.0, dest.z - mob.getZ());
      double dist = flat.length();
      double speed = Math.min(maxHorizSpeed, dist / (double)Math.max(1, flight) * 1.1);
      Vec3 dir = dist < 1.0E-4 ? Vec3.ZERO : flat.normalize();
      mob.setDeltaMovement(dir.x * speed, up, dir.z * speed);
      mob.hurtMarked = true;
      if (mob.level() instanceof ServerLevel level) {
         Fx.burst(level, ParticleTypes.EXPLOSION, mob.position().add(0.0, 0.2, 0.0), 8, 0.6, 0.05);
         Fx.sound(level, mob.position(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2F, 0.7F);
      }

      return flight;
   }

   public static void circleAround(Mob mob, LivingEntity target, double radius, double speed, boolean clockwise, int frame, double offset, double speedMult) {
      int dir = clockwise ? 1 : -1;
      double t = (double)(dir * frame) * 0.5 * speed / radius + offset;
      Vec3 movePos = target.position().add(radius * Math.cos(t), 0.0, radius * Math.sin(t));
      mob.getNavigation().moveTo(movePos.x, movePos.y, movePos.z, speed * speedMult);
   }

   public static void groundErupt(ServerLevel level, Mob source, Vec3 center, double radius, float damage, double up) {
      Fx.burst(level, ParticleTypes.EXPLOSION, center.add(0.0, 0.5, 0.0), 14, radius * 0.6, 0.1);
      Fx.burst(level, ParticleTypes.SONIC_BOOM, center.add(0.0, 0.5, 0.0), 1, 0.0, 0.0);
      Fx.sound(level, center, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.1F, 0.7F);
      Fx.shake(level, center, 16.0, 0.2F, 0, 10);
      Fx.groundImpact(level, center, radius, false);
      AABB area = new AABB(center, center).inflate(radius);

      for (Player player : level.getEntitiesOfClass(
         Player.class, area, p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && p.position().distanceToSqr(center) <= radius * radius
      )) {
         Dmg.hurt(player, level, source.damageSources().mobAttack(source), damage);
         player.push(0.0, up, 0.0);
         player.hurtMarked = true;
      }
   }

   public static void shockwave(ServerLevel level, Mob source, double radius, float damage, double horiz, double vert) {
      shockwave(level, source, radius, damage, 0.0F, 0, horiz, vert);
   }

   public static void shockwave(ServerLevel level, Mob source, double radius, float damage, float pctMaxHp, int shieldDisableTicks, double horiz, double vert) {
      Vec3 center = source.position();
      Fx.burst(level, ParticleTypes.EXPLOSION, center.add(0.0, 1.0, 0.0), 12, radius * 0.3, 0.1);
      Fx.burst(level, ParticleTypes.SONIC_BOOM, center.add(0.0, 1.2, 0.0), 1, 0.0, 0.0);
      Fx.sound(level, center, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.4F, 0.8F);
      Fx.shake(level, center, 20.0, 0.25F, 0, 12);
      Fx.groundImpact(level, center, Math.min(radius, 5.0), false);

      for (Player player : level.getEntitiesOfClass(
         Player.class, source.getBoundingBox().inflate(radius), p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
      )) {
         if (damage > 0.0F) {
            float dmg = damage + player.getMaxHealth() * pctMaxHp;
            Dmg.hurt(player, level, source.damageSources().mobAttack(source), dmg);
            if (shieldDisableTicks > 0) {
               BossDefenses.disableShield(level, player, shieldDisableTicks);
            }
         }

         Vec3 push = player.position().subtract(center);
         if (push.lengthSqr() < 1.0E-4) {
            push = new Vec3(0.0, 0.0, 1.0);
         }

         push = push.normalize();
         player.push(push.x * horiz, vert, push.z * horiz);
         player.hurtMarked = true;
      }
   }

   public static boolean blinkNear(Mob mob, LivingEntity target, double radius, ParticleOptions trail) {
      return blinkNear(mob, target, radius, trail, spot -> true);
   }

   public static boolean blinkNear(Mob mob, LivingEntity target, double radius, ParticleOptions trail, Predicate<Vec3> allowed) {
      if (mob.level() instanceof ServerLevel level) {
         Vec3 var18 = mob.position();

         for (int i = 0; i < 8; i++) {
            double a = (Math.PI / 4) * (double)i;
            double x = target.getX() + Math.cos(a) * radius;
            double z = target.getZ() + Math.sin(a) * radius;
            double y = target.getY();
            if (allowed.test(new Vec3(x, y, z))) {
               AABB box = mob.getBoundingBox().move(x - mob.getX(), y - mob.getY(), z - mob.getZ());
               if (level.noCollision(mob, box)) {
                  Fx.burst(level, trail, var18.add(0.0, (double)mob.getBbHeight() * 0.5, 0.0), 30, 0.5, 0.15);
                  Fx.sound(level, var18, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.3F, 0.6F);
                  mob.teleportTo(x, y, z);
                  mob.getLookControl().setLookAt(target);
                  Fx.burst(level, trail, mob.position().add(0.0, (double)mob.getBbHeight() * 0.5, 0.0), 30, 0.5, 0.15);
                  Fx.sound(level, mob.position(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.3F, 0.7F);
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
