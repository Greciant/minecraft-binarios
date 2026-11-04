package com.awakenedmod.awakened.client.limitless;

import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessComfort;
import com.awakenedmod.awakened.content.limitless.LimitlessCast;
import com.awakenedmod.awakened.content.limitless.purple.PurpleCast;
import com.awakenedmod.awakened.core.Lab;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

final class PurpleFov {
   private static final Lab.Value PULL = Lab.CLIENT.def("limitless.purple.fov_pull", 0.06, 0.0, 0.3, 0.005);
   private static final Lab.Value KICK = Lab.CLIENT.def("limitless.purple.fov_kick", 0.08, 0.0, 0.3, 0.005);
   private static final Lab.Value KICK_TICKS = Lab.CLIENT.def("limitless.purple.fov_kick_ticks", 10.0, 1.0, 40.0, 1.0);

   private PurpleFov() {
   }

   static void register(IEventBus gameBus) {
      gameBus.addListener(PurpleFov::onComputeFov);
   }

   private static void onComputeFov(ComputeFovModifierEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && event.getPlayer() == mc.player) {
         PurpleCast cast = PurpleCast.of(mc.player);
         if (cast.pulling()) {
            LimitlessCast c = cast.cast();
            long now = mc.level.getGameTime();
            float push;
            if (c.live()) {
               push = -PULL.getF() * Ease.smooth(cast.pullIn((double)now));
            } else {
               if (c.state() != 2) {
                  return;
               }

               float u = (float)(now - (long)c.end()) / KICK_TICKS.getF();
               if (u >= 1.0F) {
                  return;
               }

               push = KICK.getF() * (float)Math.sqrt(cast.strength().ballScale()) * (1.0F - u) * (1.0F - u);
            }

            float scale = (float)((Double)mc.options.fovEffectScale().get()).doubleValue() * LimitlessComfort.distortion(mc);
            if (push != 0.0F && scale > 0.0F) {
               event.setNewFovModifier(event.getNewFovModifier() * (1.0F + push * scale));
            }
         }
      }
   }
}
