package com.awakenedmod.awakened.content.item;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.DataComponents;

public final class PrismComponents {
   private static final DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "awakened");
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LIGHTNING_COLOR = COMPONENTS.registerComponentType(
      "lightning_color", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT)
   );

   private PrismComponents() {
   }

   public static void register(IEventBus modEventBus) {
      COMPONENTS.register(modEventBus);
   }
}
