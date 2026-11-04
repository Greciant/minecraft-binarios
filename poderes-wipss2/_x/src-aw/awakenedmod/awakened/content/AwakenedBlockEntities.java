package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.content.block.InfusionTankBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AwakenedBlockEntities {
   private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "awakened");
   public static final Supplier<BlockEntityType<InfusionTankBlockEntity>> INFUSION_TANK = BLOCK_ENTITIES.register(
      "infusion_tank", () -> Builder.of(InfusionTankBlockEntity::new, new Block[]{(Block)AwakenedBlocks.INFUSION_TANK.get()}).build(null)
   );

   private AwakenedBlockEntities() {
   }

   public static void register(IEventBus modEventBus) {
      BLOCK_ENTITIES.register(modEventBus);
   }
}
