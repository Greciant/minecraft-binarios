package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.ruin.RuinGripAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

public final class RuinGripClient {
   private RuinGripClient() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(RuinGripClient::onComputeFov);
   }

   private static void onComputeFov(ComputeFovModifierEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && event.getPlayer() == mc.player) {
         AttributeInstance speed = mc.player.getAttribute(Attributes.MOVEMENT_SPEED);
         if (speed != null && speed.getModifier(RuinGripAbility.ROOT_ID) != null) {
            event.setNewFovModifier(1.0F);
         }
      }
   }
}
