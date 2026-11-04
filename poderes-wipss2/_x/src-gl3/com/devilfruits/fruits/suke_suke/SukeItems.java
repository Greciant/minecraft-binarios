package com.devilfruits.fruits.suke_suke;

import com.devilfruits.core.fruit.DevilFruitItem;
import com.devilfruits.registry.DFItems;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

public final class SukeItems {
   public static final DeferredItem<DevilFruitItem> FRUIT = DFItems.ITEMS
      .register("suke_suke_no_mi", () -> new DevilFruitItem(SukeSuke.ID, DevilFruitItem.properties(Rarity.EPIC)));

   static void init() {
   }

   private SukeItems() {
   }
}
