package com.awakenedmod.awakened.client.chrono;

import com.awakenedmod.awakened.client.chrono.ending.FilmBurnEnding;
import com.awakenedmod.awakened.content.chronokinetic.ChronoSounds;
import com.awakenedmod.awakened.core.net.ChronoFxPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class ChronoCues {
   private static final float HITS_BASE = 0.6F;
   private static final float HITS_PER = 0.11F;
   private static final float HITS_MAX = 1.0F;
   private static final int RUN_CLEARANCE = 7;
   private static final float RUN_PITCH = 0.08F;

   private ChronoCues() {
   }

   static void started(ClientLevel level, ChronoFxPayload p) {
      ChronoSounds.stop().playLocal(level, p.pos(), 1.0F, 1.0F);
   }

   static void tick(ClientLevel level) {
      for (ChronoFxClient.Effect e : ChronoFxClient.effects(ChronoFxPayload.Kind.TIME_STOP)) {
         if (e.ended()) {
            if (!e.resumed) {
               e.resumed = true;
               resume(level, e);
            }
         } else if (e.resolve < 0) {
            if (ChronoTimeStop.steadyTick(ChronoFxClient.now() - e.start)) {
               ChronoSounds.tickTower().playLocal(level, e.pos, 1.0F, 1.0F);
               e.lastSteady = ChronoFxClient.now();
            }
         } else {
            int r = ChronoFxClient.now() - e.resolve;
            if (r == swellAt()) {
               e.swell = playSwell(e.pos);
            }

            finaleTicks(level, e, r);
         }
      }
   }

   static void resolved(ClientLevel level, ChronoFxClient.Effect e) {
      if (ChronoFxClient.now() - e.lastSteady >= 7) {
         finaleTicks(level, e, 0);
      }

      if (swellAt() == 0) {
         e.swell = playSwell(e.pos);
      }
   }

   private static void finaleTicks(ClientLevel level, ChronoFxClient.Effect e, int r) {
      int[] run = ChronoTimeStop.FINALE_TICKS;

      for (int i = 0; i < run.length; i++) {
         if (run[i] == r) {
            float pitch = 1.0F + 0.08F * (float)i / (float)Math.max(1, run.length - 1);
            ChronoSounds.tickTower().playLocal(level, e.pos, 1.0F, pitch);
         }
      }
   }

   private static int swellAt() {
      return Math.max(0, 15 - Math.round(14.0F));
   }

   private static SoundInstance playSwell(Vec3 pos) {
      ChronoSounds.Layer layer = ChronoSounds.resumeSwell().layers().get(0);
      SoundInstance sound = new SimpleSoundInstance(
         layer.sound().get(), SoundSource.PLAYERS, Math.min(1.0F, layer.volume()), layer.pitch(), SoundInstance.createUnseededRandom(), pos.x, pos.y, pos.z
      );
      Minecraft.getInstance().getSoundManager().play(sound);
      return sound;
   }

   private static void resume(ClientLevel level, ChronoFxClient.Effect e) {
      if (e.swell != null) {
         Minecraft.getInstance().getSoundManager().stop(e.swell);
         e.swell = null;
      }

      FilmBurnEnding.released(level, e);
      int impacts = ChronoFxClient.impactsOf(e);
      if (impacts > 0) {
         float volume = Mth.clamp(0.6F + 0.11F * (float)(impacts - 1), 0.0F, 1.0F);
         ChronoSounds.heldHits().playLocal(level, e.pos, volume, 1.0F);
      }
   }
}
