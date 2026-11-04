package com.devilfruits.core.fruit;

import com.devilfruits.core.config.FruitConfigs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;

public record FruitModuleContext(IEventBus modBus, ModContainer container) {
   public DevilFruit registerFruit(DevilFruit fruit) {
      FruitRegistry.register(fruit);
      FruitConfigs.register(fruit, this.container);
      return fruit;
   }

   public void registerClientConfig(ResourceLocation fruitId, ModConfigSpec spec) {
      this.container.registerConfig(Type.CLIENT, spec, this.container.getModId() + "-" + fruitId.getPath().replace('/', '-') + "-client.toml");
   }

   public IEventBus gameBus() {
      return NeoForge.EVENT_BUS;
   }
}
