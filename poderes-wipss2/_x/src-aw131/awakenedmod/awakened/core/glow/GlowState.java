package com.awakenedmod.awakened.core.glow;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record GlowState(long packed, int flash, int color) {
   public static final GlowState NONE = new GlowState(0L, 0, 16777215);
   public static final StreamCodec<ByteBuf, GlowState> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_LONG, GlowState::packed, ByteBufCodecs.VAR_INT, GlowState::flash, ByteBufCodecs.INT, GlowState::color, GlowState::new
   );
   private static final int FLASH_MASK_BITS = 6;
   private static final int FLASH_SEQ_WRAP = 1024;

   public float intensity(Limb limb) {
      return limb == Limb.ALL ? 0.0F : (float)(this.packed >>> 8 * limb.ordinal() & 255L) / 255.0F;
   }

   public boolean any() {
      return this.packed != 0L;
   }

   public boolean anyArm() {
      return this.intensity(Limb.LEFT_ARM) > 0.0F || this.intensity(Limb.RIGHT_ARM) > 0.0F;
   }

   public int flashSeq() {
      return this.flash >>> 6;
   }

   public int flashMask() {
      return this.flash & Limb.ALL.mask();
   }

   public GlowState with(Limb limb, float intensity) {
      int b = Mth.clamp(Math.round(Mth.clamp(intensity, 0.0F, 1.0F) * 255.0F), 0, 255);
      long next = this.packed;

      for (Limb l : Limb.REAL) {
         if (limb == Limb.ALL || limb == l) {
            int shift = 8 * l.ordinal();
            next = next & ~(255L << shift) | (long)b << shift;
         }
      }

      return new GlowState(next, this.flash, this.color);
   }

   public GlowState flashed(Limb limb) {
      int seq = (this.flashSeq() + 1) % 1024;
      return new GlowState(this.packed, seq << 6 | limb.mask(), this.color);
   }

   public GlowState withColor(int rgb) {
      return new GlowState(this.packed, this.flash, rgb & 16777215);
   }
}
