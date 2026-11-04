package com.devilfruits.fruits.doru_doru.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class WaxLanceItem extends Item {
   public WaxLanceItem(Properties properties) {
      super(properties);
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
      WaxTools.inventoryTick(stack, level, entity, slot, selected);
   }

   public boolean onDroppedByPlayer(ItemStack item, Player player) {
      return false;
   }
}
