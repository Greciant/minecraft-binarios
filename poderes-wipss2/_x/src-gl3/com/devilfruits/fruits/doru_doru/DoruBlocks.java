package com.devilfruits.fruits.doru_doru;

import com.devilfruits.fruits.doru_doru.wax.WaxBlock;
import com.devilfruits.fruits.doru_doru.wax.WaxBlockEntity;
import com.devilfruits.registry.DFBlocks;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DoruBlocks {
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "devilfruits");
   public static final DeferredBlock<WaxBlock> WAX = DFBlocks.BLOCKS
      .register(
         "doru_doru/wax",
         () -> new WaxBlock(
               Properties.of()
                  .mapColor(MapColor.TERRACOTTA_WHITE)
                  .strength(30.0F, 1500.0F)
                  .requiresCorrectToolForDrops()
                  .sound(SoundType.CANDLE)
                  .noOcclusion()
                  .dynamicShape()
                  .noLootTable()
                  .pushReaction(PushReaction.BLOCK)
                  .isRedstoneConductor((state, level, pos) -> false)
                  .isSuffocating((state, level, pos) -> (Boolean)state.getValue(WaxBlock.FULL))
                  .isViewBlocking((state, level, pos) -> (Boolean)state.getValue(WaxBlock.FULL))
            )
      );
   public static final Supplier<BlockEntityType<WaxBlockEntity>> WAX_BE = BLOCK_ENTITIES.register(
      "doru_doru/wax", () -> Builder.of(WaxBlockEntity::new, new Block[]{(Block)WAX.get()}).build(null)
   );

   static void init(IEventBus modBus) {
      BLOCK_ENTITIES.register(modBus);
   }

   private DoruBlocks() {
   }
}
