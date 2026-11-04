package com.awakenedmod.awakened.content.stormcaller;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.LoopSound;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

final class TeslaVoice {
   private static final String CRACKLE = "tesla_hum";
   private static final String BUZZ = "tesla_buzz";
   private static final float BODY_VOLUME = 0.22F;
   private static final float TARGET_GAIN = 1.3F;
   private static final float TARGET_PITCH = 1.06F;
   private static final float SNAP_VOLUME = 0.16F;
   private static final int SNAP_GAP_MIN = 8;
   private static final int SNAP_GAP_MAX = 20;
   private static final float IGNITE_VOLUME = 0.11F;
   private static final float FIZZLE_VOLUME = 0.1F;
   private static final int SIZZLE_EVERY = 10;

   private TeslaVoice() {
   }

   static void start(ServerPlayer player) {
      edge(player, (SoundEvent)AwakenedSounds.TESLA_IGNITE.get(), 0.11F);
   }

   static void body(TeslaVoice.Cue cue) {
      TeslaVoice.Clock clock = cue.clock();
      float gain = clock.hitting ? 1.3F : 1.0F;
      float pitch = clock.hitting ? 1.06F : 1.0F;
      bed(cue, "tesla_hum", (SoundEvent)AwakenedSounds.TESLA_CRACKLE.get(), 0.22F * gain, pitch);
      bed(cue, "tesla_buzz", (SoundEvent)AwakenedSounds.TESLA_BUZZ.get(), 0.22F * gain, pitch);
      if (cue.t() >= clock.nextSnap) {
         Fx.sound(
            cue.level(),
            cue.along(0.2),
            (SoundEvent)AwakenedSounds.TESLA_SNAP.get(),
            SoundSource.PLAYERS,
            0.16F,
            Mth.lerp(cue.random().nextFloat(), 0.9F, 1.15F)
         );
         clock.nextSnap = cue.t() + Mth.nextInt(cue.random(), 8, 20);
      }
   }

   static void end(ServerPlayer player) {
      silence(player);
      edge(player, (SoundEvent)AwakenedSounds.TESLA_FIZZLE.get(), 0.1F);
   }

   static void silence(ServerPlayer player) {
      LoopSound.stop(player, "tesla_hum");
      LoopSound.stop(player, "tesla_buzz");
   }

   static void hit(TeslaVoice.Cue cue, boolean hitAny) {
      TeslaVoice.Clock clock = cue.clock();
      if (hitAny && !clock.hitting) {
         Fx.sound(cue.level(), cue.end(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.3F, 2.0F);
         clock.lastSizzle = cue.t();
      } else if (hitAny && cue.t() - clock.lastSizzle >= 10) {
         Fx.sound(cue.level(), cue.end(), SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.PLAYERS, 0.25F, 1.6F + cue.random().nextFloat() * 0.4F);
         clock.lastSizzle = cue.t();
      }

      clock.hitting = hitAny;
   }

   private static void bed(TeslaVoice.Cue cue, String key, SoundEvent sound, float volume, float pitch) {
      LoopSound.keepOnBeam(cue.player(), key, 18.0, sound, SoundSource.PLAYERS, volume, pitch);
   }

   private static void edge(ServerPlayer player, SoundEvent sound, float volume) {
      Fx.sound(player.serverLevel(), player.position(), sound, SoundSource.PLAYERS, volume, 1.0F);
   }

   static final class Clock {
      private int nextSnap;
      private int lastSizzle;
      private boolean hitting;
   }

   static record Cue(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 end, int t, TeslaVoice.Clock clock) {
      RandomSource random() {
         return this.level.getRandom();
      }

      Vec3 along(double from) {
         return this.eye.lerp(this.end, from + this.random().nextDouble() * (1.0 - from));
      }
   }
}
