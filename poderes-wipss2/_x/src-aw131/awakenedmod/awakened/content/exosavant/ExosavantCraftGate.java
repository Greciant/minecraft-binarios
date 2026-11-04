package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;

public final class ExosavantCraftGate {
   private ExosavantCraftGate() {
   }

   public static void hideGatedResult(AbstractContainerMenu menu, Player player, CraftingContainer craftSlots, ResultContainer resultSlots) {
      if (player instanceof ServerPlayer serverPlayer) {
         ItemStack result = resultSlots.getItem(0);
         if (!result.isEmpty()) {
            ExoPiece piece = ExoPiece.forItem(result.getItem());
            if (piece != null && !canCraft(serverPlayer, piece) && !gridHolds(craftSlots, result)) {
               resultSlots.setItem(0, ItemStack.EMPTY);
               menu.setRemoteSlot(0, ItemStack.EMPTY);
               serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY));
            }
         }
      }
   }

   private static boolean gridHolds(CraftingContainer craftSlots, ItemStack result) {
      for (int i = 0; i < craftSlots.getContainerSize(); i++) {
         if (craftSlots.getItem(i).is(result.getItem())) {
            return true;
         }
      }

      return false;
   }

   private static boolean canCraft(ServerPlayer player, ExoPiece piece) {
      PowerData data = PowerService.data(player);
      boolean isExosavant = data.hasPower() && data.powerId().equals("exosavant");
      return isExosavant && data.masteryLevel() >= piece.threshold();
   }
}
