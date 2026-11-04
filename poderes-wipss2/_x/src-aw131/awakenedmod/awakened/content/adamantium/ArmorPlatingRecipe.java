package com.awakenedmod.awakened.content.adamantium;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.AwakenedRecipes;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class ArmorPlatingRecipe extends CustomRecipe {
   private static final int CENTRE_INDEX = 4;

   public ArmorPlatingRecipe(CraftingBookCategory category) {
      super(category);
   }

   public boolean matches(CraftingInput input, Level level) {
      return !plated(input).isEmpty();
   }

   public ItemStack assemble(CraftingInput input, Provider registries) {
      return plated(input);
   }

   public RecipeSerializer<? extends CustomRecipe> getSerializer() {
      return (RecipeSerializer<? extends CustomRecipe>)AwakenedRecipes.ARMOR_PLATING.get();
   }

   public boolean canCraftInDimensions(int width, int height) {
      return width >= 3 && height >= 3;
   }

   private static ItemStack plated(CraftingInput input) {
      if (input.width() == 3 && input.height() == 3) {
         ItemStack centre = input.getItem(4);
         if (centre.getCount() == 1 && AdamantiumPlating.isPlatable(centre) && !AdamantiumPlating.isMaxed(centre)) {
            for (int i = 0; i < input.size(); i++) {
               if (i != 4 && !input.getItem(i).is((Item)AwakenedItems.ADAMANTIUM_INGOT.get())) {
                  return ItemStack.EMPTY;
               }
            }

            return AdamantiumPlating.withRounds(centre, AdamantiumPlating.rounds(centre) + 1);
         } else {
            return ItemStack.EMPTY;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }
}
