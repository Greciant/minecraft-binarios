package com.devilfruits.core.limb;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.fx.ServerFx;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class LimbTestCommand {
   public static final ResourceLocation FX = DevilFruits.id("limb_test");
   public static final List<String> SKINS = List.of("own", "classic", "slim", "hd");
   public static final List<String> MODES = List.of("off", "12px", "3", "10", "20", "bent", "leg", "thin", "twist");

   public static LiteralArgumentBuilder<CommandSourceStack> node() {
      return (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("gomu")
         .then(
            Commands.literal("limbtest")
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("skin", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("own", "classic", "slim", "hd", "off"), b))
                        .executes(ctx -> run(ctx, StringArgumentType.getString(ctx, "skin"), "10")))
                     .then(
                        Commands.argument("length", StringArgumentType.word())
                           .suggests((c, b) -> SharedSuggestionProvider.suggest(MODES, b))
                           .executes(ctx -> run(ctx, StringArgumentType.getString(ctx, "skin"), StringArgumentType.getString(ctx, "length")))
                     )
               )
         );
   }

   private static int run(CommandContext<CommandSourceStack> ctx, String skin, String length) throws CommandSyntaxException {
      ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
      int s = SKINS.indexOf(skin);
      int m = "off".equals(skin) ? 0 : MODES.indexOf(length);
      if ((s >= 0 || "off".equals(skin)) && m >= 0) {
         ServerFx.sendTo(player, FX, player, player.position(), Vec3.ZERO, 0.0F, 0L, Math.max(s, 0) | m << 4);
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("limb test: " + (m == 0 ? "off" : skin + " " + length)), false);
         return 1;
      } else {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("skin: own | classic | slim | hd | off, length: " + String.join(" | ", MODES)));
         return 0;
      }
   }

   private LimbTestCommand() {
   }
}
