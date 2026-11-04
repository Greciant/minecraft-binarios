package com.devilfruits.fruits.gura_gura.ability;

import com.devilfruits.core.combat.AbilityProjectiles;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.fruits.gura_gura.GuraFx;
import com.devilfruits.fruits.gura_gura.entity.QuakeSlashEntity;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public final class QuakeDeflect {
   public static int sweep(@Nullable Entity user, ImpactShape shape, @Nullable Vec3 push) {
      if (user != null && user.level() instanceof ServerLevel level) {
         int var12 = 0;

         for (Projectile p : level.getEntitiesOfClass(
            Projectile.class,
            shape.bounds(),
            px -> px.isAlive() && px.getOwner() != user && !(px instanceof QuakeSlashEntity) && shape.power(px.position()) > 0.0
         )) {
            Vec3 v = p.getDeltaMovement();
            Vec3 dir = away(shape, p.position(), push);
            double speed = Math.max(1.2, v.length() * 1.15);
            p.setDeltaMovement(dir.scale(speed));
            p.setOwner(user);
            p.hasImpulse = true;
            p.hurtMarked = true;
            if (p instanceof AbstractHurtingProjectile hurting) {
               hurting.accelerationPower = 0.1;
            }

            if (p instanceof AbstractArrow arrow) {
               arrow.setCritArrow(true);
            }

            ServerFx.send(p, GuraFx.AURA_DEFLECT, p.position(), v, 1.0F, 0L, 0);
            var12++;
         }

         Vec3 fallback = push != null && push.lengthSqr() > 1.0E-6 ? push.normalize() : new Vec3(0.0, 1.0, 0.0);
         return var12 + AbilityProjectiles.shatter(level, user, at -> shape.power(at) > 0.0, fallback);
      } else {
         return 0;
      }
   }

   public static int sweep(@Nullable Entity user, Vec3 centre, double radius) {
      return sweep(user, ImpactShape.sphere(centre, radius), null);
   }

   private static Vec3 away(ImpactShape shape, Vec3 at, @Nullable Vec3 push) {
      if (push != null && push.lengthSqr() > 1.0E-6) {
         return push.normalize().add(0.0, 0.08, 0.0).normalize();
      } else {
         Vec3 out = at.subtract(shape.center());
         return out.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : out.normalize().add(0.0, 0.25, 0.0).normalize();
      }
   }

   private QuakeDeflect() {
   }
}
