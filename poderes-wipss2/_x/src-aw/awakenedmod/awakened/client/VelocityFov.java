package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

public final class VelocityFov {
   private static final float MAX_SPEED_FACTOR = 1.25F;

   private VelocityFov() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(VelocityFov::onComputeFov);
   }

   private static void onComputeFov(ComputeFovModifierEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && event.getPlayer() == mc.player) {
         if ("velocity".equals(ClientPowerState.get().powerId()) && VelocityVisualState.of(mc.player).overdrive()) {
            float walking = mc.player.getAbilities().getWalkingSpeed();
            if (!(walking <= 0.0F)) {
               float speedFactor = ((float)mc.player.getAttributeValue(Attributes.MOVEMENT_SPEED) / walking + 1.0F) / 2.0F;
               if (!(speedFactor <= 1.25F)) {
                  float capped = event.getFovModifier() * (1.25F / speedFactor);
                  double effectScale = (Double)mc.options.fovEffectScale().get();
                  event.setNewFovModifier((float)Mth.lerp(effectScale, 1.0, (double)capped));
               }
            }
         }
      }
   }
}
