package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

public final class ExoFlightFov {
   private static final double CRUISE_FLOOR = 0.9;
   private static final float WIDEN_CRUISE = 0.1F;
   private static final float WIDEN_AB = 0.2F;
   private static final float AB_EASE = 0.18F;
   private static final float IGNITE_PUNCH = 0.08F;
   private static final float IGNITE_DECAY = 0.86F;
   private static float easedAb;
   private static float ignite;
   private static boolean lastAb;

   private ExoFlightFov() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ExoFlightFov::onComputeFov);
   }

   private static void onComputeFov(ComputeFovModifierEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && event.getPlayer() == mc.player) {
         ExoVisual vis = (ExoVisual)mc.player.getData(AwakenedAttachments.EXO_VISUAL);
         if ("exosavant".equals(ClientPowerState.get().powerId()) && vis.flying() && vis.jet()) {
            Vec3 v = mc.player.getDeltaMovement();
            double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
            float s = (float)Mth.clamp((horiz - 0.9) / 1.7000000000000002, 0.0, 1.0);
            boolean ab = vis.afterburner();
            if (ab && !lastAb) {
               ignite = 0.08F;
            }

            lastAb = ab;
            ignite *= 0.86F;
            float abTarget = ab ? 0.2F * s : 0.0F;
            easedAb = easedAb + (abTarget - easedAb) * 0.18F;
            float widen = 0.1F * s + easedAb + ignite;
            if (!(widen <= 1.0E-4F)) {
               event.setNewFovModifier(event.getNewFovModifier() * (1.0F + widen));
            }
         } else {
            easedAb = 0.0F;
            ignite = 0.0F;
            lastAb = false;
         }
      }
   }
}
