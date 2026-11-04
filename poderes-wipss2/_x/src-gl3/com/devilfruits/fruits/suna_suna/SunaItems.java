package com.devilfruits.fruits.suna_suna;

import com.devilfruits.core.fruit.DevilFruitItem;
import com.devilfruits.registry.DFItems;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

public final class SunaItems {
   public static final DeferredItem<DevilFruitItem> FRUIT = DFItems.ITEMS
      .register("suna_suna_no_mi", () -> new DevilFruitItem(SunaSuna.ID, DevilFruitItem.properties(Rarity.EPIC)));

   static void init() {
   }

   private SunaItems() {
   }
}
