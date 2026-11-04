package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.core.PowerService;
import java.util.function.ToIntFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class CommandMastery {
   private static ToIntFunction<Player> clientView = viewer -> 0;

   private CommandMastery() {
   }

   public static void clientView(ToIntFunction<Player> view) {
      clientView = view;
   }

   public static int of(Player viewer) {
      return viewer instanceof ServerPlayer server ? PowerService.data(server).masteryLevel() : clientView.applyAsInt(viewer);
   }
}
