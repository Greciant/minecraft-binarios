package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.SpiderVisual;
import com.awakenedmod.awakened.core.net.SpiderSwingPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class SpiderDiveInput {
   private static final DoubleTap SPRINT = new DoubleTap();

   private SpiderDiveInput() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(SpiderDiveInput::onClientTick);
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      boolean sprint = player != null && mc.screen == null && mc.options.keySprint.isDown();
      if (player != null && canDive(player)) {
         if (SPRINT.update(sprint, (Boolean)mc.options.toggleSprint().get())) {
            int slot = SpiderSwingController.swingSlot();
            if (slot >= 0) {
               boolean diving = ((SpiderVisual)player.getData(AwakenedAttachments.SPIDER_VISUAL)).diving();
               if (!diving) {
                  SpiderTrickController.cancelCurrent();
               }

               SpiderSwingController.send(SpiderSwingPayload.of(diving ? 9 : 8, slot));
            }
         }
      } else {
         SPRINT.reset(sprint);
      }
   }

   private static boolean canDive(LocalPlayer player) {
      return "spider".equals(ClientPowerState.get().livePowerId())
         && !player.onGround()
         && !player.isInWater()
         && !player.getAbilities().flying
         && !SpiderSwingController.swinging()
         && !SpiderSwingController.gliding()
         && !SpiderSwingController.zipping()
         && !SpiderSwingController.striking();
   }
}
