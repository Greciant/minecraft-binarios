package com.devilfruits.client.vfx;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

public final class VfxAudio {
   private static final SoundSource[] DUCKED = new SoundSource[]{SoundSource.AMBIENT, SoundSource.WEATHER, SoundSource.MUSIC};
   private static float duck;
   private static float wanted;
   private static float applied;

   private VfxAudio() {
   }

   public static void duck(float amount) {
      wanted = Math.max(wanted, Math.min(1.0F, amount));
   }

   public static float factor(SoundSource source) {
      if (duck <= 0.0F) {
         return 1.0F;
      } else {
         for (SoundSource s : DUCKED) {
            if (s == source) {
               return 1.0F - duck;
            }
         }

         return 1.0F;
      }
   }

   public static void tick() {
      duck = duck + (wanted - duck) * (wanted > duck ? 0.2F : 0.08F);
      if (wanted == 0.0F && duck < 0.01F) {
         duck = 0.0F;
      }

      wanted = 0.0F;
      if (Math.abs(duck - applied) > 0.02F || duck == 0.0F && applied != 0.0F) {
         applied = duck;
         Minecraft mc = Minecraft.getInstance();

         for (SoundSource s : DUCKED) {
            mc.getSoundManager().updateSourceVolume(s, mc.options.getSoundSourceVolume(s));
         }
      }
   }

   public static void reset() {
      wanted = 0.0F;
      duck = 0.0F;
      if (applied != 0.0F) {
         applied = 0.0F;
         Minecraft mc = Minecraft.getInstance();

         for (SoundSource s : DUCKED) {
            mc.getSoundManager().updateSourceVolume(s, mc.options.getSoundSourceVolume(s));
         }
      }
   }
}
