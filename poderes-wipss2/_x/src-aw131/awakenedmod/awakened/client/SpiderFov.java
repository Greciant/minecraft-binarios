package com.awakenedmod.awakened.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

public final class SpiderFov {
   private static final double START = 0.35;
   private static final double FULL = 1.7;
   private static final float MAX_WIDEN = 0.2F;

   private SpiderFov() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(SpiderFov::onComputeFov);
   }

   private static void onComputeFov(ComputeFovModifierEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && event.getPlayer() == mc.player) {
         boolean swinging = SpiderClientState.swinging() && !SpiderClientState.hanging();
         boolean zipping = SpiderClientState.zipping();
         if ("spider".equals(ClientPowerState.get().powerId()) && (swinging || zipping)) {
            Vec3 v = mc.player.getDeltaMovement();
            double speed = Math.sqrt(v.x * v.x + v.z * v.z + v.y * v.y);
            float t = (float)Mth.clamp((speed - 0.35) / 1.35, 0.0, 1.0);
            if (!(t <= 0.0F)) {
               event.setNewFovModifier(event.getNewFovModifier() * (1.0F + 0.2F * t));
            }
         }
      }
   }
}
