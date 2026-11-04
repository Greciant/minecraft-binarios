package com.devilfruits.core.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;

public class SeastoneItem extends Item {
   public SeastoneItem(Properties properties) {
      super(properties);
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      addLore(tooltip);
   }

   static void addLore(List<Component> tooltip) {
      tooltip.add(Component.translatable("item.devilfruits.seastone.lore").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
      tooltip.add(Component.translatable("item.devilfruits.seastone.hint").withStyle(ChatFormatting.DARK_AQUA));
   }

   public static class Block extends BlockItem {
      public Block(net.minecraft.world.level.block.Block block, Properties properties) {
         super(block, properties);
      }

      public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
         SeastoneItem.addLore(tooltip);
      }
   }
}
