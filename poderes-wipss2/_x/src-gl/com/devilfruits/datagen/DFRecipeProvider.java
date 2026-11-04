package com.devilfruits.datagen;

import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModules;
import com.devilfruits.registry.DFItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public class DFRecipeProvider extends RecipeProvider {
   public DFRecipeProvider(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries);
   }

   public static Criterion<TriggerInstance> hasItem(ItemLike item) {
      return has(item);
   }

   protected void buildRecipes(RecipeOutput output) {
      ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, (ItemLike)DFItems.GOLD_KEY.get())
         .requires(Items.GOLD_NUGGET, 3)
         .unlockedBy("has_gold_nugget", has(Items.GOLD_NUGGET))
         .save(output);

      for (FruitModule module : FruitModules.all()) {
         if (DFTagProviders.isPlayable(module)) {
            module.data().recipes(output);
         }
      }
   }
}
