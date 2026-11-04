package com.devilfruits.core.gacha;

import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.network.DFNetwork;
import com.devilfruits.registry.DFItems;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CasketCommand {
   public static LiteralArgumentBuilder<CommandSourceStack> node() {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("casket")
                  .then(
                     Commands.literal("give")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                                 .executes(ctx -> give(ctx, EntityArgument.getPlayers(ctx, "targets"), (Item)DFItems.CASKET.get(), 1)))
                              .then(
                                 Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                    .executes(
                                       ctx -> give(
                                             ctx,
                                             EntityArgument.getPlayers(ctx, "targets"),
                                             (Item)DFItems.CASKET.get(),
                                             IntegerArgumentType.getInteger(ctx, "count")
                                          )
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("key")
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                              .executes(ctx -> give(ctx, EntityArgument.getPlayers(ctx, "targets"), (Item)DFItems.GOLD_KEY.get(), 1)))
                           .then(
                              Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                 .executes(
                                    ctx -> give(
                                          ctx,
                                          EntityArgument.getPlayers(ctx, "targets"),
                                          (Item)DFItems.GOLD_KEY.get(),
                                          IntegerArgumentType.getInteger(ctx, "count")
                                       )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("roll")
                  .then(
                     Commands.argument("rolls", IntegerArgumentType.integer(1, 1000000))
                        .executes(ctx -> roll(ctx, IntegerArgumentType.getInteger(ctx, "rolls")))
                  )
            ))
         .then(
            Commands.literal("preview")
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("tier", StringArgumentType.word())
                        .suggests(
                           (ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(GachaTier.values()).map(t -> t.name().toLowerCase(Locale.ROOT)), b)
                        )
                        .executes(ctx -> preview(ctx, StringArgumentType.getString(ctx, "tier"), null)))
                     .then(
                        Commands.argument("fruit", ResourceLocationArgument.id())
                           .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(FruitRegistry.ids(), b))
                           .executes(ctx -> preview(ctx, StringArgumentType.getString(ctx, "tier"), ResourceLocationArgument.getId(ctx, "fruit")))
                     )
               )
         );
   }

   private static int give(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, Item item, int count) {
      for (ServerPlayer player : targets) {
         int i = 0;

         while (i < count) {
            ItemStack stack = new ItemStack(item, Math.min(count - i, item.getDefaultMaxStackSize()));
            i += stack.getCount();
            if (!player.getInventory().add(stack)) {
               player.drop(stack, false);
            }
         }
      }

      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(
            () -> Component.translatable("commands.devilfruits.casket.give", new Object[]{count, new ItemStack(item).getHoverName(), targets.size()}), true
         );
      return targets.size();
   }

   private static int roll(CommandContext<CommandSourceStack> ctx, int rolls) {
      List<GachaRoll.Candidate<DevilFruit>> candidates = GachaRoll.candidates(((CommandSourceStack)ctx.getSource()).getServer());
      if (!GachaRoll.rollable(candidates, CoreConfig::gachaTierWeight)) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("message.devilfruits.casket.no_fruit"));
         return 0;
      } else {
         RandomSource random = RandomSource.create();
         Map<GachaTier, Integer> tiers = new EnumMap<>(GachaTier.class);
         Map<DevilFruit, Integer> fruits = new LinkedHashMap<>();

         for (int i = 0; i < rolls; i++) {
            GachaRoll.Candidate<DevilFruit> c = GachaRoll.roll(candidates, CoreConfig::gachaTierWeight, random).orElseThrow();
            tiers.merge(c.tier(), 1, Integer::sum);
            fruits.merge(c.value(), 1, Integer::sum);
         }

         CommandSourceStack source = (CommandSourceStack)ctx.getSource();
         source.sendSuccess(() -> Component.translatable("commands.devilfruits.casket.roll", new Object[]{rolls, candidates.size()}), false);

         for (GachaTier tier : GachaTier.values()) {
            int n = tiers.getOrDefault(tier, 0);
            source.sendSuccess(
               () -> Component.literal("  ")
                     .append(Component.translatable(tier.translationKey()).withStyle(tier.color()))
                     .append(
                        Component.literal(String.format(Locale.ROOT, "  %d  (%.1f %%)", n, 100.0 * (double)n / (double)rolls)).withStyle(ChatFormatting.GRAY)
                     ),
               false
            );

            for (GachaRoll.Candidate<DevilFruit> c : candidates) {
               if (c.tier() == tier) {
                  int f = fruits.getOrDefault(c.value(), 0);
                  source.sendSuccess(
                     () -> Component.literal("    - ")
                           .append(c.value().displayName())
                           .append(
                              Component.literal(String.format(Locale.ROOT, "  %d  (%.1f %%)", f, 100.0 * (double)f / (double)rolls))
                                 .withStyle(ChatFormatting.DARK_GRAY)
                           ),
                     false
                  );
               }
            }
         }

         return rolls;
      }
   }

   private static int preview(CommandContext<CommandSourceStack> ctx, String tierName, ResourceLocation fruitId) throws CommandSyntaxException {
      ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();

      GachaTier tier;
      try {
         tier = GachaTier.valueOf(tierName.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var9) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("commands.devilfruits.casket.unknown_tier", new Object[]{tierName}));
         return 0;
      }

      DevilFruit fruit = fruitId != null ? FruitRegistry.get(fruitId) : null;
      if (fruit == null) {
         List<DevilFruit> ofTier = FruitRegistry.playable().stream().filter(f -> f.hasItem() && f.config().gachaTier() == tier).toList();
         List<DevilFruit> pool = ofTier.isEmpty() ? FruitRegistry.playable().stream().filter(DevilFruit::hasItem).toList() : ofTier;
         Optional<DevilFruit> any = pool.isEmpty() ? Optional.empty() : Optional.of(pool.get(player.getRandom().nextInt(pool.size())));
         if (any.isEmpty()) {
            ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("message.devilfruits.casket.no_fruit"));
            return 0;
         }

         fruit = any.get();
      }

      DFNetwork.sendTo(
         player,
         new CasketRevealPayload(
            player.getId(), tier, fruit.id(), player.getRandom().nextLong(), CasketOpening.revealTicks(), (Boolean)CoreConfig.CASKET_CINEMATIC.get(), true
         )
      );
      DevilFruit shown = fruit;
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(
            () -> Component.translatable(
                  "commands.devilfruits.casket.preview", new Object[]{Component.translatable(tier.translationKey()), shown.displayName()}
               ),
            false
         );
      return 1;
   }

   private CasketCommand() {
   }
}
