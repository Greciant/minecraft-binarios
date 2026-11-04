package com.awakenedmod.awakened.core.npc;

import com.awakenedmod.awakened.core.Fx;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public final class ProjectileDodge {
   private static final double SCAN_RADIUS = 40.0;
   private static final int MIN_TICKS_TO_IMPACT = 2;
   private static final int MAX_TICKS_TO_IMPACT = 20;
   private static final double MIN_ALONG = 2.0;
   private static final double MISS_BY = 1.5;
   private static final int STEP_TICKS = 6;
   private static final double STEP_SPEED = 0.3;
   private static final double STEP_CHECK = 2.4;
   private static final int COOLDOWN = 60;
   private static final int SCAN_INTERVAL = 2;
   private final Mob mob;
   private final Predicate<Entity> hostileOwner;
   private Vec3 stepDir = Vec3.ZERO;
   private int stepTicks;
   private int startAt = Integer.MIN_VALUE;
   private int readyAt;
   private int lastRolledShot = -1;

   public ProjectileDodge(Mob mob, Predicate<Entity> hostileOwner) {
      this.mob = mob;
      this.hostileOwner = hostileOwner;
   }

   public boolean busy() {
      return this.stepTicks > 0;
   }

   public boolean stepping() {
      return this.busy() && this.mob.tickCount >= this.startAt && this.stepTicks < 6;
   }

   public void cancel() {
      this.stepTicks = 0;
   }

   public void scan(ServerLevel level, float chance, int reactionTicks) {
      if (!this.busy() && this.mob.tickCount >= this.readyAt && this.mob.tickCount % 2 == 0) {
         Vec3 center = this.mob.getBoundingBox().getCenter();

         for (Projectile proj : level.getEntitiesOfClass(Projectile.class, this.mob.getBoundingBox().inflate(40.0))) {
            Vec3 vel = proj.getDeltaMovement();
            double speed = vel.length();
            if (!(speed < 0.5) && proj.getOwner() != null && this.hostileOwner.test(proj.getOwner())) {
               Vec3 toSelf = center.subtract(proj.position());
               Vec3 dir = vel.scale(1.0 / speed);
               double along = toSelf.dot(dir);
               if (!(along < 2.0) && !(along / speed > 20.0)) {
                  Vec3 closest = proj.position().add(dir.scale(along));
                  if (!(closest.distanceToSqr(center) > 2.25)) {
                     if (proj.getId() == this.lastRolledShot) {
                        return;
                     }

                     this.lastRolledShot = proj.getId();
                     int delay = Math.max(0, reactionTicks / 3);
                     if (!(along / speed - (double)delay < 2.0) && !(this.mob.getRandom().nextFloat() >= chance)) {
                        Vec3 perp = new Vec3(-dir.z, 0.0, dir.x);
                        perp = perp.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : perp.normalize();
                        if (perp.dot(center.subtract(closest)) < 0.0) {
                           perp = perp.scale(-1.0);
                        }

                        if (!this.safeStep(level, perp)) {
                           return;
                        }

                        this.stepDir = perp;
                        this.stepTicks = 6;
                        this.startAt = this.mob.tickCount + delay;
                        this.readyAt = this.mob.tickCount + 60;
                        return;
                     }

                     return;
                  }
               }
            }
         }
      }
   }

   private boolean safeStep(ServerLevel level, Vec3 dir) {
      Vec3 from = this.mob.position().add(0.0, 0.5, 0.0);
      Vec3 to = from.add(dir.scale(2.4));
      return NpcNav.clearLine(level, this.mob, from, to) && NpcNav.safeBelow(level, to, 3);
   }

   public boolean tick(ServerLevel level, LivingEntity target) {
      if (this.busy() && this.mob.tickCount >= this.startAt) {
         boolean first = this.stepTicks == 6;
         if (first) {
            this.mob.getNavigation().stop();
            Fx.sound(level, this.mob.position(), (SoundEvent)SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.HOSTILE, 0.8F, 1.4F);
            Fx.burst(level, ParticleTypes.CLOUD, this.mob.position().add(0.0, 0.2, 0.0), 4, 0.3, 0.02);
         }

         if (target != null) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
         }

         this.mob.setDeltaMovement(this.stepDir.x * 0.3, this.mob.getDeltaMovement().y, this.stepDir.z * 0.3);
         if (first) {
            this.mob.hurtMarked = true;
         }

         this.stepTicks--;
         return true;
      } else {
         return false;
      }
   }
}
