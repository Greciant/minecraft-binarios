package com.awakenedmod.awakened.content.limitless;

import com.awakenedmod.awakened.core.Fx;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public final class ShotHold {
   private static final double STILL = 1.0E-4;
   private static final Map<Projectile, ShotHold.Original> ORIGINAL = new WeakHashMap<>();
   private final Projectile shot;
   private final Vec3 heading;
   private boolean released;

   public ShotHold(Projectile shot, Vec3 heading) {
      this.shot = shot;
      this.heading = heading.lengthSqr() < 1.0E-8 ? new Vec3(0.0, -1.0, 0.0) : heading.normalize();
      ORIGINAL.computeIfAbsent(shot, ShotHold.Original::new).holders++;
      stopAccelerating(shot);
      shot.setNoGravity(true);
   }

   public Projectile shot() {
      return this.shot;
   }

   public Vec3 heading() {
      return this.heading;
   }

   public void hold(Vec3 at) {
      if (at != null) {
         this.shot.setPos(at.x, at.y, at.z);
      }

      this.shot.setDeltaMovement(this.heading.scale(1.0E-4));
      push(this.shot);
   }

   public void release(Vec3 velocity) {
      if (!this.released) {
         this.released = true;
         ShotHold.Original was = ORIGINAL.get(this.shot);
         if (was != null && --was.holders <= 0) {
            ORIGINAL.remove(this.shot);
            this.shot.setNoGravity(was.noGravity);
            if (this.shot instanceof AbstractHurtingProjectile hurting) {
               hurting.accelerationPower = was.acceleration;
            }
         }

         this.shot.setDeltaMovement(velocity);
         push(this.shot);
      }
   }

   public void drop(ServerLevel level) {
      if (drops(this.shot)) {
         this.release(this.heading.scale(-0.01).add(0.0, -0.05, 0.0));
      } else {
         fizzle(level, this.shot);
      }
   }

   public static boolean drops(Entity shot) {
      return shot instanceof AbstractArrow || shot.getType() == EntityType.SNOWBALL || shot.getType() == EntityType.EGG;
   }

   public static void stopAccelerating(Projectile shot) {
      if (shot instanceof AbstractHurtingProjectile hurting) {
         hurting.accelerationPower = 0.0;
      }
   }

   public static void fizzle(ServerLevel level, Projectile shot) {
      Vec3 at = shot.position();
      Fx.burst(level, ParticleTypes.SMOKE, at, 6, 0.08, 0.01);
      Fx.sound(level, at, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.35F, 1.6F);
      ORIGINAL.remove(shot);
      shot.discard();
   }

   public static void forgetAll() {
      ORIGINAL.clear();
   }

   public static void push(Entity e) {
      e.hasImpulse = true;
      e.hurtMarked = true;
   }

   private static final class Original {
      final boolean noGravity;
      final double acceleration;
      int holders;

      Original(Projectile shot) {
         this.noGravity = shot.isNoGravity();
         this.acceleration = shot instanceof AbstractHurtingProjectile hurting ? hurting.accelerationPower : 0.0;
      }
   }
}
