package com.devilfruits.fruits.doru_doru.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.model.geo.GeoOverlays;
import com.devilfruits.client.render.FormRenderers;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.DoruEntities;
import com.devilfruits.fruits.doru_doru.client.atelier.SculptClient;
import com.devilfruits.fruits.doru_doru.client.atelier.SculptGhost;
import com.devilfruits.fruits.doru_doru.client.atelier.SculptHud;
import com.devilfruits.fruits.doru_doru.client.atelier.SculptKeys;
import com.devilfruits.fruits.doru_doru.client.dev.DoruCapture;
import com.devilfruits.fruits.doru_doru.client.dev.WaxKeyCapture;
import com.devilfruits.fruits.doru_doru.client.render.ChampionRenderer;
import com.devilfruits.fruits.doru_doru.client.render.WaxCoatingLayer;
import com.devilfruits.fruits.doru_doru.client.render.WaxLanceRenderer;
import com.devilfruits.fruits.doru_doru.client.vfx.ServiceClient;
import com.devilfruits.fruits.doru_doru.client.wax.WaxBakedModel;
import com.devilfruits.fruits.doru_doru.client.wax.WaxMesher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

public final class DoruClient {
   private static final ChampionRenderer CHAMPION = new ChampionRenderer();

   public static void init(IEventBus modBus) {
      DoruClientFx.register();
      modBus.addListener(WaxBakedModel::wrap);
      modBus.addListener(e -> WaxMesher.clear());
      AutoTest.addScript(DevilFruits.id("vfx_doru_wax"), new DoruCapture("wax"));
      AutoTest.addScript(DevilFruits.id("vfx_doru_atelier"), new DoruCapture("atelier"));
      AutoTest.addScript(DevilFruits.id("vfx_doru_depth"), new DoruCapture("depth"));
      AutoTest.addScript(DevilFruits.id("vfx_doru_tools"), new DoruCapture("tools"));
      AutoTest.addScript(DevilFruits.id("vfx_doru_service"), new DoruCapture("service"));
      AutoTest.addScript(DevilFruits.id("vfx_doru_champion"), new DoruCapture("champion"));
      AutoTest.addScript(DevilFruits.id("vfx_doru_checks"), new DoruCapture("checks"));
      AutoTest.addScript(DevilFruits.id("vfx_doru_waxkey"), new WaxKeyCapture());
      SculptClient.init();
      SculptGhost.init();
      modBus.addListener(SculptKeys::register);
      modBus.addListener(e -> e.registerAbove(VanillaGuiLayers.CROSSHAIR, DoruDoru.id("sculpt"), SculptHud::render));
      NeoForge.EVENT_BUS.addListener(SculptHud::onLayer);
      NeoForge.EVENT_BUS.addListener(DoruClientEvents::onClientTick);
      NeoForge.EVENT_BUS.addListener(DoruClientEvents::onClientTickEnd);
      modBus.addListener(DoruClientEvents::onItemColors);
      modBus.addListener(DoruClientEvents::addLayers);
      FormRenderers.register(DoruDoru.id("champion"), CHAMPION);
      NeoForge.EVENT_BUS.addListener(e -> {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null && level.getGameTime() % 100L == 0L) {
            CHAMPION.prune(level);
         }
      });
      GeoOverlays.register(WaxCoatingLayer::geo);
      NeoForge.EVENT_BUS.addListener(ServiceClient::tick);
      modBus.addListener(e -> e.registerEntityRenderer(DoruEntities.LANCE.get(), WaxLanceRenderer::new));
   }

   private DoruClient() {
   }
}
