package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.LoopSound;
import com.awakenedmod.awakened.core.terrain.RemovedBlock;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class RuinBreachFx {
   private static final Lab.Value CHUNKS = Lab.SERVER.def("ruin.breach.chunks", 5.0, 0.0, 16.0, 1.0);
   private static final Lab.Value BORE_CHUNKS = Lab.SERVER.def("ruin.breach.bore_chunks", 0.0, 0.0, 6.0, 1.0);
   private static final Lab.Value BORE_RIM = Lab.SERVER.def("ruin.breach.bore_rim", 3.0, 0.0, 12.0, 1.0);
   private static final Lab.Value BORE_PUFFS = Lab.SERVER.def("ruin.breach.bore_puffs", 4.0, 0.0, 24.0, 1.0);
   private static final Lab.Value PRESS_SHAKE = Lab.SERVER.def("ruin.breach.press_shake", 0.14, 0.0, 0.6, 0.02);
   private static final Lab.Value CRACKS = Lab.SERVER.def("ruin.breach.cracks", 40.0, 0.0, 120.0, 1.0);
   private static final Lab.Value RIM = Lab.SERVER.def("ruin.breach.rim", 10.0, 0.0, 40.0, 1.0);
   private static final Lab.Value RIM_STAGE = Lab.SERVER.def("ruin.breach.rim_stage", 5.0, 1.0, 9.0, 1.0);
   private static final Lab.Value POUR = Lab.SERVER.def("ruin.breach.pour", 1.0, 0.0, 3.0, 0.1);
   private static final Lab.Value POUR_LIFE = Lab.SERVER.def("ruin.breach.pour_life", 80.0, 0.0, 200.0, 1.0);
   private static final Lab.Value THROUGH = Lab.SERVER.def("ruin.breach.through", 1.0, 0.0, 3.0, 0.1);
   private static final Lab.Value GRIND_VOL = Lab.SERVER.def("ruin.breach.grind_vol", 1.0, 0.0, 3.0, 0.05);
   private static final Lab.Value GRIND_PITCH = Lab.SERVER.def("ruin.breach.grind_pitch", 1.45, 0.5, 2.0, 0.05);
   private static final int PUFFS = 24;
   private static final double PRESS_SHAKE_RADIUS = 10.0;
   private static final int PRESS_SHAKE_TICKS = 2;
   private static final int PRESS_SHAKE_FADE = 6;
   private static final int CHUNK_LIFE = 50;
   private static final int CRACK_BURST_LIFE = 14;
   private static final int RIM_OPEN = 3;
   private static final int RIM_CLOSE = 8;
   private static final int POUR_EVERY = 5;
   private static final int CRUMBLE_EVERY = 5;
   private static final int CRUNCH_BUCKET = 6;
   private static final int CRUNCH_WINDOW = 3;
   private static final int GRIND_SWELL = 5;
   private static final double PALM_SIDE = 0.34;
   private static final double PALM_DROP = 0.3;
   private static final double PALM_REACH = 0.85;
   private static final int SNAP_STAGE = 8;
   private static final DustParticleOptions ASH_CLOUD = Fx.dust(10130831, 2.5F);

   private RuinBreachFx() {
   }

   public static void preloadLabKeys() {
   }

   static void press(ServerLevel level, ServerPlayer player, Vec3 faceCenter, Vec3 normal) {
      RuinFx.slamHand(player);
      Vec3 at = faceCenter.add(normal.scale(0.06));
      Vfx.effect(VfxEntity.Kind.RUIN_CRACK_FLASH, at).scale(1.4).color(2828326, 10130831).lifetime(14).along(normal).spawn(level);
      level.sendParticles(ParticleTypes.ASH, at.x, at.y, at.z, 30, 0.4, 0.4, 0.4, 0.01);
      level.sendParticles(ParticleTypes.WHITE_ASH, at.x, at.y, at.z, 8, 0.35, 0.35, 0.35, 0.02);
      RuinFx.handAsh(level, at.add(normal.scale(0.3)), 3.0F);
      Fx.sound(level, at, SoundEvents.DRIPSTONE_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 0.55F);
      Fx.sound(level, at, SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 0.8F, 0.5F);
      float shake = PRESS_SHAKE.getF();
      if (shake > 0.0F) {
         Fx.shake(level, at, 10.0, shake, 2, 6);
      }
   }

   static void crackAhead(ServerLevel level, List<BlockPos> positions, int ticks) {
      int life = Math.max(1, ticks);
      forSolid(level, positions, pos -> RuinCracks.sweep(level, pos, 9, 0, life, 0, 1));
   }

   static void snapAhead(ServerLevel level, List<BlockPos> positions, int ticks) {
      int hold = Math.max(0, ticks - 1);
      forSolid(level, positions, pos -> RuinCracks.snap(level, pos, 8, 0, hold));
   }

   private static void forSolid(ServerLevel level, List<BlockPos> positions, Consumer<BlockPos> crack) {
      int cracks = CRACKS.getI();

      for (BlockPos pos : positions) {
         if (cracks <= 0) {
            return;
         }

         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && !state.getCollisionShape(level, pos).isEmpty()) {
            crack.accept(pos);
            cracks--;
         }
      }
   }

   static void opened(ServerLevel level, List<RemovedBlock> blocks, int openTicks, Predicate<BlockPos> carving, @Nullable LongSet boreRim) {
      if (!blocks.isEmpty()) {
         boolean bore = boreRim != null;
         crumble(level, blocks, bore ? BORE_PUFFS.getI() : 24, bore ? BORE_CHUNKS.getI() : CHUNKS.getI());
         rim(level, blocks, openTicks, carving, boreRim);
         pour(level, blocks);
         Vec3 mid = RuinGroundFx.centre(blocks);
         if (RuinFxClock.once(level, "ruin_breach_crumble_" + RuinGroundFx.areaKey(mid), 5)) {
            float pitch = 0.5F + 0.08F * level.random.nextFloat();
            Fx.sound(level, mid, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, bore ? 0.8F : 1.1F, pitch);
            Fx.sound(level, mid, SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, bore ? 0.6F : 0.9F, pitch + 0.05F);
         }
      }
   }

   private static void crumble(ServerLevel level, List<RemovedBlock> blocks, int puffs, int chunks) {
      RandomSource random = level.random;
      int step = Math.max(1, blocks.size() / Math.max(1, puffs));

      for (int i = 0; puffs > 0 && i < blocks.size(); i += step) {
         RemovedBlock block = blocks.get(i);
         Vec3 c = Vec3.atCenterOf(block.pos());
         level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, block.state()), c.x, c.y, c.z, 8, 0.35, 0.35, 0.35, 0.05);
         level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, block.state()), c.x, c.y, c.z, 4, 0.4, 0.4, 0.4, 0.0);
         level.sendParticles(ParticleTypes.ASH, c.x, c.y, c.z, 4, 0.4, 0.4, 0.4, 0.01);
      }

      List<RemovedBlock> solid = RuinGroundFx.solid(level, blocks);
      Map<Long, Integer> floorOf = RuinGroundFx.columnFloors(blocks);
      int n = Math.min(chunks, solid.size());

      for (int i = 0; i < n; i++) {
         RemovedBlock block = solid.get(random.nextInt(solid.size()));
         int floor = floorOf.getOrDefault(RuinGroundFx.columnKey(block.pos()), block.pos().getY());
         double size = 0.4 + random.nextDouble() * 0.3;
         Vec3 velocity = new Vec3((random.nextDouble() - 0.5) * 0.08, 0.03 + random.nextDouble() * 0.05, (random.nextDouble() - 0.5) * 0.08);
         RuinGroundFx.throwChunk(level, block, size, size, size, velocity, floor, 50, 6.0F + random.nextFloat() * 10.0F);
      }
   }

   private static void rim(ServerLevel level, List<RemovedBlock> blocks, int openTicks, Predicate<BlockPos> carving, @Nullable LongSet boreRim) {
      int max = boreRim != null ? BORE_RIM.getI() : RIM.getI();
      if (max > 0) {
         LongSet gone = new LongOpenHashSet();
         blocks.forEach(b -> gone.add(b.pos().asLong()));
         LongSet seen = new LongOpenHashSet();
         List<BlockPos> rim = new ArrayList<>();

         for (RemovedBlock block : blocks) {
            for (Direction d : Direction.values()) {
               BlockPos n = block.pos().relative(d);
               long key = n.asLong();
               if (!gone.contains(key) && seen.add(key) && !carving.test(n) && (boreRim == null || !boreRim.contains(key))) {
                  BlockState state = level.getBlockState(n);
                  if (!state.isAir() && !state.getCollisionShape(level, n).isEmpty()) {
                     rim.add(n);
                  }
               }
            }
         }

         int stage = RIM_STAGE.getI();
         int hold = Math.max(0, openTicks - 3 - 8);
         int step = Math.max(1, (rim.size() + max - 1) / max);
         int offset = rim.isEmpty() ? 0 : level.random.nextInt(step);

         for (int i = offset; i < rim.size(); i += step) {
            BlockPos pos = rim.get(i);
            int peak = stage - level.random.nextInt(2);
            if (boreRim != null) {
               boreRim.add(pos.asLong());
               RuinCracks.snap(level, pos, peak, 0, hold + 3);
            } else {
               RuinCracks.sweep(level, pos, peak, 0, 3, hold, 8);
            }
         }
      }
   }

   private static void pour(ServerLevel level, List<RemovedBlock> blocks) {
      float density = POUR.getF();
      int life = POUR_LIFE.getI();
      if (!(density <= 0.0F) && life > 0) {
         List<BlockPos> tops = new ArrayList<>();
         Map<Long, Integer> topOf = new HashMap<>();

         for (RemovedBlock block : blocks) {
            topOf.merge(RuinGroundFx.columnKey(block.pos()), block.pos().getY(), Math::max);
         }

         for (RemovedBlock block : blocks) {
            if (topOf.get(RuinGroundFx.columnKey(block.pos())) == block.pos().getY()) {
               tops.add(block.pos());
            }
         }

         int count = Math.max(1, Math.round(density * 2.0F));
         RuinFxClock.every(
            level,
            life,
            t -> {
               if (t % 5 == 0 && !tops.isEmpty()) {
                  BlockPos top = tops.get(level.random.nextInt(tops.size()));
                  BlockPos ceiling = top.above();
                  BlockState state = level.getBlockState(ceiling);
                  if (!state.isAir() && state.getFluidState().isEmpty()) {
                     double y = (double)top.getY() + 0.95;
                     level.sendParticles(
                        new BlockParticleOption(ParticleTypes.FALLING_DUST, state),
                        (double)top.getX() + 0.5,
                        y,
                        (double)top.getZ() + 0.5,
                        count,
                        0.35,
                        0.02,
                        0.35,
                        0.0
                     );
                  }
               }
            }
         );
      }
   }

   static void restored(ServerLevel level, List<RemovedBlock> blocks) {
      RuinGroundFx.settle(level, blocks, 12);
      Vec3 mid = RuinGroundFx.centre(blocks);
      if (RuinFxClock.once(level, "ruin_grind_" + RuinGroundFx.areaKey(mid), 12)) {
         Fx.sound(level, mid, (SoundEvent)AwakenedSounds.RUIN_GRIND.get(), SoundSource.BLOCKS, 0.4F, 0.9F);
      }
   }

   static void breakthrough(ServerLevel level, Vec3 at, Vec3 dir, BlockState last) {
      float k = THROUGH.getF();
      if (!(k <= 0.0F)) {
         int n = Math.max(1, Math.round(6.0F * k));
         RandomSource random = level.random;

         for (int i = 0; i < n; i++) {
            double push = 0.08 + random.nextDouble() * 0.12;
            double sx = (random.nextDouble() - 0.5) * 0.08;
            double sy = (random.nextDouble() - 0.5) * 0.06;
            double sz = (random.nextDouble() - 0.5) * 0.08;
            level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 0, dir.x * push + sx, dir.y * push + sy + 0.01, dir.z * push + sz, 1.0);
         }

         level.sendParticles(ASH_CLOUD, at.x, at.y, at.z, n * 2, 0.8, 0.8, 0.8, 0.0);
         level.sendParticles(ParticleTypes.ASH, at.x, at.y, at.z, n * 4, 0.9, 0.9, 0.9, 0.02);
         if (!last.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, last), at.x, at.y, at.z, n * 3, 0.6, 0.6, 0.6, 0.12);
         }

         Fx.sound(level, at, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.1F, 0.6F);
         Fx.sound(level, at, SoundEvents.TUFF_BREAK, SoundSource.PLAYERS, 0.8F, 0.5F);
      }
   }

   static void boreTick(ServerLevel level, ServerPlayer player, String key, Vec3 head, int age, boolean moving) {
      float env = Math.min(1.0F, (float)(age + 1) / 5.0F) * (moving ? 1.0F : 0.55F);
      LoopSound.keepAt(level, key, head, (SoundEvent)AwakenedSounds.RUIN_RUMBLE.get(), SoundSource.PLAYERS, GRIND_VOL.getF() * env, GRIND_PITCH.getF());
      if (moving && RuinFx.beat(age, key.hashCode(), 6, 3)) {
         BlockPos at = BlockPos.containing(head);
         BlockState state = level.getBlockState(at);
         if (!state.isAir()) {
            Fx.sound(level, head, state.getSoundType().getBreakSound(), SoundSource.PLAYERS, 0.9F, 0.5F + 0.1F * level.random.nextFloat());
         }
      }

      if (age % 4 == 0) {
         Vec3 palm = AimUtil.shoulderHand(player, false, 0.34, 0.3, 0.85);
         RuinFx.handAsh(level, palm, 0.6F);
      }
   }

   static void boreStop(ServerLevel level, String key) {
      LoopSound.stopAt(level, key);
   }
}
