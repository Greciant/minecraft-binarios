package com.devilfruits.registry;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.DataComponents;

public final class DFDataComponents {
   public static final DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "devilfruits");
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> FRUIT_INSTANCE = COMPONENTS.registerComponentType(
      "fruit_instance", b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC)
   );
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> ABILITY = COMPONENTS.registerComponentType(
      "ability", b -> b.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC)
   );

   private DFDataComponents() {
   }
}
