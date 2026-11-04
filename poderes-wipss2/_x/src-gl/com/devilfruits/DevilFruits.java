package com.devilfruits;

import com.devilfruits.core.CoreModEvents;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.fruit.FruitModuleContext;
import com.devilfruits.core.haki.HakiConfig;
import com.devilfruits.core.network.DFNetwork;
import com.devilfruits.core.physics.PhysicsConfig;
import com.devilfruits.core.poneglyph.Poneglyphs;
import com.devilfruits.datagen.DFDataGen;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.registry.DFRegistries;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import org.slf4j.Logger;

@Mod("devilfruits")
public final class DevilFruits {
   public static final String MODID = "devilfruits";
   public static final Logger LOGGER = LogUtils.getLogger();

   public DevilFruits(IEventBus modBus, ModContainer container) {
      DFRegistries.register(modBus);
      container.registerConfig(Type.COMMON, CoreConfig.SPEC);
      container.registerConfig(Type.CLIENT, CoreClientConfig.SPEC);
      container.registerConfig(Type.COMMON, HakiConfig.SPEC, "devilfruits-haki-common.toml");
      container.registerConfig(Type.COMMON, PhysicsConfig.SPEC, "devilfruits-sable-common.toml");
      BuiltinFruits.register(new FruitModuleContext(modBus, container));
      Pirates.init(modBus, container);
      Poneglyphs.init(modBus, container);
      modBus.addListener(DFNetwork::register);
      modBus.addListener(CoreModEvents::onCommonSetup);
      modBus.addListener(CoreModEvents::onConfigLoad);
      modBus.addListener(CoreModEvents::onConfigReload);
      modBus.addListener(CoreModEvents::onRegisterGameTests);
      modBus.addListener(DFDataGen::gather);
   }

   public static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("devilfruits", path);
   }
}
