package com.devilfruits.core;

import com.devilfruits.core.config.FruitConfigs;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModules;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.gametest.FrameworkGameTests;
import com.devilfruits.core.gametest.GachaGameTests;
import com.devilfruits.core.gametest.fixture.DebugZoan;
import com.devilfruits.core.haki.HakiCombat;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.rules.DFGameRules;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

public final class CoreModEvents {
   public static void onCommonSetup(FMLCommonSetupEvent event) {
      HakiCombat.init();
      PhysicsWorld.init();
      FruitRegistry.freeze();
      event.enqueueWork(DFGameRules::register);
   }

   public static void onConfigLoad(Loading event) {
      FruitConfigs.upgrade(event.getConfig().getSpec());
   }

   public static void onConfigReload(Reloading event) {
      if (FruitConfigs.isFruitSpec(event.getConfig().getSpec())) {
         CoreEvents.broadcastAbilityTables();
      }
   }

   public static void onRegisterGameTests(RegisterGameTestsEvent event) {
      if (FruitRegistry.get(DebugZoan.ID) != null) {
         event.register(FrameworkGameTests.class);
      }

      event.register(GachaGameTests.class);

      for (FruitModule module : FruitModules.all()) {
         for (Class<?> tests : module.gameTests()) {
            event.register(tests);
         }
      }
   }

   private CoreModEvents() {
   }
}
