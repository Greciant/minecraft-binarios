package com.devilfruits.datagen;

import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public interface FruitDataContributor {
   FruitDataContributor NONE = new FruitDataContributor() {
   };

   default void itemModels(DFItemModelProvider models) {
   }

   default void blockStates(DFBlockStateProvider states) {
   }

   default void itemTags(DFTagProviders.ItemTagGen tags) {
   }

   default void blockTags(DFTagProviders.BlockTagGen tags) {
   }

   default void damageTypes(BootstrapContext<DamageType> context) {
   }

   default void damageTypeTags(DFTagProviders.DamageTypeTagGen tags) {
   }

   default void advancements(Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper efh) {
   }

   default void recipes(RecipeOutput output) {
   }
}
