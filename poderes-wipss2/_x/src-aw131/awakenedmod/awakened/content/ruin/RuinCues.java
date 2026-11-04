package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class RuinCues {
   static final int[] DEEPER_AT = new int[]{5, 10, 20};
   private static final float[] LANDED_PITCH = new float[]{0.8F, 0.68F, 0.58F, 0.5F};
   private static final double AWAKEN_RING = 2.5;
   private static final int AWAKEN_CRACK_PEAK = 7;
   private static final int AWAKEN_CRACK_HOLD = 60;
   private static final int AWAKEN_CRACK_BLOCKS = 25;
   private static final int AWAKEN_ASH_TICKS = 40;
   private static final int AWAKEN_ARM_HOLD = 30;
   private static final int AWAKEN_ARM_FADE = 30;
   private static final double HAND_SIDE = 0.34;
   private static final double HAND_DROP = 0.45;
   private static final double HAND_REACH = 0.5;
   private static final int CRUMBLE_SETTLE_TICKS = 10;
   private static final DustParticleOptions CRACK_DUST = Fx.dust(2828326, 0.9F);

   private RuinCues() {
   }

   public static void stackLanded(ServerLevel level, LivingEntity target, Vec3 at) {
      int depth = depth(RuinStacks.stacks(target));
      Fx.burst(level, CRACK_DUST, at, 4 + 2 * depth, 0.18, 0.03);
      Fx.burst(level, ParticleTypes.WHITE_ASH, at, 3 + depth, 0.2, 0.02);
      Fx.sound(level, at, SoundEvents.TUFF_BREAK, SoundSource.PLAYERS, 0.55F, LANDED_PITCH[depth]);
      if (depth > 0) {
         Fx.sound(level, at, SoundEvents.DEEPSLATE_HIT, SoundSource.PLAYERS, 0.35F + 0.1F * (float)depth, LANDED_PITCH[depth] - 0.05F);
      }
   }

   static int depth(int stacks) {
      int depth = 0;

      for (int at : DEEPER_AT) {
         if (stacks >= at) {
            depth++;
         }
      }

      return depth;
   }

   static void tense(ServerLevel level, Vec3 at) {
      Fx.sound(level, at, SoundEvents.TUFF_STEP, SoundSource.PLAYERS, 0.45F, 0.5F);
      Fx.sound(level, at, SoundEvents.GRAVEL_HIT, SoundSource.PLAYERS, 0.3F, 0.45F);
      Fx.burst(level, CRACK_DUST, at.add(0.0, 0.1, 0.0), 3, 0.25, 0.0);
   }

   static void crumble(ServerLevel level, Vec3 feet) {
      Fx.sound(level, feet, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 0.9F, 0.55F);
      Fx.sound(level, feet, SoundEvents.TUFF_BREAK, SoundSource.PLAYERS, 0.7F, 0.5F);
      RuinFxClock.after(level, 10, () -> Fx.sound(level, feet, SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.8F, 0.5F));
   }

   static void awaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 feet = player.position();
      Fx.Floor floor = Fx.floorUnder(level, feet.x, feet.y + 0.2, feet.z, 3);
      if (floor != null) {
         Vec3 ground = new Vec3(feet.x, floor.topY(), feet.z);
         RuinCracks.stamp(level, ground, 2.5, 7, 60, 25);
         level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR, floor.state()), ground.x, ground.y + 0.1, ground.z, 16, 1.0, 0.05, 1.0, 0.0);
      }

      RuinArm.pulse(player, 1.0F, 30, 30);
      Fx.sound(level, feet, (SoundEvent)AwakenedSounds.RUIN_GRIND.get(), SoundSource.PLAYERS, 0.8F, 0.8F);
      Fx.sound(level, feet, SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 1.0F, 0.45F);
      RuinFxClock.every(level, 40, t -> {
         if (t % 2 == 0 && !player.isRemoved()) {
            for (boolean off : new boolean[]{false, true}) {
               Vec3 hand = AimUtil.shoulderHand(player, off, 0.34, 0.45, 0.5);
               level.sendParticles(ParticleTypes.SMOKE, hand.x, hand.y, hand.z, 0, 0.0, 0.05, 0.0, 1.0);
               level.sendParticles(ParticleTypes.WHITE_ASH, hand.x, hand.y + 0.1, hand.z, 2, 0.12, 0.15, 0.12, 0.02);
               level.sendParticles(ParticleTypes.ASH, hand.x, hand.y, hand.z, 2, 0.1, 0.1, 0.1, 0.01);
            }
         }
      });
   }
}
