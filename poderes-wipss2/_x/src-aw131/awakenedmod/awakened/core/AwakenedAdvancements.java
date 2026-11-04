package com.awakenedmod.awakened.core;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;

public final class AwakenedAdvancements {
   public static final String ROOT = "awakened/root";
   public static final String USE_ABILITY = "awakened/use_ability";
   public static final String UNLOCK_ABILITY = "awakened/unlock_ability";
   public static final String REROLL = "awakened/reroll";
   private static final String CRITERION = "awarded";

   private AwakenedAdvancements() {
   }

   public static void grant(ServerPlayer player, String path) {
      MinecraftServer server = player.serverLevel().getServer();
      if (server != null) {
         ServerAdvancementManager manager = server.getAdvancements();
         AdvancementHolder holder = manager.get(AwId.of(path));
         if (holder != null) {
            player.getAdvancements().award(holder, "awarded");
         }
      }
   }
}
