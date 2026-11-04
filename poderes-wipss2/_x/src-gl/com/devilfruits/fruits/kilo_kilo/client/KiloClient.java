package com.devilfruits.fruits.kilo_kilo.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.camera.CameraOffsets;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.fruits.kilo_kilo.KiloKilo;
import com.devilfruits.fruits.kilo_kilo.client.dev.KiloCapture;
import com.devilfruits.fruits.kilo_kilo.client.dev.KiloDropCheck;
import com.devilfruits.fruits.kilo_kilo.client.dev.KiloFloorCheck;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

public final class KiloClient {
   public static void init(IEventBus modBus) {
      KiloClientFx.register();
      modBus.addListener(KiloParticleProviders::register);
      modBus.addListener(e -> e.registerAbove(VanillaGuiLayers.HOTBAR, KiloKilo.id("weight"), WeightHud::render));
      AnimationController.addPoseLayer(new KiloPose());
      CameraOffsets.register(KiloClientEvents::cameraSag);
      NeoForge.EVENT_BUS.addListener(KiloClientEvents::onClientTick);
      NeoForge.EVENT_BUS.addListener(KiloClientEvents::onLogout);
      NeoForge.EVENT_BUS.addListener(KiloClientEvents::onFov);
      AutoTest.addScript(KiloKilo.ID, new KiloCapture(false));
      AutoTest.addScript(DevilFruits.id("vfx_kilo"), new KiloCapture(true));
      AutoTest.addScript(DevilFruits.id("vfx_kilo_drop"), new KiloDropCheck());
      AutoTest.addScript(DevilFruits.id("vfx_kilo_floor"), new KiloFloorCheck());
   }

   private KiloClient() {
   }
}
