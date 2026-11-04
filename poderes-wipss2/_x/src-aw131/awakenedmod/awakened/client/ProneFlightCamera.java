package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.ProneFlight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class ProneFlightCamera {
   private static final double LIFT = 0.85;
   private static final int EASE_TICKS = 4;
   private static float progress;
   private static float progressOld;

   private ProneFlightCamera() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ProneFlightCamera::onClientTick);
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      boolean prone = player != null && !player.isRemoved() && ProneFlight.holds(player);
      progressOld = progress;
      float step = 0.25F;
      progress = prone ? Math.min(1.0F, progress + step) : Math.max(0.0F, progress - step);
   }

   public static double lift(Entity cameraEntity, float partialTick) {
      if (progress <= 0.0F && progressOld <= 0.0F) {
         return 0.0;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && cameraEntity == mc.player) {
            float t = Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), progressOld, progress);
            float eased = t * t * (3.0F - 2.0F * t);
            return 0.85 * (double)eased * (double)mc.player.getScale();
         } else {
            return 0.0;
         }
      }
   }
}
