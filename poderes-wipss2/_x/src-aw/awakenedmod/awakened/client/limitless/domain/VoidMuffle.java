package com.awakenedmod.awakened.client.limitless.domain;

import com.awakenedmod.awakened.client.SoundDuck;
import com.awakenedmod.awakened.client.voidview.VoidView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;

public final class VoidMuffle implements SoundDuck.Source {
   private static final float INTRO_GAIN = 0.05F;
   private static final float OUTSIDE_GAIN = 0.15F;
   private boolean intro;
   private boolean inside;

   private VoidMuffle() {
   }

   static void register() {
      SoundDuck.add(new VoidMuffle());
   }

   @Override
   public boolean tick(Minecraft mc) {
      boolean nowIntro = mc.player != null && VoidIntro.current(0.0F) != null;
      boolean nowInside = mc.player != null && VoidView.hidesWorld();
      if (nowIntro == this.intro && nowInside == this.inside) {
         return false;
      } else {
         this.intro = nowIntro;
         this.inside = nowInside;
         return true;
      }
   }

   @Override
   public float gain(SoundInstance sound) {
      if (this.intro) {
         return 0.05F;
      } else if (this.inside && !sound.isRelative()) {
         return VoidView.contains(sound.getX(), sound.getY(), sound.getZ()) ? 1.0F : 0.15F;
      } else {
         return 1.0F;
      }
   }

   @Override
   public boolean holdsMusic() {
      return this.intro || this.inside;
   }
}
