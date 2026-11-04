package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.BlockCracks;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.LoopSound;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class HeavyHandFx {
   private static final String HUM = "graviton_heavy_hand_bend_";
   private static final int PIN_CREAK_EVERY = 12;
   private static final double CHIP_REACH = 2.4;
   private static final int HOLD_KNOCK_EVERY = 6;
   private static final int HOLD_CHIP_EVERY = 5;

   private HeavyHandFx() {
   }

   public static BlockState chip(BlockState floor) {
      return !floor.is(Blocks.GRASS_BLOCK)
            && !floor.is(Blocks.MYCELIUM)
            && !floor.is(Blocks.PODZOL)
            && !floor.is(Blocks.MOSS_BLOCK)
            && !floor.is(Blocks.DIRT_PATH)
         ? floor
         : Blocks.DIRT.defaultBlockState();
   }

   private static Object bodyCracks(LivingEntity body) {
      return BlockCracks.owner(body, "heavy_hand_bend_body");
   }

   static void press(ServerLevel level, ServerPlayer caster, LivingEntity target) {
      Fx.sound(level, centre(target), (SoundEvent)AwakenedSounds.HAND_GRIP.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
   }

   static void holding(ServerLevel level, ServerPlayer caster, LivingEntity target, boolean lift, float f, int ticks) {
      LoopSound.keep(
         target,
         "graviton_heavy_hand_bend_" + caster.getId(),
         (SoundEvent)AwakenedSounds.HAND_BEND.get(),
         SoundSource.PLAYERS,
         0.45F + 0.35F * f,
         (lift ? 0.95F : 0.75F) + 0.35F * f
      );
      if (target instanceof ServerPlayer victim && ticks % 3 == 0) {
         Fx.shakeFor(victim, victim.getEyePosition(), 4.0, 0.04F + 0.08F * f, 2, 4);
      }

      Fx.Floor floor = Fx.floorUnder(level, target.getX(), target.getY() + 0.2, target.getZ(), 3);
      if (floor != null) {
         if (ticks % 5 == 1) {
            double a = level.random.nextDouble() * Math.PI * 2.0;
            double r = 2.4 + (double)target.getBbWidth() * 0.5;
            Vec3 from = new Vec3(target.getX() + Math.cos(a) * r, floor.topY() + 0.12, target.getZ() + Math.sin(a) * r);
            double flight = 7.0;
            Vec3 v = new Vec3(-Math.cos(a) * r / flight, lift ? 0.34 : 0.16, -Math.sin(a) * r / flight);
            Vfx.blockThrown(level, from, chip(floor.state()), 0.2, 0.2, 0.2, 26, (float)(level.random.nextDouble() * 360.0), v, floor.topY() + 0.1, 30.0F);
         }

         if (ticks % 6 == 0) {
            Fx.sound(
               level,
               new Vec3(target.getX(), floor.topY(), target.getZ()),
               floor.state().getSoundType().getHitSound(),
               SoundSource.PLAYERS,
               0.2F + 0.2F * f,
               0.6F
            );
         }
      }
   }

   static void holdEnd(ServerPlayer caster, @Nullable LivingEntity target) {
      if (target != null) {
         LoopSound.stop(target, "graviton_heavy_hand_bend_" + caster.getId());
      }
   }

   static void fizzle(ServerLevel level, ServerPlayer caster, @Nullable LivingEntity target) {
      boolean here = target != null && target.level() == level;
      Fx.sound(level, here ? centre(target) : caster.position(), (SoundEvent)AwakenedSounds.SHEET_RELEASE.get(), SoundSource.PLAYERS, 0.4F, 1.5F);
      if (here) {
         dirt(level, target.position(), 4, 0.05);
      }
   }

   static void yank(ServerLevel level, ServerPlayer caster, LivingEntity target, Vec3 wasFeet) {
      dirt(level, wasFeet, 8, 0.15);
      Fx.Floor floor = Fx.floorUnder(level, wasFeet.x, wasFeet.y + 0.2, wasFeet.z, 3);
      if (floor != null) {
         Vec3 toCaster = caster.position().subtract(wasFeet).multiply(1.0, 0.0, 1.0);
         Vec3 dir = toCaster.lengthSqr() < 1.0E-6 ? Vec3.ZERO : toCaster.normalize();

         for (int i = 0; i < 2; i++) {
            Vec3 v = dir.scale(0.35 + 0.1 * (double)i).add(0.0, 0.3, 0.0);
            Vfx.blockThrown(
               level,
               wasFeet.add(0.0, 0.15, 0.0),
               chip(floor.state()),
               0.25,
               0.25,
               0.25,
               30,
               (float)(level.random.nextDouble() * 360.0),
               v,
               floor.topY() + 0.12,
               28.0F
            );
         }
      }

      Fx.sound(level, wasFeet.add(caster.position()).scale(0.5), (SoundEvent)AwakenedSounds.HAND_YANK.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
   }

   static void shove(ServerLevel level, ServerPlayer caster, LivingEntity target) {
      Vec3 feet = target.position();
      Fx.sound(level, centre(target), (SoundEvent)AwakenedSounds.HAND_SHOVE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
      dirt(level, feet, 6, 0.12);
   }

   static void sliding(ServerLevel level, LivingEntity target, int ticks) {
      Fx.Floor floor = Fx.floorUnder(level, target.getX(), target.getY() + 0.2, target.getZ(), 2);
      if (floor != null && !(target.getY() - floor.topY() > 0.6)) {
         if (ticks % 2 == 0) {
            level.sendParticles(
               new BlockParticleOption(ParticleTypes.BLOCK, chip(floor.state())), target.getX(), floor.topY() + 0.05, target.getZ(), 2, 0.2, 0.02, 0.2, 0.1
            );
         }

         BlockPos under = BlockCracks.floorTop(level, target.getX(), target.getY(), target.getZ());
         if (under != null) {
            BlockCracks.crack(level, bodyCracks(target), under, 3, BlockCracks.Timing.countDown(16));
         }
      }
   }

   static void commit(ServerLevel level, ServerPlayer caster, LivingEntity target, boolean lift) {
      if (!lift) {
         Fx.sound(level, centre(target), (SoundEvent)AwakenedSounds.PULL.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
      }
   }

   static void dragging(ServerLevel level, ServerPlayer caster, LivingEntity target, int ticks) {
      LoopSound.keep(
         target,
         "graviton_heavy_hand_bend_" + caster.getId(),
         (SoundEvent)AwakenedSounds.HAND_BEND.get(),
         SoundSource.PLAYERS,
         0.8F,
         Math.max(0.55F, 0.95F - (float)ticks * 0.02F)
      );
   }

   static void crush(ServerLevel level, ServerPlayer caster, LivingEntity target, double floorY) {
      Vec3 feet = new Vec3(target.getX(), floorY, target.getZ());
      double w = Math.max(0.5, (double)target.getBbWidth() * 0.6);
      Fx.Floor floor = Fx.floorUnder(level, feet.x, feet.y + 0.5, feet.z, 3);
      if (floor != null) {
         BlockState chip = chip(floor.state());
         double spin = level.random.nextDouble() * Math.PI * 2.0;

         for (int i = 0; i < 8; i++) {
            double a = spin + (Math.PI * 2) * (double)i / 8.0;
            double r = w + 1.4;
            Vec3 from = new Vec3(feet.x + Math.cos(a) * r, floor.topY() + 0.3, feet.z + Math.sin(a) * r);
            Vec3 v = new Vec3(-Math.cos(a) * r / 3.0, 0.02, -Math.sin(a) * r / 3.0);
            Vfx.blockThrown(level, from, chip, 0.26, 0.26, 0.26, 30, (float)(level.random.nextDouble() * 360.0), v, floor.topY() + 0.13, 20.0F);
         }

         level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, chip), feet.x, floor.topY() + 0.1, feet.z, 12, w * 0.6, 0.05, w * 0.6, 0.18);
         Fx.sound(level, feet, floor.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 0.8F, 0.5F);
      }

      LoopSound.stop(target, "graviton_heavy_hand_bend_" + caster.getId());
      Fx.shake(level, feet, 8.0, 0.24F, 2, 8);
      Fx.sound(level, feet, (SoundEvent)AwakenedSounds.HAND_CRUSH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
   }

   static void pinned(ServerLevel level, LivingEntity target, int ticks) {
      if (ticks % 12 == 6) {
         Fx.sound(level, target.position(), (SoundEvent)AwakenedSounds.STONE_CREAK.get(), SoundSource.PLAYERS, 0.6F, 0.9F);
      }
   }

   static void unpinned(ServerLevel level, LivingEntity target) {
      dirt(level, target.position(), 5, 0.15);
      Fx.sound(level, centre(target), (SoundEvent)AwakenedSounds.SHEET_RELEASE.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
   }

   static void pinLost(LivingEntity target) {
      BlockCracks.clear(bodyCracks(target));
   }

   static void launch(ServerLevel level, ServerPlayer caster, LivingEntity target) {
      Vec3 feet = target.position();
      Fx.Floor floor = Fx.floorUnder(level, feet.x, feet.y + 0.2, feet.z, 3);
      if (floor != null) {
         BlockState chip = chip(floor.state());

         for (int i = 0; i < 5; i++) {
            double a = (Math.PI * 2) * (double)i / 5.0 + level.random.nextDouble() * 0.6;
            Vec3 v = new Vec3(Math.cos(a) * 0.05, 0.6 + level.random.nextDouble() * 0.2, Math.sin(a) * 0.05);
            Vfx.blockThrown(
               level,
               feet.add(Math.cos(a) * 0.35, 0.1, Math.sin(a) * 0.35),
               chip,
               0.24,
               0.24,
               0.24,
               45,
               (float)(level.random.nextDouble() * 360.0),
               v,
               floor.topY() + 0.12,
               25.0F
            );
         }

         Fx.sound(level, feet, floor.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 0.6F, 0.8F);
      }

      Fx.sound(level, feet, (SoundEvent)AwakenedSounds.HAND_LAUNCH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
   }

   static void apex(ServerLevel level, LivingEntity target) {
      Fx.sound(level, centre(target), (SoundEvent)AwakenedSounds.MASS_HEAVIER.get(), SoundSource.PLAYERS, 0.5F, 0.8F);
   }

   static void landed(ServerLevel level, LivingEntity target) {
      Vec3 feet = target.position();
      dirt(level, feet, 10, 0.18);
      BlockCracks.disc(level, bodyCracks(target), feet, 0.8, d -> 4, BlockCracks.Timing.holdThenFade(10, 20));
      Fx.Floor floor = Fx.floorUnder(level, feet.x, feet.y + 0.2, feet.z, 3);
      if (floor != null) {
         Fx.sound(level, feet, floor.state().getSoundType().getFallSound(), SoundSource.PLAYERS, 1.0F, 0.7F);
      }

      Fx.sound(level, feet, (SoundEvent)AwakenedSounds.MASS_LAND.get(), SoundSource.PLAYERS, 0.6F, 1.15F);
   }

   static void released(LivingEntity target) {
      BlockCracks.release(bodyCracks(target), 10);
   }

   private static Vec3 centre(LivingEntity e) {
      return e.position().add(0.0, (double)e.getBbHeight() * 0.5, 0.0);
   }

   private static void dirt(ServerLevel level, Vec3 feet, int count, double speed) {
      Fx.Floor floor = Fx.floorUnder(level, feet.x, feet.y + 0.2, feet.z, 3);
      if (floor != null) {
         level.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK, chip(floor.state())), feet.x, floor.topY() + 0.1, feet.z, count, 0.3, 0.05, 0.3, speed
         );
      }
   }
}
