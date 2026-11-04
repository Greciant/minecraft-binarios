package com.devilfruits.fruits.doru_doru.item;

import com.devilfruits.core.gacha.CasketKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

public class WaxKeyItem extends Item implements CasketKey {
   public WaxKeyItem(Properties properties) {
      super(properties);
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
      WaxTools.inventoryTick(stack, level, entity, slot, selected);
   }

   @Override
   public void onCasketOpened(ServerPlayer player, ItemStack key) {
      WaxTools.dissolve(player, key, 40, false);
   }

   public boolean onDroppedByPlayer(ItemStack item, Player player) {
      return false;
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.devilfruits.wax_key.desc").withStyle(ChatFormatting.GRAY));
   }
}
