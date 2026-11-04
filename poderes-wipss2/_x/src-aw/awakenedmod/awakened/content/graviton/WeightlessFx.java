package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.content.vfx.BlockFxEntity;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.BlockCracks;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.LoopSound;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

final class WeightlessFx {
   private static final DustParticleOptions STREAK = Fx.dust(1971474, 1.8F);
   private static final DustParticleOptions DRIFT = Fx.dust(16769732, 0.7F);
   private static final Lab.Value CHIPS = Lab.SERVER.def("graviton.weightless.chips", 14.0, 0.0, 32.0, 1.0);
   private static final Lab.Value CHIP_MIN = Lab.SERVER.def("graviton.weightless.chip.min", 0.22, 0.05, 1.0, 0.01);
   private static final Lab.Value CHIP_MAX = Lab.SERVER.def("graviton.weightless.chip.max", 0.5, 0.05, 1.0, 0.01);
   private static final Lab.Value CHIP_RISE_LOW = Lab.SERVER.def("graviton.weightless.chip.rise_low_share", 0.24, 0.0, 1.5, 0.01);
   private static final Lab.Value CHIP_RISE_HIGH = Lab.SERVER.def("graviton.weightless.chip.rise_high_share", 1.12, 0.0, 1.5, 0.01);
   private static final Lab.Value CHIP_RISE_TICKS = Lab.SERVER.def("graviton.weightless.chip.rise_ticks", 20.0, 1.0, 60.0, 1.0);
   private static final Lab.Value EDGE_CHIPS = Lab.SERVER.def("graviton.weightless.edge.chips", 12.0, 0.0, 32.0, 1.0);
   private static final Lab.Value EDGE_SIZE = Lab.SERVER.def("graviton.weightless.edge.size", 0.26, 0.05, 1.0, 0.01);
   private static final Lab.Value EDGE_ORBIT = Lab.SERVER.def("graviton.weightless.edge.orbit", 0.02, 0.0, 0.2, 0.005);
   private static final Lab.Value DUST_PILLARS = Lab.SERVER.def("graviton.weightless.cast.dust", 24.0, 0.0, 64.0, 1.0);
   private static final Lab.Value DROP_VEL = Lab.SERVER.def("graviton.weightless.chip.drop_vel", 0.5, 0.0, 2.0, 0.05);
   private static final Lab.Value HUM_VOLUME = Lab.SERVER.def("graviton.weightless.hum.volume", 0.7, 0.0, 1.0, 0.05);
   private static final Lab.Value HUM_PITCH = Lab.SERVER.def("graviton.weightless.hum.pitch", 1.0, 0.5, 2.0, 0.05);
   private static final float HUM_SAG = 0.3F;
   private static final int CHIP_AFTER_DROP = 40;
   private static final String DROP_CRACKS = "weightless_drop";

   private WeightlessFx() {
   }

