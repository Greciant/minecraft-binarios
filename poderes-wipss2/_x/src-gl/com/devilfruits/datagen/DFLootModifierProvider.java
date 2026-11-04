package com.devilfruits.datagen;

import com.devilfruits.core.gacha.CasketLootModifier;
import com.devilfruits.core.haki.HakiScrollLootModifier;
import com.devilfruits.core.poneglyph.PrimerLootModifier;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

public class DFLootModifierProvider extends GlobalLootModifierProvider {
   public DFLootModifierProvider(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "devilfruits");
   }

   protected void start() {
      CasketLootModifier.DEFAULT_CHANCES
         .forEach(
            (tablex, chance) -> {
               ResourceLocation id = ResourceLocation.parse(tablex);
               String path = id.getPath();
               String name = path.startsWith("archaeology/")
                  ? path.substring(path.indexOf(47) + 1) + "_archaeology"
                  : path.substring(path.indexOf(47) + 1).replace('/', '_');
               this.add(
                  "casket_in_" + name,
                  new CasketLootModifier(
                     new LootItemCondition[]{LootTableIdCondition.builder(id).build()}, chance, CasketLootModifier.REPLACING.contains(tablex)
                  ),
                  new ICondition[0]
               );
            }
         );

      for (String table : new String[]{
         "chests/pillager_outpost", "chests/ancient_city", "chests/stronghold_library", "chests/end_city_treasure", "chests/bastion_treasure"
      }) {
         this.add(
            "haki_scroll_in_" + table.substring(table.indexOf(47) + 1),
            new HakiScrollLootModifier(new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace(table)).build()}),
            new ICondition[0]
         );
      }

      this.primer("shipwreck", "chests/shipwreck_map", "chests/shipwreck_supply", "chests/shipwreck_treasure");
      this.primer("buried_treasure", "chests/buried_treasure");
      this.primer("underwater_ruin", "chests/underwater_ruin_small", "chests/underwater_ruin_big");
      this.primer("poneglyph_ruins", "devilfruits:chests/poneglyph_ruins");
   }

   private void primer(String source, String... tables) {
      for (String table : tables) {
         ResourceLocation id = table.contains(":") ? ResourceLocation.parse(table) : ResourceLocation.withDefaultNamespace(table);
         this.add(
            "primer_in_" + id.getPath().substring(id.getPath().indexOf(47) + 1),
            new PrimerLootModifier(new LootItemCondition[]{LootTableIdCondition.builder(id).build()}, source),
            new ICondition[0]
         );
      }
   }
}
