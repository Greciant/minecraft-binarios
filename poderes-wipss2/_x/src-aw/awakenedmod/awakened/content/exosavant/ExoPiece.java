package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.content.AwakenedItems;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

public enum ExoPiece {
   GAUNTLETS("exosavant_gauntlets", 0, null, "gauntlets", AwakenedItems.EXOSAVANT_GAUNTLETS),
   BOOTS("exosavant_boots", 3, EquipmentSlot.FEET, null, AwakenedItems.EXOSAVANT_BOOTS),
   CHESTPLATE("exosavant_chestplate", 5, EquipmentSlot.CHEST, null, AwakenedItems.EXOSAVANT_CHESTPLATE),
   LEGGINGS("exosavant_leggings", 7, EquipmentSlot.LEGS, null, AwakenedItems.EXOSAVANT_LEGGINGS),
   HELMET("exosavant_helmet", 10, EquipmentSlot.HEAD, null, AwakenedItems.EXOSAVANT_HELMET),
   CHIP("exosavant_chip", 13, null, "core", AwakenedItems.EXOSAVANT_CHIP);

   private final String itemId;
   private final int threshold;
   private final EquipmentSlot armorSlot;
   private final String curiosSlot;
   private final Supplier<Item> item;
   public static final int ALL_MASK = (1 << values().length) - 1;
   public static final int WORN_COUNT = values().length - 1;

   private ExoPiece(String itemId, int threshold, EquipmentSlot armorSlot, String curiosSlot, Supplier<Item> item) {
      this.itemId = itemId;
      this.threshold = threshold;
      this.armorSlot = armorSlot;
      this.curiosSlot = curiosSlot;
      this.item = item;
   }

   public int bit() {
      return 1 << this.ordinal();
   }

   public int threshold() {
      return this.threshold;
   }

   public Item item() {
      return this.item.get();
   }

   public EquipmentSlot armorSlot() {
      return this.armorSlot;
   }

   public String curiosSlot() {
      return this.curiosSlot;
   }

   public Component displayName() {
      return Component.translatable("item.awakened." + this.itemId);
   }

   public Component shortName() {
      return Component.translatable("awakened.exosavant.piece." + this.name().toLowerCase(Locale.ROOT));
   }

   public boolean countsAsSuit() {
      return this != CHIP;
   }

   public static ExoPiece forItem(Item item) {
      for (ExoPiece p : values()) {
         if (p.item() == item) {
            return p;
         }
      }

      return null;
   }
}
