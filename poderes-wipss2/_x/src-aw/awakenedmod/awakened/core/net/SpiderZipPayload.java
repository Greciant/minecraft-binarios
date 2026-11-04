package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SpiderZipPayload(int slot, int mode, int targetId) implements CustomPacketPayload {
   public static final int TERRAIN = 0;
   public static final int LAUNCH = 1;
   public static final int YANK = 2;
   public static final int PULL = 3;
   public static final int STOP = 4;
   public static final Type<SpiderZipPayload> TYPE = new Type(AwId.of("spider_zip"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SpiderZipPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      SpiderZipPayload::slot,
      ByteBufCodecs.VAR_INT,
      SpiderZipPayload::mode,
      ByteBufCodecs.VAR_INT,
      SpiderZipPayload::targetId,
      SpiderZipPayload::new
   );

   public static SpiderZipPayload self(int slot, boolean launch) {
      return new SpiderZipPayload(slot, launch ? 1 : 0, -1);
   }

   public static SpiderZipPayload yank(int slot, int targetId) {
      return new SpiderZipPayload(slot, 2, targetId);
   }

   public static SpiderZipPayload pull(int slot) {
      return new SpiderZipPayload(slot, 3, -1);
   }

   public static SpiderZipPayload stop(int slot) {
      return new SpiderZipPayload(slot, 4, -1);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
