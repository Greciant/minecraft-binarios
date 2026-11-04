package com.devilfruits.datagen;

import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModules;
import com.devilfruits.registry.DFBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class DFBlockStateProvider extends BlockStateProvider {
   public DFBlockStateProvider(PackOutput output, ExistingFileHelper efh) {
      super(output, "devilfruits", efh);
   }

   protected void registerStatesAndModels() {
      this.simpleBlock((Block)DFBlocks.SEASTONE_BLOCK.get());

      for (FruitModule module : FruitModules.all()) {
         module.data().blockStates(this);
      }
   }
}
