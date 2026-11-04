package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class VelocityWindSound extends AbstractTickableSoundInstance {
   private static final float EASE = 0.12F;
   private static final float VOLUME_BASE = 0.18F;
   private static final float VOLUME_PER_GEAR = 0.1F;
   private static final float PITCH_BASE = 0.7F;
   private static final float PITCH_PER_GEAR = 0.1F;
   private static final float SILENT = 0.01F;
   private final LocalPlayer player;

   VelocityWindSound(LocalPlayer player) {
      super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
      this.player = player;
      this.looping = true;
      this.delay = 0;
      this.relative = true;
      this.x = 0.0;
      this.y = 0.0;
      this.z = 0.0;
      this.volume = 0.02F;
      this.pitch = 0.7F;
   }

   public void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (!this.player.isRemoved() && mc.player == this.player) {
         VelocityVisual vis = VelocityVisualState.of(this.player);
         if (!vis.running()) {
            this.volume = this.volume + (0.0F - this.volume) * 0.12F;
            if (this.volume < 0.01F) {
               this.stop();
            }
         } else {
            float g = (float)Math.max(0, Math.min(5, vis.gear()));
            this.volume = this.volume + (0.18F + 0.1F * g - this.volume) * 0.12F;
            this.pitch = this.pitch + (0.7F + 0.1F * g - this.pitch) * 0.12F;
         }
      } else {
         this.stop();
      }
   }
}
