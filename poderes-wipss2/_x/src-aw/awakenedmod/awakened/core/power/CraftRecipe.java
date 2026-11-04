package com.awakenedmod.awakened.core.power;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public record CraftRecipe(ItemStack result, int width, int height, List<ItemStack> grid, ItemStack station, int unlockLevel) {
   public CraftRecipe(ItemStack result, int width, int height, List<ItemStack> grid, ItemStack station, int unlockLevel) {
      if (width > 0 && height > 0) {
         if (grid.size() != width * height) {
            throw new IllegalArgumentException("grid must hold width*height cells");
         } else {
            grid = List.copyOf(grid);
            this.result = result;
            this.width = width;
            this.height = height;
            this.grid = grid;
            this.station = station;
            this.unlockLevel = unlockLevel;
         }
      } else {
         throw new IllegalArgumentException("recipe grid must be non-empty");
      }
   }

   public boolean hasStation() {
      return !this.station.isEmpty();
   }

   public CraftRecipe withUnlock(int level) {
      return new CraftRecipe(this.result, this.width, this.height, this.grid, this.station, level);
   }

   public static CraftRecipe shaped(ItemLike result, List<String> pattern, Map<Character, ItemLike> key) {
      int height = pattern.size();
      int width = 0;

      for (String row : pattern) {
         width = Math.max(width, row.length());
      }

      List<ItemStack> grid = new ArrayList<>(width * height);

      for (String row : pattern) {
         for (int x = 0; x < width; x++) {
            char c = x < row.length() ? row.charAt(x) : 32;
            ItemLike item = key.get(c);
            grid.add(item == null ? ItemStack.EMPTY : new ItemStack(item));
         }
      }

      return new CraftRecipe(new ItemStack(result), width, height, grid, ItemStack.EMPTY, 0);
   }

   public static CraftRecipe shapeless(ItemLike result, List<? extends ItemLike> ingredients) {
      int count = Math.min(ingredients.size(), 9);
      int width = count <= 3 ? count : 3;
      int height = (count + 2) / 3;
      List<ItemStack> grid = new ArrayList<>(width * height);

      for (int i = 0; i < width * height; i++) {
         grid.add(i < count ? new ItemStack(ingredients.get(i)) : ItemStack.EMPTY);
      }

      return new CraftRecipe(new ItemStack(result), width, height, grid, ItemStack.EMPTY, 0);
   }

   public static CraftRecipe smelting(ItemLike result, ItemLike input) {
      return new CraftRecipe(new ItemStack(result), 1, 1, List.of(new ItemStack(input)), new ItemStack(Items.FURNACE), 0);
   }
}
