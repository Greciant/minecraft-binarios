package com.awakenedmod.awakened.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

public final class AeroFov {
   private static final double START = 0.45;
   private static final double FULL = 1.35;
   private static final float MAX_WIDEN = 0.18F;

   private AeroFov() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(AeroFov::onComputeFov);
   }

   private static void onComputeFov(ComputeFovModifierEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && event.getPlayer() == mc.player) {
         if ("aerokinetic".equals(ClientPowerState.get().powerId()) && AeroSurfController.ridingLocal()) {
            Vec3 v = mc.player.getDeltaMovement();
            double speed = v.length();
            float t = (float)Mth.clamp((speed - 0.45) / 0.9000000000000001, 0.0, 1.0);
            if (!(t <= 0.0F)) {
               event.setNewFovModifier(event.getNewFovModifier() * (1.0F + 0.18F * t));
            }
         }
      }
   }
}
