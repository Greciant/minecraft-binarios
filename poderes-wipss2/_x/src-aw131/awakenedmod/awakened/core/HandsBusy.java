package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.HandsBusyPayload;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.neoforged.neoforge.network.PacketDistributor;

public final class HandsBusy {
   private static final Set<UUID> NOTIFIED = new HashSet<>();

   private HandsBusy() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(HandsBusy::onUseStart);
      gameBus.addListener(HandsBusy::onRightClickItem);
      gameBus.addListener(HandsBusy::onLogout);
   }

   public static boolean busy(ServerPlayer player) {
      PowerData data = AwakenedAttachments.get(player);
      if (!data.awakened()) {
         return false;
      } else {
         Power power = PowerRegistry.get(data.powerId());
         return power != null && power.occupiesHands(player);
      }
   }

   public static void syncTick(ServerPlayer player) {
      boolean now = busy(player);
      UUID id = player.getUUID();
      if (now != NOTIFIED.contains(id)) {
         if (now) {
            NOTIFIED.add(id);
            player.stopUsingItem();
         } else {
            NOTIFIED.remove(id);
         }

         PacketDistributor.sendToPlayer(player, new HandsBusyPayload(now), new CustomPacketPayload[0]);
      }
   }

   private static void onUseStart(Start event) {
      if (blocked(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   private static void onRightClickItem(RightClickItem event) {
      if (blocked(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   private static boolean blocked(LivingEntity entity) {
      if (entity instanceof ServerPlayer player && busy(player)) {
         return true;
      }

      return false;
   }

   private static void onLogout(PlayerLoggedOutEvent event) {
      NOTIFIED.remove(event.getEntity().getUUID());
   }
}
