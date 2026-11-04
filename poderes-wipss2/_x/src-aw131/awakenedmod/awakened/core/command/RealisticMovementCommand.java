package com.awakenedmod.awakened.core.command;

import com.awakenedmod.awakened.core.RealisticMovement;
import com.awakenedmod.awakened.core.rules.ServerRules;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class RealisticMovementCommand {
   private RealisticMovementCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(RealisticMovementCommand::onRegister);
   }

   private static void onRegister(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)Commands.literal("awakened")
               .then(
                  OnOffCommand.node(
                     "realisticmovement",
                     "commands.awakened.realisticmovement",
                     RealisticMovement::enabled,
                     RealisticMovement::set,
                     ServerRules.REALISTIC_MOVEMENT
                  )
               )
         );
   }

   public static boolean isOp(CommandSourceStack src) {
      return src.hasPermission(2);
   }
}
