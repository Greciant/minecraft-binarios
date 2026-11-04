package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.LoopSound;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RuinFx {
   static final DustParticleOptions ASH_DUST = Fx.dust(10130831, 1.1F);
   static final DustParticleOptions CRACK_DUST = Fx.dust(2828326, 0.9F);
   private static final ParticleOptions GRAVEL_FALL = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.GRAVEL.defaultBlockState());
   static final ParticleOptions GRIT_FALL = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.TUFF.defaultBlockState());
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final String GRIP_LOOP = "ruin_grip";
   private static final String GRIP_RUMBLE = "ruin_grip_rumble";
   private static final Lab.Value GRIP_ROT_MIN = Lab.SERVER.def("ruin.grip.rot_min", 0.3, 0.0, 1.0, 0.1);
   private static final Lab.Value GRIP_ROT_MAX = Lab.SERVER.def("ruin.grip.rot_max", 1.0, 0.0, 1.0, 0.1);
   private static final Lab.Value HAND_ASH = Lab.SERVER.def("ruin.hand.ash", 1.0, 0.0, 3.0, 0.1);
   private static final Lab.Value GRIP_CRACK_REACH = Lab.SERVER.def("ruin.grip.crack_reach", 0.6, 0.2, 1.5, 0.05);
   private static final Lab.Value GRIP_CRACK_FEED = Lab.SERVER.def("ruin.grip.crack_feed", 3.0, 1.0, 6.0, 1.0);
   private static final Lab.Value GRIP_DUST = Lab.SERVER.def("ruin.grip.dust", 1.0, 0.0, 3.0, 0.1);
   private static final Lab.Value GRIP_GRIND_VOL = Lab.SERVER.def("ruin.grip.grind_vol", 0.5, 0.0, 1.5, 0.05);
   private static final Lab.Value GRIP_RUMBLE_VOL = Lab.SERVER.def("ruin.grip.rumble_vol", 0.35, 0.0, 1.5, 0.05);
   private static final Lab.Value GRIP_RUMBLE_PITCH = Lab.SERVER.def("ruin.grip.rumble_pitch", 1.5, 0.5, 2.0, 0.05);
   private static final int GRIP_CRACK_LIFE_PER_FEED = 2;
   private static final double GRIP_CRACK_STANDOFF = 0.045;
   private static final float GRIP_GROWTH_RATE = 1.4F;
   private static final int GRIP_CRACKLE_BUCKET = 14;
   private static final int GRIP_CRACKLE_WINDOW = 7;
   private static final float SLAM_ROT = 1.0F;
   private static final int SLAM_ROT_HOLD = 10;
   private static final int SLAM_ROT_FADE = 24;
   private static final double ARM_SIDE = 0.34;
   private static final double ARM_DROP = 0.3;
   private static final double ARM_REACH = 0.45;

   private RuinFx() {
   }

   public static void preloadLabKeys() {
      RuinRushFx.preloadLabKeys();
   }

   public static void gripReach(ServerPlayer caster) {
      RuinArm.set(caster, GRIP_ROT_MIN.getF());
   }

   public static void gripClose(ServerLevel level, Vec3 gripPoint) {
      Fx.burst(level, CRACK_DUST, gripPoint, 8, 0.15, 0.03);
      Fx.burst(level, ParticleTypes.WHITE_ASH, gripPoint, 12, 0.25, 0.02);
      Fx.sound(level, gripPoint, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 0.8F, 0.55F);
      Fx.sound(level, gripPoint, SoundEvents.DEEPSLATE_HIT, SoundSource.PLAYERS, 0.6F, 0.5F);
   }

   public static void gripTick(ServerLevel level, ServerPlayer caster, LivingEntity target, Vec3 gripPoint, int holdTicks, int maxTicks, boolean closed) {
      float progress = Math.min(1.0F, (float)holdTicks / (float)maxTicks);
      RuinArm.set(caster, Mth.lerp(progress, GRIP_ROT_MIN.getF(), GRIP_ROT_MAX.getF()));
      if (holdTicks % 3 == 0) {
         handAsh(level, AimUtil.shoulderHand(caster, false, 0.34, 0.3, 0.45), 1.0F);
      }

      if (closed) {
         bodyCrack(level, target, gripPoint, Math.min(1.0F, progress * 1.4F), holdTicks);
         int dust = Math.round(GRIP_DUST.getF());
         if (dust > 0) {
            if (holdTicks % 2 == 0) {
               level.sendParticles(GRAVEL_FALL, gripPoint.x, gripPoint.y - 0.08, gripPoint.z, dust, 0.1, 0.03, 0.1, 0.0);
               level.sendParticles(ParticleTypes.WHITE_ASH, gripPoint.x, gripPoint.y, gripPoint.z, dust, 0.14, 0.14, 0.14, 0.005);
            }

            if (holdTicks % 3 == 0) {
               level.sendParticles(ParticleTypes.ASH, gripPoint.x, gripPoint.y + 0.1, gripPoint.z, dust, 0.15, 0.2, 0.15, 0.01);
            }
         }

         float swell = 0.6F + 0.4F * progress;
         LoopSound.keep(target, "ruin_grip", SoundEvents.BRUSH_GRAVEL, SoundSource.PLAYERS, GRIP_GRIND_VOL.getF() * swell, 0.55F);
         LoopSound.keep(
            target,
            "ruin_grip_rumble",
            (SoundEvent)AwakenedSounds.RUIN_RUMBLE.get(),
            SoundSource.PLAYERS,
            GRIP_RUMBLE_VOL.getF() * swell,
            GRIP_RUMBLE_PITCH.getF()
         );
         if (beat(holdTicks, target.getId(), 14, 7)) {
            Fx.burst(level, CRACK_DUST, gripPoint, 4, 0.12, 0.05);
            Fx.sound(level, gripPoint, SoundEvents.GRAVEL_HIT, SoundSource.PLAYERS, 0.45F, 0.5F + 0.15F * level.random.nextFloat());
         }
      }
   }

   public static void gripStack(ServerLevel level, LivingEntity target, Vec3 gripPoint, int stacksNow) {
      int grit = Mth.clamp(4 + stacksNow / 2, 4, 14);
      Fx.burst(level, CRACK_DUST, gripPoint, grit, 0.22, 0.04);
      Fx.burst(level, ParticleTypes.WHITE_ASH, gripPoint, 3, 0.1, 0.02);
      Fx.sound(level, gripPoint, SoundEvents.TUFF_BREAK, SoundSource.PLAYERS, 0.55F, 0.5F + 0.02F * (float)Math.min(stacksNow, 10));
   }

   public static void gripEnd(ServerLevel level, ServerPlayer caster, LivingEntity target, Vec3 gripPoint, boolean closed) {
      gripSilence(caster, target);
      if (closed) {
         Fx.burst(level, ParticleTypes.ASH, gripPoint, 8, 0.3, 0.02);
         Fx.burst(level, ASH_DUST, gripPoint, 5, 0.2, 0.03);
         Fx.sound(level, gripPoint, SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.5F, 0.6F);
      }
   }

   public static void gripSilence(ServerPlayer caster, LivingEntity target) {
      RuinArm.off(caster);
      LoopSound.stop(target, "ruin_grip");
      LoopSound.stop(target, "ruin_grip_rumble");
   }

   static void bodyCrack(ServerLevel level, LivingEntity target, Vec3 point, float growth, int tick) {
      int feed = Math.max(1, GRIP_CRACK_FEED.getI());
      if (tick % feed == 1 % feed) {
         Vec3 normal = surfaceNormal(target.getBoundingBox(), point);
         Vfx.effect(VfxEntity.Kind.RUIN_GRIP_CRACK, point.add(normal.scale(0.045)))
            .scale(GRIP_CRACK_REACH.get())
            .secondary(new Vec3((double)target.getId(), (double)Mth.clamp(growth, 0.0F, 1.0F), 0.0))
            .color(2828326, 10130831)
            .lifetime(feed * 2)
            .along(normal)
            .spawn(level);
      }
   }

   static void slamHand(ServerPlayer caster) {
      RuinArm.pulse(caster, 1.0F, 10, 24);
   }

   static void handAsh(ServerLevel level, Vec3 at, float scale) {
      int n = Math.round(HAND_ASH.getF() * scale);
      if (n > 0) {
         level.sendParticles(GRIT_FALL, at.x, at.y - 0.05, at.z, n, 0.08, 0.05, 0.08, 0.0);
         level.sendParticles(ParticleTypes.ASH, at.x, at.y, at.z, n * 2, 0.12, 0.1, 0.12, 0.01);
         level.sendParticles(CRACK_DUST, at.x, at.y, at.z, n, 0.1, 0.08, 0.1, 0.0);
      }
   }

   static boolean beat(int tick, int salt, int bucket, int window) {
      int b = tick / bucket;
      int h = b * -1640531535 + salt * -2048144777;
      h ^= h >>> 15;
      h *= 739982445;
      h ^= h >>> 12;
      return tick % bucket == Math.floorMod(h, window);
   }

   private static Vec3 surfaceNormal(AABB box, Vec3 point) {
      double best = Math.abs(point.y - box.maxY);
      Vec3 normal = UP;
      double d;
      if ((d = Math.abs(point.y - box.minY)) < best) {
         best = d;
         normal = new Vec3(0.0, -1.0, 0.0);
      }

      if ((d = Math.abs(point.x - box.minX)) < best) {
         best = d;
         normal = new Vec3(-1.0, 0.0, 0.0);
      }

      if ((d = Math.abs(point.x - box.maxX)) < best) {
         best = d;
         normal = new Vec3(1.0, 0.0, 0.0);
      }

      if ((d = Math.abs(point.z - box.minZ)) < best) {
         best = d;
         normal = new Vec3(0.0, 0.0, -1.0);
      }

      if (Math.abs(point.z - box.maxZ) < best) {
         normal = new Vec3(0.0, 0.0, 1.0);
      }

      return normal;
   }
}
