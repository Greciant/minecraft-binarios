package com.awakenedmod.awakened.core.command;

import com.awakenedmod.awakened.core.CatalystConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class BankProgressCommand {
   private BankProgressCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(BankProgressCommand::onRegister);
   }

   private static void onRegister(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)Commands.literal("awakened")
               .then(
                  OnOffCommand.node(
                     "bankprogress",
                     "commands.awakened.bankprogress",
                     server -> CatalystConfig.bankProgress(),
                     CatalystConfig::setBankProgress,
                     CatalystConfig.BANK_PROGRESS
                  )
               )
         );
   }
}
