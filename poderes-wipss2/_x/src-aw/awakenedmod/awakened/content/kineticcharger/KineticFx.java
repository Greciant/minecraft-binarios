package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxBolt;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.content.vfx.VoxelSprites;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.LoopSound;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.glow.GlowState;
import com.awakenedmod.awakened.core.glow.Limb;
import com.awakenedmod.awakened.core.glow.LimbGlow;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class KineticFx {
   public static final int THEME_RGB = 13073919;
   private static final float OWNER_DIM = 0.35F;
   private static final int CHIME_SLOW = 6;
   private static final int CHIME_FAST = 3;
   private static final int GATHER_SLOW = 4;
   private static final int GATHER_FAST = 2;
   private static final int KNOT_EVERY = 3;
   private static final float KNOT_FROM = 0.35F;
   private static final int ARC_SLOW = 4;
   private static final int ARC_FAST = 2;
   private static final int WAKE_EVERY = 2;
   private static final int WAKE_ARC_EVERY = 6;
   private static final double SHOULDER_HALF_WIDTH = 0.31;
   private static final double SHOULDER_DROP = 0.25;
   private static final double REACH_PALM = 0.7;
   private static final double REACH_FOREARM = 0.42;
   private static final double REACH_ELBOW = 0.28;
   private static final double ARM_PUNCH = 0.0;
   private static final double ARM_REST = 75.0;
   private static final double ARM_HANG = 90.0;
   private static final double THROW_HELD_AT = 0.25;
   private static final double THROW_CHARGED_AT = 1.2;
   private static final double THROW_TORSO_COCKED = 30.0;
   private static final double[][] THROW_ARM_PATH = new double[][]{
      {0.0, 0.0, 0.0, 0.0}, {0.25, -70.0, -25.0, 8.0}, {1.2, -70.0, -25.0, 8.0}, {2.7, -135.0, 12.0, -26.0}, {4.2, -200.0, -10.0, -20.0}
   };
   private static final double CHARGE_LEVEL_AT = 0.3;
   private static final double CHARGE_DRAWN_AT = 2.3;
   private static final double CHARGE_DRAW_BACK = 0.4375;
   private static final int FRONT_LIFE = 8;
   private static final int STEP_FRONT_LIFE = 6;
   private static final int ARC_LIFE = 4;
   private static final int KNUCKLE_ARC_LIFE = 3;
   private static final int WAKE_ARC_LIFE = 3;
   private static final double GATHER_REACH_LOOSE = 0.55;
   private static final double GATHER_REACH_TIGHT = 0.28;
   private static final double GATHER_SCALE_HAND = 0.32;
   private static final int GATHER_LIFE_HAND = 6;
   private static final double KNOT_SMALL = 0.16;
   private static final double KNOT_FULL = 0.3;
   private static final double FOOTSTEP_BLOCKS = 1.25;
   private static final double IMPACT_BURST_MAX = 3.9;
   private static final double IMPACT_FRONT_GROUND_REACH = 3.0;
   private static final int RELEASE_SPECKS_FROM = 1;
   private static final int RELEASE_ARCS_FROM = 3;
   private static final int RELEASE_CUBES_FROM = 6;
   private static final int RELEASE_FOOTSTEPS_AT = 10;
   private static final double WAKE_WIDTH_TAP = 0.75;
   private static final double WAKE_WIDTH_POUR = 0.65;
   private static final double RECOIL_FLASH_RADIUS = 0.35;
   private static final double RECOIL_FLASH_RADIUS_FILL = 0.25;
   private static final double RECOIL_BURST_SHARE = 0.5;
   private static final double RECOIL_STREAM_WIDTH = 0.35;
   private static final Lab.Value RECOIL_RIPPLE = Lab.SERVER.def("kinetic.recoil.ripple", 0.8, 0.0, 2.0, 0.05);
   private static final Lab.Value RECOIL_SHAKE = Lab.SERVER.def("kinetic.recoil.shake", 0.05, 0.0, 1.0, 0.01);
   private static final float RECOIL_SHAKE_FILL = 0.08F;
   private static final double RECOIL_KICK_FRONT = 1.0;
   private static final double RECOIL_KICK_FRONT_FILL = 0.6;
   private static final double RECOIL_KICK_THICK = 0.55;
   private static final double RECOIL_KICK_THICK_FILL = 0.35;
   private static final double RECOIL_KICK_AHEAD = 0.45;
   private static final double RECOIL_FLOOR_FRONT = 0.8;
   private static final double RECOIL_GATHER_LOOSE = 0.4;
   private static final double RECOIL_GATHER_TIGHT = 0.22;
   private static final double RECOIL_ARC_JITTER = 0.08;
   private static final Lab.Value RINGS_BASE = Lab.SERVER.def("kinetic.ring.count_base", 1.0, 0.0, 8.0, 1.0);
   private static final Lab.Value RINGS_SPAN = Lab.SERVER.def("kinetic.ring.count_span", 3.0, 0.0, 8.0, 1.0);
   private static final Lab.Value RING_STAGGER = Lab.SERVER.def("kinetic.ring.stagger", 1.0, 0.0, 10.0, 1.0);
   private static final Lab.Value RING_AHEAD = Lab.SERVER.def("kinetic.ring.ahead", 2.0, 0.0, 12.0, 0.1);
   private static final Lab.Value RING_BAND_NARROW = Lab.SERVER.def("kinetic.ring.band_narrow", 0.2, 0.02, 2.0, 0.01);
   private static final Lab.Value RING_BAND_WIDE = Lab.SERVER.def("kinetic.ring.band_wide", 0.65, 0.02, 2.0, 0.01);
   private static final Lab.Value RING_DEPTH_NARROW = Lab.SERVER.def("kinetic.ring.depth_narrow", 0.35, 0.02, 3.0, 0.01);
   private static final Lab.Value RING_DEPTH_WIDE = Lab.SERVER.def("kinetic.ring.depth_wide", 0.9, 0.02, 3.0, 0.01);
   private static final Lab.Value RING_SOLID_FIRST = Lab.SERVER.def("kinetic.ring.solid_first", 1.0, 0.0, 2.0, 0.01);
   private static final Lab.Value RING_SOLID_STEP = Lab.SERVER.def("kinetic.ring.solid_step", 0.07, 0.0, 1.0, 0.01);
   private static final Lab.Value RING_SOLID_MIN = Lab.SERVER.def("kinetic.ring.solid_min", 0.7, 0.0, 2.0, 0.01);
   private static final Lab.Value RING_POP = Lab.SERVER.def("kinetic.ring.pop", 1.0, 1.0, 40.0, 1.0);
   private static final Lab.Value RING_HOLD = Lab.SERVER.def("kinetic.ring.hold", 6.0, 0.0, 40.0, 1.0);
   private static final Lab.Value RING_FADE = Lab.SERVER.def("kinetic.ring.fade", 6.0, 1.0, 40.0, 1.0);
   private static final Lab.Value RING_DUST_SCALE = Lab.SERVER.def("kinetic.ring.dust_scale", 1.0, 0.0, 4.0, 0.1);
   private static final Lab.Value RING_FAN_SCALE = Lab.SERVER.def("kinetic.ring.fan_scale", 1.0, 0.0, 4.0, 0.1);
   private static final double FLASH_RADIUS = 0.55;
   private static final double FLASH_RADIUS_FILL = 0.45;
   private static final int FLASH_LIFE = 3;
   private static final int DUST_LINES_BASE = 2;
   private static final int DUST_LINES_FILL = 2;
   private static final double DUST_STATION = 2.0;
   private static final double DUST_FIRST = 1.5;
   private static final int DUST_GRAINS = 2;
   private static final double DUST_FLOOR_REACH = 3.5;
   private static final double DUST_FLOOR_ABOVE = 1.5;
   private static final int DUST_FLOOR_SCAN = 7;
   private static final double DUST_SPEED = 0.35;
   private static final double DUST_RISE = 0.12;
   private static final int RING_GRAINS_BASE = 3;
   private static final double RING_GRAINS_PER_BLOCK = 4.0;
   private static final int RING_PUFFS_BASE = 2;
   private static final double RING_PUFFS_PER_BLOCK = 1.5;
   private static final double RING_DUST_SPREAD = 0.5;
   private static final double RING_PUFF_SPEED = 0.06;
   private static final Lab.Value STRIKE_BURST_VOLUME = Lab.SERVER.def("kinetic.strike.burst_volume", 0.6, 0.0, 2.0, 0.05);
   private static final float STRIKE_BURST_VOLUME_FILL = 0.4F;
   private static final float STRIKE_BURST_PITCH = 1.1F;
   private static final float STRIKE_BURST_PITCH_FILL = 0.2F;
   private static final Lab.Value STRIKE_WHOOSH_VOLUME = Lab.SERVER.def("kinetic.strike.whoosh_volume", 0.35, 0.0, 2.0, 0.05);
   private static final float STRIKE_WHOOSH_VOLUME_FILL = 0.25F;
   private static final float STRIKE_WHOOSH_PITCH = 0.95F;
   private static final Lab.Value STRIKE_SHAKE = Lab.SERVER.def("kinetic.strike.shake", 0.08, 0.0, 1.0, 0.01);
   private static final float STRIKE_SHAKE_FILL = 0.14F;
   private static final int STRIKE_SHAKE_TICKS = 2;
   private static final int STRIKE_SHAKE_FADE = 6;
   private static final double STRIKE_SHAKE_RADIUS = 8.0;
   private static final double HIT_BURST = 0.5;
   private static final double HIT_BURST_FILL = 0.45;
   private static final int HIT_BURST_LIFE = 4;
   private static final double HIT_WAKE_AHEAD = 0.6;
   private static final double HIT_WAKE_RADIUS = 0.7;
   private static final double HIT_WAKE_RADIUS_FILL = 0.4;
   private static final double HIT_WAKE_BAND = 0.12;
   private static final double HIT_WAKE_DEPTH = 0.25;
   private static final float HIT_WAKE_SOLID = 0.8F;
   private static final Lab.Value MIX_CHARGE = Lab.SERVER.def("kinetic.sound.charge", 1.0, 0.0, 2.0, 0.05);
   private static final Lab.Value MIX_RELEASE = Lab.SERVER.def("kinetic.sound.release", 1.0, 0.0, 2.0, 0.05);
   private static final Lab.Value MIX_RUPTURE = Lab.SERVER.def("kinetic.sound.rupture", 1.0, 0.0, 2.0, 0.05);
   private static final float REGISTER_ARM = 1.0F;
   private static final float REGISTER_LEG = 0.8F;
   private static final float THROW_MIX = 0.7F;
   private static final float TONE_VOLUME = 0.5F;
   private static final float TONE_PITCH = 0.6F;
   private static final float WINDUP_CHIME_VOLUME = 0.3F;
   private static final float WINDUP_CHIME_PITCH = 0.9F;
   private static final float WINDUP_CHIME_PITCH_FULL = 1.8F;
   private static final int BEAT_SLOW = 12;
   private static final int BEAT_FAST = 5;
   private static final float BEAT_VOLUME = 0.5F;
   private static final float BEAT_VOLUME_FILL = 0.3F;
   private static final float BEAT_PITCH = 0.9F;
   private static final float BEAT_PITCH_FILL = 0.5F;
   private static final float HUM_VOLUME = 0.22F;
   private static final float HUM_VOLUME_FILL = 0.18F;
   private static final float HUM_PITCH = 0.7F;
   private static final float HUM_PITCH_FILL = 0.7F;
   private static final float CRACKLE_FROM = 0.5F;
   private static final int CRACKLE_EVERY = 3;
   private static final float CRACKLE_CHANCE = 0.8F;
   private static final float CRACKLE_VOLUME = 0.16F;
   private static final float CRACKLE_PITCH = 1.5F;
   private static final float CRACKLE_PITCH_SPAN = 0.5F;
   private static final float STRAIN_VOLUME = 0.45F;
   private static final float STRAIN_PITCH = 0.5F;
   private static final float SLING_VOLUME = 0.55F;
   private static final float SLING_PITCH = 1.35F;
   private static final float CRACK_FROM = 0.05F;
   private static final float CRACK_VOLUME = 0.1F;
   private static final float CRACK_VOLUME_FILL = 0.5F;
   private static final float CRACK_PITCH = 1.3F;
   private static final float BOOM_VOLUME = 0.35F;
   private static final float BOOM_VOLUME_FILL = 0.4F;
   private static final float BOOM_PITCH = 0.85F;
   private static final float BOOM_PITCH_FILL = 0.15F;
   private static final float WEIGHT_VOLUME = 0.12F;
   private static final float WEIGHT_VOLUME_FILL = 0.2F;
   private static final float WEIGHT_PITCH = 0.5F;
   private static final float THROW_CRACK_FROM = 0.15F;
   private static final float THROW_CRACK_VOLUME_POWER = 0.35F;
   private static final float THROW_CRACK_PITCH = 1.5F;
   private static final double IMPACT_CRACK_RADIUS_FROM = 2.0;
   private static final double IMPACT_CRACK_RADIUS_SPAN = 6.0;
   private static final float IMPACT_CRACK_VOLUME = 0.4F;
   private static final float IMPACT_CRACK_PITCH = 1.4F;
   private static final float RECOIL_MIX = 0.6F;
   private static final float REGISTER_VENT = 1.15F;
   private static final float POP_VOLUME = 0.5F;
   private static final float POP_VOLUME_FILL = 0.25F;
   private static final float POP_PITCH = 0.7F;
   private static final float POP_PITCH_FILL = 0.1F;
   private static final float VENT_AIR_VOLUME = 0.35F;
   private static final float VENT_AIR_VOLUME_FILL = 0.3F;
   private static final float VENT_AIR_PITCH = 1.3F;
   private static final float VENT_AIR_PITCH_FILL = 0.25F;
   private static final float VENT_HIT_VOLUME = 0.45F;
   private static final float VENT_HIT_VOLUME_FILL = 0.45F;
   private static final float VENT_HIT_PITCH = 1.3F;
   private static final float VENT_HIT_PITCH_FILL = 0.3F;
   private static final float VENT_THUD_VOLUME = 0.8F;
   private static final float GLOW_FLOOR = 0.12F;
   private static final float GLOW_CHARGE_SPAN = 0.6F;
   private static final float GLOW_POUR_SPAN = 0.28F;
   private static final float GLOW_STOMP_BASE = 0.25F;
   private static final float GLOW_STOMP_PER = 0.04F;
   private static final float GLOW_STOMP_MAX = 0.75F;
   private static final double HIP_HEIGHT = 0.75;
   private static final double HIP_HALF_WIDTH = 0.12;
   private static final double REACH_SOLE = 0.72;
   private static final double REACH_SHIN = 0.45;
   private static final double LEG_CHARGE = -58.0;
   private static final Lab.Value RUPTURE_HEAVE = Lab.SERVER.def("kinetic.rupture.heave", 1.0, 0.0, 3.0, 0.05);
   private static final Lab.Value RUPTURE_COPIES = Lab.SERVER.def("kinetic.rupture.copies", 100.0, 0.0, 150.0, 1.0);
   private static final Lab.Value RUPTURE_GLOW = Lab.SERVER.def("kinetic.rupture.glow", 1.0, 0.0, 3.0, 0.05);
   private static final Lab.Value RUPTURE_OUTLINE = Lab.SERVER.def("kinetic.rupture.outline", 1.0, 0.0, 3.0, 0.05);
   private static final float OUTLINE_PULSE = 0.25F;
   private static final float OUTLINE_PULSE_RATE = 0.35F;
   private static final Lab.Value RUPTURE_DUST_SCALE = Lab.SERVER.def("kinetic.rupture.dust_scale", 1.0, 0.0, 4.0, 0.1);
   private static final Lab.Value RUPTURE_RUMBLE_VOLUME = Lab.SERVER.def("kinetic.rupture.rumble_volume", 0.7, 0.0, 2.0, 0.05);
   private static final Lab.Value RUPTURE_STOMP_VOLUME = Lab.SERVER.def("kinetic.rupture.stomp_volume", 0.9, 0.0, 2.0, 0.05);
   private static final double RUPTURE_FLASH_RADIUS = 0.6;
   private static final double RUPTURE_FLASH_RADIUS_FILL = 0.5;
   private static final int RUPTURE_FLASH_LIFE = 3;
   private static final double RUPTURE_STOMP_BURST = 0.6;
   private static final double RUPTURE_STOMP_BURST_FILL = 0.5;
   private static final double RUPTURE_STOMP_FRONT = 1.1;
   private static final double RUPTURE_STOMP_FRONT_FILL = 0.6;
   private static final float RUPTURE_INTENSITY = 0.8F;
   private static final float RUPTURE_INTENSITY_FILL = 0.3F;
   private static final double HEAVE_STEP = 1.0;
   private static final double HEAVE_PLATE_MIN = 0.86;
   private static final double HEAVE_PLATE_SPAN = 0.1;
   private static final double HEAVE_THICK = 0.5;
   private static final double HEAVE_SEAT = 0.01;
   private static final double HEAVE_RISE_MIN = 0.25;
   private static final double HEAVE_RISE_SPAN = 0.4;
   private static final double HEAVE_RISE_FILL = 0.3;
   private static final float HEAVE_TILT_MIN = 6.0F;
   private static final float HEAVE_TILT_SPAN = 14.0F;
   private static final int HEAVE_LIFE_MIN = 10;
   private static final int HEAVE_LIFE_SPAN = 5;
   private static final float HEAVE_YAW_JITTER = 25.0F;
   private static final int HEAVE_DELAY_SCATTER = 1;
   private static final double RUPTURE_DUST_STEP = 1.0;
   private static final int RUPTURE_GRAINS_BASE = 3;
   private static final double RUPTURE_GRAINS_PER_BLOCK = 2.0;
   private static final int RUPTURE_PUFFS_BASE = 1;
   private static final double RUPTURE_PUFFS_PER_BLOCK = 1.5;
   private static final double RUPTURE_DUST_UP = 0.32;
   private static final double RUPTURE_GRAIN_SPEED = 0.22;
   private static final double RUPTURE_DUST_SPREAD = 0.9;
   private static final float STOMP_ANVIL_VOLUME = 0.45F;
   private static final float STOMP_ANVIL_PITCH = 0.4F;
   private static final float STOMP_EXPLODE_VOLUME = 0.5F;
   private static final float STOMP_EXPLODE_VOLUME_FILL = 0.5F;
   private static final float STOMP_EXPLODE_PITCH = 0.6F;
   private static final float STOMP_STONE_PITCH = 0.6F;
   private static final float STOMP_CRACK_VOLUME = 0.2F;
   private static final float STOMP_CRACK_VOLUME_FILL = 0.25F;
   private static final float STOMP_CRACK_PITCH = 1.6F;
   private static final float RUMBLE_VOLUME_FILL = 0.4F;
   private static final float RUMBLE_PITCH = 0.5F;
   private static final float RUMBLE_PITCH_SPAN = 0.15F;
   private static final float RUMBLE_DEEP_SHARE = 0.6F;
   private static final float RUMBLE_DEEP_PITCH = 0.45F;
   private static final float RUMBLE_PULSE_SHARE = 0.3F;
   private static final float RUMBLE_PULSE_PITCH = 0.5F;
   private static final float RUPTURE_SHAKE = 0.12F;
   private static final float RUPTURE_SHAKE_FILL = 0.18F;
   private static final int RUPTURE_SHAKE_TICKS = 3;
   private static final int RUPTURE_SHAKE_FADE = 8;
   private static final float RUPTURE_WORLD_SHAKE = 0.05F;
   private static final float RUPTURE_WORLD_SHAKE_FILL = 0.08F;
   private static final double RUPTURE_WORLD_SHAKE_RADIUS = 12.0;
   private static final double RUPTURE_HIT_BURST = 0.6;
   private static final double RUPTURE_HIT_BURST_FILL = 0.5;
   private static final double RUPTURE_HIT_FRONT = 0.9;
   private static final double RUPTURE_HIT_FRONT_FILL = 0.5;
   private static final double RUPTURE_HIT_WAKE_SPEED = 0.55;
   private static final double RUPTURE_HIT_WAKE_SPEED_FILL = 0.3;
   private static final double RUPTURE_HIT_WAKE_WIDTH = 0.9;
   private static final double RUPTURE_HIT_WAKE_WIDTH_FILL = 0.4;
   private static final double RUPTURE_HIT_WAKE_HEIGHT = 0.35;
   private static final int RUPTURE_HIT_WAKE_INTERVAL = 2;
   private static final int POUR_GRAINS = 1;
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final Map<UUID, double[]> STRIDES = new ConcurrentHashMap<>();
   private static final Map<UUID, int[]> HOLD_SOUND = new ConcurrentHashMap<>();
   private static final int LAST_BEAT = 0;
   private static final int TONE_PLAYED = 1;
   private static final int NO_BEAT = -1000000;

   private KineticFx() {
   }

   public static void preloadLabKeys() {
   }

   public static void chargeStart(ServerPlayer player) {
      limb(player, arm(player), 0.12F);
      chargeTone(player, 1.0F, 0.7F);
      sound(player, MIX_CHARGE, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 0.7F);
   }

   public static void chargeTick(ServerPlayer player, int ticks, float charge, float pour) {
      ServerLevel level = player.serverLevel();
      float build = Math.min(1.0F, charge + pour);
      limb(player, arm(player), 0.12F + charge * 0.6F + pour * 0.28F);
      double seconds = (double)ticks / 20.0;
      Vec3 palm = throwHand(player, 0.7, seconds);
      VoxelSprites.Look own = own(player);
      if (ticks % cadence(4, 2, build) == 0) {
         double reach = Mth.lerp((double)build, 0.55, 0.28);
         KineticVoxel.gather(level, around(level.random, palm, reach), palm, 0.32, 6, own);
      }

      if (build >= 0.35F && ticks % 3 == 0) {
         KineticVoxel.knot(level, palm, Mth.lerp((double)build, 0.16, 0.3), own);
      }

      if (pour > 0.0F && ticks % cadence(4, 2, pour) == 0) {
         ownArc(player, palm, throwHand(player, 0.42, seconds), 4, 1.0);
      }

      chargeSound(player, ticks, charge, pour);
   }

   public static void throwRelease(ServerPlayer player, float power) {
      ServerLevel level = player.serverLevel();
      Limb arm = arm(player);
      LimbGlow.flash(player, arm);
      limb(player, arm, 0.0F);
      VoxelSprites.burst(level, fist(player), 0.45 + (double)power * 0.6, own(player), player.getLookAngle());
      releaseSound(player, power);
   }

   private static Vec3 throwHand(ServerPlayer player, double reach, double seconds) {
      double[][] path = THROW_ARM_PATH;
      double[] rot = path[path.length - 1];

      for (int i = 1; i < path.length; i++) {
         if (seconds <= path[i][0]) {
            double[] a = path[i - 1];
            double[] b = path[i];
            double t = Mth.clamp((seconds - a[0]) / (b[0] - a[0]), 0.0, 1.0);
            if (i == 1) {
               t = 1.0 - Math.pow(1.0 - t, 3.0);
            }

            rot = new double[]{0.0, Mth.lerp(t, a[1], b[1]), Mth.lerp(t, a[2], b[2]), Mth.lerp(t, a[3], b[3])};
            break;
         }
      }

      double last = path[path.length - 1][0];
      double cock = Mth.clamp((seconds - 1.2) / (last - 1.2), 0.0, 1.0);
      return armPoint(player, false, reach, cock * 30.0, rot[1], rot[2], rot[3]);
   }

   public static void wake(ServerLevel level, Vec3 pos, Vec3 velocity, int tick, float pour) {
      if (tick % 2 == 0) {
         KineticVoxel.wake(level, pos, velocity, 2, 0.75 + (double)pour * 0.65);
         if (tick % 6 == 0 && velocity.lengthSqr() > 0.01) {
            Vec3 back = velocity.scale(-1.4);
            Vec3 to = pos.add(back).add(jitter(level.random, 0.18 + (double)pour * 0.1));
            Vfx.pixelArc(level, pos, to, 3, 8072383, 13073919);
         }
      }
   }

   public static void impact(ServerLevel level, Vec3 pos, Vec3 normal, double radius) {
      KineticVoxel.burst(level, pos, Math.min(radius * 0.55, 3.9), normal);
      AimUtil.groundedPositionIfFound(level, pos)
         .filter(ground -> pos.y - ground.y <= 3.0)
         .ifPresent(ground -> Vfx.cubeFront(level, ground, radius * 1.1, 0.7 + radius * 0.25, 8, 8072383, 13073919));
      sound(level, pos, MIX_RELEASE, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9F, (float)Mth.clamp(1.4 - radius * 0.15, 0.7, 1.4));
      sound(
         level,
         pos,
         MIX_RELEASE,
         (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(),
         0.5F + (float)radius * 0.12F,
         (float)Mth.clamp(1.5 - radius * 0.15, 0.7, 1.4)
      );
      float big = (float)Mth.clamp((radius - 2.0) / 6.0, 0.0, 1.0);
      if (big > 0.0F) {
         sound(level, pos, MIX_RELEASE, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.4F * big, 1.4F);
      }
   }

   public static Vec3 palms(ServerPlayer player) {
      return palm(player, false).add(palm(player, true)).scale(0.5);
   }

   private static Vec3 palm(ServerPlayer player, boolean offHand) {
      return hand(player, offHand, 0.7, 0.0, 0.0);
   }

   public static void recoilWindup(ServerPlayer player, float progress) {
      ServerLevel level = player.serverLevel();
      float p = Mth.clamp(progress, 0.0F, 1.0F);
      armsGlow(player, 0.12F + p * 0.6F);
      Vec3 into = palm(player, player.tickCount % 2 == 0);
      KineticVoxel.gather(level, around(level.random, into, 0.4), into, 0.32, 6, own(player));
      palmArc(player);
      windupChime(player, p, 1.0F, 0.6F);
   }

   public static void recoilPour(ServerPlayer player, int ticks, float fill) {
      ServerLevel level = player.serverLevel();
      float f = Mth.clamp(fill, 0.0F, 1.0F);
      armsGlow(player, 0.72F + f * 0.28F);
      if (ticks % cadence(4, 2, f) == 0) {
         Vec3 into = palm(player, ticks % 2 == 0);
         double reach = Mth.lerp((double)f, 0.4, 0.22);
         KineticVoxel.gather(level, around(level.random, into, reach), into, 0.32, 6, own(player));
      }

      if (ticks % cadence(4, 2, f) == 0) {
         palmArc(player);
      }

      if (f >= 0.35F && ticks % 3 == 0) {
         KineticVoxel.knot(level, palms(player), Mth.lerp((double)f, 0.16, 0.3), own(player));
      }

      pourLayers(player, ticks, f, 1.0F, 0.6F);
   }

   public static void recoilFire(ServerPlayer player, Vec3 muzzle, Vec3 center, Vec3 dir, double radius, float fill, boolean caught) {
      recoilFire(player, muzzle, center, dir, radius, fill, caught, true);
   }

   public static void recoilFire(ServerPlayer player, Vec3 muzzle, Vec3 center, Vec3 dir, double radius, float fill, boolean caught, boolean shake) {
      ServerLevel level = player.serverLevel();
      float f = Mth.clamp(fill, 0.0F, 1.0F);
      Vec3 d = Vfx.safeDir(dir);
      LimbGlow.flash(player, Limb.LEFT_ARM);
      LimbGlow.flash(player, Limb.RIGHT_ARM);
      armsGlow(player, 0.0F);
      Vfx.effect(VfxEntity.Kind.SPHERE, muzzle.add(d.scale(0.2)))
         .scale(0.35 + (double)f * 0.25)
         .color(13073919, 16182527)
         .lifetime(3)
         .ownerView(player.getId(), 0.35F)
         .spawn(level);
      KineticVoxel.flare(level, muzzle);
      double lead = center.subtract(muzzle).length();
      if (lead > 0.3) {
         VoxelSprites.stream(level, muzzle, d, lead, radius * 0.35, own(player));
      }

      KineticVoxel.burst(level, center, Math.min(radius * 0.5, 3.9), d);
      float ripple = RECOIL_RIPPLE.getF();
      if (ripple > 0.0F) {
         KineticVoxel.airRipple(
            level,
            center,
            d,
            radius,
            ringBand(radius),
            ringDepth(radius),
            ripple,
            0,
            Math.max(1, RING_POP.getI()),
            Math.max(0, RING_HOLD.getI()),
            Math.max(1, RING_FADE.getI())
         );
      }

      AimUtil.groundedPositionIfFound(level, center)
         .filter(ground -> center.y - ground.y <= 3.0)
         .ifPresent(ground -> Vfx.cubeFront(level, ground, radius * 0.8, 0.5 + (double)f * 0.3, 6, 8072383, 13073919));
      Vec3 feet = feet(player);
      Vec3 flat = new Vec3(d.x, 0.0, d.z);
      Vec3 kick = flat.lengthSqr() > 1.0E-4 ? feet.add(flat.normalize().scale(0.45)) : feet;
      Vfx.cubeFront(level, kick, 1.0 + (double)f * 0.6, 0.55 + (double)f * 0.35, 6, 8072383, 13073919);
      if (shake) {
         Fx.shakeFor(player, player.getEyePosition(), 8.0, RECOIL_SHAKE.getF() + f * 0.08F, 2, 6);
      }

      detonation(level, muzzle, f, 1.15F);
      sound(level, muzzle, MIX_RELEASE, (SoundEvent)SoundEvents.WIND_CHARGE_BURST.value(), 0.35F + f * 0.3F, 1.3F - f * 0.25F);
      sound(player, MIX_RELEASE, SoundEvents.FIREWORK_ROCKET_BLAST, 0.5F + f * 0.25F, 0.7F - f * 0.1F);
      if (caught) {
         sound(level, center, MIX_RELEASE, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), 0.45F + f * 0.45F, 1.3F - f * 0.3F);
         sound(level, center, MIX_RELEASE, SoundEvents.AMETHYST_BLOCK_HIT, 0.8F, Mth.lerp(f, 1.2F, 0.7F));
      }
   }

   private static void armsGlow(ServerPlayer player, float intensity) {
      limb(player, Limb.LEFT_ARM, intensity);
      limb(player, Limb.RIGHT_ARM, intensity);
   }

   private static void palmArc(ServerPlayer player) {
      RandomSource random = player.serverLevel().random;
      ownArc(player, palm(player, false).add(jitter(random, 0.08)), palm(player, true).add(jitter(random, 0.08)), 4, 1.0);
   }

   public static void strikeWindup(ServerPlayer player, float progress) {
      ServerLevel level = player.serverLevel();
      float p = Mth.clamp(progress, 0.0F, 1.0F);
      limb(player, arm(player), 0.12F + p * 0.6F);
      double seconds = (double)p * 0.3;
      Vec3 fist = chargeHand(player, 0.7, seconds);
      KineticVoxel.gather(level, around(level.random, chargeHand(player, 0.28, seconds), 0.2), fist, 0.32, 6, own(player));
      ownArc(player, fist, around(level.random, fist, 0.16), 3, 1.0);
      windupChime(player, p, 1.0F);
   }

   public static void strikePour(ServerPlayer player, int ticks, float fill) {
      ServerLevel level = player.serverLevel();
      float f = Mth.clamp(fill, 0.0F, 1.0F);
      limb(player, arm(player), 0.72F + f * 0.28F);
      double seconds = (double)ticks / 20.0;
      Vec3 fist = chargeHand(player, 0.7, seconds);
      if (ticks % cadence(4, 2, f) == 0) {
         KineticVoxel.gather(level, around(level.random, chargeHand(player, 0.28, seconds), 0.2), fist, 0.32, 6, own(player));
      }

      if (ticks % cadence(4, 2, f) == 0) {
         ownArc(player, fist, around(level.random, fist, 0.16), 3, 1.0);
      }

      pourLayers(player, ticks, f, 1.0F, 1.0F);
   }

   public static void strikeSling(ServerPlayer player) {
      sling(player, 1.0F);
   }

   private static Vec3 chargeHand(ServerPlayer player, double reach, double seconds) {
      if (seconds < 0.3) {
         double p = Math.max(0.0, seconds / 0.3);
         double eased = 1.0 - Math.pow(1.0 - p, 3.0);
         return hand(player, reach, Mth.lerp(eased, 90.0, 0.0));
      } else {
         double t = Mth.clamp((seconds - 0.3) / 1.9999999999999998, 0.0, 1.0);
         return hand(player, false, reach, 0.0, -t * 0.4375);
      }
   }

   public static Vec3 fist(ServerPlayer player) {
      return hand(player, 0.7, 0.0);
   }

   public static void strikeWave(ServerPlayer player, Vec3 origin, Vec3 dir, double reach, float fill) {
      strikeWave(player, origin, dir, reach, fill, true);
   }

   public static void strikeWave(ServerPlayer player, Vec3 origin, Vec3 dir, double reach, float fill, boolean shake) {
      ServerLevel level = player.serverLevel();
      Limb arm = arm(player);
      LimbGlow.flash(player, arm);
      limb(player, arm, 0.0F);
      Vec3 d = Vfx.safeDir(dir);
      Vfx.effect(VfxEntity.Kind.SPHERE, origin.add(d.scale(0.25)))
         .scale(0.55 + (double)fill * 0.45)
         .color(16182527, 16777215)
         .lifetime(3)
         .ownerView(player.getId(), 0.35F)
         .spawn(level);
      KineticVoxel.flare(level, origin);
      VoxelSprites.burst(level, origin.add(d.scale(0.3)), 0.45 + (double)fill * 0.45, own(player), d);
      double ahead = RING_AHEAD.get();
      int stagger = RING_STAGGER.getI();
      float solidFirst = RING_SOLID_FIRST.getF();
      float solidStep = RING_SOLID_STEP.getF();
      float solidMin = RING_SOLID_MIN.getF();
      int popTicks = Math.max(1, RING_POP.getI());
      int holdTicks = Math.max(0, RING_HOLD.getI());
      int fadeTicks = Math.max(1, RING_FADE.getI());
      double dustScale = RING_DUST_SCALE.get();
      int asked = RINGS_BASE.getI() + Math.round(fill * (float)RINGS_SPAN.getI());
      int rings = asked <= 0 ? 0 : Math.max(2, asked);
      double span = Math.max(0.0, reach - ahead);
      double farRadius = ImpactStrikeAbility.farFor(fill);
      Vec3 far = origin;
      int farPop = 0;

      for (int i = 0; i < rings; i++) {
         double dist = rings == 1 ? ahead : ahead + (double)i * span / (double)(rings - 1);
         double radius = tubeRadius(dist, reach, farRadius);
         float solid = Math.max(solidMin, solidFirst - (float)i * solidStep);
         Vec3 at = origin.add(d.scale(dist));
         int pop = i * stagger;
         KineticVoxel.airRipple(level, at, d, radius, ringBand(radius), ringDepth(radius), solid, pop, popTicks, holdTicks, fadeTicks);
         atPop(level, at, pop, () -> ringDust(level, at, radius, dustScale));
         far = at;
         farPop = pop;
      }

      dustFan(level, origin, d, reach, farRadius, fill);
      if (shake) {
         Fx.shakeFor(player, player.getEyePosition(), 8.0, STRIKE_SHAKE.getF() + fill * 0.14F, 2, 6);
      }

      detonation(level, origin, fill, 1.0F);
      sound(level, origin, MIX_RELEASE, (SoundEvent)SoundEvents.WIND_CHARGE_BURST.value(), STRIKE_BURST_VOLUME.getF() + fill * 0.4F, 1.1F - fill * 0.2F);
      Vec3 whooshAt = far;
      float whoosh = STRIKE_WHOOSH_VOLUME.getF() + fill * 0.25F;
      atPop(level, whooshAt, farPop, () -> sound(level, whooshAt, MIX_RELEASE, SoundEvents.BREEZE_SHOOT, whoosh, 0.95F));
   }

   public static void strikeWaveHit(ServerLevel level, LivingEntity target, Vec3 origin, Vec3 away, float fill) {
      Vec3 chest = target.position().add(0.0, (double)target.getBbHeight() * 0.6, 0.0);
      Vfx.effect(VfxEntity.Kind.SPHERE, chest).scale(0.5 + (double)fill * 0.45).color(16182527, 16777215).lifetime(4).spawn(level);
      Vec3 out = Vfx.safeDir(away);
      KineticVoxel.airRipple(
         level,
         chest.add(out.scale(0.6)),
         out,
         0.7 + (double)fill * 0.4,
         0.12,
         0.25,
         0.8F,
         0,
         Math.max(1, RING_POP.getI()),
         Math.max(0, RING_HOLD.getI()),
         Math.max(1, RING_FADE.getI())
      );
      sound(level, chest, MIX_RELEASE, SoundEvents.AMETHYST_BLOCK_HIT, 0.9F, Mth.lerp(fill, 1.2F, 0.7F));
   }

   private static double tubeRadius(double dist, double reach, double farRadius) {
      double t = reach <= 0.0 ? 1.0 : Mth.clamp(dist / reach, 0.0, 1.0);
      return Mth.lerp(t, ImpactStrikeAbility.RADIUS_NEAR.get(), farRadius);
   }

   private static double ringBand(double radius) {
      return acrossTaper(radius, RING_BAND_NARROW.get(), RING_BAND_WIDE.get());
   }

   private static double ringDepth(double radius) {
      return acrossTaper(radius, RING_DEPTH_NARROW.get(), RING_DEPTH_WIDE.get());
   }

   private static double acrossTaper(double radius, double atNarrow, double atWide) {
      double near = ImpactStrikeAbility.RADIUS_NEAR.get();
      double far = ImpactStrikeAbility.RADIUS_FAR.get();
      double lo = Math.min(near, far);
      double hi = Math.max(near, far);
      double t = hi - lo <= 1.0E-6 ? 1.0 : Mth.clamp((radius - lo) / (hi - lo), 0.0, 1.0);
      return Mth.lerp(t, atNarrow, atWide);
   }

   private static void dustFan(ServerLevel level, Vec3 origin, Vec3 dir, double reach, double farRadius, float fill) {
      Vec3 flat = new Vec3(dir.x, 0.0, dir.z);
      if (!(flat.lengthSqr() < 0.05)) {
         flat = flat.normalize();
         Vec3 side = flat.yRot((float) (Math.PI / 2));
         double fan = RING_FAN_SCALE.get();
         int lines = (int)Math.round((double)(2 + Math.round(fill * 2.0F)) * fan);
         int grains = (int)Math.round(2.0 * fan);
         if (lines > 0) {
            double speed = 0.35 * (0.8 + (double)fill * 0.5);

            for (int l = 0; l < lines; l++) {
               double share = lines == 1 ? 0.0 : -1.0 + 2.0 * (double)l / (double)(lines - 1);

               for (double s = 1.5; s <= reach; s += 2.0) {
                  Vec3 at = origin.add(flat.scale(s)).add(side.scale(share * tubeRadius(s, reach, farRadius)));
                  Fx.Floor floor = dustFloor(level, at, origin.y);
                  if (floor != null) {
                     double y = floor.topY() + 0.1;
                     Vec3 out = at.subtract(origin);
                     Vec3 line = new Vec3(out.x, 0.0, out.z);
                     line = line.lengthSqr() < 1.0E-4 ? flat : line.normalize();
                     Vec3 throwDir = line.scale(speed);
                     level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), at.x, y, at.z, grains, 0.3, 0.1, 0.3, 0.15);
                     level.sendParticles(ParticleTypes.CLOUD, at.x, y + 0.15, at.z, 0, throwDir.x, 0.12, throwDir.z, 1.0);
                     level.sendParticles(ParticleTypes.POOF, at.x, y + 0.1, at.z, 0, throwDir.x * 0.6, 0.06, throwDir.z * 0.6, 1.0);
                  }
               }
            }
         }
      }
   }

   private static void ringDust(ServerLevel level, Vec3 center, double radius, double scale) {
      Fx.Floor floor = dustFloor(level, center, center.y);
      if (floor != null && !(scale <= 0.0)) {
         double y = floor.topY() + 0.1;
         double spread = radius * 0.5;
         int grains = (int)Math.round((3.0 + radius * 4.0) * scale);
         int puffs = (int)Math.round((2.0 + radius * 1.5) * scale);
         if (grains > 0 || puffs > 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), center.x, y, center.z, grains, spread, 0.1, spread, 0.12);
            level.sendParticles(ParticleTypes.CLOUD, center.x, y + 0.15, center.z, puffs, spread, 0.05, spread, 0.06);
            level.sendParticles(ParticleTypes.POOF, center.x, y + 0.1, center.z, Math.max(1, puffs / 2), spread * 0.8, 0.05, spread * 0.8, 0.036);
         }
      }
   }

   private static Fx.Floor dustFloor(ServerLevel level, Vec3 at, double fromY) {
      Fx.Floor floor = Fx.floorUnder(level, at.x, at.y, at.z, 7);
      return floor != null && !(fromY - floor.topY() > 3.5) && !(floor.topY() - fromY > 1.5) ? floor : null;
   }

   private static void atPop(ServerLevel level, Vec3 at, int pop, Runnable fx) {
      if (pop <= 0) {
         fx.run();
      } else {
         VfxBolt.delay(level, at, () -> at, pop + 1, reached -> fx.run());
      }
   }

   public static void ruptureWindup(ServerPlayer player, float progress) {
      ServerLevel level = player.serverLevel();
      float p = Mth.clamp(progress, 0.0F, 1.0F);
      limb(player, leg(player), 0.12F + p * 0.6F);
      Vec3 sole = foot(player, 0.72, -58.0);
      KineticVoxel.gather(level, around(level.random, foot(player, 0.45, -58.0), 0.18), sole, 0.32, 6, own(player));
      ownArc(player, sole, around(level.random, sole, 0.16), 3, 1.0);
      windupChime(player, p, 0.8F);
      wedgeOutline(player, player.tickCount, 0.0F);
   }

   public static void rupturePour(ServerPlayer player, int ticks, float fill) {
      ServerLevel level = player.serverLevel();
      float f = Mth.clamp(fill, 0.0F, 1.0F);
      limb(player, leg(player), 0.72F + f * 0.28F);
      Vec3 sole = foot(player, 0.72, -58.0);
      if (ticks % cadence(4, 2, f) == 0) {
         KineticVoxel.gather(level, around(level.random, foot(player, 0.45, -58.0), 0.18), sole, 0.32, 6, own(player));
      }

      if (ticks % cadence(4, 2, f) == 0) {
         Fx.Floor floor = dustFloor(level, sole, sole.y);
         if (floor != null) {
            Vec3 ground = new Vec3(sole.x, floor.topY(), sole.z).add(jitter(level.random, 0.12));
            ownArc(player, sole, ground, 4, 1.0);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), ground.x, floor.topY() + 0.05, ground.z, 1, 0.1, 0.05, 0.1, 0.08);
         } else {
            ownArc(player, sole, around(level.random, sole, 0.16), 3, 1.0);
         }
      }

      pourLayers(player, ticks, f, 0.8F, 1.0F);
      wedgeOutline(player, player.tickCount, f);
   }

   public static void ruptureSling(ServerPlayer player) {
      sling(player, 0.8F);
   }

   private static void wedgeOutline(ServerPlayer player, int tick, float fill) {
      if (tick % 2 == 0) {
         float bright = RUPTURE_OUTLINE.getF();
         if (!(bright <= 0.0F)) {
            ServerLevel level = player.serverLevel();
            Vec3 dir = Vfx.safeDir(AimUtil.lookFlat(player));
            Vec3 foot = AimUtil.groundedPositionIfFound(level, player.position()).orElse(null);
            if (foot != null) {
               double length = RuptureKickAbility.lengthFor(fill);
               float pulse = 1.0F - 0.25F * (0.5F + 0.5F * Mth.sin((float)tick * 0.35F));
               Vfx.effect(VfxEntity.Kind.KINETIC_WEDGE, foot)
                  .scale(RuptureKickAbility.widthFor(fill))
                  .secondary(new Vec3(dir.x * length, (double)VfxEntity.packRuptureLight(RuptureKickAbility.NEAR.get(), (double)bright), dir.z * length))
                  .color(8072383, 13073919)
                  .lifetime(4)
                  .intensity(pulse)
                  .spawn(level);
            }
         }
      }
   }

   public static void ruptureKick(ServerPlayer player, Vec3 origin, Vec3 flatDir, double length, double halfNear, double halfFar, float fill) {
      ruptureKick(player, origin, flatDir, length, halfNear, halfFar, fill, true);
   }

   public static void ruptureKick(ServerPlayer player, Vec3 origin, Vec3 flatDir, double length, double halfNear, double halfFar, float fill, boolean shake) {
      ServerLevel level = player.serverLevel();
      Limb leg = leg(player);
      LimbGlow.flash(player, leg);
      limb(player, leg, 0.0F);
      Vec3 dir = Vfx.safeDir(new Vec3(flatDir.x, 0.0, flatDir.z));
      Vec3 foot = AimUtil.groundedPositionIfFound(level, origin).orElse(null);
      if (foot != null) {
         double heaveScale = RUPTURE_HEAVE.get();
         int copies = RUPTURE_COPIES.getI();
         double dustScale = RUPTURE_DUST_SCALE.get();
         float rumble = RUPTURE_RUMBLE_VOLUME.getF();
         float stomp = RUPTURE_STOMP_VOLUME.getF();
         double speed = Math.max(0.1, RuptureKickAbility.SPEED.get());
         int travel = Math.max(1, (int)Math.ceil(length / speed));
         Vfx.effect(VfxEntity.Kind.SPHERE, foot.add(0.0, 0.25, 0.0))
            .scale(0.6 + (double)fill * 0.5)
            .color(13073919, 16182527)
            .lifetime(3)
            .ownerView(player.getId(), 0.35F)
            .spawn(level);
         KineticVoxel.flare(level, foot.add(0.0, 0.1, 0.0));
         KineticVoxel.burst(level, foot.add(0.0, 0.15, 0.0), 0.6 + (double)fill * 0.5, UP);
         Vfx.cubeFront(level, foot, 1.1 + (double)fill * 0.6, 0.6 + (double)fill * 0.3, 6, 8072383, 13073919);
         float glow = RUPTURE_GLOW.getF();
         Vfx.effect(VfxEntity.Kind.KINETIC_RUPTURE, foot)
            .scale(halfFar)
            .secondary(new Vec3(dir.x * length, (double)VfxEntity.packRuptureLight(halfNear, (double)glow), dir.z * length))
            .color(8072383, 13073919)
            .lifetime(travel + 14)
            .intensity(0.8F + fill * 0.3F)
            .spawn(level);
         heave(level, foot, dir, length, halfNear, halfFar, fill, speed, heaveScale, copies, glow);

         for (int t = 1; t <= travel; t++) {
            double from = (double)(t - 1) * speed;
            double to = Math.min(length, (double)t * speed);
            Vec3 at = foot.add(dir.scale(to));
            boolean deep = t % 2 == 1;
            atPop(level, at, t, () -> ruptureFront(level, foot, dir, from, to, length, halfNear, halfFar, fill, dustScale, rumble, deep));
         }

         Fx.Floor under = dustFloor(level, foot, foot.y);
         sound(level, foot, MIX_RUPTURE, SoundEvents.ANVIL_LAND, 0.45F * stomp, 0.4F);
         sound(level, foot, MIX_RUPTURE, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), (0.5F + fill * 0.5F) * stomp, 0.6F);
         sound(level, foot, MIX_RUPTURE, SoundEvents.DEEPSLATE_BREAK, stomp, 0.6F);
         if (under != null) {
            sound(level, foot, MIX_RUPTURE, under.state().getSoundType().getBreakSound(), stomp, 0.7F);
         }

         sound(level, foot, MIX_RUPTURE, SoundEvents.LIGHTNING_BOLT_THUNDER, (0.2F + fill * 0.25F) * stomp, 1.6F);
         if (shake) {
            Fx.shake(level, foot, 12.0, 0.05F + fill * 0.08F, 3, 8);
            Fx.shakeFor(player, player.getEyePosition(), 8.0, 0.12F + fill * 0.18F, 3, 8);
         }
      }
   }

   public static void ruptureHit(ServerLevel level, LivingEntity target, float fill) {
      Vec3 feet = target.position();
      KineticVoxel.burst(level, feet.add(0.0, 0.2, 0.0), 0.6 + (double)fill * 0.5, UP);
      Vfx.cubeFront(level, feet, 0.9 + (double)fill * 0.5, 0.5 + (double)fill * 0.3, 6, 8072383, 13073919);
      Vec3 rise = new Vec3(0.0, 0.55 + (double)fill * 0.3, 0.0);
      KineticVoxel.wake(level, feet.add(0.0, (double)target.getBbHeight() * 0.35 + rise.y * 2.0, 0.0), rise, 2, 0.9 + (double)fill * 0.4);
      sound(level, feet, MIX_RUPTURE, SoundEvents.AMETHYST_BLOCK_HIT, 0.9F, Mth.lerp(fill, 1.1F, 0.7F));
      Fx.Floor floor = dustFloor(level, feet, feet.y);
      if (floor != null) {
         sound(level, feet, MIX_RUPTURE, floor.state().getSoundType().getBreakSound(), 0.7F, 0.7F);
      }
   }

   private static void heave(
      ServerLevel level,
      Vec3 foot,
      Vec3 dir,
      double length,
      double halfNear,
      double halfFar,
      float fill,
      double speed,
      double heaveScale,
      int copies,
      float glow
   ) {
      if (copies > 0 && !(heaveScale <= 0.0)) {
         RandomSource random = level.random;
         Vec3 side = new Vec3(dir.z, 0.0, -dir.x);
         float yaw = Vfx.yawAlong(dir);
         Set<Long> seen = new HashSet<>();
         List<double[]> cells = new ArrayList<>();

         for (double s = 0.5; s <= length; s++) {
            double half = Mth.lerp(s / length, halfNear, halfFar);

            for (double u = -half; u <= half; u++) {
               Vec3 at = foot.add(dir.scale(s)).add(side.scale(u));
               int bx = Mth.floor(at.x);
               int bz = Mth.floor(at.z);
               if (seen.add((long)bx << 32 | (long)bz & 4294967295L)) {
                  cells.add(new double[]{(double)bx, (double)bz, s, u});
               }
            }
         }

         if (cells.size() > copies) {
            List<double[]> kept = new ArrayList<>();

            for (double[] c : cells) {
               boolean far = c[2] > length * 0.5;
               if (!far || ((int)c[0] + (int)c[1] & 1) == 0) {
                  kept.add(c);
               }
            }

            cells = kept;
            if (kept.size() > copies) {
               double stride = (double)kept.size() / (double)copies;
               kept = new ArrayList<>();

               for (double i = 0.0; i < (double)cells.size() && kept.size() < copies; i += stride) {
                  kept.add(cells.get((int)i));
               }

               cells = kept;
            }
         }

         for (double[] cx : cells) {
            double s = cx[2];
            double ux = cx[3];
            Vec3 at = new Vec3(cx[0] + 0.5, foot.y, cx[1] + 0.5);
            Fx.Floor floor = dustFloor(level, at, foot.y);
            if (floor != null) {
               Vec3 bottom = new Vec3(at.x, floor.topY() - 0.5 - 0.01, at.z);
               double plate = 0.86 + random.nextDouble() * 0.1;
               double rise = (0.25 + random.nextDouble() * 0.4) * (0.7 + (double)fill * 0.3) * heaveScale;
               float tilt = (6.0F + random.nextFloat() * 14.0F) * (ux >= 0.0 ? 1.0F : -1.0F);
               int life = 10 + random.nextInt(6);
               int delay = (int)Math.ceil(s / speed) + random.nextInt(2);
               Vfx.blockHeave(
                  level, bottom, floor.state(), plate, 0.5, plate, life, yaw + (random.nextFloat() - 0.5F) * 2.0F * 25.0F, rise, tilt, delay, 13073919, glow
               );
            }
         }
      }
   }

   private static void ruptureFront(
      ServerLevel level,
      Vec3 foot,
      Vec3 dir,
      double from,
      double to,
      double length,
      double halfNear,
      double halfFar,
      float fill,
      double dustScale,
      float rumble,
      boolean deep
   ) {
      RandomSource random = level.random;
      Vec3 side = new Vec3(dir.z, 0.0, -dir.x);
      Fx.Floor frontFloor = null;
      Vec3 frontAt = foot.add(dir.scale(to));

      for (double s = Math.floor(from / 1.0 + 1.0E-6) * 1.0; s <= to; s++) {
         if (!(s <= from) && !(s > length)) {
            double half = Mth.lerp(s / length, halfNear, halfFar);
            Vec3 at = foot.add(dir.scale(s));
            Fx.Floor floor = dustFloor(level, at, foot.y);
            if (floor != null) {
               frontFloor = floor;
               if (!(dustScale <= 0.0)) {
                  double y = floor.topY() + 0.1;
                  double spread = half * 0.9;
                  int grains = (int)Math.round((3.0 + half * 2.0) * dustScale);
                  int puffs = (int)Math.round((1.0 + half * 1.5) * dustScale);
                  if (grains > 0) {
                     level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), at.x, y, at.z, grains, spread, 0.15, spread, 0.22);
                  }

                  for (int p = 0; p < puffs; p++) {
                     Vec3 puff = at.add(side.scale((random.nextDouble() - 0.5) * 2.0 * spread)).add(dir.scale((random.nextDouble() - 0.5) * 1.0));
                     double up = 0.32 * (0.8 + random.nextDouble() * 0.4) * (0.8 + (double)fill * 0.4);
                     level.sendParticles(ParticleTypes.CLOUD, puff.x, y + 0.1, puff.z, 0, 0.0, up, 0.0, 1.0);
                     level.sendParticles(ParticleTypes.POOF, puff.x, y + 0.05, puff.z, 0, 0.0, up * 0.5, 0.0, 1.0);
                  }
               }
            }
         }
      }

      if (rumble > 0.0F && frontFloor != null) {
         float volume = rumble * (0.6F + fill * 0.4F);
         sound(level, frontAt, MIX_RUPTURE, frontFloor.state().getSoundType().getBreakSound(), volume, 0.5F + random.nextFloat() * 0.15F);
         if (deep) {
            sound(level, frontAt, MIX_RUPTURE, SoundEvents.DEEPSLATE_BREAK, volume * 0.6F, 0.45F);
            sound(level, frontAt, MIX_RUPTURE, SoundEvents.CONDUIT_AMBIENT_SHORT, volume * 0.3F, 0.5F);
         }
      }
   }

   public static void releaseStep(ServerPlayer player, int level, boolean up) {
      float pitch = 0.7F + (float)level * 0.08F;
      if (up) {
         LimbGlow.flash(player, Limb.ALL);
         KineticVoxel.burst(player.serverLevel(), chest(player), 0.5 + (double)level * 0.09);
         sound(player, MIX_RELEASE, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.8F, pitch);
         sound(player, MIX_RELEASE, SoundEvents.WARDEN_HEARTBEAT, 0.5F, 0.9F + (float)level * 0.05F);
      } else {
         sound(player, MIX_RELEASE, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.45F, pitch * 0.8F);
      }

      LimbGlow.set(player, Limb.ALL, (float)level / 10.0F, 13073919);
   }

   public static void releaseTick(ServerPlayer player, int level) {
      if (level <= 0) {
         STRIDES.remove(player.getUUID());
      } else {
         ServerLevel world = player.serverLevel();
         RandomSource random = world.random;
         raiseAll(player, (float)level / 10.0F);
         int tick = player.tickCount;
         Vec3 velocity = player.getKnownMovement();
         if (level >= 1 && tick % Math.max(2, 12 - level) == 0) {
            KineticVoxel.trail(world, hand(player, random.nextBoolean(), 0.7, 75.0), velocity);
         }

         if (level >= 3 && tick % Math.max(2, 7 - level / 2) == 0) {
            boolean off = random.nextBoolean();
            Vfx.pixelArc(world, hand(player, off, 0.28, 75.0), hand(player, off, 0.7, 75.0), 3, 8072383, 13073919);
         }

         if (level >= 6 && tick % Math.max(2, 13 - level) == 0) {
            KineticVoxel.trail(world, around(random, chest(player), 0.35), velocity);
         }

         if (level >= 10) {
            footstep(player, world);
         }

         loop(player, MIX_CHARGE, "kinetic_release_hum", SoundEvents.BEACON_AMBIENT, 0.12F + (float)level * 0.03F, 0.8F + (float)level * 0.08F);
      }
   }

   public static void releaseEmpty(ServerPlayer player) {
      bankEmpty(player);
      LimbGlow.set(player, Limb.ALL, 0.0F, 13073919);
      KineticVoxel.burst(player.serverLevel(), chest(player), 0.9);
      STRIDES.remove(player.getUUID());
   }

   public static void stockpileStart(ServerPlayer player) {
      KineticVisual.setStomping(player, true);
      limb(player, Limb.LEFT_LEG, 0.25F);
      limb(player, Limb.RIGHT_LEG, 0.25F);
   }

   public static void stomp(ServerPlayer player, int index) {
      ServerLevel level = player.serverLevel();
      Limb planted = index % 2 == 1 ? Limb.RIGHT_LEG : Limb.LEFT_LEG;
      LimbGlow.flash(player, planted);
      float glow = Math.min(0.75F, 0.25F + (float)index * 0.04F);
      limb(player, Limb.LEFT_LEG, glow);
      limb(player, Limb.RIGHT_LEG, glow);
      int rise = Math.min(index, 12);
      Vfx.cubeFront(level, feet(player), 1.0 + (double)rise * 0.08, 0.5 + (double)rise * 0.05, 6, 8072383, 13073919);
      sound(player, MIX_CHARGE, SoundEvents.WARDEN_HEARTBEAT, 0.45F, 1.2F);
      sound(player, MIX_CHARGE, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, Math.min(1.9F, 0.7F + (float)index * 0.06F));
   }

   public static void stockpileEnd(ServerPlayer player) {
      KineticVisual.setStomping(player, false);
      limb(player, Limb.LEFT_LEG, 0.0F);
      limb(player, Limb.RIGHT_LEG, 0.0F);
   }

   public static void magnetTick(ServerLevel level, Entity loose) {
      Vec3 velocity = loose.getDeltaMovement();
      if ((loose.tickCount + loose.getId()) % 8 == 0 && velocity.horizontalDistanceSqr() > 0.002) {
         KineticVoxel.trail(level, loose.position().add(0.0, 0.15, 0.0), velocity);
      }
   }

   public static void landing(ServerPlayer player, float fall) {
      float f = Math.min(fall, 15.0F);
      LimbGlow.flash(player, Limb.LEFT_LEG);
      LimbGlow.flash(player, Limb.RIGHT_LEG);
      Vfx.cubeFront(player.serverLevel(), feet(player), 1.0 + (double)f * 0.15, 0.5 + (double)f * 0.06, 8, 8072383, 13073919);
      sound(player, MIX_CHARGE, SoundEvents.AMETHYST_BLOCK_HIT, Math.min(1.0F, 0.3F + f * 0.06F), 0.6F);
      sound(player, MIX_CHARGE, SoundEvents.AMETHYST_BLOCK_CHIME, Math.min(0.8F, 0.2F + f * 0.05F), 0.8F);
   }

   public static void bankFlash(ServerPlayer player) {
      LimbGlow.flash(player, Limb.BODY);
      sound(player, MIX_CHARGE, SoundEvents.AMETHYST_BLOCK_CHIME, 0.4F, 1.2F);
   }

   public static void pourCapped(ServerPlayer player) {
      sound(player, MIX_CHARGE, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.35F, 1.9F);
      sound(player, MIX_CHARGE, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.45F, 0.5F);
   }

   public static void bankEmpty(ServerPlayer player) {
      sound(player, MIX_RELEASE, (SoundEvent)SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 0.7F, 1.0F);
   }

   public static void holdEnd(ServerPlayer player) {
      limb(player, Limb.LEFT_ARM, 0.0F);
      limb(player, Limb.RIGHT_ARM, 0.0F);
      limb(player, Limb.LEFT_LEG, 0.0F);
      limb(player, Limb.RIGHT_LEG, 0.0F);
      HOLD_SOUND.remove(player.getUUID());
   }

   public static void forget(ServerPlayer player) {
      if (player != null) {
         STRIDES.remove(player.getUUID());
         HOLD_SOUND.remove(player.getUUID());
      }
   }

   public static void awaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 chest = chest(player);
      KineticVoxel.inrush(level, chest, 4.0, 0, 1.0F);
      KineticVoxel.burst(level, chest, 2.0);
      Vfx.cubeFront(level, feet(player), 4.0, 1.2, 8, 8072383, 13073919);
      LimbGlow.flash(player, Limb.ALL);
      sound(player, MIX_RELEASE, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.0F, 0.7F);
      sound(player, MIX_RELEASE, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 0.8F);
      sound(player, MIX_RELEASE, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.9F);
   }

   private static VoxelSprites.Look own(ServerPlayer player) {
      return KineticVoxel.lookFor(player, 0.35F);
   }

   private static void ownArc(ServerPlayer player, Vec3 from, Vec3 to, int life, double scale) {
      Vfx.effect(VfxEntity.Kind.KINETIC_PIXEL_ARC, from)
         .scale(scale)
         .secondary(to.subtract(from))
         .color(8072383, 13073919)
         .lifetime(life)
         .ownerView(player.getId(), 0.35F)
         .spawn(player.serverLevel());
   }

   private static void chargeSound(ServerPlayer player, int ticks, float charge, float pour) {
      float build = Math.min(1.0F, charge + pour);
      if (ticks % cadence(6, 3, build) == 0) {
         sound(player, MIX_CHARGE, SoundEvents.AMETHYST_BLOCK_CHIME, 0.4F, Mth.lerp(build, 0.8F, 1.9F));
      }

      if (pour > 0.0F) {
         pourLayers(player, ticks, pour, 1.0F, 0.7F);
      }
   }

   private static void windupChime(ServerPlayer player, float progress, float register) {
      windupChime(player, progress, register, 1.0F);
   }

   private static void windupChime(ServerPlayer player, float progress, float register, float mix) {
      if (progress <= 0.0F) {
         chargeTone(player, register, mix);
      }

      sound(player, MIX_CHARGE, SoundEvents.AMETHYST_BLOCK_CHIME, 0.3F * mix, Mth.lerp(progress, 0.9F, 1.8F) * register);
   }

   private static void chargeTone(ServerPlayer player, float register, float mix) {
      int[] clock = holdClock(player);
      if (clock[1] == 0) {
         clock[1] = 1;
         sound(player, MIX_CHARGE, SoundEvents.BEACON_POWER_SELECT, 0.5F * mix, 0.6F * register);
      }
   }

   private static void pourLayers(ServerPlayer player, int ticks, float fill, float register, float mix) {
      pourBeat(player, ticks, fill, register, mix);
      loop(player, MIX_CHARGE, "kinetic_pour_hum", SoundEvents.CONDUIT_AMBIENT_SHORT, (0.22F + fill * 0.18F) * mix, (0.7F + fill * 0.7F) * register);
      if (fill > 0.5F && ticks % 3 == 0) {
         RandomSource random = player.serverLevel().random;
         float chance = (fill - 0.5F) / 0.5F * 0.8F;
         if (random.nextFloat() < chance) {
            SoundEvent tick = random.nextBoolean() ? SoundEvents.AMETHYST_CLUSTER_BREAK : SoundEvents.AMETHYST_BLOCK_RESONATE;
            sound(player, MIX_CHARGE, tick, 0.16F * mix, (1.5F + random.nextFloat() * 0.5F) * register);
         }
      }
   }

   private static void pourBeat(ServerPlayer player, int ticks, float fill, float register, float mix) {
      int[] clock = holdClock(player);
      int last = clock[0];
      if (last == -1000000 || ticks < last || ticks - last >= cadence(12, 5, fill)) {
         clock[0] = ticks;
         sound(player, MIX_CHARGE, SoundEvents.WARDEN_HEARTBEAT, (0.5F + fill * 0.3F) * mix, (0.9F + fill * 0.5F) * register);
      }
   }

   private static int[] holdClock(ServerPlayer player) {
      return HOLD_SOUND.computeIfAbsent(player.getUUID(), id -> new int[]{-1000000, 0});
   }

   private static void sling(ServerPlayer player, float register) {
      sound(player, MIX_RELEASE, SoundEvents.PLAYER_ATTACK_SWEEP, 0.55F, 1.35F * register);
   }

   private static void detonation(ServerLevel level, Vec3 at, float fill, float register) {
      if (fill > 0.05F) {
         sound(level, at, MIX_RELEASE, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.1F + fill * 0.5F, 1.3F * register);
      }

      sound(level, at, MIX_RELEASE, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), 0.35F + fill * 0.4F, (0.85F - fill * 0.15F) * register);
      sound(level, at, MIX_RELEASE, SoundEvents.ANVIL_LAND, 0.12F + fill * 0.2F, 0.5F * register);
   }

   private static void releaseSound(ServerPlayer player, float power) {
      sound(player, MIX_RELEASE, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9F, Mth.lerp(power, 1.3F, 0.9F));
      sound(player, MIX_RELEASE, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), 0.3F + power * 0.6F, Mth.lerp(power, 1.6F, 1.1F));
      if (power > 0.15F) {
         sound(player, MIX_RELEASE, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.35F * power, 1.5F);
      }
   }

   private static int cadence(int slow, int fast, float build) {
      return Math.max(fast, Math.round(Mth.lerp(Mth.clamp(build, 0.0F, 1.0F), (float)slow, (float)fast)));
   }

   private static void limb(ServerPlayer player, Limb limb, float intensity) {
      LimbGlow.set(player, limb, Math.max(intensity, floor(player)), 13073919);
   }

   private static void raiseAll(ServerPlayer player, float floor) {
      GlowState glow = LimbGlow.of(player);

      for (Limb l : Limb.REAL) {
         if (glow.intensity(l) < floor - 0.003921569F) {
            LimbGlow.set(player, l, floor, 13073919);
         }
      }
   }

   private static float floor(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      return !"kinetic_charger".equals(data.powerId()) ? 0.0F : (float)KineticBank.releaseLevel(data) / 10.0F;
   }

   private static void footstep(ServerPlayer player, ServerLevel level) {
      double[] stride = STRIDES.computeIfAbsent(player.getUUID(), id -> new double[1]);
      if (player.onGround()) {
         stride[0] += player.getKnownMovement().horizontalDistance();
         if (stride[0] >= 1.25) {
            stride[0] = 0.0;
            Vfx.cubeFront(level, feet(player), 1.1, 0.55, 6, 8072383, 13073919);
         }
      }
   }

   private static Limb arm(ServerPlayer player) {
      return player.getMainArm() == HumanoidArm.LEFT ? Limb.LEFT_ARM : Limb.RIGHT_ARM;
   }

   private static Vec3 hand(ServerPlayer player, double reach, double armDeg) {
      return hand(player, false, reach, armDeg);
   }

   private static Vec3 hand(ServerPlayer player, boolean offHand, double reach, double armDeg) {
      return hand(player, offHand, reach, armDeg, 0.0);
   }

   private static Vec3 hand(ServerPlayer player, boolean offHand, double reach, double armDeg, double alongAim) {
      Vec3 at = AimUtil.shoulderHand(
         player.getEyePosition(), (double)player.getYRot(), (double)player.getXRot() + armDeg, AimUtil.handSign(player, offHand), 0.31, 0.25, reach
      );
      return alongAim == 0.0 ? at : at.add(AimUtil.direction((double)player.getYRot(), (double)player.getXRot()).scale(alongAim));
   }

   private static Vec3 armPoint(ServerPlayer player, boolean offHand, double reach, double torsoYaw, double pitch, double yaw, double roll) {
      double side = AimUtil.handSign(player, offHand);
      double yawRad = Math.toRadians((double)player.getYRot());
      Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
      Vec3 right = AimUtil.horizontalRight((double)player.getYRot());
      double[] shoulder = rotY(torsoYaw, new double[]{-0.31, 0.0, 0.0});
      double[] dir = rotY(torsoYaw, rotZ(roll, rotY(yaw, rotX(pitch + (double)player.getXRot(), new double[]{0.0, 1.0, 0.0}))));
      Vec3 origin = player.getEyePosition().add(0.0, -0.25, 0.0);
      return origin.add(toWorld(shoulder, side, right, forward)).add(toWorld(dir, side, right, forward).scale(reach));
   }

   private static Vec3 toWorld(double[] m, double side, Vec3 right, Vec3 forward) {
      return right.scale(-m[0] * side).add(0.0, -m[1], 0.0).add(forward.scale(-m[2]));
   }

   private static double[] rotX(double deg, double[] v) {
      double c = Math.cos(Math.toRadians(deg));
      double s = Math.sin(Math.toRadians(deg));
      return new double[]{v[0], c * v[1] - s * v[2], s * v[1] + c * v[2]};
   }

   private static double[] rotY(double deg, double[] v) {
      double c = Math.cos(Math.toRadians(deg));
      double s = Math.sin(Math.toRadians(deg));
      return new double[]{c * v[0] + s * v[2], v[1], -s * v[0] + c * v[2]};
   }

   private static double[] rotZ(double deg, double[] v) {
      double c = Math.cos(Math.toRadians(deg));
      double s = Math.sin(Math.toRadians(deg));
      return new double[]{c * v[0] - s * v[1], s * v[0] + c * v[1], v[2]};
   }

   private static Vec3 feet(ServerPlayer player) {
      return player.position().add(0.0, 0.08, 0.0);
   }

   private static Limb leg(ServerPlayer player) {
      return player.getMainArm() == HumanoidArm.LEFT ? Limb.LEFT_LEG : Limb.RIGHT_LEG;
   }

   private static Vec3 foot(ServerPlayer player, double reach, double legDeg) {
      return AimUtil.shoulderHand(
         player.position().add(0.0, 0.75, 0.0), (double)player.getYRot(), 90.0 + legDeg, AimUtil.handSign(player, false), 0.12, 0.0, reach
      );
   }

   private static Vec3 chest(ServerPlayer player) {
      return player.position().add(0.0, (double)player.getBbHeight() * 0.6, 0.0);
   }

   private static Vec3 around(RandomSource random, Vec3 center, double reach) {
      double ang = random.nextDouble() * Math.PI * 2.0;
      double r = reach * (0.7 + random.nextDouble() * 0.3);
      return center.add(Math.cos(ang) * r, (random.nextDouble() - 0.5) * reach * 0.8, Math.sin(ang) * r);
   }

   private static Vec3 jitter(RandomSource random, double spread) {
      return new Vec3((random.nextDouble() - 0.5) * 2.0 * spread, (random.nextDouble() - 0.5) * 2.0 * spread, (random.nextDouble() - 0.5) * 2.0 * spread);
   }

   private static void sound(ServerPlayer player, Lab.Value group, SoundEvent event, float volume, float pitch) {
      sound(player.serverLevel(), player.position(), group, event, volume, pitch);
   }

   private static void loop(ServerPlayer player, Lab.Value group, String key, SoundEvent event, float volume, float pitch) {
      float heard = volume * group.getF();
      if (heard > 0.0F) {
         LoopSound.keep(player, key, event, SoundSource.PLAYERS, heard, pitch);
      }
   }

   private static void sound(ServerLevel level, Vec3 pos, Lab.Value group, SoundEvent event, float volume, float pitch) {
      float heard = volume * group.getF();
      if (!(heard <= 0.0F)) {
         Fx.sound(level, pos, event, SoundSource.PLAYERS, heard, pitch);
      }
   }
}
