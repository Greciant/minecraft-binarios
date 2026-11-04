package com.awakenedmod.awakened.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class AeroWindSound {
   private static final double TOP_SPEED = 1.35;
   private static final float EASE = 0.12F;
   private static final float GLIDE_VOLUME = 0.55F;
   private static final float FLIGHT_VOLUME = 0.75F;
   private static final float GLIDE_PITCH = 0.55F;
   private static final float FLIGHT_PITCH = 0.75F;
   private static AeroWindSound.Instance playing;

   private AeroWindSound() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(AeroWindSound::onClientTick);
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (playing != null && playing.isStopped()) {
         playing = null;
      }

      LocalPlayer player = mc.player;
      boolean active = player != null && "aerokinetic".equals(ClientPowerState.get().powerId()) && (AeroClientState.flying() || AeroClientState.gliding());
      if (active && playing == null) {
         playing = new AeroWindSound.Instance(player);
         mc.getSoundManager().play(playing);
      } else if (!active && playing != null) {
         mc.getSoundManager().stop(playing);
         playing = null;
      }
   }

   private static final class Instance extends AbstractTickableSoundInstance {
      private final LocalPlayer player;

      private Instance(LocalPlayer player) {
         super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
         this.player = player;
         this.looping = true;
         this.delay = 0;
         this.relative = true;
         this.x = 0.0;
         this.y = 0.0;
         this.z = 0.0;
         this.volume = 0.02F;
         this.pitch = 0.55F;
      }

      public void tick() {
         Minecraft mc = Minecraft.getInstance();
         if (!this.player.isRemoved() && mc.player == this.player) {
            boolean flying = AeroClientState.flying();
            boolean gliding = AeroClientState.gliding();
            if (!flying && !gliding) {
               this.stop();
            } else {
               float targetVolume = flying ? 0.75F : 0.55F;
               float targetPitch = flying ? 0.75F : 0.55F;
               float speedFrac = (float)Mth.clamp(this.player.getDeltaMovement().length() / 1.35, 0.0, 1.0);
               float speedWeight = gliding && !flying ? 0.55F + 0.45F * speedFrac : 0.75F + 0.25F * speedFrac;
               float pitchRise = gliding && !flying ? 0.35F : 0.15F;
               this.volume = this.volume + (targetVolume * speedWeight - this.volume) * 0.12F;
               this.pitch = this.pitch + (targetPitch + speedFrac * pitchRise - this.pitch) * 0.12F;
            }
         } else {
            this.stop();
         }
      }
   }
}
