package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.net.PassiveTogglePayload;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PassiveToggleClient {
   private static final Map<String, Boolean> STATES = new HashMap<>();

   private PassiveToggleClient() {
   }

   public static void apply(Map<String, Boolean> states) {
      STATES.clear();
      STATES.putAll(states);
   }

   public static boolean enabled(PassiveToggle toggle) {
      return STATES.getOrDefault(toggle.key(), toggle.defaultOn());
   }

   public static void toggle(PassiveToggle toggle) {
      STATES.put(toggle.key(), !enabled(toggle));
      send(new PassiveTogglePayload(toggle.key()));
   }

   private static void send(CustomPacketPayload payload) {
      PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }
}
