package com.awakenedmod.awakened.core.power;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.PassiveToggleSyncPayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PassiveToggleService {
   private PassiveToggleService() {
   }

   public static void toggle(ServerPlayer player, String key) {
      if (player != null) {
         PowerData data = PowerService.data(player);
         Power power = PowerRegistry.get(data.powerId());
         if (power != null) {
            for (PassiveToggle toggle : power.passiveToggles()) {
               if (toggle.key().equals(key)) {
                  boolean on = !toggle.enabled(data);
                  data.setVar(toggle.var(), on ? 1.0F : 0.0F);
                  player.displayClientMessage(
                     Component.translatable(
                        on ? "awakened.passive.enabled" : "awakened.passive.disabled", new Object[]{Component.translatable(toggle.nameKey())}
                     ),
                     true
                  );
                  sync(player);
                  return;
               }
            }
         }
      }
   }

   public static boolean enabled(ServerPlayer player, String key) {
      PowerData data = PowerService.data(player);
      Power power = PowerRegistry.get(data.powerId());
      if (power == null) {
         return true;
      } else {
         for (PassiveToggle toggle : power.passiveToggles()) {
            if (toggle.key().equals(key)) {
               return toggle.enabled(data);
            }
         }

         return true;
      }
   }

   public static void sync(ServerPlayer player) {
      if (player != null) {
         PowerData data = PowerService.data(player);
         Power power = PowerRegistry.get(data.powerId());
         Map<String, Boolean> states = new HashMap<>();
         if (power != null) {
            for (PassiveToggle toggle : power.passiveToggles()) {
               states.put(toggle.key(), toggle.enabled(data));
            }
         }

         PacketDistributor.sendToPlayer(player, new PassiveToggleSyncPayload(states), new CustomPacketPayload[0]);
      }
   }
}
