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

public final class SpiderWindSound {
   private static final float TOP = 1.6F;
   private static final float EASE = 0.25F;
   private static SpiderWindSound.Instance playing;

   private SpiderWindSound() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(SpiderWindSound::onClientTick);
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (playing != null && playing.isStopped()) {
         playing = null;
      }

      LocalPlayer player = mc.player;
      boolean on = player != null && SpiderSwingController.active();
      if (on) {
         if (playing == null) {
            playing = new SpiderWindSound.Instance(player);
            mc.getSoundManager().play(playing);
         }
      } else if (playing != null) {
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
         this.pitch = 0.8F;
      }

      public void tick() {
         Minecraft mc = Minecraft.getInstance();
         if (!this.player.isRemoved() && mc.player == this.player && SpiderSwingController.active()) {
            float speed = (float)this.player.getDeltaMovement().length();
            float s = Mth.clamp(speed / 1.6F, 0.0F, 1.0F);
            this.volume = this.volume + (0.6F * s * s - this.volume) * 0.25F;
            this.pitch = this.pitch + (0.8F + 0.5F * s - this.pitch) * 0.25F;
         } else {
            this.stop();
         }
      }
   }
}
