package com.devilfruits.fruits.kage_kage;

import com.devilfruits.core.fruit.DevilFruitItem;
import com.devilfruits.fruits.kage_kage.item.HollowCorpseItem;
import com.devilfruits.fruits.kage_kage.item.MoriaBladeItem;
import com.devilfruits.fruits.kage_kage.item.MoriaScissorsItem;
import com.devilfruits.registry.DFCreativeTabs;
import com.devilfruits.registry.DFDataComponents;
import com.devilfruits.registry.DFItems;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class KageItems {
   public static final DeferredItem<DevilFruitItem> FRUIT = DFItems.ITEMS
      .register("kage_kage_no_mi", () -> new DevilFruitItem(KageKage.ID, DevilFruitItem.properties(Rarity.EPIC)));
   public static final DeferredItem<MoriaScissorsItem> MORIA_SCISSORS = DFItems.ITEMS
      .register("moria_scissors", () -> new MoriaScissorsItem(Tiers.NETHERITE, new Properties().stacksTo(1).fireResistant().rarity(Rarity.RARE)));
   public static final DeferredItem<MoriaBladeItem> MORIA_BLADE = DFItems.ITEMS
      .register("moria_scissors_blade", () -> new MoriaBladeItem(new Properties().stacksTo(1).fireResistant().rarity(Rarity.RARE)));
   public static final DeferredItem<Item> CADAVER_FLESH = DFItems.ITEMS.register("cadaver_flesh", () -> new Item(new Properties()));
   public static final DeferredItem<Item> CADAVER_ARM = DFItems.ITEMS.register("cadaver_arm", () -> new Item(new Properties().stacksTo(16)));
   public static final DeferredItem<Item> CADAVER_LEG = DFItems.ITEMS.register("cadaver_leg", () -> new Item(new Properties().stacksTo(16)));
   public static final DeferredItem<Item> CADAVER_HEAD = DFItems.ITEMS.register("cadaver_head", () -> new Item(new Properties().stacksTo(16)));
   public static final DeferredItem<Item> CADAVER_TORSO = DFItems.ITEMS.register("cadaver_torso", () -> new Item(new Properties().stacksTo(16)));
   public static final DeferredItem<Item> HOLLOW_CORPSE = DFItems.ITEMS
      .register("hollow_corpse", () -> new HollowCorpseItem(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SPLIT = DFDataComponents.COMPONENTS
      .registerComponentType("kage_kage/split", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> OFF_HAND_NEXT = DFDataComponents.COMPONENTS
      .registerComponentType("kage_kage/off_hand_next", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

   static void init() {
      DFCreativeTabs.addExtra(MORIA_SCISSORS);
      DFCreativeTabs.addExtra(CADAVER_FLESH);
      DFCreativeTabs.addExtra(CADAVER_ARM);
      DFCreativeTabs.addExtra(CADAVER_LEG);
      DFCreativeTabs.addExtra(CADAVER_HEAD);
      DFCreativeTabs.addExtra(CADAVER_TORSO);
      DFCreativeTabs.addExtra(HOLLOW_CORPSE);
   }

   private KageItems() {
   }
}
