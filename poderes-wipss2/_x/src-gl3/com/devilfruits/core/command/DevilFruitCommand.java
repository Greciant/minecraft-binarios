package com.devilfruits.core.command;

import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityRegistry;
import com.devilfruits.core.ability.CastResult;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.gacha.CasketCommand;
import com.devilfruits.core.limb.LimbTestCommand;
import com.devilfruits.core.logia.LogiaCounter;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.logia.LogiaState;
import com.devilfruits.core.physics.PhysicsCommand;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.mobs.pirate.PirateCommand;
import com.devilfruits.registry.DFAttachments;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class DevilFruitCommand {
   private static final DynamicCommandExceptionType UNKNOWN_FRUIT = new DynamicCommandExceptionType(
      id -> Component.translatable("commands.devilfruits.unknown_fruit", new Object[]{String.valueOf(id)})
   );
   private static final SuggestionProvider<CommandSourceStack> FRUITS = (ctx, builder) -> SharedSuggestionProvider.suggestResource(FruitRegistry.ids(), builder);
   private static final SuggestionProvider<CommandSourceStack> ABILITIES = (ctx, builder) -> {
      List<ResourceLocation> ids = new ArrayList<>();

      for (DevilFruit fruit : FruitRegistry.all()) {
         fruit.abilities().forEach(a -> ids.add(a.id()));
      }

      return SharedSuggestionProvider.suggestResource(ids, builder);
   };
   private static final SuggestionProvider<CommandSourceStack> FORMS = (ctx, builder) -> {
      Set<String> names = new LinkedHashSet<>();

      for (DevilFruit fruit : FruitRegistry.all()) {
         for (FruitForm form : fruit.forms()) {
            names.add(form.name());
         }
      }

      return SharedSuggestionProvider.suggest(names, builder);
   };
   private static final List<Supplier<LiteralArgumentBuilder<CommandSourceStack>>> NODES = new ArrayList<>();
   private static final SuggestionProvider<CommandSourceStack> LOGIA_STATES = (ctx, builder) -> {
      Set<String> names = new LinkedHashSet<>();

      for (LogiaState state : LogiaState.values()) {
         names.add(state.serializedName());
      }

      return SharedSuggestionProvider.suggest(names, builder);
   };
   private static final SuggestionProvider<CommandSourceStack> LOGIA_COUNTERS = (ctx, builder) -> {
      Set<String> names = new LinkedHashSet<>();

      for (DevilFruit fruit : FruitRegistry.all()) {
         if (fruit.logia() != null) {
            for (LogiaCounter counter : fruit.logia().counters()) {
               names.add(counter.id());
            }
         }
      }

      return SharedSuggestionProvider.suggest(names, builder);
   };

   public static void addNode(Supplier<LiteralArgumentBuilder<CommandSourceStack>> node) {
      NODES.add(node);
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> root = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("devilfruit")
         .requires(source -> source.hasPermission(2));
      NODES.forEach(node -> root.then((ArgumentBuilder)node.get()));
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)root.then(
                                                   PhysicsCommand.node()
                                                ))
                                                .then(PirateCommand.node()))
                                             .then(CasketCommand.node()))
                                          .then(LimbTestCommand.node()))
                                       .then(
                                          Commands.literal("give")
                                             .then(
                                                Commands.argument("targets", EntityArgument.players())
                                                   .then(
                                                      Commands.argument("fruit", ResourceLocationArgument.id())
                                                         .suggests(FRUITS)
                                                         .executes(ctx -> give(ctx, EntityArgument.getPlayers(ctx, "targets"), fruit(ctx)))
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("remove")
                                          .then(
                                             Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> remove(ctx, EntityArgument.getPlayers(ctx, "targets")))
                                          )
                                    ))
                                 .then(
                                    Commands.literal("form")
                                       .then(
                                          Commands.argument("targets", EntityArgument.players())
                                             .then(
                                                Commands.argument("form", StringArgumentType.word())
                                                   .suggests(FORMS)
                                                   .executes(
                                                      ctx -> form(ctx, EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "form"))
                                                   )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("setmastery")
                                    .then(
                                       Commands.argument("targets", EntityArgument.players())
                                          .then(
                                             Commands.argument("level", IntegerArgumentType.integer(1, 10))
                                                .executes(
                                                   ctx -> setMastery(
                                                         ctx, EntityArgument.getPlayers(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "level")
                                                      )
                                                )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("reset")
                                 .then(
                                    Commands.argument("targets", EntityArgument.players())
                                       .executes(ctx -> reset(ctx, EntityArgument.getPlayers(ctx, "targets")))
                                 )
                           ))
                        .then(Commands.literal("list").executes(DevilFruitCommand::list)))
                     .then(
                        Commands.literal("cast")
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("ability", ResourceLocationArgument.id())
                                    .suggests(ABILITIES)
                                    .executes(
                                       ctx -> cast(
                                             ctx,
                                             ((CommandSourceStack)ctx.getSource()).getPlayerOrException(),
                                             ResourceLocationArgument.getId(ctx, "ability"),
                                             40
                                          )
                                    ))
                                 .then(
                                    Commands.argument("ticks", IntegerArgumentType.integer(0, 1200))
                                       .executes(
                                          ctx -> cast(
                                                ctx,
                                                ((CommandSourceStack)ctx.getSource()).getPlayerOrException(),
                                                ResourceLocationArgument.getId(ctx, "ability"),
                                                IntegerArgumentType.getInteger(ctx, "ticks")
                                             )
                                       )
                                 )
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("info").executes(ctx -> info(ctx, ((CommandSourceStack)ctx.getSource()).getPlayerOrException())))
                        .then(Commands.argument("target", EntityArgument.player()).executes(ctx -> info(ctx, EntityArgument.getPlayer(ctx, "target"))))
                  ))
               .then(
                  Commands.literal("worldfruit")
                     .then(
                        Commands.literal("release")
                           .then(Commands.argument("fruit", ResourceLocationArgument.id()).suggests(FRUITS).executes(ctx -> releaseWorldFruit(ctx, fruit(ctx))))
                     )
               ))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("logia")
                     .then(
                        Commands.literal("state")
                           .then(
                              Commands.argument("targets", EntityArgument.players())
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("state", StringArgumentType.word())
                                          .suggests(LOGIA_STATES)
                                          .executes(
                                             ctx -> logiaState(ctx, EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "state"), 200)
                                          ))
                                       .then(
                                          Commands.argument("ticks", IntegerArgumentType.integer(1, 72000))
                                             .executes(
                                                ctx -> logiaState(
                                                      ctx,
                                                      EntityArgument.getPlayers(ctx, "targets"),
                                                      StringArgumentType.getString(ctx, "state"),
                                                      IntegerArgumentType.getInteger(ctx, "ticks")
                                                   )
                                             )
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("counter")
                        .then(
                           Commands.argument("targets", EntityArgument.players())
                              .then(
                                 Commands.argument("counter", StringArgumentType.word())
                                    .suggests(LOGIA_COUNTERS)
                                    .then(
                                       Commands.argument("ticks", IntegerArgumentType.integer(0, 72000))
                                          .executes(
                                             ctx -> logiaCounter(
                                                   ctx,
                                                   EntityArgument.getPlayers(ctx, "targets"),
                                                   StringArgumentType.getString(ctx, "counter"),
                                                   IntegerArgumentType.getInteger(ctx, "ticks")
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static DevilFruit fruit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ResourceLocation id = ResourceLocationArgument.getId(ctx, "fruit");
      DevilFruit fruit = FruitRegistry.get(id);
      if (fruit == null) {
         throw UNKNOWN_FRUIT.create(id);
      } else {
         return fruit;
      }
   }

   private static int give(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, DevilFruit fruit) {
      for (ServerPlayer player : targets) {
         FruitRules.grantPower(player, fruit, true);
      }

      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(() -> Component.translatable("commands.devilfruits.give", new Object[]{fruit.displayName(), targets.size()}), true);
      return targets.size();
   }

   private static int logiaState(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, String name, int ticks) {
      LogiaState state = null;

      for (LogiaState s : LogiaState.values()) {
         if (s.serializedName().equals(name)) {
            state = s;
         }
      }

      if (state == null) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("commands.devilfruits.logia.unknown_state", new Object[]{name}));
         return 0;
      } else {
         int changed = 0;

         for (ServerPlayer player : targets) {
            if (LogiaManager.isLogia(player)) {
               LogiaManager.forceState(player, state, ticks);
               changed++;
            } else {
               ((CommandSourceStack)ctx.getSource())
                  .sendFailure(Component.translatable("commands.devilfruits.logia.not_logia", new Object[]{player.getDisplayName()}));
            }
         }

         int count = changed;
         LogiaState shown = state;
         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(
               () -> Component.translatable("commands.devilfruits.logia.state", new Object[]{count, Component.translatable(shown.translationKey())}), true
            );
         return changed;
      }
   }

   private static int logiaCounter(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, String counter, int ticks) {
      int changed = 0;

      for (ServerPlayer player : targets) {
         if (LogiaManager.forceCounter(player, counter, ticks)) {
            changed++;
         } else {
            ((CommandSourceStack)ctx.getSource())
               .sendFailure(Component.translatable("commands.devilfruits.logia.no_counter", new Object[]{player.getDisplayName(), counter}));
         }
      }

      int count = changed;
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(() -> Component.translatable("commands.devilfruits.logia.counter", new Object[]{count, counter, ticks}), true);
      return changed;
   }

   private static int remove(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
      for (ServerPlayer player : targets) {
         FruitRules.removePower(player);
      }

      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable("commands.devilfruits.remove", new Object[]{targets.size()}), true);
      return targets.size();
   }

   private static int form(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, String form) {
      int changed = 0;

      for (ServerPlayer player : targets) {
         DevilFruit fruit = DFAttachments.data(player).fruit();
         if (fruit != null) {
            String name = form.equals(fruit.baseForm().name()) ? "" : form;
            FormManager.Result result = FormManager.request(player, name, FormManager.Reason.COMMAND);
            if (result != FormManager.Result.SWITCHED && result != FormManager.Result.ALREADY) {
               ((CommandSourceStack)ctx.getSource())
                  .sendFailure(Component.translatable("commands.devilfruits.form.failed", new Object[]{player.getDisplayName(), form, result.name()}));
            } else {
               changed++;
            }
         }
      }

      int count = changed;
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable("commands.devilfruits.form", new Object[]{count, form}), true);
      return changed;
   }

   private static int cast(CommandContext<CommandSourceStack> ctx, ServerPlayer player, ResourceLocation abilityId, int ticks) {
      Ability ability = AbilityRegistry.get(abilityId);
      DevilFruit fruit = ability == null ? null : AbilityRegistry.owner(ability);
      if (fruit == null) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("commands.devilfruits.cast.unknown", new Object[]{abilityId.toString()}));
         return 0;
      } else {
         PlayerFruitData data = DFAttachments.data(player);
         if (!fruit.id().equals(data.fruitId())) {
            FruitRules.grantPower(player, fruit, false);
            data = DFAttachments.data(player);
         }

         if (data.mastery() < ability.config().requiredMastery()) {
            Mastery.setLevel(player, data, ability.config().requiredMastery(), false);
         }

         if (data.form() == null || data.form().find(abilityId) == null) {
            for (FruitForm form : fruit.forms()) {
               if (form.find(abilityId) != null) {
                  FormManager.request(player, form.isBase() ? "" : form.name(), FormManager.Reason.COMMAND);
                  break;
               }
            }
         }

         data.clearCooldowns();
         data.setEnergy((float)Mastery.maxEnergy(data));
         CoreEvents.syncNow(player);
         CastResult result = AbilityManager.press(player, ability);
         CastType type = ability.castType();
         if (type == CastType.CHARGED || type == CastType.HELD) {
            ServerScheduler.later(player.serverLevel(), Math.max(1, ticks), () -> AbilityManager.release(player, ability));
         }

         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(() -> Component.translatable("commands.devilfruits.cast", new Object[]{ability.displayName(), result.name()}), true);
         return 1;
      }
   }

   private static int setMastery(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, int level) {
      for (ServerPlayer player : targets) {
         PlayerFruitData data = DFAttachments.data(player);
         data.setMasteryXp(0.0F);
         Mastery.setLevel(player, data, level, data.hasFruit());
         CoreEvents.syncNow(player);
      }

      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(() -> Component.translatable("commands.devilfruits.setmastery", new Object[]{targets.size(), level}), true);
      return targets.size();
   }

   private static int reset(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
      for (ServerPlayer player : targets) {
         AbilityManager.cancelAll(player);
         PlayerFruitData data = DFAttachments.data(player);
         data.clearCooldowns();
         data.setEnergy((float)Mastery.maxEnergy(data));
         CoreEvents.syncNow(player);
      }

      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable("commands.devilfruits.reset", new Object[]{targets.size()}), true);
      return targets.size();
   }

   private static int list(CommandContext<CommandSourceStack> ctx) {
      WorldFruitData world = WorldFruitData.get(((CommandSourceStack)ctx.getSource()).getServer());
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(() -> Component.translatable("commands.devilfruits.list.header", new Object[]{FruitRegistry.all().size()}), false);

      for (DevilFruit fruit : FruitRegistry.all()) {
         WorldFruitData.State state = world.state(fruit);
         MutableComponent where = state == WorldFruitData.State.EATEN
            ? Component.translatable("commands.devilfruits.list.eaten", new Object[]{world.ownerName(fruit)})
            : Component.translatable("commands.devilfruits.list." + state.name().toLowerCase(Locale.ROOT));
         MutableComponent line = Component.literal(" - ")
            .append(fruit.displayName().withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" [" + fruit.id() + "] ").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.translatable(fruit.type().translationKey()).withStyle(ChatFormatting.GRAY))
            .append(Component.literal(" · " + fruit.forms().size() + "F/" + fruit.abilities().size() + "A · ").withStyle(ChatFormatting.DARK_GRAY))
            .append(where);
         if (fruit.isDevOnly()) {
            line.append(Component.literal(" (dev)").withStyle(ChatFormatting.DARK_PURPLE));
         }

         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> line, false);
      }

      return FruitRegistry.all().size();
   }

   private static int info(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
      PlayerFruitData data = DFAttachments.data(player);
      DevilFruit fruit = data.fruit();
      Component fruitName = fruit == null ? Component.translatable("commands.devilfruits.none") : fruit.displayName();
      Component formName = (Component)(data.form() == null ? Component.literal("-") : data.form().displayName());
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(
            () -> Component.translatable(
                  "commands.devilfruits.info",
                  new Object[]{
                     player.getDisplayName(),
                     fruitName,
                     formName,
                     data.mastery(),
                     Math.round(data.masteryXp()),
                     Math.round(data.energy()),
                     Mastery.maxEnergy(data)
                  }
               ),
            false
         );
      return data.hasFruit() ? 1 : 0;
   }

   private static int releaseWorldFruit(CommandContext<CommandSourceStack> ctx, DevilFruit fruit) {
      WorldFruitData.get(((CommandSourceStack)ctx.getSource()).getServer()).setFree(fruit);
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(() -> Component.translatable("commands.devilfruits.worldfruit.release", new Object[]{fruit.displayName()}), true);
      return 1;
   }

   private DevilFruitCommand() {
   }
}
