package com.devilfruits.fruits.supa_supa;

import com.devilfruits.core.fruit.DevilFruitItem;
import com.devilfruits.registry.DFItems;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

public final class SupaItems {
   public static final DeferredItem<DevilFruitItem> FRUIT = DFItems.ITEMS
      .register("supa_supa_no_mi", () -> new DevilFruitItem(SupaSupa.ID, DevilFruitItem.properties(Rarity.EPIC)));

   static void init() {
   }

   private SupaItems() {
   }
}
