package com.awakenedmod.awakened.core.command;

import com.awakenedmod.awakened.core.AwakenedConfigs;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

final class ConfigFeedback {
   private ConfigFeedback() {
   }

   static boolean failed(CommandSourceStack source, AwakenedConfigs.Saved where, Supplier<Component> already) {
      if (where.saved()) {
         return false;
      } else if (where == AwakenedConfigs.Saved.UNCHANGED) {
         source.sendSuccess(already, false);
         return true;
      } else {
         source.sendFailure(
            Component.translatable(where == AwakenedConfigs.Saved.FAILED ? "commands.awakened.config.save_failed" : "commands.awakened.config.unavailable")
         );
         return true;
      }
   }

   static void scope(CommandSourceStack source, AwakenedConfigs.Saved where, ConfigValue<?> value) {
      String file = AwakenedConfigs.fileName(value);
      Component line;
      if (source.getServer().isDedicatedServer()) {
         line = Component.translatable("commands.awakened.config.saved_to", new Object[]{"config/" + file});
      } else {
         line = switch (where) {
            case NEW_WORLD_COPY -> Component.translatable("commands.awakened.config.world_copy", new Object[]{file});
            case THIS_WORLD -> Component.translatable("commands.awakened.config.scope_world");
            case SHARED_FILE -> Component.translatable("commands.awakened.config.scope_all");
            default -> null;
         };
      }

      if (line != null) {
         source.sendSuccess(() -> line.copy().withStyle(ChatFormatting.GRAY), false);
      }

      if (source.getServer().isDedicatedServer() && AwakenedConfigs.reachesPlayers(value)) {
         source.sendSuccess(() -> Component.translatable("commands.awakened.config.rejoin").withStyle(ChatFormatting.GRAY), false);
      }
   }
}
