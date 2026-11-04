package com.impactframe;

import com.impactframe.client.ImpactClientSetup;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod("impactframe")
public final class ImpactFrameMod {
   public static final String MOD_ID = "impactframe";

   public ImpactFrameMod(IEventBus modBus, ModContainer container) {
      if (FMLEnvironment.dist.isClient()) {
         container.registerConfig(Type.CLIENT, ImpactConfig.SPEC);
         ImpactClientSetup.register(container);
      }

      container.registerConfig(Type.COMMON, ImpactCommonConfig.SPEC);
   }
}
