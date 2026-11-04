package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public final class VelocityStateSounds {
   private static final float PHASE_PITCH = 1.6F;
   private static final float PHASE_VOLUME = 0.55F;
   private static final float PHASE_TREMOLO_HZ = 7.0F;
   private static final float PHASE_TREMOLO_DEPTH = 0.4F;
   private static final float TIMELESS_BED_PITCH = 0.5F;
   private static final float TIMELESS_BED_VOLUME = 0.7F;
   private static final float TIMELESS_PITCH = 0.5F;
   private static final float TIMELESS_VOLUME = 0.3F;
   private static final float TIMELESS_PULSE_DEPTH = 0.5F;
   private static final int TIMELESS_PULSE_TICKS = 40;
   private static final float TUNNEL_ROAR_VOLUME = 1.0F;
   private static final float TUNNEL_ROAR_PITCH = 0.55F;
   private static final float TUNNEL_HEART_VOLUME = 0.8F;
   private static final float TUNNEL_HEART_PITCH = 1.1F;
   private static final float LANE_ROAR_PITCH_LIFT = 0.15F;
   private static final float LANE_WHINE_PITCH_FROM = 1.2F;
   private static final float LANE_WHINE_PITCH_TO = 2.0F;
   private static final float LANE_WHINE_VOLUME = 0.6F;
   private static final float LANE_WHINE_EASE = 0.5F;
   private static final float EASE = 0.12F;
   private static final float SILENT = 0.01F;
   private static final float TWO_PI = (float) (Math.PI * 2);
   private static final Map<VelocityStateSounds.Layer, VelocityStateSounds.Instance> PLAYING = new EnumMap<>(VelocityStateSounds.Layer.class);

   private VelocityStateSounds() {
   }

   private static VelocityVisual visual(LocalPlayer player) {
      return VelocityVisualState.of(player);
   }

   private static boolean insideWormhole(LocalPlayer player) {
      return visual(player).tunnel() || VelocityLaneTransitClient.active();
   }

   static void tick(Minecraft mc, LocalPlayer local) {
      PLAYING.values().removeIf(AbstractTickableSoundInstance::isStopped);
      if (local != null) {
         for (VelocityStateSounds.Layer layer : VelocityStateSounds.Layer.values()) {
            if (layer.gate.test(local) && !PLAYING.containsKey(layer)) {
               VelocityStateSounds.Instance instance = new VelocityStateSounds.Instance(layer, local);
               PLAYING.put(layer, instance);
               mc.getSoundManager().play(instance);
            }
         }
      }
   }

   private static final class Instance extends AbstractTickableSoundInstance {
      private final VelocityStateSounds.Layer layer;
      private final LocalPlayer player;
      private int age;

      private Instance(VelocityStateSounds.Layer layer, LocalPlayer player) {
         super(layer.sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
         this.layer = layer;
         this.player = player;
         this.looping = true;
         this.delay = 0;
         this.relative = true;
         this.x = 0.0;
         this.y = 0.0;
         this.z = 0.0;
         this.volume = 0.02F;
         this.pitch = layer.startPitch();
      }

      public void tick() {
         Minecraft mc = Minecraft.getInstance();
         if (this.player.isRemoved() || mc.player != this.player) {
            this.stop();
         } else if (!this.layer.gate.test(this.player)) {
            this.volume = this.volume + (0.0F - this.volume) * this.layer.volumeEase();
            if (this.volume < 0.01F) {
               this.stop();
            }
         } else {
            this.age++;
            this.volume = this.volume + (this.layer.targetVolume(this.age) - this.volume) * this.layer.volumeEase();
            this.pitch = this.pitch + (this.layer.targetPitch(this.age) - this.pitch) * this.layer.pitchEase();
         }
      }
   }

   private static enum Layer {
      PHASE_HUM(SoundEvents.BEACON_AMBIENT, p -> VelocityStateSounds.visual(p).phasing()) {
         @Override
         float targetVolume(int age) {
            float wobble = 0.5F + 0.5F * Mth.sin((float)age * (float) (Math.PI * 2) * 7.0F / 20.0F);
            return 0.55F * (1.0F - 0.4F * wobble);
         }

         @Override
         float targetPitch(int age) {
            return 1.6F;
         }
      },
      TIMELESS_BED(SoundEvents.AMBIENT_UNDERWATER_LOOP, p -> VelocityStateSounds.visual(p).timeless()) {
         @Override
         float targetVolume(int age) {
            return 0.7F;
         }

         @Override
         float targetPitch(int age) {
            return 0.5F;
         }
      },
      TIMELESS_DRONE(SoundEvents.CONDUIT_AMBIENT, p -> VelocityStateSounds.visual(p).timeless()) {
         @Override
         float targetVolume(int age) {
            float beat = 0.5F + 0.5F * Mth.sin((float)age * (float) (Math.PI * 2) / 40.0F);
            return 0.3F * (0.5F + 0.5F * beat);
         }

         @Override
         float targetPitch(int age) {
            return 0.5F;
         }
      },
      TUNNEL_ROAR(SoundEvents.ELYTRA_FLYING, VelocityStateSounds::insideWormhole) {
         @Override
         float targetVolume(int age) {
            return 1.0F;
         }

         @Override
         float targetPitch(int age) {
            return 0.55F + (VelocityLaneTransitClient.active() ? 0.15F : 0.0F);
         }
      },
      TUNNEL_HEART(SoundEvents.WARDEN_HEARTBEAT, VelocityStateSounds::insideWormhole) {
         @Override
         float targetVolume(int age) {
            return 0.8F;
         }

         @Override
         float targetPitch(int age) {
            return 1.1F;
         }
      },
      LANE_WHINE(SoundEvents.BEACON_AMBIENT, p -> VelocityLaneTransitClient.active()) {
         @Override
         float targetVolume(int age) {
            return 0.6F * VelocityLaneTransitClient.exit(1.0F);
         }

         @Override
         float targetPitch(int age) {
            return Mth.lerp(VelocityLaneTransitClient.exit(1.0F), 1.2F, 2.0F);
         }

         @Override
         float pitchEase() {
            return 0.5F;
         }

         @Override
         float volumeEase() {
            return 0.5F;
         }
      };

      final SoundEvent sound;
      final Predicate<LocalPlayer> gate;

      private Layer(SoundEvent sound, Predicate<LocalPlayer> gate) {
         this.sound = sound;
         this.gate = gate;
      }

      abstract float targetVolume(int var1);

      abstract float targetPitch(int var1);

      float startPitch() {
         return this.targetPitch(0);
      }

      float pitchEase() {
         return 0.12F;
      }

      float volumeEase() {
         return 0.12F;
      }
   }
}
