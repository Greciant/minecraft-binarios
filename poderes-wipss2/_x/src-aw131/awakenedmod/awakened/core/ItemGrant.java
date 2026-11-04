package com.awakenedmod.awakened.core;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ItemGrant {
   private ItemGrant() {
   }

   public static void to(Player player, ItemStack stack) {
      if (!stack.isEmpty()) {
         if (!player.addItem(stack)) {
            player.drop(stack, false);
         }
      }
   }
}
