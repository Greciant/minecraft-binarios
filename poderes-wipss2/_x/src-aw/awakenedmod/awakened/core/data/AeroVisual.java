package com.awakenedmod.awakened.core.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record AeroVisual(boolean gliding) {
   public static final AeroVisual NONE = new AeroVisual(false);
   public static final StreamCodec<RegistryFriendlyByteBuf, AeroVisual> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, AeroVisual::gliding, AeroVisual::new
   );
}
