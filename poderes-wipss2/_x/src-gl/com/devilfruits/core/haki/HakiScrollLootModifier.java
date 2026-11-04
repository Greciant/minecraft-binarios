package com.devilfruits.core.haki;

import com.devilfruits.registry.DFItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class HakiScrollLootModifier extends LootModifier {
   public static final MapCodec<HakiScrollLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, HakiScrollLootModifier::new));

   public HakiScrollLootModifier(LootItemCondition[] conditions) {
      super(conditions);
   }

   protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
      if ((Boolean)HakiConfig.SCROLL_LOOT.get() && (double)context.getRandom().nextFloat() < (Double)HakiConfig.SCROLL_LOOT_CHANCE.get()) {
         generatedLoot.add(new ItemStack((ItemLike)DFItems.HAKI_SCROLL.get()));
      }

      return generatedLoot;
   }

   public MapCodec<? extends IGlobalLootModifier> codec() {
      return CODEC;
   }
}
