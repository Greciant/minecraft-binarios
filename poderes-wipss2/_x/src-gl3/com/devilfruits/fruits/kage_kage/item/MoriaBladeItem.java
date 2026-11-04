package com.devilfruits.fruits.kage_kage.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class MoriaBladeItem extends Item {
   public MoriaBladeItem(Properties properties) {
      super(properties);
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
      if (!level.isClientSide() && entity instanceof ServerPlayer player) {
         ItemStack main = player.getMainHandItem();
         if (player.getOffhandItem() != stack || !(main.getItem() instanceof MoriaScissorsItem) || !MoriaScissorsItem.isSplit(main)) {
            MoriaScissorsItem.join(player);
         }
      }
   }

   public boolean onDroppedByPlayer(ItemStack item, Player player) {
      return false;
   }
}
