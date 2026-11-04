package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.content.adamantium.ArmorPlatingRecipe;
import com.awakenedmod.awakened.content.item.PrismRecipe;
import com.awakenedmod.awakened.content.relic.RelicUpgradeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AwakenedRecipes {
   private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, "awakened");
   public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<RelicUpgradeRecipe>> RELIC_UPGRADE = SERIALIZERS.register(
      "relic_upgrade", () -> new SimpleCraftingRecipeSerializer(RelicUpgradeRecipe::new)
   );
   public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<ArmorPlatingRecipe>> ARMOR_PLATING = SERIALIZERS.register(
      "armor_plating", () -> new SimpleCraftingRecipeSerializer(ArmorPlatingRecipe::new)
   );
   public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<PrismRecipe>> PRISM = SERIALIZERS.register(
      "prism", () -> new SimpleCraftingRecipeSerializer(PrismRecipe::new)
   );

   private AwakenedRecipes() {
   }

   public static void register(IEventBus modEventBus) {
      SERIALIZERS.register(modEventBus);
   }
}
