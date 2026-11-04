package com.awakenedmod.awakened.core.command;

import com.awakenedmod.awakened.core.AwakenedConfigs;
import com.awakenedmod.awakened.core.PowerLimits;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.UniquePowerRoll;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.rules.ServerRules;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Comparator;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class UniquePowersCommand {
   private static final int MAX_LIMIT = 1000;
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_POWERS = (ctx, builder) -> SharedSuggestionProvider.suggest(
         PowerRegistry.rollable().stream().map(Power::id), builder
      );

   private UniquePowersCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(UniquePowersCommand::onRegister);
   }

   private static void onRegister(RegisterCommandsEvent event) {
      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> node = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("awakened")
         .then(
            ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                       "uniquepowers"
                                    )
                                    .requires(RealisticMovementCommand::isOp))
                                 .executes(ctx -> report((CommandSourceStack)ctx.getSource())))
                              .then(Commands.literal("on").executes(ctx -> set((CommandSourceStack)ctx.getSource(), true))))
                           .then(Commands.literal("off").executes(ctx -> set((CommandSourceStack)ctx.getSource(), false))))
                        .then(Commands.literal("toggle").executes(ctx -> toggle((CommandSourceStack)ctx.getSource()))))
                     .then(Commands.literal("limits").executes(ctx -> listLimits((CommandSourceStack)ctx.getSource()))))
                  .then(
                     Commands.literal("limit")
                        .then(
                           ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("power", StringArgumentType.word())
                                    .suggests(SUGGEST_POWERS)
                                    .executes(ctx -> reportLimit((CommandSourceStack)ctx.getSource(), StringArgumentType.getString(ctx, "power"))))
                                 .then(
                                    Commands.literal("reset")
                                       .executes(ctx -> resetLimit((CommandSourceStack)ctx.getSource(), StringArgumentType.getString(ctx, "power")))
                                 ))
                              .then(
                                 Commands.argument("count", IntegerArgumentType.integer(0, 1000))
                                    .executes(
                                       ctx -> setLimit(
                                             (CommandSourceStack)ctx.getSource(),
                                             StringArgumentType.getString(ctx, "power"),
                                             IntegerArgumentType.getInteger(ctx, "count")
                                          )
                                    )
                              )
                        )
                  ))
               .then(
                  ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("defaultlimit")
                           .executes(ctx -> reportDefaultLimit((CommandSourceStack)ctx.getSource())))
                        .then(Commands.literal("reset").executes(ctx -> resetDefaultLimit((CommandSourceStack)ctx.getSource()))))
                     .then(
                        Commands.argument("count", IntegerArgumentType.integer(0, 1000))
                           .executes(ctx -> setDefaultLimit((CommandSourceStack)ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))
                     )
               )
         );
      dispatcher.register(node);
   }

   private static int report(CommandSourceStack source) {
      MinecraftServer server = source.getServer();
      boolean on = UniquePowerRoll.enabled(server);
      source.sendSuccess(() -> Component.translatable("commands.awakened.uniquepowers.status", new Object[]{OnOffCommand.state(on)}), false);
      describeLimits(source);
      noteIfInactive(source, server, "commands.awakened.uniquepowers.limits.inactive");
      return on ? 1 : 0;
   }

   private static int reportDefaultLimit(CommandSourceStack source) {
      MinecraftServer server = source.getServer();
      source.sendSuccess(
         () -> Component.translatable("commands.awakened.uniquepowers.defaultlimit.status", new Object[]{limitText(PowerLimits.defaultLimit())}), false
      );
      noteIfInactive(source, server, "commands.awakened.uniquepowers.limits.inactive");
      return PowerLimits.defaultLimit();
   }

   private static int toggle(CommandSourceStack source) {
      return set(source, !UniquePowerRoll.enabled(source.getServer()));
   }

   private static int set(CommandSourceStack source, boolean value) {
      MinecraftServer server = source.getServer();
      AwakenedConfigs.Saved where = UniquePowerRoll.set(server, value);
      if (ConfigFeedback.failed(source, where, () -> Component.translatable("commands.awakened.uniquepowers.already", new Object[]{OnOffCommand.state(value)}))
         )
       {
         return 0;
      } else {
         source.sendSuccess(() -> Component.translatable("commands.awakened.uniquepowers.set", new Object[]{OnOffCommand.state(value)}), true);
         ConfigFeedback.scope(source, where, ServerRules.UNIQUE_POWERS);
         describeLimits(source);
         if (!value && (customLimits() > 0 || PowerLimits.defaultLimit() != (Integer)ServerRules.DEFAULT_LIMIT.getDefault())) {
            source.sendSuccess(() -> Component.translatable("commands.awakened.uniquepowers.set.off.kept").withStyle(ChatFormatting.GRAY), false);
         }

         return value ? 1 : 0;
      }
   }

   private static void describeLimits(CommandSourceStack source) {
      source.sendSuccess(
         () -> Component.translatable("commands.awakened.uniquepowers.defaultlimit.status", new Object[]{limitText(PowerLimits.defaultLimit())}), false
      );
      int own = customLimits();
      if (own > 0) {
         source.sendSuccess(
            () -> Component.translatable(
                     own == 1 ? "commands.awakened.uniquepowers.overrides.one" : "commands.awakened.uniquepowers.overrides", new Object[]{own}
                  )
                  .withStyle(ChatFormatting.GRAY),
            false
         );
      }
   }

   private static int customLimits() {
      return (int)PowerRegistry.rollable().stream().filter(power -> PowerLimits.hasOwnLimit(power.id())).count();
   }

   private static int listLimits(CommandSourceStack source) {
      MinecraftServer server = source.getServer();
      Map<String, Integer> holders = PowerService.holderCounts(server, null);
      source.sendSuccess(() -> Component.translatable("commands.awakened.uniquepowers.limits.header").withStyle(ChatFormatting.GOLD), false);
      PowerRegistry.rollable().stream().sorted(Comparator.comparing(Power::id)).forEach(power -> source.sendSuccess(() -> entry(power, holders), false));
      noteIfInactive(source, server, "commands.awakened.uniquepowers.limits.inactive");
      return PowerRegistry.rollable().size();
   }

   private static int reportLimit(CommandSourceStack source, String powerId) {
      MinecraftServer server = source.getServer();
      Power power = PowerRegistry.get(powerId);
      if (power == null) {
         source.sendFailure(Component.translatable("commands.awakened.uniquepowers.unknown", new Object[]{powerId}));
         return 0;
      } else {
         Map<String, Integer> holders = PowerService.holderCounts(server, null);
         source.sendSuccess(() -> entry(power, holders), false);
         return PowerLimits.limit(powerId);
      }
   }

   private static int setLimit(CommandSourceStack source, String powerId, int count) {
      MinecraftServer server = source.getServer();
      Power power = PowerRegistry.get(powerId);
      if (power == null) {
         source.sendFailure(Component.translatable("commands.awakened.uniquepowers.unknown", new Object[]{powerId}));
         return 0;
      } else {
         AwakenedConfigs.Saved where = PowerLimits.set(server, powerId, count);
         if (ConfigFeedback.failed(
            source, where, () -> Component.translatable("commands.awakened.uniquepowers.limit.already", new Object[]{power.displayName(), limitText(count)})
         )) {
            return 0;
         } else {
            source.sendSuccess(
               () -> Component.translatable("commands.awakened.uniquepowers.limit.set", new Object[]{power.displayName(), limitText(count)}), true
            );
            ConfigFeedback.scope(source, where, ServerRules.LIMITS);
            noteIfInactive(source, server, "commands.awakened.uniquepowers.limit.saved");
            return Math.max(1, count);
         }
      }
   }

   private static int resetLimit(CommandSourceStack source, String powerId) {
      MinecraftServer server = source.getServer();
      Power power = PowerRegistry.get(powerId);
      if (power == null) {
         source.sendFailure(Component.translatable("commands.awakened.uniquepowers.unknown", new Object[]{powerId}));
         return 0;
      } else {
         AwakenedConfigs.Saved where = PowerLimits.set(server, powerId, null);
         if (ConfigFeedback.failed(
            source,
            where,
            () -> Component.translatable(
                  "commands.awakened.uniquepowers.limit.reset.already", new Object[]{power.displayName(), limitText(PowerLimits.limit(powerId))}
               )
         )) {
            return 0;
         } else {
            source.sendSuccess(
               () -> Component.translatable(
                     "commands.awakened.uniquepowers.limit.reset", new Object[]{power.displayName(), limitText(PowerLimits.limit(powerId))}
                  ),
               true
            );
            ConfigFeedback.scope(source, where, ServerRules.LIMITS);
            return 1;
         }
      }
   }

   private static int setDefaultLimit(CommandSourceStack source, int count) {
      MinecraftServer server = source.getServer();
      AwakenedConfigs.Saved where = PowerLimits.setDefault(server, count);
      if (ConfigFeedback.failed(
         source, where, () -> Component.translatable("commands.awakened.uniquepowers.defaultlimit.already", new Object[]{limitText(count)})
      )) {
         return 0;
      } else {
         source.sendSuccess(() -> Component.translatable("commands.awakened.uniquepowers.defaultlimit.set", new Object[]{limitText(count)}), true);
         ConfigFeedback.scope(source, where, ServerRules.DEFAULT_LIMIT);
         noteIfInactive(source, server, "commands.awakened.uniquepowers.limit.saved");
         return Math.max(1, count);
      }
   }

   private static int resetDefaultLimit(CommandSourceStack source) {
      return setDefaultLimit(source, (Integer)ServerRules.DEFAULT_LIMIT.getDefault());
   }

   private static Component entry(Power power, Map<String, Integer> holders) {
      int held = holders.getOrDefault(power.id(), 0);
      boolean full = PowerLimits.atCapacity(power.id(), held);
      int limit = PowerLimits.limit(power.id());
      MutableComponent line = limit == 0
         ? Component.translatable("commands.awakened.uniquepowers.limits.entry.any", new Object[]{power.displayName(), held})
         : Component.translatable("commands.awakened.uniquepowers.limits.entry", new Object[]{power.displayName(), held, limitText(limit)});
      return line.withStyle(full ? ChatFormatting.RED : ChatFormatting.WHITE);
   }

   private static Component limitText(int limit) {
      if (limit == 0) {
         return Component.translatable("commands.awakened.uniquepowers.players.any");
      } else {
         return limit == 1
            ? Component.translatable("commands.awakened.uniquepowers.players.one")
            : Component.translatable("commands.awakened.uniquepowers.players.many", new Object[]{limit});
      }
   }

   private static void noteIfInactive(CommandSourceStack source, MinecraftServer server, String key) {
      if (!UniquePowerRoll.enabled(server)) {
         source.sendSuccess(() -> Component.translatable(key).withStyle(ChatFormatting.GRAY), false);
      }
   }
}
