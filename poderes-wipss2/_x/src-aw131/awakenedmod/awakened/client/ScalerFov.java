package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.scaler.MassLeapAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

public final class ScalerFov {
   private ScalerFov() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ScalerFov::onComputeFov);
   }

   private static void onComputeFov(ComputeFovModifierEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && event.getPlayer() == mc.player) {
         if ("scaler".equals(ClientPowerState.get().powerId())) {
            AttributeInstance speed = mc.player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && speed.hasModifier(MassLeapAbility.BRACE_SLOW_ID)) {
               event.setNewFovModifier(1.0F);
            }
         }
      }
   }
}
