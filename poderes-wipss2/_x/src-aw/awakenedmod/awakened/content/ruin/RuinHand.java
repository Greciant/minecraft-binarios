package com.awakenedmod.awakened.content.ruin;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record RuinHand(int rot) {
   public static final RuinHand NONE = new RuinHand(0);
   public static final int MAX = 10;
   public static final StreamCodec<FriendlyByteBuf, RuinHand> STREAM_CODEC = StreamCodec.of(
      (buf, h) -> buf.writeByte(h.rot), buf -> new RuinHand(Mth.clamp(buf.readByte(), 0, 10))
   );

   public static RuinHand of(float amount) {
      return new RuinHand(Mth.clamp(Math.round(Mth.clamp(amount, 0.0F, 1.0F) * 10.0F), 0, 10));
   }

   public boolean any() {
      return this.rot > 0;
   }

   public int crackStage() {
      return Math.max(0, this.rot - 1);
   }

   public float amount() {
      return (float)this.rot / 10.0F;
   }
}
