package com.awakenedmod.awakened.content.brawler;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record MarkState(int mark, int flavour, long expiryTick) {
   public static final MarkState NONE = new MarkState(-1, 0, 0L);
   public static final StreamCodec<RegistryFriendlyByteBuf, MarkState> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, MarkState::mark, ByteBufCodecs.VAR_INT, MarkState::flavour, ByteBufCodecs.VAR_LONG, MarkState::expiryTick, MarkState::new
   );

   public static MarkState of(Mark mark, Mark.Flavour flavour, long expiryTick) {
      return new MarkState(mark.ordinal(), flavour.ordinal(), expiryTick);
   }

   public Mark markOrNull() {
      return Mark.byId(this.mark);
   }

   public Mark.Flavour flavourValue() {
      return Mark.Flavour.byId(this.flavour);
   }

   public boolean live(long gameTime) {
      return this.markOrNull() != null && gameTime < this.expiryTick;
   }
}
