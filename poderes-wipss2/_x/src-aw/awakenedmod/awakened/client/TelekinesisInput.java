package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.net.AbilityActionPayload;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TelekinesisInput {
   private TelekinesisInput() {
   }

   public static void register(IEventBus gameBus) {
      ClickClaims.add(TelekinesisInput::gripping, () -> send(0));
      gameBus.addListener(TelekinesisInput::onScroll);
   }

   private static void onScroll(MouseScrollingEvent event) {
      if (gripping()) {
         double dy = event.getScrollDeltaY();
         if (dy != 0.0) {
            event.setCanceled(true);
            send(dy > 0.0 ? 1 : 2);
         }
      }
   }

   private static void send(int action) {
      PacketDistributor.sendToServer(new AbilityActionPayload(ClientPowerState.get().activeSlot(), action), new CustomPacketPayload[0]);
   }

   private static boolean gripping() {
      ClientPowerState state = ClientPowerState.get();
      if (!state.awakened()) {
         return false;
      } else {
         Power power = PowerRegistry.get(state.powerId());
         if (power == null) {
            return false;
         } else {
            Ability ability = power.ability(state.activeSlot());
            return ability != null && ability.usesGripControls() && AbilityKeyInput.heldFor(state.activeSlot());
         }
      }
   }
}
