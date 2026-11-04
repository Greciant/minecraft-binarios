package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class ExoJetSound {
   private static final float TOP_MS = 52.0F;
   private static final Map<ExoJetSound.Layer, ExoJetSound.Instance> PLAYING = new EnumMap<>(ExoJetSound.Layer.class);

   private ExoJetSound() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ExoJetSound::onClientTick);
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      PLAYING.values().removeIf(AbstractTickableSoundInstance::isStopped);
      LocalPlayer player = mc.player;
      boolean engineOn = player != null && "exosavant".equals(ClientPowerState.get().powerId()) && flying(player);
      if (engineOn) {
         for (ExoJetSound.Layer layer : ExoJetSound.Layer.values()) {
            if (!PLAYING.containsKey(layer)) {
               ExoJetSound.Instance instance = new ExoJetSound.Instance(layer, player);
               PLAYING.put(layer, instance);
               mc.getSoundManager().play(instance);
            }
         }
      } else if (!PLAYING.isEmpty()) {
         PLAYING.values().forEach(mc.getSoundManager()::stop);
         PLAYING.clear();
      }
   }

   private static boolean flying(LocalPlayer player) {
      ExoVisual vis = (ExoVisual)player.getData(AwakenedAttachments.EXO_VISUAL);
      return vis.flying() && !vis.sputtering();
   }

   private static final class Instance extends AbstractTickableSoundInstance {
      private final ExoJetSound.Layer layer;
      private final LocalPlayer player;

      private Instance(ExoJetSound.Layer layer, LocalPlayer player) {
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
         this.pitch = layer.targetPitch((ExoVisual)player.getData(AwakenedAttachments.EXO_VISUAL), 0.0F);
      }

      public void tick() {
         Minecraft mc = Minecraft.getInstance();
         if (!this.player.isRemoved() && mc.player == this.player) {
            ExoVisual vis = (ExoVisual)this.player.getData(AwakenedAttachments.EXO_VISUAL);
            if (vis.flying() && !vis.sputtering()) {
               float s = Mth.clamp((float)vis.flightSpeed() / 52.0F, 0.0F, 1.0F);
               this.volume = this.volume + (this.layer.targetVolume(vis, s) - this.volume) * this.layer.ease;
               this.pitch = this.pitch + (this.layer.targetPitch(vis, s) - this.pitch) * this.layer.ease;
            } else {
               this.stop();
            }
         } else {
            this.stop();
         }
      }
   }

   private static enum Layer {
      TURBINE(SoundEvents.BEACON_AMBIENT, 0.15F) {
         @Override
         float targetVolume(ExoVisual vis, float s) {
            return !vis.jet() ? 0.16F : 0.5F + 0.25F * s + (vis.afterburner() ? 0.2F : 0.0F);
         }

         @Override
         float targetPitch(ExoVisual vis, float s) {
            return !vis.jet() ? 0.8F : Math.min(2.0F, 0.72F + 0.55F * s + (vis.afterburner() ? 0.15F : 0.0F));
         }
      },
      RUSH(SoundEvents.ELYTRA_FLYING, 0.25F) {
         @Override
         float targetVolume(ExoVisual vis, float s) {
            return vis.jet() ? 0.55F * s * s + (vis.afterburner() ? 0.15F : 0.0F) : 0.0F;
         }

         @Override
         float targetPitch(ExoVisual vis, float s) {
            return 0.85F + 0.35F * s;
         }
      },
      THRUST(SoundEvents.ELYTRA_FLYING, 0.3F) {
         @Override
         float targetVolume(ExoVisual vis, float s) {
            return !vis.jet() ? 0.0F : 0.3F + 0.35F * s + (vis.afterburner() ? 0.3F : 0.0F);
         }

         @Override
         float targetPitch(ExoVisual vis, float s) {
            return (vis.afterburner() ? 0.45F : 0.5F) + 0.1F * s;
         }
      },
      AFTERBURN(SoundEvents.BLAZE_BURN, 0.3F) {
         @Override
         float targetVolume(ExoVisual vis, float s) {
            return vis.jet() && vis.afterburner() ? 0.45F : 0.0F;
         }

         @Override
         float targetPitch(ExoVisual vis, float s) {
            return 0.55F;
         }
      };

      final SoundEvent sound;
      final float ease;

      private Layer(SoundEvent sound, float ease) {
         this.sound = sound;
         this.ease = ease;
      }

      abstract float targetVolume(ExoVisual var1, float var2);

      abstract float targetPitch(ExoVisual var1, float var2);
   }
}
