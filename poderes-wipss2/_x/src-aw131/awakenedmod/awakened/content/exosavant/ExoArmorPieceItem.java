package com.awakenedmod.awakened.content.exosavant;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class ExoArmorPieceItem extends Item implements Equipable {
   private final EquipmentSlot exoSlot;

   public ExoArmorPieceItem(Properties properties, EquipmentSlot slot) {
      super(properties);
      this.exoSlot = slot;
   }

   public EquipmentSlot getEquipmentSlot() {
      return this.exoSlot;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      return this.swapWithEquipmentSlot(this, level, player, hand);
   }

   public boolean isEnchantable(ItemStack stack) {
      return stack.getMaxStackSize() == 1;
   }

   public int getEnchantmentValue() {
      return 15;
   }
}
