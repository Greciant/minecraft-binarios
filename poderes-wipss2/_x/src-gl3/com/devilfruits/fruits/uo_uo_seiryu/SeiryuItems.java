package com.devilfruits.fruits.uo_uo_seiryu;

import com.devilfruits.core.fruit.DevilFruitItem;
import com.devilfruits.fruits.uo_uo_seiryu.item.HassaikaiItem;
import com.devilfruits.registry.DFItems;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.neoforge.registries.DeferredItem;

public final class SeiryuItems {
   public static final DeferredItem<DevilFruitItem> FRUIT = DFItems.ITEMS
      .register("uo_uo_no_mi_seiryu", () -> new DevilFruitItem(UoUoSeiryu.ID, DevilFruitItem.properties(Rarity.EPIC)));
   public static final DeferredItem<HassaikaiItem> HASSAIKAI = DFItems.ITEMS
      .register(
         "hassaikai",
         () -> new HassaikaiItem(Tiers.NETHERITE, new Properties().rarity(Rarity.EPIC).fireResistant().attributes(HassaikaiItem.createAttributes()))
      );

   static void init() {
   }

   private SeiryuItems() {
   }
}
