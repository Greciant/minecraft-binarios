package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.core.caster.Caster;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

public final class PyroBeamSounds {
   public static final PyroBeamSounds.Cue[] IGNITION = new PyroBeamSounds.Cue[]{PyroBeamSounds.Cue.of(AwakenedSounds.FLAME_BEAM_IGNITE, 0.43F, 1.0F)};
   public static final PyroBeamSounds.Cue[] BODY_HIT = new PyroBeamSounds.Cue[]{
      PyroBeamSounds.Cue.of(AwakenedSounds.FLAME_BEAM_IMPACT, 0.27F, 0.85F), PyroBeamSounds.Cue.of(SoundEvents.GENERIC_BURN, 0.35F, 0.9F)
   };
   public static final PyroBeamSounds.Cue[] WALL_HIT = new PyroBeamSounds.Cue[]{PyroBeamSounds.Cue.of(AwakenedSounds.FLAME_BEAM_IMPACT, 0.22F, 1.15F)};
   public static final PyroBeamSounds.Cue[] SHUTDOWN = new PyroBeamSounds.Cue[]{
      PyroBeamSounds.Cue.of(AwakenedSounds.FLAME_BEAM_SHUTDOWN, 0.33F, 1.0F).ramps(0.3F)
   };
   public static final PyroBeamSounds.Cue[] SHUTDOWN_DRY = new PyroBeamSounds.Cue[]{PyroBeamSounds.Cue.of(AwakenedSounds.FLAME_BEAM_SPUTTER, 0.43F, 1.0F)};
   public static final PyroBeamSounds.Cue POP = PyroBeamSounds.Cue.of(AwakenedSounds.FLAME_BEAM_CRACKLE, 0.13F, 0.9F).jitter(0.3F);
   public static final PyroBeamSounds.Cue GOUT = PyroBeamSounds.Cue.of(AwakenedSounds.FLAME_BEAM_GOUT, 0.13F, 0.85F).jitter(0.25F);
   public static final PyroBeamSounds.Cue SURGE = PyroBeamSounds.Cue.of(AwakenedSounds.FLAME_BEAM_GOUT, 0.13F, 0.6F).jitter(0.15F);
   public static final PyroBeamSounds.Cue THROB = PyroBeamSounds.Cue.of(AwakenedSounds.FLAME_BEAM_THROB, 0.22F, 0.95F).jitter(0.1F);

   private PyroBeamSounds() {
   }

   public static void play(Caster caster, Vec3 at, float ramp, float scale, FlameTone tone, PyroBeamSounds.Cue[] cues) {
      for (PyroBeamSounds.Cue cue : cues) {
         PyroFlames.sound(caster, at, cue.sound().get(), (cue.volume() + cue.rampGain() * ramp) * scale, cue.pitch(), tone);
      }
   }

   public static record Cue(Supplier<SoundEvent> sound, float volume, float pitch, float jitter, float rampGain) {
      public static PyroBeamSounds.Cue of(SoundEvent sound, float volume, float pitch) {
         return of((Supplier<SoundEvent>)(() -> sound), volume, pitch);
      }

      public static PyroBeamSounds.Cue of(Supplier<SoundEvent> sound, float volume, float pitch) {
         return new PyroBeamSounds.Cue(sound, volume, pitch, 0.0F, 0.0F);
      }

      public PyroBeamSounds.Cue jitter(float range) {
         return new PyroBeamSounds.Cue(this.sound, this.volume, this.pitch, range, this.rampGain);
      }

      public PyroBeamSounds.Cue ramps(float gain) {
         return new PyroBeamSounds.Cue(this.sound, this.volume, this.pitch, this.jitter, gain);
      }

      public float pitchAt(float u) {
         return this.pitch + this.jitter * u;
      }
   }
}
