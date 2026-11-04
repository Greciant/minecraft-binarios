package com.awakenedmod.awakened.content.spider;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class WebGauntletsItem extends Item implements ICurioItem {
   public static final String SLOT = "gauntlets";

   public WebGauntletsItem(Properties properties) {
      super(properties);
   }

   public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
      resync(slotContext);
   }

   public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
      resync(slotContext);
   }

   private static void resync(SlotContext slotContext) {
      if (slotContext.entity() instanceof ServerPlayer player) {
         SpiderTypeSync.push(player);
      }
   }
}
