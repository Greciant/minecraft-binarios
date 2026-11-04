package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.screen.RadialWheelScreen;
import com.awakenedmod.awakened.content.summoning.SoulData;
import com.awakenedmod.awakened.content.summoning.SummonAttachments;
import com.awakenedmod.awakened.content.summoning.SummonSouls;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.net.SoulMenuPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SummonWheel {
   private static final int COLOR = -6591530;
   private static final Lab.Value SLOTS = Lab.CLIENT.def("summon.wheel.slots", 5.0, 2.0, 8.0, 1.0);
   private static final Lab.Value RECENT_SLOTS = Lab.CLIENT.def("summon.wheel.recent_slots", 2.0, 0.0, 8.0, 1.0);

   private SummonWheel() {
   }

   public static void preloadLabKeys() {
      SLOTS.key();
      RECENT_SLOTS.key();
   }

   public static RadialWheelScreen create() {
      Player player = Minecraft.getInstance().player;
      if (player == null) {
         return null;
      } else {
         SoulData souls = SummonAttachments.souls(player);
         List<String> types = SummonSouls.quickPicks(souls, SLOTS.getI(), RECENT_SLOTS.getI());
         if (types.isEmpty()) {
            return null;
         } else {
            ClientPowerState state = ClientPowerState.get();
            boolean infinite = state.infiniteStamina();
            float stamina = state.stamina();
            String active = souls.active();
            List<RadialWheelScreen.RadialOption> options = new ArrayList<>();

            for (String type : types) {
               int cost = SummonSouls.summonCost(type);
               boolean affordable = infinite || stamina >= (float)cost;
               options.add(
                  new RadialWheelScreen.RadialOption(
                     null,
                     -6591530,
                     SummonSouls.displayName(type),
                     false,
                     null,
                     type.equals(active),
                     options.size(),
                     Component.translatable("awakened.summon_wheel.cost", new Object[]{cost}),
                     affordable,
                     SoulIcons.of(type, souls.count(type))
                  )
               );
            }

            return new RadialWheelScreen(
               Component.translatable("awakened.summon_wheel.title"), options, index -> select(types, index), AwakenedKeybinds.SECONDARY_ABILITY, -1
            );
         }
      }
   }

   private static void select(List<String> types, int index) {
      if (index >= 0 && index < types.size()) {
         send(new SoulMenuPayload(0, types.get(index), 0));
      }
   }

   private static void send(CustomPacketPayload payload) {
      PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }
}
