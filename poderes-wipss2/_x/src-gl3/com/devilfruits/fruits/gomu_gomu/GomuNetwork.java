package com.devilfruits.fruits.gomu_gomu;

import com.devilfruits.fruits.gomu_gomu.client.RubberDents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class GomuNetwork {
   static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("11");
      registrar.playToClient(GomuNetwork.BulletAbsorbPayload.TYPE, GomuNetwork.BulletAbsorbPayload.STREAM_CODEC, (payload, ctx) -> RubberDents.accept(payload));
   }

   public static void absorbed(ServerPlayer user, Vec3 offset, Vec3 dir, long start, int hold) {
      if (!(user instanceof FakePlayer) && user.connection != null) {
         PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            user, new GomuNetwork.BulletAbsorbPayload(user.getId(), offset, dir, start, hold), new CustomPacketPayload[0]
         );
      }
   }

   private GomuNetwork() {
   }

   public static record BulletAbsorbPayload(int entity, Vec3 offset, Vec3 dir, long start, int hold) implements CustomPacketPayload {
      public static final Type<GomuNetwork.BulletAbsorbPayload> TYPE = new Type(GomuGomu.id("bullet_absorb"));
      public static final StreamCodec<FriendlyByteBuf, GomuNetwork.BulletAbsorbPayload> STREAM_CODEC = CustomPacketPayload.codec((p, buf) -> {
         buf.writeVarInt(p.entity);
         buf.writeVec3(p.offset);
         buf.writeVec3(p.dir);
         buf.writeVarLong(p.start);
         buf.writeVarInt(p.hold);
      }, buf -> new GomuNetwork.BulletAbsorbPayload(buf.readVarInt(), buf.readVec3(), buf.readVec3(), buf.readVarLong(), buf.readVarInt()));

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
