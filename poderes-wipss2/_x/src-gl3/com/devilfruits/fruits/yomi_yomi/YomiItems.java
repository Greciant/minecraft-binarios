package com.devilfruits.fruits.yomi_yomi;

import com.devilfruits.core.fruit.DevilFruitItem;
import com.devilfruits.fruits.yomi_yomi.item.SoulCaneItem;
import com.devilfruits.registry.DFCreativeTabs;
import com.devilfruits.registry.DFDataComponents;
import com.devilfruits.registry.DFItems;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class YomiItems {
   public static final DeferredItem<DevilFruitItem> FRUIT = DFItems.ITEMS
      .register("yomi_yomi_no_mi", () -> new DevilFruitItem(YomiYomi.ID, DevilFruitItem.properties(Rarity.EPIC)));
   public static final DeferredItem<SoulCaneItem> SOUL_CANE = DFItems.ITEMS
      .register("soul_solid", () -> new SoulCaneItem(new Properties().stacksTo(1).rarity(Rarity.RARE)));
   public static final DeferredItem<Item> SCABBARD = DFItems.ITEMS
      .register("soul_solid_scabbard", () -> new Item(new Properties().stacksTo(1).rarity(Rarity.RARE)));
   public static final DeferredItem<Item> SOUL_GUITAR = DFItems.ITEMS.register("soul_guitar", () -> new Item(new Properties().stacksTo(1).rarity(Rarity.EPIC)));
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> DRAWN = DFDataComponents.COMPONENTS
      .registerComponentType("yomi_yomi/drawn", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

   static void init() {
      DFCreativeTabs.addExtra(SOUL_CANE);
      DFCreativeTabs.addExtra(SCABBARD);
      DFCreativeTabs.addExtra(SOUL_GUITAR);
   }

   private YomiItems() {
   }
}
