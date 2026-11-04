package com.awakenedmod.awakened.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

public final class BrawlerFov {
   private static final float BASE_FOV = 70.0F;

   private BrawlerFov() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(BrawlerFov::onComputeFov);
   }

   private static void onComputeFov(ComputeFovModifierEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && event.getPlayer() == mc.player && "brawler".equals(ClientPowerState.get().powerId())) {
         float modifier = event.getNewFovModifier();
         long age = BrawlerComboState.fovKickAge(Util.getMillis());
         if (age >= 0L && age < 400L) {
            float t = 1.0F - (float)age / 400.0F;
            modifier *= 1.0F + 0.057142857F * t * t;
         }

         Vec3 v = mc.player.getDeltaMovement();
         double speed = Math.max(v.horizontalDistance(), v.y);
         float widen = (float)Mth.clamp((speed - 0.5) / 0.21999999999999997, 0.0, 1.0);
         if (widen > 0.0F) {
            modifier *= 1.0F + 0.1F * widen;
         }

         event.setNewFovModifier(modifier);
      }
   }
}
