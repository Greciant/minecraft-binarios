package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.PhaseState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class PhaseClientFlag {
   private PhaseClientFlag() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(PhaseClientFlag::onTick);
      gameBus.addListener(PhaseClientFlag::onLoggingOut);
   }

   private static void onTick(Post event) {
      LocalPlayer p = Minecraft.getInstance().player;
      if (p != null) {
         PhaseState.setClient(p.getId(), PhaseState.clientSource(p) != null);
         PhaseState.setClientFull(p.getId(), PhaseState.clientFullSource(p));
      }
   }

   private static void onLoggingOut(LoggingOut event) {
      PhaseState.clearClient();
   }
}
