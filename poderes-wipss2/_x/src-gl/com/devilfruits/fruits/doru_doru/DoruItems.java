package com.devilfruits.fruits.doru_doru;

import com.devilfruits.core.fruit.DevilFruitItem;
import com.devilfruits.fruits.doru_doru.item.WaxKeyItem;
import com.devilfruits.fruits.doru_doru.item.WaxLanceItem;
import com.devilfruits.fruits.doru_doru.item.WaxTools;
import com.devilfruits.registry.DFCreativeTabs;
import com.devilfruits.registry.DFDataComponents;
import com.devilfruits.registry.DFItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class DoruItems {
   public static final DeferredItem<DevilFruitItem> FRUIT = DFItems.ITEMS
      .register("doru_doru_no_mi", () -> new DevilFruitItem(DoruDoru.ID, DevilFruitItem.properties(Rarity.EPIC)));
   public static final DeferredItem<Item> WAX_LUMP = DFItems.ITEMS.register("wax_lump", () -> new Item(new Properties()));
   public static final DeferredItem<Item> WAX_PICKAXE = DFItems.ITEMS.register("wax_pickaxe", () -> new WaxTools.Pickaxe(tool()));
   public static final DeferredItem<Item> WAX_SHOVEL = DFItems.ITEMS.register("wax_shovel", () -> new WaxTools.Shovel(tool()));
   public static final DeferredItem<Item> WAX_AXE = DFItems.ITEMS.register("wax_axe", () -> new WaxTools.Axe(tool()));
   public static final DeferredItem<Item> WAX_HOE = DFItems.ITEMS.register("wax_hoe", () -> new WaxTools.Hoe(tool()));
   public static final DeferredItem<Item> WAX_SWORD = DFItems.ITEMS.register("wax_sword", () -> new WaxTools.Sword(tool()));
   public static final DeferredItem<Item> WAX_KEY = DFItems.ITEMS.register("wax_key", () -> new WaxKeyItem(new Properties().durability(200)));
   public static final DeferredItem<Item> WAX_LANCE = DFItems.ITEMS.register("wax_lance", () -> new WaxLanceItem(new Properties().stacksTo(1)));
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<WaxTools.Bound>> BOUND = DFDataComponents.COMPONENTS
      .registerComponentType("doru_doru/bound", b -> b.persistent(WaxTools.Bound.CODEC).networkSynchronized(WaxTools.Bound.STREAM_CODEC));

   private static Properties tool() {
      return new Properties().stacksTo(1);
   }

   static void init() {
      DFCreativeTabs.addExtra(WAX_LUMP);
   }

   private DoruItems() {
   }
}
