package com.devilfruits.core.fruit;

import com.devilfruits.datagen.FruitDataContributor;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

public interface FruitModule {
   ResourceLocation id();

   void register(FruitModuleContext var1);

   default void registerClient(IEventBus modBus) {
   }

   default FruitDataContributor data() {
      return FruitDataContributor.NONE;
   }

   default List<Class<?>> gameTests() {
      return List.of();
   }
}
