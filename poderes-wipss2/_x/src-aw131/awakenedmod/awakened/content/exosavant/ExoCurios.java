package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.curios.CuriosSlots;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

final class ExoCurios {
   private ExoCurios() {
   }

   private static IDynamicStackHandler handler(Player player, ExoPiece piece) {
      return CuriosSlots.stacks(player, piece.curiosSlot());
   }

   static boolean holds(Player player, ExoPiece piece) {
      IDynamicStackHandler handler = handler(player, piece);
      if (handler == null) {
         return false;
      } else {
         for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).is(piece.item())) {
               return true;
            }
         }

         return false;
      }
   }

   static ItemStack take(ServerPlayer player, ExoPiece piece) {
      IDynamicStackHandler handler = handler(player, piece);
      if (handler == null) {
         return ItemStack.EMPTY;
      } else {
         for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).is(piece.item())) {
               ItemStack worn = handler.getStackInSlot(i).copy();
               handler.setStackInSlot(i, ItemStack.EMPTY);
               return worn;
            }
         }

         return ItemStack.EMPTY;
      }
   }

   static boolean equip(ServerPlayer player, ExoPiece piece, ItemStack stack) {
      IDynamicStackHandler handler = handler(player, piece);
      if (handler == null) {
         return false;
      } else {
         for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).isEmpty()) {
               handler.setStackInSlot(i, stack);
               return true;
            }
         }

         return false;
      }
   }
}
