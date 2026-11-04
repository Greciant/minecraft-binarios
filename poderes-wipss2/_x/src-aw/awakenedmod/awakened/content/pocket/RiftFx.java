package com.awakenedmod.awakened.content.pocket;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RiftFx {
   private static final int RIFT_VIOLET = 9064447;
   private static final double BLINK_RADIUS = 0.9;
   private static final int BLINK_TICKS = 16;
   private static final int SWALLOW_TICKS = 30;
   public static final int SWALLOW_HOLD_TICKS = 4;

   private RiftFx() {
   }

   public static VfxEntity gate(ServerLevel level, Vec3 center, double radius, int lifetime, float yawDeg) {
      return Vfx.portalTracked(level, center, radius, 9064447, lifetime, yawDeg);
   }

   public static void doorway(ServerLevel level, Vec3 base, Vec3 forward, double halfWidth, double height, boolean dense) {
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      int cols = dense ? 10 : 6;
      int rows = dense ? 14 : 8;
      Fx.sheet(level, ParticleTypes.PORTAL, base, right, halfWidth, height, cols, rows);
      int fr = dense ? 12 : 5;

      for (int i = 0; i <= fr; i++) {
         double h = height * (double)i / (double)fr;
         frame(level, base.add(right.scale(-halfWidth)).add(0.0, h, 0.0));
         frame(level, base.add(right.scale(halfWidth)).add(0.0, h, 0.0));
         double w = -halfWidth + 2.0 * halfWidth * (double)i / (double)fr;
         frame(level, base.add(right.scale(w)));
         frame(level, base.add(right.scale(w)).add(0.0, height, 0.0));
      }
   }

   private static void frame(ServerLevel level, Vec3 p) {
      Fx.burst(level, ParticleTypes.REVERSE_PORTAL, p, 1, 0.02, 0.0);
   }

   public static void blinkMaw(ServerLevel level, Vec3 feet, Vec3 travelDir) {
      float yawDeg = (float)Math.toDegrees(Math.atan2(travelDir.x, travelDir.z));
      Vec3 center = feet.add(0.0, 1.215, 0.0);
      Vfx.portal(level, center, 0.9, 9064447, 16, yawDeg);
   }

   public static VfxEntity flatMaw(ServerLevel level, Vec3 center, double radius, int lifetime) {
      return Vfx.portalFlatTracked(level, center, radius, 9064447, lifetime);
   }

   public static void flatMawFlash(ServerLevel level, Vec3 center, double radius, int lifetime) {
      Vfx.portalFlat(level, center, radius, 9064447, lifetime);
   }

   private static double mawRadius(double width, double height) {
      return Mth.clamp(Math.max(width * 0.75, height * 0.42), 0.5, 4.0);
   }

   public static Vec3 swallowOpen(ServerLevel level, Vec3 pos, double width, double height, float yawDeg) {
      double radius = mawRadius(width, height);
      Vec3 center = pos.add(0.0, height * 0.5, 0.0);
      Vfx.portal(level, center, radius, 9064447, 30, yawDeg);
      Fx.sound(level, pos, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.4F, 0.75F);
      return center;
   }

   public static void swallowVanish(ServerLevel level, Vec3 center, double width, double height) {
      double radius = mawRadius(width, height);
      Fx.burst(level, ParticleTypes.PORTAL, center, 90, radius * 0.55, 0.9);
      Fx.burst(level, ParticleTypes.REVERSE_PORTAL, center, 45, radius * 0.45, 0.4);
      Fx.sound(level, center, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4F, 0.9F);
      Fx.sound(level, center, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 0.7F);
   }
}
