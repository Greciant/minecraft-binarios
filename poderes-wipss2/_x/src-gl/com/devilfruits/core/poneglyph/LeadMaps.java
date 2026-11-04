package com.devilfruits.core.poneglyph;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public final class LeadMaps {
   public static ItemStack create(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      BlockPos target = level.findNearestMapStructure(Poneglyphs.RUINS_TAG, player.blockPosition(), (Integer)PoneglyphConfig.LEAD_MAP_RADIUS.get(), true);
      if (target == null) {
         return ItemStack.EMPTY;
      } else {
         ItemStack map = MapItem.create(level, target.getX(), target.getZ(), (byte)2, true, true);
         MapItem.renderBiomePreviewMap(level, map);
         MapItemSavedData.addTargetDecoration(map, target, "+", MapDecorationTypes.RED_X);
         map.set(DataComponents.ITEM_NAME, Component.translatable("item.devilfruits.lead_map").withStyle(ChatFormatting.GOLD));
         return map;
      }
   }

   private LeadMaps() {
   }
}
