package com.devilfruits.fruits.suna_suna;

import com.devilfruits.fruits.suna_suna.block.QuicksandBlock;
import com.devilfruits.registry.DFBlocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class SunaBlocks {
   public static final DeferredBlock<QuicksandBlock> QUICKSAND = DFBlocks.BLOCKS
      .register(
         "suna_suna/quicksand",
         () -> new QuicksandBlock(
               Properties.of()
                  .mapColor(state -> state.getValue(QuicksandBlock.RED) ? MapColor.COLOR_ORANGE : MapColor.SAND)
                  .strength(0.5F)
                  .sound(SoundType.SAND)
                  .dynamicShape()
                  .noOcclusion()
                  .noLootTable()
                  .pushReaction(PushReaction.DESTROY)
                  .isRedstoneConductor((state, level, pos) -> false)
                  .isSuffocating((state, level, pos) -> false)
                  .isViewBlocking((state, level, pos) -> true)
            )
      );

   static void init() {
   }

   private SunaBlocks() {
   }
}
