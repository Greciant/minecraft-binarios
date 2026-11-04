package com.awakenedmod.awakened.content.limitless;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.phys.Vec3;

public record InfinityStopPayload(int defenderId, boolean melee, int projectileId, Vec3 stop, Vec3 from, int startTick, int stoppedTick)
   implements CustomPacketPayload {
   public static final Type<InfinityStopPayload> TYPE = new Type(AwId.of("limitless_infinity_stop"));
   public static final StreamCodec<RegistryFriendlyByteBuf, InfinityStopPayload> STREAM_CODEC = StreamCodec.of(
      (buf, p) -> p.write(buf), InfinityStopPayload::read
   );

   private void write(RegistryFriendlyByteBuf buf) {
      buf.writeVarInt(this.defenderId);
      buf.writeBoolean(this.melee);
      buf.writeVarInt(this.projectileId + 1);
      buf.writeDouble(this.stop.x);
      buf.writeDouble(this.stop.y);
      buf.writeDouble(this.stop.z);
      buf.writeFloat((float)this.from.x);
      buf.writeFloat((float)this.from.y);
      buf.writeFloat((float)this.from.z);
      buf.writeInt(this.startTick);
      buf.writeInt(this.stoppedTick);
   }

   private static InfinityStopPayload read(RegistryFriendlyByteBuf buf) {
      int defender = buf.readVarInt();
      boolean melee = buf.readBoolean();
      int projectile = buf.readVarInt() - 1;
      Vec3 stop = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
      Vec3 from = new Vec3((double)buf.readFloat(), (double)buf.readFloat(), (double)buf.readFloat());
      return new InfinityStopPayload(defender, melee, projectile, stop, from, buf.readInt(), buf.readInt());
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
