package com.awakenedmod.awakened.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

public final class ShadowFov {
   private ShadowFov() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ShadowFov::onComputeFov);
   }

   private static void onComputeFov(ComputeFovModifierEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && event.getPlayer() == mc.player) {
         if ("shadow".equals(ClientPowerState.get().powerId())) {
            if (mc.player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
               event.setNewFovModifier(1.0F);
            }
         }
      }
   }
}
