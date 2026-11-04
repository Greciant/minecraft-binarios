package com.awakenedmod.awakened.core.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record PyroVisual(int heatByte) {
   public static final PyroVisual COLD = new PyroVisual(0);
   public static final StreamCodec<ByteBuf, PyroVisual> STREAM_CODEC = ByteBufCodecs.BYTE.map(b -> new PyroVisual(b & 255), v -> (byte)v.heatByte);

   public static PyroVisual of(float heat01) {
      return new PyroVisual(Mth.clamp(Math.round(heat01 * 255.0F), 0, 255));
   }

   public float heat01() {
      return (float)this.heatByte / 255.0F;
   }
}
