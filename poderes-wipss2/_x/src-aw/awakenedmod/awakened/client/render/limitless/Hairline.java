package com.awakenedmod.awakened.client.render.limitless;

import net.minecraft.client.Minecraft;
import org.joml.Vector3f;

public final class Hairline {
   private Hairline() {
   }

   public static float blocksPerPixel(Minecraft mc, float dist) {
      float fov = (float)Math.toRadians((double)((Integer)mc.options.fov().get()).intValue());
      int h = Math.max(1, mc.getWindow().getHeight());
      return dist * 2.0F * (float)Math.tan((double)(fov * 0.5F)) / (float)h;
   }

   public static float halfWidth(Minecraft mc, Vector3f at, float px) {
      return 0.5F * px * blocksPerPixel(mc, at.length());
   }
}
