package com.devilfruits.fruits.gura_gura;

import com.devilfruits.core.fruit.DevilFruitItem;
import com.devilfruits.fruits.gura_gura.item.BisentoItem;
import com.devilfruits.registry.DFItems;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.neoforge.registries.DeferredItem;

public final class GuraItems {
   public static final DeferredItem<DevilFruitItem> FRUIT = DFItems.ITEMS
      .register("gura_gura_no_mi", () -> new DevilFruitItem(GuraGura.ID, DevilFruitItem.properties(Rarity.EPIC)));
   public static final DeferredItem<BisentoItem> BISENTO = DFItems.ITEMS
      .register(
         "bisento",
         () -> new BisentoItem(Tiers.NETHERITE, new Properties().rarity(Rarity.EPIC).fireResistant().attributes(BisentoItem.createBisentoAttributes()))
      );

   static void init() {
   }

   private GuraItems() {
   }
}
