package com.awakenedmod.awakened.content.relic;

import com.awakenedmod.awakened.content.AwakenedRecipes;
import com.awakenedmod.awakened.core.relic.Relic;
import com.awakenedmod.awakened.core.relic.RelicRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class RelicUpgradeRecipe extends CustomRecipe {
   private static final int REQUIRED = 4;

   public RelicUpgradeRecipe(CraftingBookCategory category) {
      super(category);
   }

   public boolean matches(CraftingInput input, Level level) {
      return upgradeId(input) != null;
   }

   public ItemStack assemble(CraftingInput input, Provider registries) {
      String upgrade = upgradeId(input);
      return upgrade == null ? ItemStack.EMPTY : RelicItem.of(upgrade);
   }

   public RecipeSerializer<? extends CustomRecipe> getSerializer() {
      return (RecipeSerializer<? extends CustomRecipe>)AwakenedRecipes.RELIC_UPGRADE.get();
   }

   public boolean canCraftInDimensions(int width, int height) {
      return width >= 2 && height >= 2;
   }

   private static String upgradeId(CraftingInput input) {
      String fromId = matchedRelicId(input);
      if (fromId == null) {
         return null;
      } else {
         Relic from = RelicRegistry.get(fromId);
         if (from == null) {
            return null;
         } else {
            Rarity next = nextRarity(from.rarity());
            return next == null ? null : rollUpgrade(from, next);
         }
      }
   }

   private static String matchedRelicId(CraftingInput input) {
      String id = null;
      int occupied = 0;

      for (int i = 0; i < input.size(); i++) {
         ItemStack stack = input.getItem(i);
         if (!stack.isEmpty()) {
            if (++occupied > 4 || !(stack.getItem() instanceof RelicItem)) {
               return null;
            }

            String stackId = RelicItem.relicId(stack);
            if (stackId == null) {
               return null;
            }

            if (id == null) {
               id = stackId;
            } else if (!id.equals(stackId)) {
               return null;
            }
         }
      }

      return occupied == 4 ? id : null;
   }

   private static Rarity nextRarity(Rarity rarity) {
      return switch (rarity) {
         case COMMON -> Rarity.UNCOMMON;
         case UNCOMMON -> Rarity.RARE;
         case RARE -> Rarity.EPIC;
         case EPIC -> null;
         default -> throw new MatchException(null, null);
      };
   }

   private static String rollUpgrade(Relic from, Rarity next) {
      List<String> preferred = new ArrayList<>();
      List<String> any = new ArrayList<>();

      for (Relic candidate : RelicRegistry.all()) {
         if (candidate.rarity() == next && !candidate.isSignature()) {
            any.add(candidate.id());
            boolean sameScope = from.isUniversal() ? candidate.isUniversal() : candidate.isUniversal() || from.powerId().equals(candidate.powerId());
            if (sameScope) {
               preferred.add(candidate.id());
            }
         }
      }

      List<String> pool = preferred.isEmpty() ? any : preferred;
      if (pool.isEmpty()) {
         return null;
      } else {
         RandomSource random = RandomSource.create((long)from.id().hashCode() * 31L + (long)next.ordinal());
         return pool.get(random.nextInt(pool.size()));
      }
   }
}
