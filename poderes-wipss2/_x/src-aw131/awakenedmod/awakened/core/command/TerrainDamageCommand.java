package com.awakenedmod.awakened.core.command;

import com.awakenedmod.awakened.Config;
import com.awakenedmod.awakened.core.AwakenedConfigs;
import com.awakenedmod.awakened.core.terrain.BlockBreaking;
import com.awakenedmod.awakened.core.terrain.TempTerrain;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TerrainDamageCommand {
   private TerrainDamageCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(TerrainDamageCommand::onRegister);
   }

   private static void onRegister(RegisterCommandsEvent event) {
      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> modes = (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Commands.literal("blockbreaking")
            .requires(RealisticMovementCommand::isOp))
         .executes(ctx -> report((CommandSourceStack)ctx.getSource()));

      for (BlockBreaking mode : BlockBreaking.values()) {
         modes.then(Commands.literal(mode.name().toLowerCase(Locale.ROOT)).executes(ctx -> set((CommandSourceStack)ctx.getSource(), mode, false)));
      }

      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("awakened").then(modes))
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("terraindamage")
                              .requires(RealisticMovementCommand::isOp))
                           .executes(ctx -> report((CommandSourceStack)ctx.getSource())))
                        .then(Commands.literal("on").executes(ctx -> set((CommandSourceStack)ctx.getSource(), BlockBreaking.PERMANENT, true))))
                     .then(Commands.literal("off").executes(ctx -> set((CommandSourceStack)ctx.getSource(), BlockBreaking.RESTORE, true))))
                  .then(
                     Commands.literal("toggle")
                        .executes(
                           ctx -> set((CommandSourceStack)ctx.getSource(), TempTerrain.permanent() ? BlockBreaking.RESTORE : BlockBreaking.PERMANENT, true)
                        )
                  )
            )
      );
   }

   private static int report(CommandSourceStack source) {
      BlockBreaking mode = TempTerrain.breaking();
      source.sendSuccess(() -> Component.translatable("commands.awakened.blockbreaking.status", new Object[]{name(mode)}), false);
      return mode.ordinal();
   }

   private static int set(CommandSourceStack source, BlockBreaking mode, boolean oldCommand) {
      if (oldCommand && TempTerrain.breaking() == BlockBreaking.OFF) {
         source.sendFailure(Component.translatable("commands.awakened.terraindamage.is_off"));
         return 0;
      } else {
         AwakenedConfigs.Saved where = AwakenedConfigs.write(Config.BLOCK_BREAKING, mode, source.getServer());
         boolean changed = !ConfigFeedback.failed(
            source, where, () -> Component.translatable("commands.awakened.blockbreaking.already", new Object[]{name(mode)})
         );
         if (changed) {
            source.sendSuccess(() -> Component.translatable("commands.awakened.blockbreaking.set", new Object[]{name(mode)}), true);
            ConfigFeedback.scope(source, where, Config.BLOCK_BREAKING);
         }

         if (oldCommand && mode == BlockBreaking.RESTORE && (changed || where == AwakenedConfigs.Saved.UNCHANGED)) {
            source.sendSuccess(() -> Component.translatable("commands.awakened.terraindamage.off_hint").withStyle(ChatFormatting.GRAY), false);
         }

         return changed ? 1 : 0;
      }
   }

   private static Component name(BlockBreaking mode) {
      return Component.translatable("commands.awakened.blockbreaking." + mode.name().toLowerCase(Locale.ROOT))
         .withStyle(mode == BlockBreaking.OFF ? ChatFormatting.RED : ChatFormatting.GREEN);
   }
}
