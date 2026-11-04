package com.devilfruits.fruits.gomu_gomu;

import com.devilfruits.core.fruit.DevilFruitItem;
import com.devilfruits.registry.DFItems;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

public final class GomuItems {
   public static final DeferredItem<DevilFruitItem> FRUIT = DFItems.ITEMS
      .register("gomu_gomu_no_mi", () -> new DevilFruitItem(GomuGomu.ID, DevilFruitItem.properties(Rarity.EPIC)));

   static void init() {
   }

   private GomuItems() {
   }
}
