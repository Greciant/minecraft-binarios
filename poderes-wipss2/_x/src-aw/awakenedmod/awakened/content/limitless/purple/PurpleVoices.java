package com.awakenedmod.awakened.content.limitless.purple;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.LoopSound;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class PurpleVoices {
   public static final Lab.Value SET = Lab.SERVER.def("limitless.purple.sound_set", 0.0, 0.0, 3.0, 1.0);
   private static final int MAX_LAYERS = 4;
   private static final String KEY_CHARGE = "limitless.purple.charge.";
   private static final String KEY_FLIGHT = "limitless.purple.flight.";
   private static final String KEY_GRIND = "limitless.purple.grind.";
   private static final float GRIND_FLOOR = 0.03F;
   private static final Supplier<SoundEvent> PORTAL = vanilla(SoundEvents.PORTAL_AMBIENT);
   private static final Supplier<SoundEvent> BEACON = vanilla(SoundEvents.BEACON_AMBIENT);
   private static final Supplier<SoundEvent> POWER = vanilla(SoundEvents.BEACON_POWER_SELECT);
   private static final Supplier<SoundEvent> CRUMBLE = vanilla(SoundEvents.DEEPSLATE_BREAK);
   private static final List<PurpleVoices.Voice> SETS = List.of(
      new PurpleVoices.Voice(
         List.of(loop(AwakenedSounds.PURPLE_CHARGE, 0.35F, 1.0F, 0.6F, 1.3F), loop(PORTAL, 0.2F, 0.6F, 0.5F, 1.0F)),
         List.of(shot(AwakenedSounds.PURPLE_READY, 1.0F, 1.0F), shot(POWER, 0.8F, 0.5F)),
         List.of(shot(AwakenedSounds.PURPLE_RELEASE, 0.8F, 1.0F), shot(vanilla(SoundEvents.WARDEN_SONIC_BOOM), 0.45F, 0.5F)),
         List.of(loop(AwakenedSounds.PURPLE_ROAR, 1.0F, 1.0F, 1.0F, 0.85F), loop(BEACON, 0.6F, 0.6F, 0.55F, 0.5F)),
         List.of(loop(AwakenedSounds.PURPLE_ERASE, 0.0F, 0.85F, 1.0F, 1.0F)),
         List.of(shot(AwakenedSounds.PURPLE_END, 1.0F, 1.0F)),
         List.of(),
         0
      ),
      new PurpleVoices.Voice(
         List.of(loop(AwakenedSounds.PURPLE_CHARGE, 0.4F, 1.0F, 0.6F, 1.3F)),
         List.of(shot(AwakenedSounds.PURPLE_READY, 1.0F, 1.0F)),
         List.of(shot(AwakenedSounds.PURPLE_RELEASE, 1.0F, 1.0F)),
         List.of(loop(AwakenedSounds.PURPLE_ROAR, 1.0F, 1.0F, 1.05F, 0.85F)),
         List.of(loop(AwakenedSounds.PURPLE_ERASE, 0.0F, 1.0F, 1.0F, 1.0F)),
         List.of(shot(AwakenedSounds.PURPLE_END, 1.0F, 1.0F)),
         List.of(),
         0
      ),
      new PurpleVoices.Voice(
         List.of(
            loop(PORTAL, 0.3F, 0.8F, 0.5F, 1.2F), loop(BEACON, 0.0F, 0.9F, 0.6F, 1.6F), loop(vanilla(SoundEvents.WARDEN_SONIC_CHARGE), 0.2F, 1.0F, 0.5F, 0.8F)
         ),
         List.of(shot(POWER, 1.0F, 0.5F), shot(vanilla(SoundEvents.RESPAWN_ANCHOR_CHARGE), 1.0F, 0.5F)),
         List.of(shot(vanilla(SoundEvents.WARDEN_SONIC_BOOM), 0.8F, 0.5F), shot(SoundEvents.GENERIC_EXPLODE::value, 0.6F, 0.5F)),
         List.of(
            loop(vanilla(SoundEvents.RAVAGER_ROAR), 0.7F, 0.7F, 0.5F, 0.45F),
            loop(vanilla(SoundEvents.ELYTRA_FLYING), 1.0F, 1.0F, 0.5F, 0.5F),
            loop(BEACON, 0.6F, 0.6F, 0.5F, 0.5F)
         ),
         List.of(pulse(CRUMBLE, 0.8F, 0.5F, 4)),
         List.of(shot(vanilla(SoundEvents.CONDUIT_DEACTIVATE), 1.0F, 0.5F)),
         List.of(),
         0
      ),
      new PurpleVoices.Voice(
         List.of(loop(AwakenedSounds.PURPLE_CHARGE, 0.15F, 0.5F, 0.5F, 1.0F)),
         List.of(shot(vanilla(SoundEvents.BREEZE_INHALE), 0.5F, 0.5F)),
         List.of(shot(vanilla(SoundEvents.WARDEN_HEARTBEAT), 0.9F, 0.5F)),
         List.of(loop(AwakenedSounds.PURPLE_ROAR, 0.1F, 0.9F, 0.6F, 0.5F)),
         List.of(loop(AwakenedSounds.PURPLE_ERASE, 0.0F, 1.0F, 0.9F, 0.9F), pulse(CRUMBLE, 1.0F, 0.5F, 5)),
         List.of(),
         List.of(shot(AwakenedSounds.PURPLE_AFTERMATH, 1.0F, 1.0F)),
         15
      )
   );

   private static PurpleVoices.Layer loop(Supplier<SoundEvent> sound, float v0, float v1, float p0, float p1) {
      return new PurpleVoices.Layer(sound, v0, v1, p0, p1, 0);
   }

   private static PurpleVoices.Layer pulse(Supplier<SoundEvent> sound, float v1, float pitch, int every) {
      return new PurpleVoices.Layer(sound, 0.0F, v1, pitch, pitch, every);
   }

   private static PurpleVoices.Shot shot(Supplier<SoundEvent> sound, float volume, float pitch) {
      return new PurpleVoices.Shot(sound, volume, pitch);
   }

   private static Supplier<SoundEvent> vanilla(SoundEvent sound) {
      return () -> sound;
   }

   private PurpleVoices() {
   }

   static void preloadLabKeys() {
   }

   private static PurpleVoices.Voice voice() {
      return SETS.get(Math.clamp((long)SET.getI(), 0, SETS.size() - 1));
   }

   static void charging(ServerLevel level, Entity caster, int ticks, float gain) {
      PurpleVoices.Voice v = voice();
      float u = Math.min(1.0F, (float)ticks / 50.0F);
      keep(caster, "limitless.purple.charge.", v.charge(), u, ticks, gain);
      if (ticks == 50) {
         play(level, caster.position(), v.ready());
      }
   }

   static void beat(ServerLevel level, Entity caster, int beat) {
      float k = (float)beat / 4.0F;
      Fx.sound(level, caster.position(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.6F + 0.6F * k, 0.6F - 0.1F * k);
   }

   static void chargeEnded(ServerLevel level, Entity caster, boolean fired) {
      stop(caster, "limitless.purple.charge.");
      if (fired) {
         play(level, caster.position(), voice().release());
      } else {
         Fx.sound(level, caster.position(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.4F);
      }
   }

   static void flying(PurpleSphereEntity sphere, float progress, float grind, int tick) {
      PurpleVoices.Voice v = voice();
      keep(sphere, "limitless.purple.flight.", v.flight(), progress, tick, 1.0F);
      if (grind > 0.03F) {
         keep(sphere, "limitless.purple.grind.", v.grind(), grind, tick, 1.0F);
      }
   }

   static void gone(ServerLevel level, PurpleSphereEntity sphere) {
      stop(sphere, "limitless.purple.flight.");
      stop(sphere, "limitless.purple.grind.");
      play(level, sphere.position(), voice().end());
   }

   static void after(ServerLevel level, PurpleSphereEntity sphere, int since) {
      PurpleVoices.Voice v = voice();
      if (since == v.afterDelay() && !v.after().isEmpty()) {
         play(level, sphere.merge().add(sphere.position()).scale(0.5), v.after());
      }
   }

   private static void keep(Entity anchor, String key, List<PurpleVoices.Layer> layers, float u, int tick, float gain) {
      for (int i = 0; i < layers.size(); i++) {
         PurpleVoices.Layer l = layers.get(i);
         if (l.every() == 0) {
            LoopSound.keep(anchor, key + i, l.sound().get(), SoundSource.PLAYERS, l.volume(u) * gain, l.pitch(u));
         } else if (tick % l.every() == 0 && anchor.level() instanceof ServerLevel level) {
            Fx.sound(level, anchor.position(), l.sound().get(), SoundSource.PLAYERS, l.volume(u) * gain, l.pitch(u));
         }
      }
   }

   private static void stop(Entity anchor, String key) {
      for (int i = 0; i < 4; i++) {
         LoopSound.stop(anchor, key + i);
      }
   }

   private static void play(ServerLevel level, Vec3 at, List<PurpleVoices.Shot> shots) {
      for (PurpleVoices.Shot s : shots) {
         Fx.sound(level, at, s.sound().get(), SoundSource.PLAYERS, s.volume(), s.pitch());
      }
   }

   static record Layer(Supplier<SoundEvent> sound, float v0, float v1, float p0, float p1, int every) {
      float volume(float u) {
         return this.v0 + (this.v1 - this.v0) * u;
      }

      float pitch(float u) {
         return this.p0 + (this.p1 - this.p0) * u;
      }
   }

   static record Shot(Supplier<SoundEvent> sound, float volume, float pitch) {
   }

   static record Voice(
      List<PurpleVoices.Layer> charge,
      List<PurpleVoices.Shot> ready,
      List<PurpleVoices.Shot> release,
      List<PurpleVoices.Layer> flight,
      List<PurpleVoices.Layer> grind,
      List<PurpleVoices.Shot> end,
      List<PurpleVoices.Shot> after,
      int afterDelay
   ) {
   }
}
