package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.phys.Vec3;

public record LaneTransitPayload(Vec3 from, Vec3 to, int duration) implements CustomPacketPayload {
   public static final Type<LaneTransitPayload> TYPE = new Type(AwId.of("velocity_lane_transit"));
   private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
      ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, LaneTransitPayload> STREAM_CODEC = StreamCodec.composite(
      VEC3, LaneTransitPayload::from, VEC3, LaneTransitPayload::to, ByteBufCodecs.VAR_INT, LaneTransitPayload::duration, LaneTransitPayload::new
   );

   public static LaneTransitPayload begin(Vec3 from, Vec3 to, int duration) {
      return new LaneTransitPayload(from, to, duration);
   }

   public static LaneTransitPayload end() {
      return new LaneTransitPayload(Vec3.ZERO, Vec3.ZERO, 0);
   }

   public boolean running() {
      return this.duration > 0;
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
