package com.awakenedmod.awakened.content.stormcaller;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class StormFx {
   static final int ARC = 9425151;

   private StormFx() {
   }

   static void sparks(ServerLevel l, Vec3 p, int count, double spread) {
      Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, p, count, spread, 0.06);
   }

   static void beam(ServerLevel l, Vec3 a, Vec3 b) {
      Vfx.effect(VfxEntity.Kind.ARC, a).scale(0.06).secondary(b.subtract(a)).color(9425151, 16777215).lifetime(4).intensity(0.7F).spawn(l);
      sparks(l, b, 5, 0.15);
   }

   static void strikeImpact(ServerLevel l, Vec3 pos, double radius) {
      Vfx.ring(l, pos.add(0.0, 0.1, 0.0), Math.max(radius, 1.4), 9425151, 10);
      sparks(l, pos.add(0.0, 0.2, 0.0), (int)(12.0 + radius * 4.0), Math.max(radius * 0.5, 0.4));
   }

   static void chargeOrb(ServerLevel l, Vec3 pos, float f) {
      Vfx.effect(VfxEntity.Kind.SPHERE, pos).scale(0.25 + (double)f * 0.55).color(9425151, 16777215).lifetime(4).intensity(0.8F).spawn(l);
      sparks(l, pos, 3, 0.3);
   }

   static void aimRing(ServerLevel l, Vec3 center, double radius) {
      Vfx.ring(l, center.add(0.0, 0.1, 0.0), radius, 9425151, 8);
   }

   static void stormDome(ServerLevel l, Vec3 center, double radius, double height) {
      Vfx.dome(l, center.add(0.0, height, 0.0), radius, 9425151, 30);
   }
}
