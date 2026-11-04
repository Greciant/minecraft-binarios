package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.net.AbilityActionPayload;
import com.awakenedmod.awakened.core.net.StrugglePayload;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ScalerClientHooks {
   private static boolean jumpHeld;

   private ScalerClientHooks() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ScalerClientHooks::onClientTick);
      ClickClaims.add(ScalerClientState::seizing, ScalerClientHooks::throwSeized);
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         ScalerClientState.reset();
         jumpHeld = false;
      } else {
         boolean down = mc.options.keyJump.isDown() && mc.screen == null;
         if (down && !jumpHeld && ScalerClientState.restrained()) {
            send(StrugglePayload.INSTANCE);
         }

         jumpHeld = down;
      }
   }

   private static void throwSeized() {
      int slot = seizeSlot();
      if (slot >= 0) {
         send(new AbilityActionPayload(slot, 4));
      }
   }

   private static int seizeSlot() {
      Power power = PowerRegistry.get(ClientPowerState.get().powerId());
      if (power == null) {
         return -1;
      } else {
         for (int slot = 0; slot < power.abilityCount(); slot++) {
            if ("clamber_seize".equals(power.ability(slot).id())) {
               return slot;
            }
         }

         return -1;
      }
   }

   private static void send(CustomPacketPayload payload) {
      PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }
}
