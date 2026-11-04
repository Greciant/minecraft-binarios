package com.impactframe.server;

import com.impactframe.network.HighDamageAttackPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
   modid = "impactframe"
)
public final class AttackDamageFeedback {
   private AttackDamageFeedback() {
   }

   @SubscribeEvent
   public static void onLivingDamage(Post event) {
      float damage = event.getNewDamage();
      if (!(damage <= 0.0F) && event.getSource().getEntity() instanceof ServerPlayer attacker && attacker.connection.hasChannel(HighDamageAttackPayload.TYPE)) {
         PacketDistributor.sendToPlayer(attacker, new HighDamageAttackPayload(event.getEntity().getId(), damage), new CustomPacketPayload[0]);
      }
   }
}
