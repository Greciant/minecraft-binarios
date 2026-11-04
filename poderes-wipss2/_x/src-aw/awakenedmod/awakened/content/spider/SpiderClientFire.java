package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.core.net.SpiderForceStopPayload;
import com.awakenedmod.awakened.core.power.Ability;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

final class SpiderClientFire {
   private SpiderClientFire() {
   }

   static <T extends Ability> T resolve(ServerPlayer player, int slot, Class<T> type) {
      T ability = Ability.resolveClientFire(player, slot, type);
      if (ability == null && player != null) {
         PacketDistributor.sendToPlayer(player, SpiderForceStopPayload.INSTANCE, new CustomPacketPayload[0]);
      }

      return ability;
   }
}
