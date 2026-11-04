package com.devilfruits.mobs.pirate;

import com.devilfruits.mobs.pirate.entity.PirateCaptain;
import com.devilfruits.mobs.pirate.entity.PirateFighter;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PirateCommand {
   private static final List<String> TYPES = List.of("deckhand", "gunner", "captain");
   private static final SuggestionProvider<CommandSourceStack> TYPE_SUGGESTIONS = (ctx, b) -> SharedSuggestionProvider.suggest(TYPES, b);
   private static final SuggestionProvider<CommandSourceStack> CLIP_SUGGESTIONS = (ctx, b) -> SharedSuggestionProvider.suggest(
         Arrays.asList(PirateClips.NAMES), b
      );

   public static LiteralArgumentBuilder<CommandSourceStack> node() {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Commands.literal("mob")
            .then(
               Commands.literal("pirate")
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("type", StringArgumentType.word())
                           .suggests(TYPE_SUGGESTIONS)
                           .executes(ctx -> spawn(ctx, -1, false)))
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("variant", IntegerArgumentType.integer(0, 7))
                                 .executes(ctx -> spawn(ctx, IntegerArgumentType.getInteger(ctx, "variant"), false)))
                              .then(Commands.literal("crew").executes(ctx -> spawn(ctx, IntegerArgumentType.getInteger(ctx, "variant"), true)))
                        )
                  )
            ))
         .then(Commands.literal("anim").then(Commands.argument("clip", StringArgumentType.word()).suggests(CLIP_SUGGESTIONS).executes(PirateCommand::anim)));
   }

   private static int spawn(CommandContext<CommandSourceStack> ctx, int variant, boolean crew) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      String name = StringArgumentType.getString(ctx, "type");

      EntityType<? extends PirateFighter> type = switch (name) {
         case "deckhand" -> (EntityType)Pirates.DECKHAND.get();
         case "gunner" -> (EntityType)Pirates.GUNNER.get();
         case "captain" -> (EntityType)Pirates.CAPTAIN.get();
         default -> null;
      };
      if (type == null) {
         source.sendFailure(Component.translatable("commands.devilfruits.pirate.unknown", new Object[]{name}));
         return 0;
      } else {
         ServerLevel level = source.getLevel();
         PirateFighter pirate = (PirateFighter)type.create(level);
         if (pirate == null) {
            return 0;
         } else {
            Vec3 at = source.getPosition();
            pirate.moveTo(at.x, at.y, at.z, source.getRotation().y, 0.0F);
            pirate.finalizeSpawn(level, level.getCurrentDifficultyAt(pirate.blockPosition()), MobSpawnType.COMMAND, null);
            if (variant >= 0) {
               pirate.setVariant(variant);
            }

            level.addFreshEntityWithPassengers(pirate);
            if (crew && pirate instanceof PirateCaptain captain) {
               captain.spawnCrew(level, level.getCurrentDifficultyAt(pirate.blockPosition()));
            }

            source.sendSuccess(
               () -> Component.translatable("commands.devilfruits.pirate.spawned", new Object[]{pirate.getDisplayName(), pirate.variant()}), true
            );
            return 1;
         }
      }
   }

   private static int anim(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      String clip = StringArgumentType.getString(ctx, "clip");
      int id = PirateClips.byName(clip);
      if (id < 0) {
         source.sendFailure(Component.translatable("commands.devilfruits.pirate.unknown_clip", new Object[]{clip}));
         return 0;
      } else {
         Vec3 at = source.getPosition();
         PirateFighter nearest = source.getLevel()
            .getEntitiesOfClass(PirateFighter.class, new AABB(at, at).inflate(16.0), LivingEntity::isAlive)
            .stream()
            .min(Comparator.comparingDouble(p -> p.distanceToSqr(at)))
            .orElse(null);
         if (nearest == null) {
            source.sendFailure(Component.translatable("commands.devilfruits.pirate.none"));
            return 0;
         } else {
            nearest.playAction(id, PirateClips.LENGTH[id]);
            source.sendSuccess(() -> Component.translatable("commands.devilfruits.pirate.anim", new Object[]{nearest.getDisplayName(), clip}), false);
            return 1;
         }
      }
   }

   private PirateCommand() {
   }
}
