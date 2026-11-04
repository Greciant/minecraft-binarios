package com.devilfruits.core.rules;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.Category;
import net.minecraft.world.level.GameRules.Key;

public final class DFGameRules {
   public static Key<BooleanValue> KEEP_FRUIT;

   public static void register() {
      KEEP_FRUIT = GameRules.register("keepFruit", Category.PLAYER, BooleanValue.create(false));
   }

   public static boolean keepFruit(ServerPlayer player) {
      return KEEP_FRUIT != null && player.serverLevel().getGameRules().getBoolean(KEEP_FRUIT);
   }

   private DFGameRules() {
   }
}
