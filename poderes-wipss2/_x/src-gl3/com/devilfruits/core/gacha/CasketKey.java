package com.devilfruits.core.gacha;

import com.devilfruits.core.config.CoreConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface CasketKey {
   default void onCasketOpened(ServerPlayer player, ItemStack key) {
      if ((Boolean)CoreConfig.CASKET_KEY_CONSUMED.get()) {
         key.shrink(1);
      }
   }
}
