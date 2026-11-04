package com.awakenedmod.awakened.content.relic;

import com.awakenedmod.awakened.Awakened;
import com.awakenedmod.awakened.core.power.CraftRecipe;
import com.awakenedmod.awakened.core.relic.Relic;
import com.awakenedmod.awakened.core.relic.RelicRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public final class RelicCraftRecipes {
   private static final List<String> PATTERN = List.of("TBT", "BCB", "TBT");
   private static final Map<Rarity, Item> TIER_METAL = Map.of(Rarity.COMMON, Items.IRON_INGOT, Rarity.UNCOMMON, Items.GOLD_INGOT, Rarity.RARE, Items.DIAMOND);
   private static final Map<Rarity, Item> BINDER = Map.of(
      Rarity.COMMON, Items.AMETHYST_SHARD, Rarity.UNCOMMON, Items.AMETHYST_SHARD, Rarity.RARE, Items.ECHO_SHARD
   );
   private static final List<RelicCraftRecipes.Craftable> CRAFTABLE = List.of(
      new RelicCraftRecipes.Craftable("iron_charm", Items.GLISTERING_MELON_SLICE, Rarity.COMMON),
      new RelicCraftRecipes.Craftable("swift_cells", Items.SUGAR, Rarity.COMMON),
      new RelicCraftRecipes.Craftable("warding_plate", Items.GLISTERING_MELON_SLICE, Rarity.UNCOMMON),
      new RelicCraftRecipes.Craftable("kinetic_band", Items.GHAST_TEAR, Rarity.UNCOMMON),
      new RelicCraftRecipes.Craftable("focus_lens", Items.BLAZE_POWDER, Rarity.UNCOMMON),
      new RelicCraftRecipes.Craftable("stamina_matrix", Items.SUGAR, Rarity.RARE),
      new RelicCraftRecipes.Craftable("flux_conduit", Items.GHAST_TEAR, Rarity.RARE)
   );

   private RelicCraftRecipes() {
   }

   public static List<CraftRecipe> list() {
      List<CraftRecipe> recipes = new ArrayList<>(CRAFTABLE.size());

      for (RelicCraftRecipes.Craftable craftable : CRAFTABLE) {
         RelicItem result = RelicItems.item(craftable.relicId());
         Relic relic = RelicRegistry.get(craftable.relicId());
         if (result != null && relic != null) {
            if (relic.rarity() != craftable.bakedRarity()) {
               Awakened.LOGGER
                  .error(
                     "Relic '{}' is {} but its recipe was generated for {}; hiding it from the Recipes tab",
                     new Object[]{craftable.relicId(), relic.rarity(), craftable.bakedRarity()}
                  );
            } else {
               Item metal = TIER_METAL.get(craftable.bakedRarity());
               Item binder = BINDER.get(craftable.bakedRarity());
               if (metal != null && binder != null) {
                  recipes.add(CraftRecipe.shaped(result, PATTERN, Map.of('T', metal, 'B', binder, 'C', craftable.core())));
               }
            }
         }
      }

      return recipes;
   }

   private static record Craftable(String relicId, Item core, Rarity bakedRarity) {
   }
}
