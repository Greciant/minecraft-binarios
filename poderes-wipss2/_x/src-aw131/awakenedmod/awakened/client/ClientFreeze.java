package com.awakenedmod.awakened.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;

public final class ClientFreeze {
   private static boolean frozen;
   private static boolean captured;
   private static float yaw;
   private static float pitch;

   private ClientFreeze() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ClientFreeze::onCameraAngles);
      gameBus.addListener(ClientFreeze::onLoggingOut);
      gameBus.addListener(e -> setFrozen(false));
   }

   private static void onLoggingOut(LoggingOut event) {
      if (frozen) {
         setFrozen(false);
      }
   }

   public static boolean isFrozen() {
      return frozen;
   }

   public static void setFrozen(boolean on) {
      frozen = on;
      captured = false;
   }

   private static void onCameraAngles(ComputeCameraAngles event) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (frozen && player != null) {
         if (!captured) {
            yaw = player.getYRot();
            pitch = player.getXRot();
            captured = true;
         }

         player.setYRot(yaw);
         player.setXRot(pitch);
         player.yRotO = yaw;
         player.xRotO = pitch;
         player.setYHeadRot(yaw);
         player.yHeadRotO = yaw;
         player.setYBodyRot(yaw);
         player.yBodyRotO = yaw;
         event.setYaw(yaw);
         event.setPitch(pitch);
      }
   }
}
