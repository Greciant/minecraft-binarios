package com.devilfruits.fruits.gomu_gomu.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.fruits.gomu_gomu.client.dev.GomuCapture;
import net.neoforged.bus.api.IEventBus;

public final class GomuClient {
   public static void init(IEventBus modBus) {
      RubberDents.init(modBus);
      StrikeClient.init();
      AxeClient.init();
      GatlingClient.init();

      for (String stage : new String[]{"m2", "m3", "m4", "m5", "m6"}) {
         AutoTest.addScript(DevilFruits.id("vfx_gomu_" + stage), new GomuCapture(stage));
      }
   }

   private GomuClient() {
   }
}
