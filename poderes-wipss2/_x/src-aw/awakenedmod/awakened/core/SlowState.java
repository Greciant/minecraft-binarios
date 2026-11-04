package com.awakenedmod.awakened.core;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SlowState(int divisor) {
   public static final SlowState NONE = new SlowState(1);
   public static final StreamCodec<ByteBuf, SlowState> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(SlowState::new, SlowState::divisor);
}
