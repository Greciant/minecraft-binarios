package com.awakenedmod.awakened.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;

public final class ClientHandsBusy {
   private static final int NOTICE_INTERVAL = 20;
   private static boolean busy;
   private static int lastNotice = Integer.MIN_VALUE;

   private ClientHandsBusy() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ClientHandsBusy::onUseStart);
      gameBus.addListener(ClientHandsBusy::onRightClickItem);
      gameBus.addListener(ClientHandsBusy::onLoggingOut);
   }

   public static boolean isBusy() {
      return busy;
   }

   public static void setBusy(boolean on) {
      busy = on;
      LocalPlayer player = Minecraft.getInstance().player;
      if (on && player != null) {
         player.stopUsingItem();
      }
   }

   private static void onUseStart(Start event) {
      if (blocked(event.getEntity())) {
         event.setCanceled(true);
         notice();
      }
   }

   private static void onRightClickItem(RightClickItem event) {
      if (blocked(event.getEntity())) {
         event.setCanceled(true);
         notice();
      }
   }

   private static boolean blocked(LivingEntity entity) {
      return busy && entity instanceof LocalPlayer && entity == Minecraft.getInstance().player;
   }

   private static void onLoggingOut(LoggingOut event) {
      busy = false;
   }

   private static void notice() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         int now = player.tickCount;
         if (now - lastNotice >= 20) {
            lastNotice = now;
            player.displayClientMessage(Component.translatable("awakened.feedback.hands_busy"), true);
         }
      }
   }
}
