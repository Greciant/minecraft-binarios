package com.awakenedmod.awakened.core.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record BrawlerVisual(int phase, int target, double x, double y, double z, int since) {
   public static final int NONE = 0;
   public static final int EXTEND = 1;
   public static final int HOLD = 2;
   public static final int RETRACT = 3;
   public static final BrawlerVisual IDLE = new BrawlerVisual(0, -1, 0.0, 0.0, 0.0, 0);
   public static final StreamCodec<RegistryFriendlyByteBuf, BrawlerVisual> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      BrawlerVisual::phase,
      ByteBufCodecs.VAR_INT,
      BrawlerVisual::target,
      ByteBufCodecs.DOUBLE,
      BrawlerVisual::x,
      ByteBufCodecs.DOUBLE,
      BrawlerVisual::y,
      ByteBufCodecs.DOUBLE,
      BrawlerVisual::z,
      ByteBufCodecs.VAR_INT,
      BrawlerVisual::since,
      BrawlerVisual::new
   );

   public boolean active() {
      return this.phase != 0;
   }

   public boolean attached() {
      return this.phase == 1 || this.phase == 2;
   }
}
