package com.devilfruits.fruits.bomu_bomu;

import com.devilfruits.core.fruit.DevilFruitItem;
import com.devilfruits.fruits.bomu_bomu.item.BombPistolItem;
import com.devilfruits.registry.DFCreativeTabs;
import com.devilfruits.registry.DFDataComponents;
import com.devilfruits.registry.DFItems;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class BomuItems {
   public static final DeferredItem<DevilFruitItem> FRUIT = DFItems.ITEMS
      .register("bomu_bomu_no_mi", () -> new DevilFruitItem(BomuBomu.ID, DevilFruitItem.properties(Rarity.EPIC)));
   public static final DeferredItem<BombPistolItem> BOMB_PISTOL = DFItems.ITEMS
      .register("bomb_pistol", () -> new BombPistolItem(new Properties().stacksTo(1).durability(640).rarity(Rarity.RARE)));
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ROUNDS = DFDataComponents.COMPONENTS
      .registerComponentType("bomu_bomu/rounds", b -> b.persistent(Codec.intRange(0, 64)).networkSynchronized(ByteBufCodecs.VAR_INT));
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> BOMB_BREATH = DFDataComponents.COMPONENTS
      .registerComponentType("bomu_bomu/bomb_breath", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

   static void init() {
      DFCreativeTabs.addExtra(BOMB_PISTOL);
   }

   private BomuItems() {
   }
}
