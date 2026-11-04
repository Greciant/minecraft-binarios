package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.CatalystConfig;
import com.awakenedmod.awakened.core.ChatFx;
import com.awakenedmod.awakened.core.PowerOwnershipRegistry;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.awaken.AwakeningCinematic;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.PassiveToggleService;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class KineticChoice {
   private static final String MARKER_KEY = "kinetic_bank";
   private static final String MOVE_KEY = "kinetic_move";
   private static final String COMMAND = "awakened kineticchoice";
   private static final SimpleCommandExceptionType NOT_PENDING = new SimpleCommandExceptionType(Component.translatable("commands.awakened.kineticchoice.none"));
   private static final SimpleCommandExceptionType NO_FRESH = new SimpleCommandExceptionType(Component.translatable("commands.awakened.kineticchoice.no_fresh"));

   private KineticChoice() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(KineticChoice::onRegister);
   }

   private static boolean pending(PowerData data) {
      return "velocity".equals(data.powerId()) && data.hasVar("kinetic_bank");
   }

   public static void onLogin(ServerPlayer player) {
      if (pending(PowerService.data(player))) {
         prompt(player);
      }
   }

   private static void prompt(ServerPlayer player) {
      MutableComponent line = ChatFx.prefix()
         .append(ChatFx.note("awakened.kinetic_charger.choice_prompt"))
         .append(
            ChatFx.button(
               "awakened.kinetic_charger.choice_keep", ChatFormatting.GREEN, "awakened kineticchoice ask keep", "awakened.kinetic_charger.choice_keep_tip"
            )
         )
         .append(Component.literal(" "))
         .append(
            ChatFx.button(
               "awakened.kinetic_charger.choice_back",
               ChatFormatting.LIGHT_PURPLE,
               "awakened kineticchoice ask back",
               "awakened.kinetic_charger.choice_back_tip"
            )
         );
      if (CatalystConfig.bankProgress()) {
         line = line.append(Component.literal(" "))
            .append(
               ChatFx.button(
                  "awakened.kinetic_charger.choice_fresh", ChatFormatting.AQUA, "awakened kineticchoice ask fresh", "awakened.kinetic_charger.choice_fresh_tip"
               )
            );
      }

      player.sendSystemMessage(line);
   }

   private static void onRegister(RegisterCommandsEvent event) {
      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("awakened")
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("kineticchoice")
                              .then(Commands.literal("keep").executes(ctx -> keep((CommandSourceStack)ctx.getSource()))))
                           .then(Commands.literal("back").executes(ctx -> switchBack((CommandSourceStack)ctx.getSource(), true))))
                        .then(Commands.literal("fresh").executes(ctx -> switchBack((CommandSourceStack)ctx.getSource(), false))))
                     .then(
                        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("ask")
                                 .then(Commands.literal("keep").executes(ctx -> ask((CommandSourceStack)ctx.getSource(), KineticChoice.Answer.KEEP))))
                              .then(Commands.literal("back").executes(ctx -> ask((CommandSourceStack)ctx.getSource(), KineticChoice.Answer.BACK))))
                           .then(Commands.literal("fresh").executes(ctx -> ask((CommandSourceStack)ctx.getSource(), KineticChoice.Answer.FRESH)))
                     ))
                  .then(Commands.literal("choose").executes(ctx -> choose((CommandSourceStack)ctx.getSource())))
            )
      );
   }

   private static int ask(CommandSourceStack source, KineticChoice.Answer answer) throws CommandSyntaxException {
      ServerPlayer player = self(source);
      if (answer == KineticChoice.Answer.FRESH && !CatalystConfig.bankProgress()) {
         throw NO_FRESH.create();
      } else {
         player.sendSystemMessage(
            ChatFx.prefix()
               .append(ChatFx.note("awakened.kinetic_charger.confirm_" + answer.id))
               .append(
                  ChatFx.button(
                     "awakened.kinetic_charger.confirm_yes",
                     ChatFormatting.GREEN,
                     "awakened kineticchoice " + answer.id,
                     "awakened.kinetic_charger.confirm_yes_tip"
                  )
               )
               .append(Component.literal(" "))
               .append(
                  ChatFx.button(
                     "awakened.kinetic_charger.confirm_no", ChatFormatting.GRAY, "awakened kineticchoice choose", "awakened.kinetic_charger.confirm_no_tip"
                  )
               )
         );
         return 1;
      }
   }

   private static int choose(CommandSourceStack source) throws CommandSyntaxException {
      prompt(self(source));
      return 1;
   }

   private static int keep(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer player = self(source);
      PowerData data = PowerService.data(player);
      clearMarker(data);
      PowerService.sync(player);
      source.sendSuccess(() -> Component.translatable("commands.awakened.kineticchoice.kept"), false);
      return 1;
   }

   private static int switchBack(CommandSourceStack source, boolean carryMastery) throws CommandSyntaxException {
      ServerPlayer player = self(source);
      if (!carryMastery && !CatalystConfig.bankProgress()) {
         throw NO_FRESH.create();
      } else {
         PowerData data = PowerService.data(player);
         AwakeningCinematic.flushPending(player);
         PowerService.tearDownPower(player);
         if (!data.migratePowerId("velocity", "kinetic_charger", carryMastery)) {
            throw NOT_PENDING.create();
         } else {
            clearMarker(data);
            PowerOwnershipRegistry.set(player.serverLevel().getServer(), player.getUUID(), "kinetic_charger");
            Power power = PowerRegistry.get("kinetic_charger");
            if (power != null) {
               power.onAwaken(player);
            }

            QuestSystem.resetForPowerChange(player);
            PowerService.sync(player);
            QuestSystem.sync(player);
            PassiveToggleService.sync(player);
            String done = carryMastery ? "commands.awakened.kineticchoice.back" : "commands.awakened.kineticchoice.fresh";
            source.sendSuccess(() -> Component.translatable(done), false);
            return 1;
         }
      }
   }

   public static int test(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer player = source.getPlayerOrException();
      if (!"velocity".equals(PowerService.data(player).powerId())) {
         PowerService.setPower(player, "velocity");
         QuestSystem.resetForPowerChange(player);
         QuestSystem.sync(player);
      }

      PowerService.data(player).setVar("kinetic_bank", 1.0F);
      PowerService.sync(player);
      prompt(player);
      return 1;
   }

   private static void clearMarker(PowerData data) {
      data.removeVar("kinetic_bank");
      data.removeVar("kinetic_move");
   }

   private static ServerPlayer self(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer player = source.getPlayerOrException();
      if (!pending(PowerService.data(player))) {
         throw NOT_PENDING.create();
      } else {
         return player;
      }
   }

   private static enum Answer {
      KEEP("keep"),
      BACK("back"),
      FRESH("fresh");

      private final String id;

      private Answer(String id) {
         this.id = id;
      }
   }
}
