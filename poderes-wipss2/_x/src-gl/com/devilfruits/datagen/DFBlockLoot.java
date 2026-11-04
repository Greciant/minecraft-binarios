package com.devilfruits.datagen;

import com.devilfruits.core.poneglyph.Poneglyphs;
import com.devilfruits.core.seastone.SeastoneGear;
import com.devilfruits.registry.DFBlocks;
import java.util.Set;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class DFBlockLoot extends BlockLootSubProvider {
   public DFBlockLoot(Provider registries) {
      super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
   }

   protected void generate() {
      this.dropSelf((Block)DFBlocks.SEASTONE_BLOCK.get());
      this.dropSelf((Block)Poneglyphs.DECIPHER_DESK.get());
      this.add((Block)SeastoneGear.SEASTONE_ORE.get(), b -> this.createOreDrop(b, (Item)SeastoneGear.RAW_SEASTONE.get()));
      this.add((Block)SeastoneGear.DEEPSLATE_SEASTONE_ORE.get(), b -> this.createOreDrop(b, (Item)SeastoneGear.RAW_SEASTONE.get()));
   }

   protected Iterable<Block> getKnownBlocks() {
      return DFBlocks.BLOCKS.getEntries().stream().map(holder -> (Block)holder.value()).toList();
   }
}
