package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Terrain;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class TsunamiFx {
   public static final int COLLAPSE_TICKS = 20;
   private static final float WALL_ANKLE = 0.3F;

   private TsunamiFx() {
   }

   public static float wallDist(float f) {
      return 2.2F + 0.2F * TsunamiAbility.height(f);
   }

   public static float wallHeight(float f) {
      float c = Mth.clamp(f, 0.0F, 1.0F);
      return Mth.lerp(c, 0.3F, TsunamiAbility.height(c) * TsunamiAbility.wallHeightFraction());
   }

   public static float pourSize(boolean wet) {
      return wet ? 1.15F : 1.0F;
   }

   static void pourTick(ServerPlayer player, Vec3 fwd, float f, int tick) {
      ServerLevel level = player.serverLevel();
      float size = pourSize(HydroVisual.of(player).wet());
      Vec3 wall = player.position().add(fwd.scale((double)wallDist(f)));
      Vec3 side = new Vec3(-fwd.z, 0.0, fwd.x);
      RandomSource r = level.random;
      double half = (double)TsunamiAbility.halfWidth(f) * 0.75 * (double)size;
      double top = (double)(wallHeight(f) * size);
      float spray = TsunamiAbility.spray();
      int lips = chance(r, spray * (0.3F + 2.7F * f * f));

      for (int i = 0; i < lips; i++) {
         Vec3 lip = wall.add(side.scale((r.nextDouble() * 2.0 - 1.0) * half)).add(0.0, top * 0.95, 0.0);
         Fx.burst(level, ParticleTypes.SPLASH, lip, 2 + (int)(4.0F * f), 0.3 + 0.3 * (double)f, 0.05 + 0.2 * (double)f);
         Fx.burst(level, ParticleTypes.FALLING_WATER, lip.add(fwd.scale(0.4)), 1 + (int)(3.0F * f), 0.4, 0.0);
      }

      int feet = chance(r, spray * (0.4F + 1.6F * f));

      for (int i = 0; i < feet; i++) {
         Vec3 foot = wall.add(side.scale((r.nextDouble() * 2.0 - 1.0) * (half + 1.5 * (double)f))).subtract(fwd.scale(0.6 + 1.5 * (double)f * r.nextDouble()));
         Fx.burst(level, ParticleTypes.SPLASH, foot.add(0.0, 0.2, 0.0), 3, 0.4, 0.06 + 0.06 * (double)f);
      }

      int every = Math.round(Mth.lerp(f, 20.0F, 8.0F));
      if (tick % every == 0) {
         HydroFx.sound(level, wall, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 0.4F + 0.8F * f, 0.5F);
      }

      if (tick % 4 == 0) {
         Fx.shake(level, wall, (double)(8.0F + 16.0F * f), 0.004F + 0.07F * f * f, 0, 6);
      }
   }

   private static int chance(RandomSource r, float n) {
      int whole = (int)n;
      return whole + (r.nextFloat() < n - (float)whole ? 1 : 0);
   }

   static void release(ServerLevel level, Vec3 origin, Vec3 fwd, float f, float grow, boolean wet) {
      float reach = TsunamiAbility.reach(f) * grow;
      Vfx.effect(VfxEntity.Kind.WATER_WAVE, origin)
         .orient(Vfx.yawAlong(fwd), 0.0F)
         .scale((double)(TsunamiAbility.halfWidth(f) * grow))
         .secondary(new Vec3((double)f, (double)grow, 0.0))
         .color(HydroPalette.bodyRgb(wet))
         .intensity(HydroPalette.bodyOpacity(wet))
         .lifetime(Mth.ceil(TsunamiAbility.travelTicks(reach)) + 20)
         .spawn(level);
      Vec3 wall = origin.add(fwd.scale((double)wallDist(f)));
      double floor = Terrain.floorNear(level, wall.x, wall.z, origin.y, 2, 3, true);
      Vec3 base = new Vec3(wall.x, Double.isNaN(floor) ? origin.y : floor, wall.z);
      Vec3 up = new Vec3(0.0, 1.0, 0.0);
      Vec3 side = new Vec3(-fwd.z, 0.0, fwd.x);
      HydroFx.splash(level, base, up, 0.9 + 0.7 * (double)f, wet);
      HydroFx.impactRing(level, base, up, 2.5 + 4.0 * (double)f, wet);
      double half = (double)TsunamiAbility.halfWidth(f) * 0.75 * (double)grow;
      double top = (double)(wallHeight(f) * grow);

      for (double w = -half; w <= half; w += 2.0) {
         Vec3 lip = base.add(side.scale(w)).add(0.0, top, 0.0);
         Fx.burst(level, ParticleTypes.SPLASH, lip, 6, 0.5, 0.15);
         Fx.burst(level, ParticleTypes.FALLING_WATER, lip.add(fwd.scale(0.8)), 3, 0.5, 0.0);
      }

      HydroFx.sound(level, base, SoundEvents.GENERIC_SPLASH, 2.0F, 0.5F);
      HydroFx.sound(level, base, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 2.0F, 0.6F);
      Fx.shake(level, origin, 20.0 + (double)reach * 0.5, 0.08F + 0.1F * f, 2, 10);
   }

   static void wake(ServerLevel level, Vec3 head, Vec3 fwd, Vec3 side, double halfWidth, double height, double floorY, int tick, boolean wet) {
      RandomSource r = level.random;
      int spray = 2 + (int)(halfWidth * 0.5);

      for (int i = 0; i < spray; i++) {
         double w = (r.nextDouble() * 2.0 - 1.0) * halfWidth * 0.85;
         Vec3 crest = head.add(side.scale(w)).add(fwd.scale(height * 0.15));
         Fx.burst(level, ParticleTypes.SPLASH, new Vec3(crest.x, floorY + height * 1.02, crest.z), 2, 0.3, 0.1);
      }

      int falls = 1 + (int)(halfWidth / 4.0);

      for (int i = 0; i < falls; i++) {
         double w = (r.nextDouble() * 2.0 - 1.0) * halfWidth * 0.8;
         Vec3 lip = head.add(side.scale(w)).add(fwd.scale(height * 0.4));
         Fx.burst(level, ParticleTypes.FALLING_WATER, new Vec3(lip.x, floorY + height * 0.7, lip.z), 3, 0.5, 0.0);
      }

      double w = (r.nextDouble() * 2.0 - 1.0) * halfWidth * 0.8;
      Vec3 toe = head.add(side.scale(w)).add(fwd.scale(Math.min(height * 0.6, 8.0)));
      Vec3 at = new Vec3(toe.x, floorY + 0.3, toe.z);
      Fx.burst(level, ParticleTypes.SPLASH, at, 5, 0.5, 0.1);
      if (tick % 18 == 1) {
         HydroFx.sound(level, head, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, (float)(1.0 + height * 0.05), 0.45F + 0.1F * r.nextFloat());
      }

      if (tick % 6 == 0) {
         Fx.shake(level, head, halfWidth + 10.0, 0.05F, 0, 6);
      }
   }

   static void crash(ServerLevel level, Vec3 head, Vec3 fwd, double halfWidth, double height, double floorY, boolean wet) {
      Vec3 at = new Vec3(head.x, floorY, head.z).add(fwd.scale(Math.min(height * 0.4, 5.0)));
      Vec3 up = new Vec3(0.0, 1.0, 0.0);
      HydroFx.splash(level, at, up, 1.6, wet);
      HydroFx.impactRing(level, at, up, halfWidth, wet);
      Vec3 side = new Vec3(-fwd.z, 0.0, fwd.x);

      for (double w = -halfWidth * 0.8; w <= halfWidth * 0.8; w += 3.0) {
         Fx.burst(level, ParticleTypes.SPLASH, at.add(side.scale(w)).add(0.0, 0.8, 0.0), 5, 0.8, 0.2);
      }

      HydroFx.sound(level, at, SoundEvents.GENERIC_SPLASH, 2.0F, 0.55F);
      HydroFx.sound(level, at, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.6F, 0.5F);
      Fx.shake(level, at, halfWidth + 14.0, 0.1F, 2, 10);
   }
}
