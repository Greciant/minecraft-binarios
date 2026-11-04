package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.content.relic.RelicCraftRecipes;
import com.awakenedmod.awakened.core.power.CraftRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class CommonRecipes {
   private CommonRecipes() {
   }

   public static List<CraftRecipe> list() {
      List<CraftRecipe> all = new ArrayList<>(framework());
      all.addAll(RelicCraftRecipes.list());
      return all;
   }

   private static List<CraftRecipe> framework() {
      return List.of(
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.CATALYST.get(),
            List.of("GAG", "GPG", " A "),
            Map.of('G', Items.GOLD_INGOT, 'A', Items.AMETHYST_SHARD, 'P', Items.ENDER_PEARL)
         ),
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.FOCUSED_CATALYST.get(),
            List.of("SBS", "BCB", "SBS"),
            Map.of('S', Items.BLAZE_ROD, 'B', Items.DIAMOND, 'C', (ItemLike)AwakenedItems.CATALYST.get())
         ),
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.HONED_CATALYST.get(),
            List.of("SBS", "BCB", "SBS"),
            Map.of('S', Items.ENDER_EYE, 'B', Items.DIAMOND, 'C', (ItemLike)AwakenedItems.FOCUSED_CATALYST.get())
         ),
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.REFINED_CATALYST.get(),
            List.of("SBS", "BCB", "SBS"),
            Map.of('S', Items.ECHO_SHARD, 'B', Items.DIAMOND, 'C', (ItemLike)AwakenedItems.HONED_CATALYST.get())
         ),
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.PRECISE_CATALYST.get(),
            List.of("ANA", "ACA", "ANA"),
            Map.of('A', (ItemLike)AwakenedItems.ADAMANTIUM_INGOT.get(), 'N', Items.NETHERITE_INGOT, 'C', (ItemLike)AwakenedItems.REFINED_CATALYST.get())
         ),
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.PERFECT_CATALYST.get(),
            List.of("ESE", "EPE", "EOE"),
            Map.of(
               'E',
               Items.ENDER_EYE,
               'S',
               Items.NETHER_STAR,
               'P',
               (ItemLike)AwakenedItems.PRECISE_CATALYST.get(),
               'O',
               (ItemLike)AwakenedItems.CORDON_CORE.get()
            )
         ),
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.TRACKER.get(), List.of("RAR", "ACA", "RAR"), Map.of('R', Items.REDSTONE, 'A', Items.AMETHYST_SHARD, 'C', Items.COMPASS)
         ),
         CraftRecipe.shaped((ItemLike)AwakenedItems.RELIC_BELT.get(), List.of("LLL", "LAL"), Map.of('L', Items.LEATHER, 'A', Items.AMETHYST_SHARD)),
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.CORDON_SPEAR.get(), List.of("C", "S", "S"), Map.of('C', (ItemLike)AwakenedItems.CORDON_CORE.get(), 'S', Items.STICK)
         ),
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.SIEGE_ALTAR.get(),
            List.of("GAG", "ACA", "GAG"),
            Map.of('G', Items.GLOWSTONE, 'A', Items.AMETHYST_BLOCK, 'C', Items.CRYING_OBSIDIAN)
         ),
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.CHALLENGE_POST.get(),
            List.of("EIE", "IBI", "PIP"),
            Map.of('E', Items.ECHO_SHARD, 'I', Items.IRON_INGOT, 'B', Items.AMETHYST_BLOCK, 'P', Items.POLISHED_BLACKSTONE)
         ),
         relicUpgrade(),
         armorPlating()
      );
   }

   private static CraftRecipe relicUpgrade() {
      ItemStack in = namedRelic("awakened.screen.recipe_relic_in");
      ItemStack out = namedRelic("awakened.screen.recipe_relic_out");
      return new CraftRecipe(out, 2, 2, List.of(in, in, in, in), ItemStack.EMPTY, 0);
   }

   private static ItemStack namedRelic(String key) {
      return named(new ItemStack((ItemLike)AwakenedItems.RELIC.get()), key);
   }

   private static CraftRecipe armorPlating() {
      ItemStack ingot = new ItemStack((ItemLike)AwakenedItems.ADAMANTIUM_INGOT.get());
      ItemStack in = namedArmor("awakened.screen.recipe_plating_in");
      ItemStack out = namedArmor("awakened.screen.recipe_plating_out");
      return new CraftRecipe(out, 3, 3, List.of(ingot, ingot, ingot, ingot, in, ingot, ingot, ingot, ingot), ItemStack.EMPTY, 0);
   }

   private static ItemStack namedArmor(String key) {
      return named(new ItemStack(Items.IRON_CHESTPLATE), key);
   }

   private static ItemStack named(ItemStack stack, String key) {
      stack.set(DataComponents.CUSTOM_NAME, Component.translatable(key));
      return stack;
   }
}
