package com.awakenedmod.awakened.content.chronokinetic;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.LoopSound;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ChronoSounds {
   private static final ChronoSounds.Cue STOP = cue(AwakenedSounds.CHRONO_STOP, 1.0F);
   private static final ChronoSounds.Cue TICK_TOWER = cue(AwakenedSounds.CHRONO_TICK_TOWER, 1.0F);
   private static final ChronoSounds.Cue RESUME_SWELL = cue(AwakenedSounds.CHRONO_RESUME_SWELL, 1.0F);
   private static final ChronoSounds.Cue HELD_HITS = cue(AwakenedSounds.CHRONO_HELD_HITS, 1.0F);
   private static final ChronoSounds.Cue STOP_BURN = cue(AwakenedSounds.CHRONO_STOP_BURN, 1.0F);
   private static final ChronoSounds.Cue TICK_ESCAPE = cue(AwakenedSounds.CHRONO_TICK_ESCAPE, 1.0F);
   private static final ChronoSounds.Cue STASIS_KEY_WIND = cue(AwakenedSounds.CHRONO_STASIS_KEY_WIND, 1.0F);
   private static final ChronoSounds.Cue STASIS_KEY_TURN = cue(AwakenedSounds.CHRONO_STASIS_KEY_TURN, 1.0F);
   private static final ChronoSounds.Cue STASIS_KEY_RELEASE = cue(AwakenedSounds.CHRONO_STASIS_KEY_RELEASE, 1.0F);
   private static final ChronoSounds.Cue REWIND = cue(AwakenedSounds.CHRONO_REWIND, 1.0F);
   private static final ChronoSounds.Cue SKIP = cue(AwakenedSounds.CHRONO_SKIP, 1.0F);
   private static final ChronoSounds.Layer WATCH_LOOP = new ChronoSounds.Layer(AwakenedSounds.CHRONO_WATCH_LOOP, 1.0F, 1.0F);
   public static final float RESUME_SWELL_SECONDS = 0.7F;
   public static final float STASIS_KEY_WIND_GAP_SECONDS = 0.08F;
   public static final float STASIS_KEY_SPRING_SECONDS = 0.15F;
   private static final String WATCH_KEY = "chrono_watch";
   private static final double WATCH_RATE_FROM = 16.0;
   private static final double WATCH_RATE_TO = 1000.0;
   private static final float WATCH_PITCH_FROM = 0.8F;
   private static final float WATCH_PITCH_TO = 2.0F;
   private static final float WATCH_VOLUME_FROM = 0.45F;
   private static final float WATCH_VOLUME_TO = 0.9F;
   private static final float WATCH_REWIND_PITCH = 0.9F;

   private ChronoSounds() {
   }

   private static ChronoSounds.Cue cue(Supplier<SoundEvent> sound, float volume) {
      return new ChronoSounds.Cue(List.of(new ChronoSounds.Layer(sound, volume, 1.0F)));
   }

   public static ChronoSounds.Cue stop() {
      return STOP;
   }

   public static ChronoSounds.Cue tickTower() {
      return TICK_TOWER;
   }

   public static ChronoSounds.Cue resumeSwell() {
      return RESUME_SWELL;
   }

   public static ChronoSounds.Cue stopBurn() {
      return STOP_BURN;
   }

   public static ChronoSounds.Cue heldHits() {
      return HELD_HITS;
   }

   public static ChronoSounds.Cue tickEscape() {
      return TICK_ESCAPE;
   }

   public static ChronoSounds.Cue stasisKeyWind() {
      return STASIS_KEY_WIND;
   }

   public static ChronoSounds.Cue stasisKeyTurn() {
      return STASIS_KEY_TURN;
   }

   public static ChronoSounds.Cue stasisKeyRelease() {
      return STASIS_KEY_RELEASE;
   }

   public static ChronoSounds.Cue rewind() {
      return REWIND;
   }

   public static ChronoSounds.Cue skip() {
      return SKIP;
   }

   public static void keepWatchLoop(Entity holder, long rate, boolean rewind) {
      double span = Math.log(62.5);
      float t = (float)Mth.clamp(Math.log((double)Math.max(1L, rate) / 16.0) / span, 0.0, 1.0);
      float pitch = Mth.lerp(t, 0.8F, 2.0F) * (rewind ? 0.9F : 1.0F);
      float volume = Mth.lerp(t, 0.45F, 0.9F) * WATCH_LOOP.volume();
      LoopSound.keep(holder, "chrono_watch", WATCH_LOOP.sound().get(), SoundSource.PLAYERS, volume, pitch);
   }

   public static void stopWatchLoop(Entity holder) {
      LoopSound.stop(holder, "chrono_watch");
   }

   public static record Cue(List<ChronoSounds.Layer> layers) {
      public void play(Level level, Vec3 pos) {
         this.play(level, pos, 1.0F);
      }

      public void play(Level level, Vec3 pos, float volumeScale) {
         for (ChronoSounds.Layer layer : this.layers) {
            Fx.sound(level, pos, layer.sound().get(), SoundSource.PLAYERS, layer.volume() * volumeScale, layer.pitch());
         }
      }

      public void playLocal(Level level, Vec3 pos, float volumeScale, float pitchScale) {
         for (ChronoSounds.Layer layer : this.layers) {
            level.playLocalSound(
               pos.x, pos.y, pos.z, layer.sound().get(), SoundSource.PLAYERS, Math.min(1.0F, layer.volume() * volumeScale), layer.pitch() * pitchScale, false
            );
         }
      }
   }

   public static record Layer(Supplier<SoundEvent> sound, float volume, float pitch) {
   }
}
