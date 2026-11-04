package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.LoopSound;
import com.awakenedmod.awakened.core.terrain.RemovedBlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class RuinGroundFx {
   private static final Lab.Value CHUNKS_PER_WAVE = Lab.SERVER.def("ruin.collapse.chunks_per_wave", 5.0, 0.0, 16.0, 1.0);
   private static final Lab.Value CHUNK_LIFE = Lab.SERVER.def("ruin.collapse.chunk_life", 44.0, 20.0, 120.0, 1.0);
   private static final Lab.Value CHUNK_SIZE = Lab.SERVER.def("ruin.collapse.chunk_size", 1.0, 0.4, 2.5, 0.05);
   private static final Lab.Value PUFFS_PER_WAVE = Lab.SERVER.def("ruin.collapse.puffs_per_wave", 32.0, 0.0, 200.0, 1.0);
   private static final Lab.Value FRACTURE_VEINS = Lab.SERVER.def("ruin.collapse.veins", 12.0, 4.0, 28.0, 1.0);
   private static final Lab.Value FRONT_INTENSITY = Lab.SERVER.def("ruin.collapse.front", 1.0, 0.0, 2.5, 0.05);
   private static final Lab.Value HAZE_INTENSITY = Lab.SERVER.def("ruin.collapse.haze", 1.0, 0.0, 2.5, 0.05);
   private static final Lab.Value HAZE_LIFE = Lab.SERVER.def("ruin.collapse.haze_life", 260.0, 60.0, 600.0, 1.0);
   private static final Lab.Value ASHFALL = Lab.SERVER.def("ruin.collapse.ashfall", 1.0, 0.0, 3.0, 0.1);
   private static final Lab.Value SHAKE = Lab.SERVER.def("ruin.collapse.shake", 0.45, 0.0, 1.0, 0.05);
   private static final Lab.Value RUMBLE_VOLUME = Lab.SERVER.def("ruin.collapse.rumble", 1.5, 0.0, 4.0, 0.1);
   private static final Lab.Value SLAM_CHUNKS = Lab.SERVER.def("ruin.collapse.slam_chunks", 10.0, 0.0, 24.0, 1.0);
   private static final Lab.Value SLAM_DUST = Lab.SERVER.def("ruin.collapse.slam_dust", 1.0, 0.0, 3.0, 0.1);
   private static final double SLAM_CRACK_RADIUS = 3.2;
   private static final int SLAM_CRACK_HOLD = 34;
   private static final int SLAM_CRACK_BLOCKS = 36;
   private static final int LANDING_CRACK_HOLD = 20;
   private static final int LANDING_CRACK_BLOCKS = 9;
   private static final double TELL_CRACK_RADIUS = 2.4;
   private static final int TELL_CRACK_BLOCKS = 21;
   private static final int CRUMBLE_EVERY = 6;
   private static final int RING_SHAKE_EVERY = 8;
   static final int GRIND_EVERY = 12;
   private static final int RUMBLE_TAIL_TICKS = 40;
   private static final int ASHFALL_TICKS = 220;
   private static final int ASHFALL_EVERY = 2;
   private static final double ASHFALL_HEIGHT = 7.0;
   private static final int CURTAIN_LINGER = 45;
   private static final int SETTLE_EVERY = 3;
   private static final int FRACTURE_LINGER = 10;
   private static final int CRACK_BURST_LIFE = 14;

   private RuinGroundFx() {
   }

   public static void preloadLabKeys() {
   }

   static void collapseCalledOff(ServerPlayer player) {
      RuinArm.off(player);
   }

   static void collapseTell(ServerLevel level, ServerPlayer player, Vec3 center, double radius, float progress) {
      double farthest = radius * 1.12;
      if (progress == 0.0F) {
         Vfx.effect(VfxEntity.Kind.RUIN_FRACTURE, center.add(0.0, 0.02, 0.0))
            .scale(farthest)
            .secondary(new Vec3(11.0, (double)FRACTURE_VEINS.getI(), 0.0))
            .color(2828326, 10130831)
            .lifetime(21)
            .spawn(level);
         RuinCracks.stamp(level, center, 2.4, 7, 21, 21);
         Fx.sound(level, center, (SoundEvent)AwakenedSounds.RUIN_GROAN.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
      }

      RuinArm.set(player, 0.2F + 0.8F * progress);
      RandomSource random = level.random;
      double reach = farthest * (double)ease(progress);
      int points = 8 + (int)(radius * 0.5);

      for (int i = 0; i < points; i++) {
         double a = random.nextDouble() * Math.PI * 2.0;
         double r = reach * Math.sqrt(random.nextDouble());
         double x = center.x + Math.cos(a) * r;
         double z = center.z + Math.sin(a) * r;
         Fx.Floor floor = Fx.floorState(level, x, center.y - 1.0, z);
         double y = floor == null ? center.y : floor.topY();
         level.sendParticles(i % 2 == 0 ? ParticleTypes.ASH : ParticleTypes.WHITE_ASH, x, y + 0.15, z, 2, 0.3, 0.1, 0.3, 0.012);
      }

      int crumbs = 4 + (int)(radius * 0.2);

      for (int i = 0; i < crumbs; i++) {
         double a = random.nextDouble() * Math.PI * 2.0;
         double x = center.x + Math.cos(a) * reach;
         double z = center.z + Math.sin(a) * reach;
         Fx.Floor floor = Fx.floorState(level, x, center.y - 1.0, z);
         if (floor != null) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), x, floor.topY() + 0.05, z, 3, 0.2, 0.03, 0.2, 0.03);
         }
      }

      level.sendParticles(ParticleTypes.ASH, center.x, center.y + 0.3, center.z, 4, 0.5, 0.2, 0.5, 0.01);
   }

   static void collapseSlam(ServerLevel level, ServerPlayer player, Vec3 center, double radius, int depth) {
      RuinFx.slamHand(player);
      Vec3 fist = center.add(AimUtil.lookFlat(player).scale(0.75));
      RuinCracks.stamp(level, fist, 3.2, 9, 34, 36);
      double farthest = radius * 1.12 + 0.5;
      int spread = 50;
      Vfx.effect(VfxEntity.Kind.RUIN_COLLAPSE_FRONT, center.add(0.0, 0.05, 0.0))
         .scale(farthest)
         .secondary(new Vec3((double)spread, (double)depth, 0.0))
         .color(10130831, 2828326)
         .intensity(FRONT_INTENSITY.getF())
         .lifetime(spread + 45)
         .spawn(level);
      Fx.Floor under = Fx.floorUnder(level, fist.x, fist.y + 0.5, fist.z, 3);
      if (under != null) {
         Vec3 ground = new Vec3(fist.x, under.topY(), fist.z);
         slamChunks(level, ground, under.state());
         pourAround(level, center, under.topY(), under.state(), SLAM_DUST.getF());
         Fx.sound(level, center, under.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 1.0F, 0.6F);
      }

      Fx.sound(level, center, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.2F, 0.7F);
      Fx.sound(level, center, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.45F, 0.5F);
      Fx.sound(level, center, SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 1.4F, 0.4F);
      Fx.sound(level, center, SoundEvents.ANCIENT_DEBRIS_BREAK, SoundSource.PLAYERS, 1.2F, 0.5F);
      float shake = SHAKE.getF();
      Fx.shake(level, center, radius + 28.0, shake, 8, 34);
      Fx.shakeFor(player, player.getEyePosition(), 8.0, shake * 0.7F, 6, 22);
      String key = rumbleKey(player);
      float volume = RUMBLE_VOLUME.getF();
      int tailFrom = spread + 3;
      RuinFxClock.every(level, spread + 40, t -> {
         if (t >= tailFrom) {
            float fade = 1.0F - (float)(t - tailFrom) / (float)(spread + 40 - tailFrom);
            LoopSound.keepAt(level, key, center, (SoundEvent)AwakenedSounds.RUIN_RUMBLE.get(), SoundSource.BLOCKS, volume * fade, 0.9F);
         }
      });
   }

   static void collapseWave(ServerLevel level, List<RemovedBlock> blocks, Vec3 center, int groundY, Map<Long, Integer> floors) {
      if (!blocks.isEmpty()) {
         RandomSource random = level.random;
         Map<Long, Integer> floorOf = columnFloors(blocks);
         List<RemovedBlock> standing = new ArrayList<>();

         for (RemovedBlock block : solid(level, blocks)) {
            if (block.pos().getY() > groundY + 2) {
               standing.add(block);
            }
         }

         List<RemovedBlock> tops = surface(level, blocks);
         List<RemovedBlock> pool = tops.isEmpty() ? blocks : tops;
         int chunks = Math.min(CHUNKS_PER_WAVE.getI(), 1 + blocks.size() / 30);
         int life = CHUNK_LIFE.getI();
         double sizeScale = CHUNK_SIZE.get();

         for (int i = 0; i < chunks; i++) {
            boolean fromStanding = !standing.isEmpty() && i % 2 == 1;
            List<RemovedBlock> from = fromStanding ? standing : pool;
            RemovedBlock blockx = from.get(random.nextInt(from.size()));
            BlockPos p = blockx.pos();
            long column = columnKey(p);
            int floor = floors.getOrDefault(column, floorOf.getOrDefault(column, p.getY()));
            Vec3 inward = new Vec3(center.x - (double)p.getX() - 0.5, 0.0, center.z - (double)p.getZ() - 0.5);
            inward = inward.lengthSqr() < 1.0E-4 ? Vec3.ZERO : inward.normalize().scale(0.025 + random.nextDouble() * 0.04);
            double lift = fromStanding ? 0.0 : 0.01 + random.nextDouble() * 0.02;
            Vec3 velocity = inward.add((random.nextDouble() - 0.5) * 0.03, lift, (random.nextDouble() - 0.5) * 0.03);
            boolean slab = i == 0 && blocks.size() > 50;
            if (slab) {
               double w = (1.3 + random.nextDouble() * 0.7) * sizeScale;
               double h = (0.5 + random.nextDouble() * 0.25) * sizeScale;
               throwChunk(level, blockx, w, h, w * (0.8 + random.nextDouble() * 0.4), velocity, floor, life, 2.5F + random.nextFloat() * 3.5F);
            } else {
               double s = (0.45 + random.nextDouble() * 0.45) * sizeScale;
               throwChunk(level, blockx, s, s, s, velocity, floor, life, 6.0F + random.nextFloat() * 10.0F);
            }
         }

         int puffs = PUFFS_PER_WAVE.getI();
         puffs(level, pool, puffs, 5);
         puffs(level, standing, puffs / 2, 4);
         int step = Math.max(1, pool.size() / Math.max(1, puffs / 3));

         for (int ix = 0; ix < pool.size(); ix += step) {
            RemovedBlock blockx = pool.get(ix);
            Vec3 c = Vec3.atCenterOf(blockx.pos());
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, blockx.state()), c.x, c.y + 0.6, c.z, 5, 0.5, 0.3, 0.5, 0.0);
            level.sendParticles(ParticleTypes.ASH, c.x, c.y + 0.7, c.z, 3, 0.5, 0.4, 0.5, 0.0);
         }

         int drift = Math.max(1, standing.size() / Math.max(1, puffs / 4));

         for (int ix = 0; ix < standing.size(); ix += drift) {
            RemovedBlock blockx = standing.get(ix);
            Vec3 c = Vec3.atCenterOf(blockx.pos());
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, blockx.state()), c.x, c.y, c.z, 4, 0.5, 0.5, 0.5, 0.0);
         }

         Vec3 ring = Vec3.atCenterOf(pool.get(random.nextInt(pool.size())).pos());
         String area = areaKey(center);
         if (RuinFxClock.once(level, "ruin_crumble_" + area, 6)) {
            Fx.sound(level, ring, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 1.2F, 0.42F);
            Fx.sound(level, ring, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.0F, 0.5F);
         }

         if (!standing.isEmpty() && RuinFxClock.once(level, "ruin_topple_" + area, 12)) {
            RemovedBlock blockx = standing.get(random.nextInt(standing.size()));
            Fx.sound(level, Vec3.atCenterOf(blockx.pos()), blockx.state().getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.1F, 0.5F);
         }

         if (RuinFxClock.once(level, "ruin_ringshake_" + area, 8)) {
            Fx.shake(level, ring, 18.0, 0.12F, 0, 8);
         }
      }
   }

   static void collapseRumble(ServerLevel level, String key, Vec3 center, float progress) {
      float swell = 0.45F + 0.55F * Math.min(1.0F, progress * 2.5F);
      LoopSound.keepAt(level, key, center, (SoundEvent)AwakenedSounds.RUIN_RUMBLE.get(), SoundSource.BLOCKS, RUMBLE_VOLUME.getF() * swell, 0.9F);
   }

   static void collapseSettle(ServerLevel level, Vec3 center, double radius, int depth) {
      Vfx.effect(VfxEntity.Kind.RUIN_PIT_HAZE, center.add(0.0, -0.2, 0.0))
         .scale(radius)
         .secondary(new Vec3(0.0, (double)depth, 0.0))
         .color(10130831, 2828326)
         .intensity(HAZE_INTENSITY.getF())
         .lifetime(HAZE_LIFE.getI())
         .spawn(level);
      float density = ASHFALL.getF();
      if (!(density <= 0.0F)) {
         RandomSource random = level.random;
         int points = Math.max(2, (int)Math.round(radius / 6.0 * (double)density));
         RuinFxClock.every(level, 220, t -> {
            if (t % 2 == 0) {
               float fade = t < 110 ? 1.0F : (float)(220 - t) / 110.0F;
               int n = Math.max(1, Math.round((float)points * fade));

               for (int i = 0; i < n; i++) {
                  double a = random.nextDouble() * Math.PI * 2.0;
                  double r = radius * Math.sqrt(random.nextDouble());
                  double x = center.x + Math.cos(a) * r;
                  double z = center.z + Math.sin(a) * r;
                  double y = center.y + 7.0 * (0.5 + random.nextDouble());
                  level.sendParticles(ParticleTypes.ASH, x, y, z, 3, 1.4, 0.6, 1.4, 0.0);
                  if (i % 2 == 0) {
                     level.sendParticles(ParticleTypes.WHITE_ASH, x, y, z, 1, 1.2, 0.5, 1.2, 0.0);
                  }
               }
            }
         });
      }
   }

   static void collapseLanding(ServerLevel level, LivingEntity body) {
      Vec3 feet = body.position();
      double width = Math.max(1.2, (double)body.getBbWidth() * 2.2);
      Vfx.effect(VfxEntity.Kind.RUIN_CRACK_FLASH, feet.add(0.0, 0.05, 0.0)).scale(width).color(2828326, 10130831).lifetime(14).spawn(level);
      RuinCracks.stamp(level, feet, width * 0.5, 8, 20, 9);
      Vec3 at = feet.add(0.0, (double)body.getBbHeight() * 0.5, 0.0);
      level.sendParticles(
         ParticleTypes.ASH, at.x, at.y, at.z, 24, (double)body.getBbWidth() * 0.6, (double)body.getBbHeight() * 0.4, (double)body.getBbWidth() * 0.6, 0.03
      );
      level.sendParticles(
         ParticleTypes.ASH, at.x, at.y, at.z, 16, (double)body.getBbWidth() * 0.5, (double)body.getBbHeight() * 0.4, (double)body.getBbWidth() * 0.5, 0.02
      );
      Fx.sound(level, feet, SoundEvents.DRIPSTONE_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 0.5F);
      Fx.Floor floor = Fx.floorUnder(level, feet.x, feet.y - 0.1, feet.z, 3);
      if (floor != null) {
         Fx.sound(level, feet, floor.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 0.8F, 0.6F);
      }
   }

   static void collapsePaid(ServerPlayer caster, int bodies) {
      float volume = Math.min(1.0F, 0.6F + 0.1F * (float)(bodies - 1));
      Fx.soundOnlyFor(caster, caster.getEyePosition(), SoundEvents.BONE_BLOCK_BREAK, SoundSource.PLAYERS, volume, 0.55F);
   }

   static void collapseRise(ServerLevel level, int y, List<RemovedBlock> blocks) {
      if (!blocks.isEmpty()) {
         settle(level, blocks, PUFFS_PER_WAVE.getI() / 2);
         Vec3 mid = centre(blocks);
         String area = areaKey(mid);
         if (RuinFxClock.once(level, "ruin_grind_" + area, 12)) {
            Fx.sound(level, mid, (SoundEvent)AwakenedSounds.RUIN_GRIND.get(), SoundSource.BLOCKS, 0.55F, 0.85F + level.random.nextFloat() * 0.15F);
         }

         int rise = 140;
         if (RuinFxClock.once(level, "ruin_rise_" + area, rise)) {
            double extent = extent(blocks, mid);
            String key = "ruin_rise_rumble_" + area;
            RandomSource random = level.random;
            RuinFxClock.every(level, rise + 10, t -> {
               float env = t < 10 ? (float)t / 10.0F : Math.max(0.0F, (float)(rise + 10 - t) / 20.0F);
               LoopSound.keepAt(level, key, mid, (SoundEvent)AwakenedSounds.RUIN_RUMBLE.get(), SoundSource.BLOCKS, 0.5F * Math.min(1.0F, env), 0.8F);
               if (t % 3 == 0 && extent > 0.5) {
                  double a = random.nextDouble() * Math.PI * 2.0;
                  double r = extent * Math.sqrt(random.nextDouble());
                  level.sendParticles(ParticleTypes.ASH, mid.x + Math.cos(a) * r, mid.y + 3.0, mid.z + Math.sin(a) * r, 5, 1.5, 1.5, 1.5, 0.01);
               }
            });
         }
      }
   }

   static void collapseClosed(ServerLevel level, Vec3 center, double radius) {
      RandomSource random = level.random;
      int points = 12 + (int)radius;

      for (int i = 0; i < points; i++) {
         double a = random.nextDouble() * Math.PI * 2.0;
         double r = radius * (0.3 + 0.65 * Math.sqrt(random.nextDouble()));
         double x = center.x + Math.cos(a) * r;
         double z = center.z + Math.sin(a) * r;
         Fx.Floor floor = Fx.floorState(level, x, center.y - 1.0, z);
         double y = floor == null ? center.y : floor.topY();
         level.sendParticles(ParticleTypes.WHITE_ASH, x, y + 0.2, z, 6, 0.8, 0.2, 0.8, 0.02);
         if (floor != null && i % 3 == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), x, y + 0.1, z, 4, 0.4, 0.1, 0.4, 0.04);
         }
      }

      Fx.sound(level, center, (SoundEvent)AwakenedSounds.RUIN_GRIND.get(), SoundSource.BLOCKS, 0.7F, 0.7F);
      Fx.sound(level, center, SoundEvents.DEEPSLATE_PLACE, SoundSource.BLOCKS, 1.2F, 0.5F);
      Fx.shake(level, center, radius + 10.0, 0.1F, 0, 10);
   }

   private static void slamChunks(ServerLevel level, Vec3 ground, BlockState state) {
      int chunks = SLAM_CHUNKS.getI();
      RandomSource random = level.random;

      for (int i = 0; i < chunks; i++) {
         double a = ((double)i + random.nextDouble() * 0.6) * Math.PI * 2.0 / (double)Math.max(1, chunks);
         double out = 0.5 + random.nextDouble() * 0.5;
         double in = 0.04 + random.nextDouble() * 0.04;
         double size = 0.3 + random.nextDouble() * 0.22;
         Vec3 from = ground.add(Math.cos(a) * out, size * 0.5, Math.sin(a) * out);
         Vec3 velocity = new Vec3(-Math.cos(a) * in, 0.02 + random.nextDouble() * 0.03, -Math.sin(a) * in);
         Vfx.blockThrown(
            level,
            from,
            state,
            size,
            size,
            size,
            30 + random.nextInt(14),
            random.nextFloat() * 360.0F,
            velocity,
            ground.y + size * 0.1,
            4.0F + random.nextFloat() * 6.0F
         );
      }
   }

   private static void pourAround(ServerLevel level, Vec3 center, double groundY, BlockState state, float scale) {
      int n = Math.round(16.0F * scale);
      double r = 2.2;

      for (int i = 0; i < n; i++) {
         double a = (Math.PI * 2) * (double)i / (double)Math.max(1, n) + level.random.nextDouble() * 0.3;
         double x = center.x + Math.cos(a) * r;
         double z = center.z + Math.sin(a) * r;
         level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, state), x, groundY + 0.4, z, 4, 0.4, 0.2, 0.4, 0.0);
         level.sendParticles(ParticleTypes.ASH, x, groundY + 0.8, z, 2, 0.4, 0.3, 0.4, 0.0);
      }
   }

   static String rumbleKey(ServerPlayer player) {
      return "ruin_collapse_" + player.getUUID();
   }

   private static float ease(float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      return 1.0F - (1.0F - t) * (1.0F - t);
   }

   static String areaKey(Vec3 at) {
      return (Mth.floor(at.x) >> 3) + "_" + (Mth.floor(at.z) >> 3);
   }

   private static void puffs(ServerLevel level, List<RemovedBlock> blocks, int max, int perPuff) {
      if (!blocks.isEmpty() && max > 0) {
         int step = Math.max(1, blocks.size() / max);

         for (int i = 0; i < blocks.size(); i += step) {
            RemovedBlock block = blocks.get(i);
            Vec3 c = Vec3.atCenterOf(block.pos());
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, block.state()), c.x, c.y + 0.4, c.z, perPuff, 0.4, 0.3, 0.4, 0.05);
         }
      }
   }

   static void settle(ServerLevel level, List<RemovedBlock> blocks, int max) {
      if (!blocks.isEmpty() && max > 0) {
         int step = Math.max(1, blocks.size() / max);

         for (int i = 0; i < blocks.size(); i += step) {
            RemovedBlock block = blocks.get(i);
            Vec3 c = Vec3.atCenterOf(block.pos());
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, block.state()), c.x, c.y + 1.2, c.z, 3, 0.4, 0.3, 0.4, 0.0);
            level.sendParticles(ParticleTypes.ASH, c.x, c.y + 1.0, c.z, 2, 0.4, 0.3, 0.4, 0.01);
         }
      }
   }

   static void throwChunk(ServerLevel level, RemovedBlock block, double sx, double sy, double sz, Vec3 velocity, int floorY, int life, float tumble) {
      Vec3 from = Vec3.atCenterOf(block.pos());
      BlockState state = block.state();
      Vfx.blockThrown(level, from, state, sx, sy, sz, life, level.random.nextFloat() * 360.0F, velocity, (double)floorY + sy * 0.5, tumble);
   }

   static Map<Long, Integer> columnFloors(List<RemovedBlock> blocks) {
      Map<Long, Integer> floors = new HashMap<>();

      for (RemovedBlock block : blocks) {
         floors.merge(columnKey(block.pos()), block.pos().getY(), Math::min);
      }

      return floors;
   }

   private static List<RemovedBlock> surface(ServerLevel level, List<RemovedBlock> blocks) {
      Map<Long, RemovedBlock> tops = new HashMap<>();

      for (RemovedBlock block : blocks) {
         if (!block.state().getCollisionShape(level, block.pos()).isEmpty()) {
            tops.merge(columnKey(block.pos()), block, (a, b) -> (RemovedBlock)(a.pos().getY() >= b.pos().getY() ? a : b));
         }
      }

      return new ArrayList<>(tops.values());
   }

   static List<RemovedBlock> solid(ServerLevel level, List<RemovedBlock> blocks) {
      List<RemovedBlock> out = new ArrayList<>(blocks.size());

      for (RemovedBlock block : blocks) {
         if (!block.state().getCollisionShape(level, block.pos()).isEmpty()) {
            out.add(block);
         }
      }

      return out;
   }

   static long columnKey(BlockPos p) {
      return (long)p.getX() << 32 ^ (long)p.getZ() & 4294967295L;
   }

   static Vec3 centre(List<RemovedBlock> blocks) {
      double x = 0.0;
      double y = 0.0;
      double z = 0.0;

      for (RemovedBlock block : blocks) {
         x += (double)block.pos().getX();
         y += (double)block.pos().getY();
         z += (double)block.pos().getZ();
      }

      int n = Math.max(1, blocks.size());
      return new Vec3(x / (double)n + 0.5, y / (double)n + 0.5, z / (double)n + 0.5);
   }

   private static double extent(List<RemovedBlock> blocks, Vec3 mid) {
      double far = 0.0;

      for (RemovedBlock block : blocks) {
         double dx = (double)block.pos().getX() + 0.5 - mid.x;
         double dz = (double)block.pos().getZ() + 0.5 - mid.z;
         far = Math.max(far, dx * dx + dz * dz);
      }

      return Math.sqrt(far);
   }
}
