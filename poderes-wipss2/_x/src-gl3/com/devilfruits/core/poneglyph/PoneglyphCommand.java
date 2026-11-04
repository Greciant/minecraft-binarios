package com.devilfruits.core.poneglyph;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class PoneglyphCommand {
   private static final SuggestionProvider<CommandSourceStack> KNOWLEDGE = (ctx, b) -> SharedSuggestionProvider.suggestResource(
         PoneglyphData.knowledgeBranches().keySet(), b
      );
   private static final SuggestionProvider<CommandSourceStack> PONEGLYPHS = (ctx, b) -> SharedSuggestionProvider.suggestResource(
         PoneglyphData.poneglyphs().keySet(), b
      );

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("devilfruit").requires(s -> s.hasPermission(2)))
               .then(
                  ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("knowledge")
                           .then(
                              Commands.literal("grant")
                                 .then(
                                    Commands.argument("players", EntityArgument.players())
                                       .then(Commands.argument("id", ResourceLocationArgument.id()).suggests(KNOWLEDGE).executes(ctx -> grant(ctx, true)))
                                 )
                           ))
                        .then(
                           Commands.literal("revoke")
                              .then(
                                 Commands.argument("players", EntityArgument.players())
                                    .then(Commands.argument("id", ResourceLocationArgument.id()).suggests(KNOWLEDGE).executes(ctx -> grant(ctx, false)))
                              )
                        ))
                     .then(Commands.literal("list").then(Commands.argument("player", EntityArgument.player()).executes(PoneglyphCommand::list)))
               ))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("poneglyph")
                     .then(
                        Commands.literal("give")
                           .then(
                              ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("player", EntityArgument.player()).then(give("rubbings")))
                                    .then(give("encrypted")))
                                 .then(give("deciphered"))
                           )
                     ))
                  .then(
                     Commands.literal("literacy")
                        .then(
                           Commands.argument("player", EntityArgument.player())
                              .then(
                                 Commands.argument("level", IntegerArgumentType.integer(0, 5))
                                    .executes(
                                       ctx -> {
                                          ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                          int level = IntegerArgumentType.getInteger(ctx, "level");
                                          Literacy.set(player, level, Literacy.xpFor(level));
                                          ((CommandSourceStack)ctx.getSource())
                                             .sendSuccess(
                                                () -> Component.translatable(
                                                      "commands.devilfruits.poneglyph.literacy", new Object[]{player.getDisplayName(), level}
                                                   ),
                                                true
                                             );
                                          return level;
                                       }
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static LiteralArgumentBuilder<CommandSourceStack> give(String kind) {
      return (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal(kind)
         .then(
            Commands.argument("id", ResourceLocationArgument.id())
               .suggests(PONEGLYPHS)
               .executes(
                  ctx -> {
                     ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                     ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
                     PoneglyphData.Poneglyph def = PoneglyphData.poneglyph(id);
                     if (def == null) {
                        ((CommandSourceStack)ctx.getSource())
                           .sendFailure(Component.translatable("commands.devilfruits.poneglyph.unknown", new Object[]{id.toString()}));
                        return 0;
                     } else {
                        switch (kind) {
                           case "rubbings":
                              for (int cell = 0; cell < 9; cell++) {
                                 hand(player, RubbingService.rubbing(id, cell));
                              }
                              break;
                           case "encrypted": {
                              ItemStack stack = new ItemStack((ItemLike)Poneglyphs.ENCRYPTED_BLUEPRINT.get());
                              stack.set(
                                 (DataComponentType)Poneglyphs.ENCRYPTED.get(),
                                 new BlueprintData.Encrypted(id, ThreadLocalRandom.current().nextLong(), BlueprintData.Progress.EMPTY)
                              );
                              hand(player, stack);
                              break;
                           }
                           default: {
                              ItemStack stack = new ItemStack((ItemLike)Poneglyphs.DECIPHERED_BLUEPRINT.get());
                              stack.set((DataComponentType)Poneglyphs.DECIPHERED.get(), new BlueprintData.Deciphered(def.knowledge(), id));
                              hand(player, stack);
                           }
                        }

                        ((CommandSourceStack)ctx.getSource())
                           .sendSuccess(
                              () -> Component.translatable("commands.devilfruits.poneglyph.given", new Object[]{kind, id.toString(), player.getDisplayName()}),
                              true
                           );
                        return 1;
                     }
                  }
               )
         );
   }

   private static void hand(ServerPlayer player, ItemStack stack) {
      if (!player.getInventory().add(stack)) {
         player.drop(stack, false);
      }
   }

   private static int grant(CommandContext<CommandSourceStack> ctx, boolean grant) throws CommandSyntaxException {
      Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
      ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
      if (PoneglyphData.knowledge(id) == null) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("commands.devilfruits.knowledge.unknown", new Object[]{id.toString()}));
         return 0;
      } else {
         int n = 0;

         for (ServerPlayer player : players) {
            if (grant ? Knowledge.learn(player, id).result() == Knowledge.Result.LEARNED : Knowledge.revoke(player, id)) {
               n++;
            }
         }

         int count = n;
         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(
               () -> Component.translatable(
                     grant ? "commands.devilfruits.knowledge.granted" : "commands.devilfruits.knowledge.revoked", new Object[]{id.toString(), count}
                  ),
               true
            );
         return n;
      }
   }

   private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
      Collection<ResourceLocation> learned = Knowledge.get(player).learned();
      Component names = Component.literal(learned.isEmpty() ? "-" : String.join(", ", learned.stream().map(ResourceLocation::toString).sorted().toList()));
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(
            () -> Component.translatable(
                  "commands.devilfruits.knowledge.list",
                  new Object[]{player.getDisplayName(), learned.size(), names, Literacy.level(player), Literacy.get(player).xp()}
               ),
            false
         );
      return learned.size();
   }

   private PoneglyphCommand() {
   }
}
