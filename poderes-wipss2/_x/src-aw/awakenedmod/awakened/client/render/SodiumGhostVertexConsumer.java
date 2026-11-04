package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;

final class SodiumGhostVertexConsumer extends GhostVertexConsumer implements VertexBufferWriter {
   private final boolean canUseIntrinsics;

   SodiumGhostVertexConsumer(VertexConsumer inner, float alpha, float red, float green, float blue) {
      super(inner, alpha, red, green, blue);
      this.canUseIntrinsics = VertexBufferWriter.tryOf(inner) != null;
   }

   public boolean canUseIntrinsics() {
      return this.canUseIntrinsics;
   }

   public void push(MemoryStack stack, long ptr, int count, VertexFormat format) {
      int colorOffset = format.getOffset(VertexFormatElement.COLOR);
      if (colorOffset >= 0) {
         long stride = (long)format.getVertexSize();

         for (int i = 0; i < count; i++) {
            long colorPtr = ptr + (long)i * stride + (long)colorOffset;
            ColorAttribute.set(colorPtr, this.scale(ColorAttribute.get(colorPtr)));
         }
      }

      VertexBufferWriter.of(this.inner).push(stack, ptr, count, format);
   }

   private int scale(int abgr) {
      int r = clamp((int)((float)ColorABGR.unpackRed(abgr) * this.red));
      int g = clamp((int)((float)ColorABGR.unpackGreen(abgr) * this.green));
      int b = clamp((int)((float)ColorABGR.unpackBlue(abgr) * this.blue));
      int a = clamp((int)((float)ColorABGR.unpackAlpha(abgr) * this.alpha));
      return a << 24 | b << 16 | g << 8 | r;
   }

   private static int clamp(int channel) {
      return channel < 0 ? 0 : (channel > 255 ? 255 : channel);
   }
}