   static void cast(ServerLevel level, Vec3 center, double radius, double bodyRise, int dropAt, boolean crowded) {
      int pillars = DUST_PILLARS.getI();

      for (int i = 0; i < pillars; i++) {
         double a = level.random.nextDouble() * Math.PI * 2.0;
         double r = radius * Math.sqrt(level.random.nextDouble());
         double x = center.x + Math.cos(a) * r;
         double z = center.z + Math.sin(a) * r;
         Fx.Floor floor = floorNear(level, x, center.y, z);
         if (floor != null) {
            level.sendParticles(
               new BlockParticleOption(ParticleTypes.DUST_PILLAR, floor.state()),
               x,
               floor.topY() + 0.05,
               z,
               0,
               0.0,
               1.0,
               0.0,
               0.25 + level.random.nextDouble() * 0.15
            );
         }
      }

      Fx.Floor centreFloor = floorNear(level, center.x, center.y, center.z);
      Vfx.ring(level, center.add(0.0, 0.05, 0.0), radius, 16769732, 12);
      chips(level, center, radius, bodyRise, dropAt, crowded);
      Fx.sound(level, center, (SoundEvent)AwakenedSounds.WEIGHTLESS_CAST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
      if (centreFloor != null) {
         Fx.sound(level, center, centreFloor.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 0.5F, 1.4F);
      }
   }

   private static void chips(ServerLevel level, Vec3 center, double radius, double bodyRise, int dropAt, boolean crowded) {
      int inner = CHIPS.getI();
      int edge = EDGE_CHIPS.getI();
      if (crowded) {
         inner /= 2;
         edge /= 2;
      }

      double min = CHIP_MIN.get();
      double max = Math.max(min, CHIP_MAX.get());
      int riseTicks = CHIP_RISE_TICKS.getI();
      double riseHigh = CHIP_RISE_HIGH.get() * bodyRise;
      double riseLow = CHIP_RISE_LOW.get() * bodyRise;

      for (int i = 0; i < inner; i++) {
         double a = level.random.nextDouble() * Math.PI * 2.0;
         double r = radius * 0.9 * Math.sqrt(level.random.nextDouble());
         double x = center.x + Math.cos(a) * r;
         double z = center.z + Math.sin(a) * r;
         Fx.Floor floor = floorNear(level, x, center.y, z);
         if (floor != null) {
            double size = min + level.random.nextDouble() * (max - min);
            double big = max > min ? (size - min) / (max - min) : 0.0;
            double rise = Mth.lerp(big, riseHigh, riseLow) * (0.85 + 0.3 * level.random.nextDouble());
            float spin = (2.0F + level.random.nextFloat() * 4.0F) * (level.random.nextBoolean() ? 1.0F : -1.0F);
            spawnChip(level, x, z, floor, size, rise, riseTicks, spin, dropAt, 0.0);
         }
      }

      double edgeSize = EDGE_SIZE.get();
      double orbit = EDGE_ORBIT.get() * (level.random.nextBoolean() ? 1.0 : -1.0);

      for (int ix = 0; ix < edge; ix++) {
         double a = (Math.PI * 2) * (double)ix / (double)edge + level.random.nextDouble() * 0.2;
         double x = center.x + Math.cos(a) * radius;
         double z = center.z + Math.sin(a) * radius;
         Fx.Floor floor = floorNear(level, x, center.y, z);
         if (floor != null) {
            double rise = 0.3 + level.random.nextDouble() * 0.3;
            float spin = 3.0F * (level.random.nextBoolean() ? 1.0F : -1.0F);
            BlockFxEntity.spawnFloating(
               level,
               new Vec3(x, floor.topY() + edgeSize * 0.5, z),
               floor.state(),
               edgeSize,
               dropAt + 40,
               level.random.nextFloat() * 360.0F,
               rise,
               riseTicks / 2,
               0.05,
               spin,
               dropAt,
               floor.topY() + edgeSize * 0.5,
               -DROP_VEL.get(),
               center.x,
               center.z,
               orbit
            );
         }
      }
   }

   private static void spawnChip(
      ServerLevel level, double x, double z, Fx.Floor floor, double size, double rise, int riseTicks, float spin, int dropAt, double orbit
   ) {
      double rest = floor.topY() + size * 0.5;
      BlockFxEntity.spawnFloating(
         level,
         new Vec3(x, rest, z),
         floor.state(),
         size,
         dropAt + 40,
         level.random.nextFloat() * 360.0F,
         rise,
         riseTicks,
         0.08,
         spin,
         dropAt,
         rest,
         -DROP_VEL.get(),
         x,
         z,
         orbit
      );
   }

   static void fieldTick(ServerLevel level, Vec3 center, double radius, int age, int total, int tell, String loopKey) {
      double fade = age > total - tell ? (double)(total - age) / (double)tell : 1.0;
      if (age % 3 == 0) {
         double a = level.random.nextDouble() * Math.PI * 2.0;
         double r = radius * Math.sqrt(level.random.nextDouble());
         double x = center.x + Math.cos(a) * r;
         double z = center.z + Math.sin(a) * r;
         Fx.Floor floor = floorNear(level, x, center.y, z);
         if (floor != null) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR, floor.state()), x, floor.topY() + 0.05, z, 0, 0.0, 1.0, 0.0, 0.25);
         }
      }

      int warn = age - (total - tell);
      if (warn == 0) {
         Vfx.ring(level, center.add(0.0, 0.06, 0.0), radius, 16747069, tell);
         Fx.sound(level, center, (SoundEvent)AwakenedSounds.PULL.get(), SoundSource.PLAYERS, 0.9F, 0.7F);
      }

      LoopSound.keepAt(
         level,
         loopKey,
         center,
         (SoundEvent)AwakenedSounds.WEIGHTLESS_ZONE.get(),
         SoundSource.PLAYERS,
         (float)(HUM_VOLUME.get() * fade),
         HUM_PITCH.getF() * (1.0F - 0.3F * (float)(1.0 - fade))
      );
   }

   static void dropBeat(ServerLevel level, Vec3 center) {
      Fx.sound(level, center, (SoundEvent)AwakenedSounds.WEIGHTLESS_DROP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
   }

   static void lifted(ServerLevel level, LivingEntity e) {
      GravitonGround.floorKick(level, e.position(), 8);
      Fx.sound(level, e.position(), (SoundEvent)AwakenedSounds.MASS_LIGHTER.get(), SoundSource.PLAYERS, 0.45F, 1.3F);
   }

   static void dropRush(ServerLevel level, LivingEntity e) {
      Vec3 top = e.position().add(0.0, (double)e.getBbHeight(), 0.0);

      for (int i = 0; i < 6; i++) {
         Fx.burst(level, STREAK, top.add(0.0, (double)i * 0.35, 0.0), 2, 0.12, 0.0);
      }
   }

   static void dropImpact(ServerLevel level, LivingEntity e, double fell) {
      Vec3 feet = e.position();
      int stage = 3 + (int)Math.round(3.0 * Mth.clamp(fell / 12.0, 0.0, 1.0));
      BlockPos under = BlockCracks.floorTop(level, feet.x, feet.y - 0.5, feet.z);
      if (under != null) {
         BlockCracks.crack(level, BlockCracks.owner(e, "weightless_drop"), under, stage, BlockCracks.Timing.holdThenFade(20, 4 * (stage + 1)));
      }

      GravitonGround.floorKick(level, feet, 14);
      Fx.Floor floor = Fx.floorUnder(level, feet.x, feet.y - 0.05, feet.z, 3);
      if (floor != null) {
         Fx.sound(level, feet, floor.state().getSoundType().getFallSound(), SoundSource.PLAYERS, 0.9F, 0.7F);
      }

      for (ServerPlayer p : level.players()) {
         if (p.position().distanceToSqr(feet) <= 36.0) {
            Fx.shakeFor(p, feet, 6.0, 0.05F, 1, 3);
         }
      }
   }

   static void clearCracks(LivingEntity e) {
      BlockCracks.clear(BlockCracks.owner(e, "weightless_drop"));
   }

   static void projectileDrift(ServerLevel level, Vec3 pos) {
      Fx.burst(level, DRIFT, pos, 2, 0.1, 0.0);
   }

   static void stopLoop(ServerLevel level, String key) {
      LoopSound.stopAt(level, key);
   }

   private static Fx.Floor floorNear(ServerLevel level, double x, double y, double z) {
      Fx.Floor floor = Fx.floorUnder(level, x, y + 1.0, z, 4);
      return floor != null && !(Math.abs(floor.topY() - y) > 1.5) && floor.state().getFluidState().isEmpty() ? floor : null;
   }
}
