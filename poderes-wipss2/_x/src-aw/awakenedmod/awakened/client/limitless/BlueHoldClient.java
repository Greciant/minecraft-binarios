package com.awakenedmod.awakened.client.limitless;

import com.awakenedmod.awakened.client.OwnAbilityKey;
import com.awakenedmod.awakened.content.limitless.blue.BlueHoldState;
import com.awakenedmod.awakened.content.limitless.blue.BlueMotion;
import com.awakenedmod.awakened.core.PulledBody;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class BlueHoldClient {
   private static int hold = -1;
   private static int ticks;
   private static boolean done;

   private BlueHoldClient() {
   }

   @SubscribeEvent
   public static void onTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      if (p != null && !mc.isPaused()) {
         BlueHoldState state = BlueHoldState.of(p);
         if (!state.live()) {
            hold = -1;
         } else {
            if (state.started() != hold) {
               hold = state.started();
               ticks = 0;
               done = false;
            }

            if (done || !OwnAbilityKey.held("limitless_blue")) {
               done = true;
            } else if (!PulledBody.mustLetGo(p)) {
               Vec3 target = state.target();
               if (BlueMotion.arrived(p.position(), target, 0.0)) {
                  done = true;
               } else {
                  p.setDeltaMovement(BlueMotion.step(p.position(), p.getDeltaMovement(), target, ticks++));
                  p.resetFallDistance();
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLoggingOut(LoggingOut event) {
      hold = -1;
      ticks = 0;
      done = false;
   }
}
