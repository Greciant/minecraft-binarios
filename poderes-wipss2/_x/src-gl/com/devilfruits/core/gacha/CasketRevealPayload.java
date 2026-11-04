package com.devilfruits.core.gacha;

import com.devilfruits.DevilFruits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record CasketRevealPayload(int entityId, GachaTier tier, ResourceLocation fruit, long seed, int revealTicks, boolean cinematic, boolean preview)
   implements CustomPacketPayload {
   public static final Type<CasketRevealPayload> TYPE = new Type(DevilFruits.id("casket_reveal"));
   public static final StreamCodec<FriendlyByteBuf, CasketRevealPayload> STREAM_CODEC = CustomPacketPayload.codec(
      (p, buf) -> {
         buf.writeVarInt(p.entityId);
         buf.writeEnum(p.tier);
         buf.writeResourceLocation(p.fruit);
         buf.writeLong(p.seed);
         buf.writeVarInt(p.revealTicks);
         buf.writeBoolean(p.cinematic);
         buf.writeBoolean(p.preview);
      },
      buf -> new CasketRevealPayload(
            buf.readVarInt(),
            (GachaTier)buf.readEnum(GachaTier.class),
            buf.readResourceLocation(),
            buf.readLong(),
            buf.readVarInt(),
            buf.readBoolean(),
            buf.readBoolean()
         )
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
