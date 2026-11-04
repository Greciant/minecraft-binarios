package com.awakenedmod.awakened.content.ruin;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

record RuinStrip(Vec3 apex, Vec3 dir, double length, double halfNear, double halfFar, int travel) {
   double halfAt(double s) {
      return this.length <= 1.0E-6 ? this.halfNear : Mth.lerp(Mth.clamp(s / this.length, 0.0, 1.0), this.halfNear, this.halfFar);
   }

   double frontAt(int age) {
      double t = Mth.clamp((double)age / (double)Math.max(1, this.travel), 0.0, 1.0);
      return this.length * (1.0 - Math.pow(1.0 - t, 1.5));
   }

   int arrival(double s) {
      double f = Mth.clamp(s / Math.max(1.0E-6, this.length), 0.0, 1.0);
      double t = 1.0 - Math.pow(1.0 - f, 0.6666666666666666);
      return Mth.clamp((int)Math.ceil(t * (double)this.travel), 0, this.travel);
   }

   Vec3 at(double s, double u) {
      return new Vec3(this.apex.x + this.dir.x * s - this.dir.z * u, this.apex.y, this.apex.z + this.dir.z * s + this.dir.x * u);
   }

   Vec3 right() {
      return new Vec3(-this.dir.z, 0.0, this.dir.x);
   }
}
