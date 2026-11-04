package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.exosavant.ExoPiece;
import net.minecraft.world.entity.LivingEntity;

public final class ExoSuitVisibility {
   private ExoSuitVisibility() {
   }

   public static int visibleMask(LivingEntity entity, int mask) {
      if (entity != null && mask != 0) {
         AccessoryVisibility visibility = AccessoryVisibility.of(entity);
         int visible = mask;

         for (ExoPiece piece : ExoPiece.values()) {
            if ((visible & piece.bit()) != 0 && !shows(visibility, piece)) {
               visible &= ~piece.bit();
            }
         }

         return visible;
      } else {
         return mask;
      }
   }

   public static boolean showsChip(LivingEntity entity) {
      return entity != null && showsChip(AccessoryVisibility.of(entity));
   }

   public static boolean showsChip(AccessoryVisibility visibility) {
      return shows(visibility, ExoPiece.CHIP);
   }

   private static boolean shows(AccessoryVisibility visibility, ExoPiece piece) {
      return piece.armorSlot() != null ? !visibility.hidesArmor(piece.armorSlot()) : visibility.shows(piece.curiosSlot(), piece.item());
   }
}
