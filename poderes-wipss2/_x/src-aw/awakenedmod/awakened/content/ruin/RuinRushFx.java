package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.LoopSound;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class RuinRushFx {
   private static final Lab.Value TRAIL_PEAK = Lab.SERVER.def("ruin.rush.trail_peak", 7.0, 2.0, 9.0, 1.0);
   private static final Lab.Value TRAIL_WIDTH = Lab.SERVER.def("ruin.rush.trail_width", 0.45, 0.0, 1.5, 0.05);
   private static final Lab.Value TRAIL_OPEN = Lab.SERVER.def("ruin.rush.trail_open", 3.0, 1.0, 12.0, 1.0);
   private static final Lab.Value TRAIL_HOLD = Lab.SERVER.def("ruin.rush.trail_hold", 30.0, 0.0, 120.0, 1.0);
   private static final Lab.Value TRAIL_CELLS = Lab.SERVER.def("ruin.rush.trail_cells", 6.0, 1.0, 16.0, 1.0);
   private static final Lab.Value CHUNK_EVERY = Lab.SERVER.def("ruin.rush.chunk_every", 5.0, 0.0, 20.0, 1.0);
   private static final Lab.Value CHUNK_SIZE = Lab.SERVER.def("ruin.rush.chunk_size", 0.26, 0.1, 0.6, 0.02);
   private static final Lab.Value HAND_SIDE = Lab.SERVER.def("ruin.rush.hand_side", 0.42, 0.0, 1.0, 0.02);
   private static final Lab.Value HAND_BACK = Lab.SERVER.def("ruin.rush.hand_back", 0.3, -0.5, 1.0, 0.05);
   private static final Lab.Value PLANT = Lab.SERVER.def("ruin.rush.plant", 1.7, 0.5, 4.0, 0.1);
   private static final Lab.Value SKID_VOL = Lab.SERVER.def("ruin.rush.skid_vol", 0.7, 0.0, 1.5, 0.05);
   private static final Lab.Value RUMBLE_VOL = Lab.SERVER.def("ruin.rush.rumble_vol", 0.45, 0.0, 1.5, 0.05);
   private static final Lab.Value RUMBLE_PITCH = Lab.SERVER.def("ruin.rush.rumble_pitch", 1.35, 0.5, 2.0, 0.05);
   private static final Lab.Value THUD_SHAKE = Lab.SERVER.def("ruin.rush.thud_shake", 0.12, 0.0, 0.5, 0.02);
   private static final Lab.Value SCRAPE_VOL = Lab.SERVER.def("ruin.drag.scrape_vol", 0.6, 0.0, 1.5, 0.05);
   private static final Lab.Value SCRAPE_PITCH = Lab.SERVER.def("ruin.drag.scrape_pitch", 0.5, 0.3, 1.2, 0.05);
   private static final Lab.Value DRAG_GRIT = Lab.SERVER.def("ruin.drag.grit", 3.0, 0.0, 8.0, 1.0);
   private static final Lab.Value DRAG_CRUMBLE = Lab.SERVER.def("ruin.drag.crumble", 2.0, 0.0, 8.0, 1.0);
   private static final Lab.Value DRAG_CHUNK_EVERY = Lab.SERVER.def("ruin.drag.chunk_every", 4.0, 0.0, 20.0, 1.0);
   private static final Lab.Value DRAG_CRACK_GROW = Lab.SERVER.def("ruin.drag.crack_grow", 40.0, 5.0, 160.0, 1.0);
   private static final Lab.Value SLAM_SHAKE = Lab.SERVER.def("ruin.slam.shake", 0.32, 0.0, 0.8, 0.02);
   private static final Lab.Value SLAM_CHIPS = Lab.SERVER.def("ruin.slam.chips", 26.0, 0.0, 60.0, 1.0);
   private static final Lab.Value SLAM_WALL_RADIUS = Lab.SERVER.def("ruin.slam.wall_radius", 2.0, 0.0, 4.0, 1.0);
   private static final Lab.Value SLAM_WALL_CHUNKS = Lab.SERVER.def("ruin.slam.wall_chunks", 6.0, 0.0, 16.0, 1.0);
   private static final Lab.Value SLAM_CHUNK_SIZE = Lab.SERVER.def("ruin.slam.chunk_size", 0.34, 0.1, 0.8, 0.02);
   private static final String SKID_LOOP = "ruin_rush_skid";
   private static final String RUMBLE_LOOP = "ruin_rush_rumble";
   private static final String DRAG_LOOP = "ruin_rush_drag";
   private static final ParticleOptions FLAKE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.LIGHT_GRAY_CONCRETE_POWDER.defaultBlockState());
   private static final ParticleOptions DARK_FLAKE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.TUFF.defaultBlockState());
   private static final float RUSH_ROT = 0.7F;
   private static final float DRAG_ROT = 0.9F;
   private static final double TRAIL_STEP = 0.5;
   private static final int TRAIL_CLOSE = 10;
   private static final int STRIDE_BUCKET = 6;
   private static final int STRIDE_WINDOW = 3;
   private static final int CHUNK_WINDOW = 3;
   private static final int CHUNK_LIFE = 28;
   private static final int WALL_CHUNK_LIFE = 44;
   private static final int DRAG_CRACK_HOLD = 34;
   private static final int CONTACT_CRACK_PEAK = 4;
   private static final int WALL_CRACK_HOLD = 40;
   private static final int BUMP_CRACK_PEAK = 6;
   private static final int BUMP_CRACK_HOLD = 30;
   private static final double FOOT_CRACK_RADIUS = 1.3;
   private static final int FOOT_CRACK_BLOCKS = 7;
   private static final double PLANT_DEAD_MUL = 1.5;
   private static final int PLANT_CRACK_HOLD = 30;
   private static final int PLANT_CRACK_BLOCKS = 13;

   private RuinRushFx() {
   }

   static void preloadLabKeys() {
   }

   static void start(ServerLevel level, ServerPlayer caster, Vec3 dir) {
      RuinArm.set(caster, 0.7F);
      Vec3 feet = caster.position();
      Fx.Floor floor = Fx.floorUnder(level, feet.x, feet.y - 0.2, feet.z, 3);
      if (floor != null) {
         Fx.debrisFan(level, new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), feet.add(0.0, 0.1, 0.0), 10, 15.0, 50.0, 0.12, 0.3);
         RuinCracks.stamp(level, new Vec3(feet.x, floor.topY(), feet.z), 0.8, 6, 20, 4);
      }

      Fx.burst(level, RuinFx.ASH_DUST, feet.add(0.0, 0.3, 0.0), 8, 0.3, 0.03);
      Fx.sound(level, feet, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 0.8F, 0.5F);
      Fx.sound(level, feet, SoundEvents.DEEPSLATE_STEP, SoundSource.PLAYERS, 0.7F, 0.6F);
   }

   static void trail(ServerLevel level, ServerPlayer caster, Vec3 from, Vec3 to, Vec3 dir, int dashTicks) {
      LoopSound.keep(caster, "ruin_rush_skid", SoundEvents.BRUSH_GRAVEL, SoundSource.PLAYERS, SKID_VOL.getF(), 0.8F);
      LoopSound.keep(caster, "ruin_rush_rumble", (SoundEvent)AwakenedSounds.RUIN_RUMBLE.get(), SoundSource.PLAYERS, RUMBLE_VOL.getF(), RUMBLE_PITCH.getF());
      Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
      Vec3 offset = side.scale(HAND_SIDE.get()).subtract(dir.scale(HAND_BACK.get()));
      wake(level, from, to, side, offset);
      Vec3 handNow = to.add(offset);
      Fx.Floor floor = Fx.floorUnder(level, handNow.x, handNow.y + 0.6, handNow.z, 4);
      if (floor != null) {
         Vec3 hand = new Vec3(handNow.x, floor.topY(), handNow.z);
         RandomSource r = level.random;
         level.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK, floor.state()),
            hand.x,
            hand.y + 0.1,
            hand.z,
            0,
            -dir.x * 0.22 + (r.nextDouble() - 0.5) * 0.1,
            0.16 + r.nextDouble() * 0.1,
            -dir.z * 0.22 + (r.nextDouble() - 0.5) * 0.1,
            1.0
         );
         level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, floor.state()), hand.x, hand.y + 0.25, hand.z, 2, 0.25, 0.08, 0.25, 0.0);
         Vec3 mid = from.add(to).scale(0.5).add(0.0, 0.9, 0.0);
         level.sendParticles(ParticleTypes.ASH, mid.x, mid.y, mid.z, 2, 0.3, 0.45, 0.3, 0.015);
         if (dashTicks % 3 == 0) {
            RuinFx.handAsh(level, hand.add(0.0, 0.25, 0.0), 1.0F);
         }

         int every = CHUNK_EVERY.getI();
         if (every > 0 && RuinFx.beat(dashTicks, caster.getId(), every, Math.min(every, 3))) {
            Vec3 velocity = new Vec3(
               -dir.x * 0.12 + side.x * (r.nextDouble() - 0.3) * 0.12, 0.24 + r.nextDouble() * 0.1, -dir.z * 0.12 + side.z * (r.nextDouble() - 0.3) * 0.12
            );
            chunk(level, hand.add(0.0, 0.15, 0.0), floor.state(), CHUNK_SIZE.get(), velocity, hand.y, 28);
         }

         if (RuinFx.beat(dashTicks, caster.getId(), 6, 3)) {
            Fx.sound(level, hand, SoundEvents.GRAVEL_STEP, SoundSource.PLAYERS, 0.5F, 0.6F + 0.1F * r.nextFloat());
            Fx.sound(level, hand, floor.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 0.45F, 0.55F + 0.1F * r.nextFloat());
         }
      }
   }

   static void pass(ServerLevel level, LivingEntity body) {
      Vec3 feet = body.position();
      Vec3 chest = feet.add(0.0, (double)body.getBbHeight() * 0.5, 0.0);
      Fx.Floor floor = Fx.floorUnder(level, feet.x, feet.y + 0.2, feet.z, 3);
      if (floor != null) {
         Fx.debrisFan(level, new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), feet.add(0.0, 0.15, 0.0), 8, 20.0, 60.0, 0.1, 0.28);
      }

      Fx.burst(level, RuinFx.CRACK_DUST, chest, 10, 0.3, 0.03);
      Fx.burst(level, ParticleTypes.WHITE_ASH, chest, 8, 0.35, 0.02);
      Fx.sound(level, chest, SoundEvents.DEEPSLATE_HIT, SoundSource.PLAYERS, 0.6F, 0.6F);
   }

   static void bump(ServerLevel level, ServerPlayer caster, @Nullable BlockHitResult wall) {
      Vec3 at = wall != null ? wall.getLocation() : caster.position().add(0.0, (double)caster.getBbHeight() * 0.5, 0.0);
      if (wall != null) {
         BlockState state = level.getBlockState(wall.getBlockPos());
         RuinCracks.wall(level, wall.getBlockPos(), wall.getDirection(), 1, 6, 30);
         Fx.blockSpall(level, at, state, 8);
         level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, state), at.x, at.y + 0.4, at.z, 6, 0.4, 0.3, 0.4, 0.0);
      }

      thud(level, caster, at, 0.8F);
   }

   static void end(ServerLevel level, ServerPlayer caster, boolean stoppedDead) {
      silence(caster);
      Vec3 feet = caster.position();
      double radius = PLANT.get() * (stoppedDead ? 1.5 : 1.0);
      Fx.Floor floor = Fx.floorUnder(level, feet.x, feet.y + 0.2, feet.z, 3);
      if (floor != null) {
         Vec3 ground = new Vec3(feet.x, floor.topY(), feet.z);
         RuinCracks.stamp(level, ground, radius * 0.6, stoppedDead ? 8 : 6, 30, 13);
         level.sendParticles(
            new BlockParticleOption(ParticleTypes.DUST_PILLAR, floor.state()),
            ground.x,
            ground.y + 0.1,
            ground.z,
            stoppedDead ? 24 : 12,
            radius * 0.3,
            0.05,
            radius * 0.3,
            0.0
         );
         Fx.debrisFan(
            level,
            new BlockParticleOption(ParticleTypes.BLOCK, floor.state()),
            ground.add(0.0, 0.1, 0.0),
            stoppedDead ? 22 : 12,
            15.0,
            55.0,
            0.12,
            stoppedDead ? 0.4 : 0.3
         );
      }

      Fx.burst(level, RuinFx.ASH_DUST, feet.add(0.0, 0.2, 0.0), stoppedDead ? 12 : 6, 0.35, 0.02);
      Fx.burst(level, ParticleTypes.WHITE_ASH, feet.add(0.0, 0.4, 0.0), stoppedDead ? 14 : 8, 0.5, 0.02);
      Fx.sound(level, feet, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 0.8F, 0.5F);
      Fx.sound(level, feet, SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.6F, 0.55F);
      if (stoppedDead) {
         Fx.sound(level, feet, SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 0.9F, 0.45F);
         Fx.shake(level, feet, 14.0, 0.18F, 1, 8);
      }
   }

   static void silence(ServerPlayer caster) {
      loopsOff(caster);
      RuinArm.off(caster);
   }

   private static void loopsOff(ServerPlayer caster) {
      LoopSound.stop(caster, "ruin_rush_skid");
      LoopSound.stop(caster, "ruin_rush_rumble");
   }

   static void catchBody(ServerLevel level, ServerPlayer caster, LivingEntity body, Vec3 at) {
      RuinArm.set(caster, 0.9F);
      Fx.burst(level, RuinFx.CRACK_DUST, at, 10, 0.2, 0.04);
      Fx.burst(level, ParticleTypes.WHITE_ASH, at, 12, 0.3, 0.02);
      crumble(level, body, 6);
      Fx.sound(level, at, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 0.8F, 0.55F);
      thud(level, caster, at, 0.9F);
   }

   static void dragTick(ServerLevel level, ServerPlayer caster, LivingEntity body, Vec3 from, Vec3 to, Vec3 dir, int dragTicks) {
      LoopSound.keep(body, "ruin_rush_drag", SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, SCRAPE_VOL.getF(), SCRAPE_PITCH.getF());
      RuinFx.bodyCrack(level, body, RuinStacks.touchPoint(caster, body), (float)dragTicks / (float)Math.max(1, DRAG_CRACK_GROW.getI()), dragTicks);
      crumble(level, body, DRAG_CRUMBLE.getI());
      BlockPos under = RuinCracks.solidUnder(level, BlockPos.containing(to.x, to.y - 0.05, to.z));
      if (under != null) {
         Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
         double half = (double)body.getBbWidth() * 0.35;
         crackFloor(level, to.add(side.scale(half)), 9, 34, true);
         crackFloor(level, to.subtract(side.scale(half)), 9, 34, true);
         BlockState floor = level.getBlockState(under);
         double y = to.y;
         RandomSource r = level.random;
         BlockParticleOption grit = new BlockParticleOption(ParticleTypes.BLOCK, floor);
         int i = 0;

         for (int n = DRAG_GRIT.getI(); i < n; i++) {
            double s = (r.nextDouble() - 0.5) * 2.0;
            level.sendParticles(
               grit,
               to.x + side.x * s * 0.3,
               y + 0.1,
               to.z + side.z * s * 0.3,
               0,
               dir.x * 0.25 + side.x * s * 0.2,
               0.18 + r.nextDouble() * 0.12,
               dir.z * 0.25 + side.z * s * 0.2,
               1.0
            );
         }

         level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, floor), to.x, y + 0.25, to.z, 2, 0.25, 0.08, 0.25, 0.0);
         if (dragTicks % 2 == 0) {
            Vec3 back = to.add(0.0, (double)body.getBbHeight() * 0.6, 0.0);
            level.sendParticles(ParticleTypes.ASH, back.x, back.y, back.z, 2, 0.25, 0.3, 0.25, 0.01);
         }

         i = DRAG_CHUNK_EVERY.getI();
         if (i > 0 && RuinFx.beat(dragTicks, body.getId(), i, Math.min(i, 3))) {
            double sign = r.nextBoolean() ? 1.0 : -1.0;
            double out = 0.12 + r.nextDouble() * 0.08;
            Vec3 velocity = new Vec3(side.x * sign * out + dir.x * 0.05, 0.22 + r.nextDouble() * 0.1, side.z * sign * out + dir.z * 0.05);
            Vec3 at = to.add(side.scale(sign * (half + 0.1))).add(0.0, 0.15, 0.0);
            chunk(level, at, floor, CHUNK_SIZE.get() * 1.15, velocity, y, 28);
         }
      }
   }

   static void dragSilence(LivingEntity body) {
      LoopSound.stop(body, "ruin_rush_drag");
   }

   static void dragRelease(ServerLevel level, LivingEntity body) {
      dragSilence(body);
      Vec3 feet = body.position();
      Fx.burst(level, RuinFx.ASH_DUST, feet.add(0.0, 0.3, 0.0), 6, 0.3, 0.02);
      Fx.sound(level, feet, SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.5F, 0.6F);
   }

   static void wallHit(ServerLevel level, ServerPlayer caster, LivingEntity body, @Nullable BlockHitResult wall) {
      loopsOff(caster);
      dragSilence(body);
      Vec3 at = impactPoint(body, wall);
      if (wall != null) {
         RuinCracks.wall(level, wall.getBlockPos(), wall.getDirection(), 1, 4, 7);
         Fx.blockSpall(level, at, level.getBlockState(wall.getBlockPos()), 6);
      }

      crumble(level, body, 4);
      thud(level, caster, at, 0.9F);
   }

   static void wallSlam(ServerLevel level, ServerPlayer caster, LivingEntity body, @Nullable BlockHitResult wall) {
      RuinFx.slamHand(caster);
      Vec3 at = impactPoint(body, wall);
      Vec3 chest = body.position().add(0.0, (double)body.getBbHeight() * 0.5, 0.0);
      if (wall != null) {
         BlockPos pos = wall.getBlockPos();
         BlockState state = level.getBlockState(pos);
         Direction face = wall.getDirection();
         Vec3 out = new Vec3((double)face.getStepX(), (double)face.getStepY(), (double)face.getStepZ());
         RuinCracks.wall(level, pos, face, SLAM_WALL_RADIUS.getI(), 9, 40);
         Fx.blockSpall(level, at, state, SLAM_CHIPS.getI());
         Fx.debrisFan(level, new BlockParticleOption(ParticleTypes.BLOCK, state), at.add(out.scale(0.1)), 12, -20.0, 60.0, 0.12, 0.3);
         wallChunks(level, at, out, state);
         BlockParticleOption dust = new BlockParticleOption(ParticleTypes.FALLING_DUST, state);
         level.sendParticles(dust, at.x, at.y, at.z, 16, 0.5, 0.6, 0.5, 0.0);
         level.sendParticles(dust, at.x, at.y + 1.0, at.z, 10, 0.6, 0.3, 0.6, 0.0);
         level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR, state), at.x, at.y - 0.4, at.z, 10, 0.4, 0.2, 0.4, 0.0);
      }

      Vec3 feet = body.position();
      Fx.Floor floor = Fx.floorUnder(level, feet.x, feet.y + 0.2, feet.z, 3);
      if (floor != null) {
         RuinCracks.stamp(level, new Vec3(feet.x, floor.topY(), feet.z), 1.3, 8, 40, 7);
      }

      crumble(level, body, 14);
      Fx.burst(level, RuinFx.CRACK_DUST, chest, 14, 0.3, 0.05);
      Fx.burst(level, ParticleTypes.WHITE_ASH, chest, 16, 0.4, 0.03);
      Fx.sound(level, at, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.1F, 0.6F);
      Fx.sound(level, at, SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 1.0F, 0.5F);
      Fx.sound(level, at, SoundEvents.TUFF_BREAK, SoundSource.PLAYERS, 0.8F, 0.55F);
      Fx.sound(level, at, SoundEvents.ANCIENT_DEBRIS_BREAK, SoundSource.PLAYERS, 0.7F, 0.5F);
      float shake = SLAM_SHAKE.getF();
      Fx.shake(level, at, 20.0, shake, 2, 12);
      Fx.shakeFor(caster, caster.getEyePosition(), 20.0, shake * 0.4F, 1, 8);
   }

   private static void wake(ServerLevel level, Vec3 from, Vec3 to, Vec3 side, Vec3 hand) {
      Vec3 seg = to.subtract(from);
      double length = seg.horizontalDistance();
      int steps = Math.max(1, Mth.ceil(length / 0.5));
      int peak = TRAIL_PEAK.getI();
      double width = TRAIL_WIDTH.get();
      int hold = TRAIL_HOLD.getI();
      int budget = TRAIL_CELLS.getI();

      for (int i = 1; i <= steps && budget > 0; i++) {
         Vec3 p = from.add(seg.scale((double)i / (double)steps));
         if (crackFloor(level, p.add(hand), 9, hold, true)) {
            budget--;
         }

         if (budget > 0 && crackFloor(level, p, peak, hold, true)) {
            budget--;
         }

         if (width > 0.0) {
            if (budget > 0 && crackFloor(level, p.add(side.scale(width)), peak - 2, hold, false)) {
               budget--;
            }

            if (budget > 0 && crackFloor(level, p.subtract(side.scale(width)), peak - 2, hold, false)) {
               budget--;
            }
         }
      }
   }

   private static boolean crackFloor(ServerLevel level, Vec3 feet, int peak, int hold, boolean crumble) {
      BlockPos pos = RuinCracks.solidUnder(level, BlockPos.containing(feet.x, feet.y + 0.4, feet.z));
      if (pos == null) {
         return false;
      } else {
         int open = TRAIL_OPEN.getI();
         if (!RuinFxClock.once(level, "ruin_rush_crack:" + pos.asLong(), open + hold + 10)) {
            return false;
         } else {
            RuinCracks.sweep(level, pos, peak, 0, open, hold, 10);
            if (crumble) {
               BlockPos at = pos.immutable();
               RuinFxClock.after(level, open, () -> crumbleBlock(level, at));
            }

            return true;
         }
      }
   }

   private static void crumbleBlock(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (!state.isAir()) {
         double x = (double)pos.getX() + 0.5;
         double y = (double)pos.getY() + 1.0;
         double z = (double)pos.getZ() + 0.5;
         level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y + 0.05, z, 4, 0.3, 0.04, 0.3, 0.05);
         level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, state), x, y + 0.3, z, 1, 0.3, 0.1, 0.3, 0.0);
      }
   }

   private static void crumble(ServerLevel level, LivingEntity body, int n) {
      if (n > 0) {
         AABB box = body.getBoundingBox();
         Vec3 c = box.getCenter();
         double w = box.getXsize() * 0.4;
         double h = box.getYsize() * 0.35;
         double d = box.getZsize() * 0.4;
         level.sendParticles(FLAKE, c.x, c.y, c.z, (n + 1) / 2, w, h, d, 0.02);
         level.sendParticles(DARK_FLAKE, c.x, c.y, c.z, n / 2 + 1, w, h, d, 0.02);
         level.sendParticles(RuinFx.GRIT_FALL, c.x, c.y + h * 0.5, c.z, Math.max(1, n / 2), w, h * 0.5, d, 0.0);
      }
   }

   private static void wallChunks(ServerLevel level, Vec3 at, Vec3 out, BlockState state) {
      int n = SLAM_WALL_CHUNKS.getI();
      if (n > 0) {
         Fx.Floor floor = Fx.floorUnder(level, at.x + out.x * 0.6, at.y, at.z + out.z * 0.6, 4);
         double restY = floor != null ? floor.topY() : at.y - 1.0;
         Vec3 u = Math.abs(out.y) > 0.5 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(-out.z, 0.0, out.x);
         Vec3 v = out.cross(u).normalize();
         RandomSource r = level.random;
         double size = SLAM_CHUNK_SIZE.get();

         for (int i = 0; i < n; i++) {
            double a = (r.nextDouble() - 0.5) * 1.6;
            double b = (r.nextDouble() - 0.5) * 1.6;
            Vec3 from = at.add(u.scale(a)).add(v.scale(b)).add(out.scale(0.15));
            Vec3 velocity = out.scale(0.06 + r.nextDouble() * 0.08).add(u.scale(a * 0.03)).add(0.0, 0.04, 0.0);
            chunk(level, from, state, size, velocity, restY, 44);
         }
      }
   }

   private static void chunk(ServerLevel level, Vec3 from, BlockState state, double size, Vec3 velocity, double floorY, int life) {
      RandomSource r = level.random;
      double s = size * (0.75 + r.nextDouble() * 0.5);
      Vfx.blockThrown(
         level,
         from,
         state,
         s,
         s * (0.7 + r.nextDouble() * 0.4),
         s,
         life + r.nextInt(10),
         r.nextFloat() * 360.0F,
         velocity,
         floorY + s * 0.1,
         8.0F + r.nextFloat() * 10.0F
      );
   }

   private static void thud(ServerLevel level, ServerPlayer caster, Vec3 at, float volume) {
      Fx.sound(level, at, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 0.7F * volume, 0.55F);
      Fx.sound(level, at, SoundEvents.DEEPSLATE_HIT, SoundSource.PLAYERS, 0.9F * volume, 0.45F);
      float shake = THUD_SHAKE.getF();
      Fx.shake(level, at, 10.0, shake, 1, 6);
      Fx.shakeFor(caster, caster.getEyePosition(), 10.0, shake * 0.5F, 1, 5);
   }

   private static Vec3 impactPoint(LivingEntity body, @Nullable BlockHitResult wall) {
      return wall != null ? wall.getLocation() : body.position().add(0.0, (double)body.getBbHeight() * 0.5, 0.0);
   }
}
