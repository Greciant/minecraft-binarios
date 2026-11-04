package com.awakenedmod.awakened.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class AeroFlightHint {
   private static boolean wasUnlocked;

   private AeroFlightHint() {
   }

   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null) {
         ClientPowerState state = ClientPowerState.get();
         boolean unlocked = "aerokinetic".equals(state.powerId()) && state.masteryLevel() >= 12;
         if (unlocked && !wasUnlocked && !(Boolean)ClientConfig.AERO_FLIGHT_HINT_SHOWN.get()) {
            ClientConfig.AERO_FLIGHT_HINT_SHOWN.set(true);
            player.displayClientMessage(
               Component.translatable("awakened.aero.flight_hint", new Object[]{AwakenedKeybinds.SECONDARY_ABILITY.getTranslatedKeyMessage()})
                  .withStyle(ChatFormatting.AQUA),
               false
            );
         }

         wasUnlocked = unlocked;
      }
   }
}
