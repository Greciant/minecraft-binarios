package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

public final class RuinRushClient {
   private static final ResourceLocation RUSH_CLIP = AwId.of("ruin_rush");
   private static final ResourceLocation DRAG_CLIP = AwId.of("ruin_rush_drag");

   private RuinRushClient() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(RuinRushClient::onComputeFov);
   }

   private static void onComputeFov(ComputeFovModifierEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null
         && event.getPlayer() == mc.player
         && isRuin()
         && (AwakenedAnimations.isPlaying(mc.player.getId(), RUSH_CLIP) || AwakenedAnimations.isPlaying(mc.player.getId(), DRAG_CLIP))) {
         double speed = mc.player.getDeltaMovement().horizontalDistance();
         float widen = (float)Mth.clamp((speed - 0.45) / 0.45, 0.0, 1.0);
         if (widen > 0.0F) {
            event.setNewFovModifier(event.getNewFovModifier() * (1.0F + 0.12F * widen));
         }
      }
   }

   private static boolean isRuin() {
      ClientPowerState state = ClientPowerState.get();
      return state.awakened() && "ruin".equals(state.powerId());
   }
}
