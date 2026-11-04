package com.devilfruits.core.poneglyph;

import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class BlueprintAssemblyRecipe extends CustomRecipe {
   public BlueprintAssemblyRecipe(CraftingBookCategory category) {
      super(category);
   }

   @Nullable
   public static ResourceLocation assembled(CraftingInput input) {
      if (input.width() == 3 && input.height() == 3) {
         ResourceLocation poneglyph = null;

         for (int i = 0; i < 9; i++) {
            ItemStack stack = input.getItem(i);
            BlueprintData.Rubbing rubbing = stack.is((Item)Poneglyphs.RUBBING_ITEM.get())
               ? (BlueprintData.Rubbing)stack.get((DataComponentType)Poneglyphs.RUBBING.get())
               : null;
            if (rubbing == null || rubbing.cell() != i || poneglyph != null && !poneglyph.equals(rubbing.poneglyph())) {
               return null;
            }

            poneglyph = rubbing.poneglyph();
         }

         return poneglyph;
      } else {
         return null;
      }
   }

   public boolean matches(CraftingInput input, Level level) {
      return assembled(input) != null;
   }

   public ItemStack assemble(CraftingInput input, Provider registries) {
      ResourceLocation poneglyph = assembled(input);
      if (poneglyph == null) {
         return ItemStack.EMPTY;
      } else {
         ItemStack out = new ItemStack((ItemLike)Poneglyphs.ENCRYPTED_BLUEPRINT.get());
         out.set(
            (DataComponentType)Poneglyphs.ENCRYPTED.get(),
            new BlueprintData.Encrypted(poneglyph, ThreadLocalRandom.current().nextLong(), BlueprintData.Progress.EMPTY)
         );
         return out;
      }
   }

   public boolean canCraftInDimensions(int width, int height) {
      return width >= 3 && height >= 3;
   }

   public RecipeSerializer<?> getSerializer() {
      return Poneglyphs.BLUEPRINT_ASSEMBLY.get();
   }
}
