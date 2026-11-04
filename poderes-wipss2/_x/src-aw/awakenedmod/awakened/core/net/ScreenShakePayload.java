package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ScreenShakePayload(double x, double y, double z, float radius, float magnitude, int duration, int fade) implements CustomPacketPayload {
   public static final Type<ScreenShakePayload> TYPE = new Type(AwId.of("screen_shake"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ScreenShakePayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
      buf.writeDouble(p.x);
      buf.writeDouble(p.y);
      buf.writeDouble(p.z);
      buf.writeFloat(p.radius);
      buf.writeFloat(p.magnitude);
      buf.writeVarInt(p.duration);
      buf.writeVarInt(p.fade);
   }, buf -> new ScreenShakePayload(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt()));

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
