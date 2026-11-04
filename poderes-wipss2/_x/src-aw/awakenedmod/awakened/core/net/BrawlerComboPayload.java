package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record BrawlerComboPayload(int count, int remainingTicks, int best, int event, boolean paused, int flavor) implements CustomPacketPayload {
   public static final int EVENT_SYNC = 0;
   public static final int EVENT_HIT = 1;
   public static final int EVENT_TIER_UP = 2;
   public static final int EVENT_DROP = 3;
   public static final int EVENT_SPENT = 4;
   public static final int EVENT_KILL_CARRY = 5;
   public static final int EVENT_FREE = 6;
   public static final int FLAVOR_FIST = 0;
   public static final int FLAVOR_ABILITY = 1;
   public static final int FLAVOR_THROW = 2;
   public static final Type<BrawlerComboPayload> TYPE = new Type(AwId.of("brawler_combo"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BrawlerComboPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      BrawlerComboPayload::count,
      ByteBufCodecs.VAR_INT,
      BrawlerComboPayload::remainingTicks,
      ByteBufCodecs.VAR_INT,
      BrawlerComboPayload::best,
      ByteBufCodecs.VAR_INT,
      BrawlerComboPayload::event,
      ByteBufCodecs.BOOL,
      BrawlerComboPayload::paused,
      ByteBufCodecs.VAR_INT,
      BrawlerComboPayload::flavor,
      BrawlerComboPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
