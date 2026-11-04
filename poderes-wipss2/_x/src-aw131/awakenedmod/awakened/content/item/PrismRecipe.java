package com.awakenedmod.awakened.content.item;

import com.awakenedmod.awakened.content.AwakenedRecipes;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class PrismRecipe extends CustomRecipe {
   private static final int CENTRE_INDEX = 4;

   public PrismRecipe(CraftingBookCategory category) {
      super(category);
   }

   public boolean matches(CraftingInput input, Level level) {
      return !prism(input).isEmpty();
   }

   public ItemStack assemble(CraftingInput input, Provider registries) {
      return prism(input);
   }

   public RecipeSerializer<? extends CustomRecipe> getSerializer() {
      return (RecipeSerializer<? extends CustomRecipe>)AwakenedRecipes.PRISM.get();
   }

   public boolean canCraftInDimensions(int width, int height) {
      return width >= 3 && height >= 3;
   }

   private static ItemStack prism(CraftingInput input) {
      if (input.width() == 3 && input.height() == 3) {
         if (input.getItem(4).getItem() instanceof DyeItem dye) {
            for (int var3 = 0; var3 < input.size(); var3++) {
               if (var3 != 4 && !input.getItem(var3).is(Items.QUARTZ)) {
                  return ItemStack.EMPTY;
               }
            }

            return PrismItem.of(dye.getDyeColor());
         } else {
            return ItemStack.EMPTY;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }
}
