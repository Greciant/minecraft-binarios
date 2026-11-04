package com.devilfruits.fruits.kilo_kilo;

import com.devilfruits.core.fruit.DevilFruitItem;
import com.devilfruits.registry.DFItems;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

public final class KiloItems {
   public static final DeferredItem<DevilFruitItem> FRUIT = DFItems.ITEMS
      .register("kilo_kilo_no_mi", () -> new DevilFruitItem(KiloKilo.ID, DevilFruitItem.properties(Rarity.EPIC)));

   static void init() {
   }

   private KiloItems() {
   }
}
