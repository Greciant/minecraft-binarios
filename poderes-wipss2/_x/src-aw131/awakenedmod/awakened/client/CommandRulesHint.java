package com.awakenedmod.awakened.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class CommandRulesHint {
   private static boolean wasCommand;

   private CommandRulesHint() {
   }

   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null) {
         ClientPowerState state = ClientPowerState.get();
         boolean command = state.awakened() && "command".equals(state.powerId());
         if (command && !wasCommand && !(Boolean)ClientConfig.COMMAND_RULES_HINT_SHOWN.get()) {
            ClientConfig.COMMAND_RULES_HINT_SHOWN.set(true);
            ClientConfig.save();
            player.displayClientMessage(
               Component.translatable("awakened.command.rules_hint", new Object[]{AwakenedKeybinds.SECONDARY_ABILITY.getTranslatedKeyMessage()})
                  .withStyle(ChatFormatting.GOLD),
               false
            );
         }

         wasCommand = command;
      }
   }
}
