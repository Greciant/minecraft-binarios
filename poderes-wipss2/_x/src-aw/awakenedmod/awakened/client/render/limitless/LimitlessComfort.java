package com.awakenedmod.awakened.client.render.limitless;

import net.minecraft.client.Minecraft;

public final class LimitlessComfort {
   private LimitlessComfort() {
   }

   public static float distortion(Minecraft mc) {
      double v = (Double)mc.options.screenEffectScale().get();
      return (float)Math.max(0.0, Math.min(1.0, v));
   }
}
