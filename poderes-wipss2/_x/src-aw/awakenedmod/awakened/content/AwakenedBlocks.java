package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.content.block.ChallengePostBlock;
import com.awakenedmod.awakened.content.block.InfusionTankBlock;
import com.awakenedmod.awakened.content.block.SiegeAltarBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;

public final class AwakenedBlocks {
   private static final Blocks BLOCKS = DeferredRegister.createBlocks("awakened");
   public static final DeferredBlock<Block> ADAMANTIUM_ORE = BLOCKS.registerBlock(
      "adamantium_ore", Block::new, Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)
   );
   public static final DeferredBlock<InfusionTankBlock> INFUSION_TANK = BLOCKS.registerBlock(
      "infusion_tank",
      InfusionTankBlock::new,
      Properties.of()
         .mapColor(MapColor.METAL)
         .strength(3.5F, 6.0F)
         .sound(SoundType.METAL)
         .noOcclusion()
         .lightLevel(state -> 4 + (Integer)state.getValue(InfusionTankBlock.CHARGE) * 2)
   );
   public static final DeferredBlock<SiegeAltarBlock> SIEGE_ALTAR = BLOCKS.registerBlock(
      "siege_altar",
      SiegeAltarBlock::new,
      Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).lightLevel(state -> 8)
   );
   public static final DeferredBlock<ChallengePostBlock> CHALLENGE_POST = BLOCKS.registerBlock(
      "challenge_post", ChallengePostBlock::new, Properties.of().mapColor(MapColor.METAL).strength(4.0F, 1200.0F).sound(SoundType.METAL).lightLevel(state -> 4)
   );

   private AwakenedBlocks() {
   }

   public static void register(IEventBus modEventBus) {
      BLOCKS.register(modEventBus);
   }
}
