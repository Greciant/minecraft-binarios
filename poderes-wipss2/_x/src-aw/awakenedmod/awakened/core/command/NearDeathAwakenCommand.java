package com.awakenedmod.awakened.core.command;

import com.awakenedmod.awakened.core.NearDeathAwaken;
import com.awakenedmod.awakened.core.rules.ServerRules;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class NearDeathAwakenCommand {
   private NearDeathAwakenCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(NearDeathAwakenCommand::onRegister);
   }

   private static void onRegister(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)Commands.literal("awakened")
               .then(
                  OnOffCommand.node(
                     "neardeathawaken", "commands.awakened.neardeathawaken", NearDeathAwaken::enabled, NearDeathAwaken::set, ServerRules.NEAR_DEATH
                  )
               )
         );
   }
}
