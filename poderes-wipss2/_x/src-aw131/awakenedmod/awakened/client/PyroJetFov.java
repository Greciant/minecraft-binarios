package com.awakenedmod.awakened.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

public final class PyroJetFov {
   private static final float WIDEN_SPEED = 0.09F;
   private static final float SPEED_EASE = 0.15F;
   private static final float IGNITE_PUNCH = 0.06F;
   private static final float IGNITE_DECAY = 0.85F;
   private static float easedSpeed;
   private static float ignite;
   private static boolean lastLit;

   private PyroJetFov() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(PyroJetFov::onComputeFov);
   }

   private static void onComputeFov(ComputeFovModifierEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && event.getPlayer() == mc.player) {
         boolean lit = PyroJetClock.lit(mc.player);
         if (lit && !lastLit) {
            ignite = 0.06F;
         }

         lastLit = lit;
         ignite *= 0.85F;
         Vec3 v = mc.player.getDeltaMovement();
         double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
         float target = lit ? (float)Mth.clamp(horiz / 1.3, 0.0, 1.0) * 0.09F : 0.0F;
         easedSpeed = easedSpeed + (target - easedSpeed) * 0.15F;
         float widen = easedSpeed + ignite;
         if (!(widen <= 1.0E-4F)) {
            event.setNewFovModifier(event.getNewFovModifier() * (1.0F + widen));
         }
      }
   }
}
