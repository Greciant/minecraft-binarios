package com.devilfruits.datagen;

import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModules;
import com.devilfruits.registry.DFDamageTypes;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableProvider.SubProviderEntry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class DFDataGen {
   public static void gather(GatherDataEvent event) {
      DataGenerator gen = event.getGenerator();
      PackOutput out = gen.getPackOutput();
      CompletableFuture<Provider> lookup = event.getLookupProvider();
      ExistingFileHelper efh = event.getExistingFileHelper();
      boolean server = event.includeServer();
      DatapackBuiltinEntriesProvider datapack = (DatapackBuiltinEntriesProvider)gen.addProvider(
         server,
         new DatapackBuiltinEntriesProvider(out, lookup, new RegistrySetBuilder().add(Registries.DAMAGE_TYPE, DFDataGen::damageTypes), Set.of("devilfruits"))
      );
      CompletableFuture<Provider> withDatapack = datapack.getRegistryProvider();
      DFTagProviders.BlockTagGen blockTags = (DFTagProviders.BlockTagGen)gen.addProvider(server, new DFTagProviders.BlockTagGen(out, lookup, efh));
      gen.addProvider(server, new DFTagProviders.ItemTagGen(out, lookup, blockTags.contentsGetter(), efh));
      gen.addProvider(server, new DFTagProviders.DamageTypeTagGen(out, withDatapack, efh));
      gen.addProvider(server, new DFLootModifierProvider(out, lookup));
      gen.addProvider(server, new AdvancementProvider(out, withDatapack, efh, List.of((registries, saver, helper) -> {
         for (FruitModule module : FruitModules.all()) {
            module.data().advancements(registries, saver, helper);
         }

         DFHakiAdvancements.generate(saver);
         DFPirateAdvancements.generate(saver);
         DFPoneglyphAdvancements.generate(saver);
      })));
      gen.addProvider(server, new DFRecipeProvider(out, lookup));
      gen.addProvider(server, new LootTableProvider(out, Set.of(), List.of(new SubProviderEntry(DFBlockLoot::new, LootContextParamSets.BLOCK)), lookup));
      gen.addProvider(event.includeClient(), new DFBlockStateProvider(out, efh));
      gen.addProvider(event.includeClient(), new DFItemModelProvider(out, efh));
   }

   private static void damageTypes(BootstrapContext<DamageType> context) {
      context.register(DFDamageTypes.DEVIL_FRUIT, new DamageType("devilfruits.devil_fruit", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
      context.register(DFDamageTypes.SECOND_FRUIT, new DamageType("devilfruits.second_fruit", DamageScaling.NEVER, 0.0F));
      context.register(DFDamageTypes.HAKI_RYUO, new DamageType("devilfruits.haki_ryuo", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));

      for (FruitModule module : FruitModules.all()) {
         module.data().damageTypes(context);
      }
   }

   private DFDataGen() {
   }
}
