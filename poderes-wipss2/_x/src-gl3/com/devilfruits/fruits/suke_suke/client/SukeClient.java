package com.devilfruits.fruits.suke_suke.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.fruits.suke_suke.clear.ShipView;
import com.devilfruits.fruits.suke_suke.client.dev.SukeCapture;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;

public final class SukeClient {
   public static void init(IEventBus modBus) {
      ClearRender.init();
      SukeHud.init(modBus);
      SukeClientFx.register();
      NeoForge.EVENT_BUS.addListener(ShipLook::onSound);
      if (ModList.get().isLoaded("sable")) {
         try {
            Class.forName("com.devilfruits.compat.sable.client.SableShipLook").getMethod("install").invoke(null);
         } catch (LinkageError | ReflectiveOperationException var6) {
            DevilFruits.LOGGER.error("Suke Suke: Sable is installed but the ship view could not be bound", var6);
         }
      }

      NeoForge.EVENT_BUS.addListener(e -> {
         if (Minecraft.getInstance().level != null) {
            ClearTransitions.tick(Minecraft.getInstance().level);
            SukeClientFx.tickShips(Minecraft.getInstance().level);
         }
      });
      NeoForge.EVENT_BUS.addListener(e -> {
         ShipView.clear();
         ClearTransitions.clear();
         Visibility.viewer(Visibility.Viewer.REAL);
      });

      for (SukeCapture.Stage stage : SukeCapture.Stage.values()) {
         String n = stage.name().toLowerCase(Locale.ROOT);
         AutoTest.addScript(DevilFruits.id("vfx_suke_" + n), new SukeCapture("vfx_suke" + n.substring(1) + "_", stage));
      }
   }

   private SukeClient() {
   }
}
