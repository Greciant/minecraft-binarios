package com.awakenedmod.awakened.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class InfusionLie {
   private static boolean active;
   private static float bodyYaw;

   private InfusionLie() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(InfusionLie::onClientTick);
   }

   public static void set(boolean active, float yaw) {
      InfusionLie.active = active;
      bodyYaw = yaw;
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         player.setForcedPose(active ? Pose.SLEEPING : null);
      }
   }

   private static void onClientTick(Post event) {
      if (active) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player == null) {
            active = false;
         } else {
            player.setForcedPose(Pose.SLEEPING);
            player.setYBodyRot(bodyYaw);
            player.setYHeadRot(bodyYaw);
         }
      }
   }
}
