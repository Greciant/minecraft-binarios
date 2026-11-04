package com.devilfruits.fruits.horo_horo;

import com.devilfruits.core.fruit.DevilFruitItem;
import com.devilfruits.registry.DFItems;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

public final class HoroItems {
   public static final DeferredItem<DevilFruitItem> FRUIT = DFItems.ITEMS
      .register("horo_horo_no_mi", () -> new DevilFruitItem(HoroHoro.ID, DevilFruitItem.properties(Rarity.EPIC)));

   static void init() {
   }

   private HoroItems() {
   }
}
