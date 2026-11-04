package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;

public final class VelocityTunnelClient {
   private static boolean forced;
   private static CameraType saved;
   private static boolean lifted;

   private VelocityTunnelClient() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(VelocityTunnelClient::onRenderPlayer);
      gameBus.addListener(VelocityTunnelClient::onClientTick);
      gameBus.addListener(VelocityTunnelClient::onRenderPlayerPost);
   }

   private static void onClientTick(Post event) {
      restoreInvisible();
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer local = mc.player;
      boolean inside = local != null && VelocityVisualState.of(local).tunnel() && !ForcedCamera.active();
      if (inside) {
         if (!forced) {
            saved = mc.options.getCameraType();
            forced = true;
         }

         if (!mc.options.getCameraType().isFirstPerson()) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
         }
      } else if (forced) {
         forced = false;
         if (saved != null) {
            mc.options.setCameraType(saved);
         }

         saved = null;
      }
   }

   private static void onRenderPlayer(Pre event) {
      restoreInvisible();
      Player player = event.getEntity();
      if (VelocityVisualState.of(player).tunnel()) {
         if (ownArms(player)) {
            player.setInvisible(false);
            lifted = true;
         } else {
            event.setCanceled(true);
         }
      }
   }

   private static void onRenderPlayerPost(net.neoforged.neoforge.client.event.RenderPlayerEvent.Post event) {
      restoreInvisible();
   }

   private static void restoreInvisible() {
      if (lifted) {
         lifted = false;
         LocalPlayer local = Minecraft.getInstance().player;
         if (local != null && VelocityVisualState.of(local).tunnel()) {
            local.setInvisible(true);
         }
      }
   }

   private static boolean ownArms(Player player) {
      return VelocityLaneTransitClient.active() && player == Minecraft.getInstance().player && FirstPersonMode.isFirstPersonPass();
   }
}
