package com.impactframe.client;

import com.impactframe.ImpactConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ImpactAudio {
   private static final SoundEvent IMPACT = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("impactframe", "impact"));

   private ImpactAudio() {
   }

   public static void play(ImpactTier tier, float scale) {
      if (ImpactConfig.soundEnabled()) {
         float volume = (float)ImpactConfig.soundVolume() * tier.soundVolume * (0.6F + 0.4F * scale);
         if (!(volume <= 0.001F)) {
            Minecraft mc = Minecraft.getInstance();
            mc.tell(() -> {
               float jitter = 1.0F + (float)(Math.random() - 0.5) * 0.08F;
               mc.getSoundManager().play(SimpleSoundInstance.forUI(IMPACT, tier.soundPitch * jitter, Math.min(volume, 1.0F)));
               if (tier.soundLayers > 1) {
                  mc.getSoundManager().play(SimpleSoundInstance.forUI(IMPACT, tier.soundPitch * 1.8F * jitter, Math.min(volume * 0.5F, 1.0F)));
               }
            });
         }
      }
   }
}
