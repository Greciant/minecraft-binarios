package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class ExoCurioItem extends Item implements ICurioItem {
   public ExoCurioItem(Properties properties) {
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
         PowerData data = PowerService.data(player);
         ExoSuit.update(player, data);
         ExoVisualState.sync(player, data);
      }
   }
}
