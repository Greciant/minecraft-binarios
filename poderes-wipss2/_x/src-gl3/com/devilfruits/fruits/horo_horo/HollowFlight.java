package com.devilfruits.fruits.horo_horo;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class HollowFlight {
   public Vec3 pos;
   public Vec3 heading;
   public final double speed;
   public final double turn;

   public HollowFlight(Vec3 pos, Vec3 heading, double speed, double turn) {
      this.pos = pos;
      this.heading = heading.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : heading.normalize();
      this.speed = speed;
      this.turn = turn;
   }

   public static Vec3 aimPoint(Entity target) {
      return target.position().add(0.0, (double)target.getBbHeight() * 0.55, 0.0);
   }

   public double step(Vec3 target) {
      Vec3 to = target.subtract(this.pos);
      double d = to.length();
      if (d > 1.0E-6) {
         Vec3 want = to.scale(1.0 / d);
         double t = this.turn + (1.0 - this.turn) * Math.max(0.0, Math.min(1.0, (4.0 - d) / 3.0));
         Vec3 h = this.heading.scale(1.0 - t).add(want.scale(t));
         this.heading = h.lengthSqr() < 1.0E-6 ? want : h.normalize();
      }

      double move = Math.min(this.speed, d);
      this.pos = this.pos.add(this.heading.scale(move));
      return target.distanceTo(this.pos);
   }
}
