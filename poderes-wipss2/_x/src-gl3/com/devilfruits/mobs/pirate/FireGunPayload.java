package com.devilfruits.mobs.pirate;

import com.devilfruits.mobs.pirate.item.FlintlockItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

public record FireGunPayload() implements CustomPacketPayload {
   public static final Type<FireGunPayload> TYPE = new Type(Pirates.id("fire_gun"));
   public static final StreamCodec<ByteBuf, FireGunPayload> STREAM_CODEC = StreamCodec.unit(new FireGunPayload());

   static void handle(ServerPlayer player) {
      if (!player.isUsingItem() && !player.isSpectator()) {
         if (player.getMainHandItem().getItem() instanceof FlintlockItem gun && !player.getCooldowns().isOnCooldown(gun)) {
            gun.fire(player, InteractionHand.MAIN_HAND);
         }
      }
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
