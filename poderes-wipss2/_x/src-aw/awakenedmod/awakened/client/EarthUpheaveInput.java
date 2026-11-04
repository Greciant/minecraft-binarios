package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.net.AbilityActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;

public final class EarthUpheaveInput {
   private static final int UPHEAVE_SLOT = 1;
   private static boolean jumpHeld;

   private EarthUpheaveInput() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(EarthUpheaveInput::onClientTick);
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      boolean down = riding(mc) && mc.options.keyJump.isDown();
      if (down && !jumpHeld) {
         PacketDistributor.sendToServer(new AbilityActionPayload(1, 5), new CustomPacketPayload[0]);
      }

      jumpHeld = down;
   }

   private static boolean riding(Minecraft mc) {
      if (mc.player != null && mc.screen == null) {
         ClientPowerState state = ClientPowerState.get();
         return state.awakened() && "geokinetic".equals(state.livePowerId()) && state.activeSlot() == 1 && !mc.player.onGround();
      } else {
         return false;
      }
   }
}
