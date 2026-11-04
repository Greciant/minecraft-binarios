package com.devilfruits.client.vfx.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public final class VfxFormat {
   public static final VertexFormat FORMAT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .add("UV0", VertexFormatElement.UV0)
      .add("UV1", VertexFormatElement.UV1)
      .add("UV2", VertexFormatElement.UV2)
      .add("Normal", VertexFormatElement.NORMAL)
      .padding(1)
      .build();

   public static int heat(float heat) {
      return heat < 0.0F ? -4096 : Math.min(32767, (int)(heat * 4096.0F));
   }

   public static int extra(int palette, float emissive) {
      return (palette & 7) << 12 | Math.min(4095, Math.max(0, (int)(emissive * 256.0F)));
   }

   public static int extra(int palette, float emissive, int atlas) {
      return extra(palette, emissive) | (atlas == 1 ? 32768 : 0);
   }

   public static int unit(float v) {
      return Math.max(-32767, Math.min(32767, (int)(v * 32767.0F)));
   }

   private VfxFormat() {
   }
}
