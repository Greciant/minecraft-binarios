package com.awakenedmod.awakened.content.adamantium;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class PlatingTooltip {
   private PlatingTooltip() {
   }

   @SubscribeEvent
   public static void onItemTooltip(ItemTooltipEvent event) {
      ItemStack stack = event.getItemStack();
      int rounds = AdamantiumPlating.rounds(stack);
      if (rounds > 0 && AdamantiumPlating.armorSlot(stack) != null) {
         event.getToolTip().add(Component.translatable("awakened.adamantium.plating.tooltip", new Object[]{rounds, 3}).withStyle(ChatFormatting.GRAY));
      }
   }
}
