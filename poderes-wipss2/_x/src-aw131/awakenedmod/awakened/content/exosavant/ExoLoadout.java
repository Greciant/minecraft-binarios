package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.ExoGear;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;

public final class ExoLoadout {
   private ExoLoadout() {
   }

   public static int equippedMask(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      if (!isExosavant(data)) {
         return 0;
      } else {
         int level = data.masteryLevel();
         int mask = 0;

         for (ExoPiece piece : ExoPiece.values()) {
            if (piece.countsAsSuit() && level >= piece.threshold() && present(player, piece)) {
               mask |= piece.bit();
            }
         }

         return mask;
      }
   }

   public static int equippedCount(ServerPlayer player) {
      return Integer.bitCount(equippedMask(player));
   }

   public static boolean anyEquipped(ServerPlayer player) {
      return equippedMask(player) != 0;
   }

   public static boolean hasPiece(ServerPlayer player, ExoPiece piece) {
      PowerData data = PowerService.data(player);
      return !isExosavant(data) ? false : data.masteryLevel() >= piece.threshold() && present(player, piece);
   }

   public static boolean chipInstalled(ServerPlayer player) {
      return hasPiece(player, ExoPiece.CHIP);
   }

   private static boolean present(ServerPlayer player, ExoPiece piece) {
      EquipmentSlot slot = piece.armorSlot();
      if (slot != null) {
         return player.getItemBySlot(slot).is(piece.item());
      } else {
         return ExoCurios.holds(player, piece) ? true : !piece.countsAsSuit() && ExoGear.get(player).get(piece.ordinal()).is(piece.item());
      }
   }

   private static boolean isExosavant(PowerData data) {
      return "exosavant".equals(data.powerId());
   }

   public static boolean usable(ServerPlayer player, ExoPiece piece) {
      if (!usableSuited(player)) {
         return false;
      } else if (!hasPiece(player, piece)) {
         AbilityFeedback.fail(player, "awakened.exosavant.not_built", piece.displayName());
         return false;
      } else {
         return true;
      }
   }

   public static boolean usableSuited(ServerPlayer player) {
      if (ExoSuit.isSuited(player)) {
         return true;
      } else {
         if (chipInstalled(player)) {
            AbilityFeedback.fail(player, "awakened.exosavant.summon_suit");
         } else {
            AbilityFeedback.fail(player, "awakened.exosavant.no_pieces");
         }

         return false;
      }
   }
}
