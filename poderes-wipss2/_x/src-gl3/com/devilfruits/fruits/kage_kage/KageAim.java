package com.devilfruits.fruits.kage_kage;

import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.util.Targeting;
import java.util.Comparator;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class KageAim {
   @Nullable
   public static LivingEntity aimed(ServerPlayer user, double range, double coneDegrees) {
      Vec3 eye = user.getEyePosition();
      Vec3 dir = AimHelper.look(user);
      double maxAngle = Math.toRadians(coneDegrees);
      AABB box = user.getBoundingBox().expandTowards(dir.scale(range)).inflate(2.0);
      LivingEntity best = null;
      double bestScore = Double.MAX_VALUE;

      for (Entity raw : user.level().getEntities(user, box, ex -> {
         if (ex instanceof LivingEntity l && l.isAlive() && !ex.isSpectator()) {
            return true;
         }

         return false;
      })) {
         Entity c = Targeting.resolve(raw);
         if (c instanceof LivingEntity) {
            LivingEntity e = (LivingEntity)c;
            if (e != user && Targeting.canHurt(user, e)) {
               Vec3 cx = e.getBoundingBox().getCenter();
               Vec3 to = cx.subtract(eye);
               double d = to.length();
               if (!(d > range + (double)e.getBbWidth() * 0.5)) {
                  double along = to.dot(dir);
                  double off = Math.sqrt(Math.max(0.0, d * d - along * along)) - (double)e.getBbWidth() * 0.5;
                  if (!(along <= 0.0) && (!(off > 0.0) || !(Math.atan2(off, along) > maxAngle)) && inSight(user, eye, cx)) {
                     double score = Math.max(0.0, off) / Math.max(1.0, along) + d * 0.002;
                     if (score < bestScore) {
                        bestScore = score;
                        best = e;
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   @Nullable
   public static LivingEntity nearest(ServerPlayer user, double radius) {
      Vec3 eye = user.getEyePosition();
      return user.level()
         .getEntitiesOfClass(
            LivingEntity.class,
            user.getBoundingBox().inflate(radius),
            e -> e != user
                  && e.isAlive()
                  && !e.isSpectator()
                  && Targeting.canHurt(user, e)
                  && (double)e.distanceTo(user) <= radius
                  && inSight(user, eye, e.getBoundingBox().getCenter())
         )
         .stream()
         .min(Comparator.comparingDouble(e -> e.distanceToSqr(user)))
         .orElse(null);
   }

   public static Vec3 aimedPoint(ServerPlayer user, double range) {
      Vec3 eye = user.getEyePosition();
      Vec3 end = eye.add(AimHelper.look(user).scale(range));
      HitResult hit = user.level().clip(new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, user));
      return hit.getType() == Type.MISS ? end : hit.getLocation();
   }

   @Nullable
   public static Vec3 groundSpot(ServerPlayer user, double range, double coneDegrees) {
      LivingEntity aimed = aimed(user, range, coneDegrees);
      Vec3 at = aimed != null ? aimed.position().add(0.0, 0.5, 0.0) : aimedPoint(user, range).add(0.0, 0.5, 0.0);
      HitResult floor = user.level().clip(new ClipContext(at, at.add(0.0, -8.5, 0.0), Block.COLLIDER, Fluid.ANY, user));
      return floor.getType() == Type.MISS ? null : floor.getLocation();
   }

   private static boolean inSight(ServerPlayer user, Vec3 eye, Vec3 at) {
      return user.level().clip(new ClipContext(eye, at, Block.COLLIDER, Fluid.NONE, user)).getType() == Type.MISS;
   }

   private KageAim() {
   }
}
