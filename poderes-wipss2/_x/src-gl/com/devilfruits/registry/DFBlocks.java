package com.devilfruits.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;

public final class DFBlocks {
   public static final Blocks BLOCKS = DeferredRegister.createBlocks("devilfruits");
   public static final DeferredBlock<Block> SEASTONE_BLOCK = BLOCKS.registerSimpleBlock(
      "seastone_block", Properties.of().mapColor(MapColor.COLOR_CYAN).strength(5.0F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)
   );

   private DFBlocks() {
   }
}
