package com.devilfruits.fruits.supa_supa.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.render.ArmOverlays;
import com.devilfruits.client.render.SkinOverrides;
import com.devilfruits.fruits.supa_supa.SupaSupa;
import com.devilfruits.fruits.supa_supa.client.blade.BladeLayer;
import com.devilfruits.fruits.supa_supa.client.blade.BladeLimbsClient;
import com.devilfruits.fruits.supa_supa.client.dev.SupaCapture;
import com.devilfruits.fruits.supa_supa.client.skate.SkateClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;
import net.neoforged.neoforge.common.NeoForge;

public final class SupaClient {
   public static void init(IEventBus modBus) {
      SupaClientFx.register();
      ArmOverlays.register(BladeLayer.FIRST_PERSON);
      SkinOverrides.register(BladeLayer.SKIN);
      modBus.addListener(SupaClient::addLayers);
      AnimationController.addPoseLayer(SkateClient.POSE);
      NeoForge.EVENT_BUS.addListener(SkateClient::onClientTick);
      NeoForge.EVENT_BUS.addListener(SkateClient::onInteraction);
      NeoForge.EVENT_BUS.addListener(SkateClient::onFov);
      NeoForge.EVENT_BUS.addListener(SupaClient::onClientTick);
      NeoForge.EVENT_BUS.addListener(SupaClient::onRenderPlayer);
      NeoForge.EVENT_BUS.addListener(e -> {
         BladeLimbsClient.clear();
         SkateClient.clear();
      });
      AutoTest.addScript(SupaSupa.ID, new SupaCapture("supa_"));
      AutoTest.addScript(DevilFruits.id("vfx_supa_look"), new SupaCapture("vfx_supa_"));
      AutoTest.addScript(DevilFruits.id("vfx_supa_m3"), new SupaCapture("vfx_supa_", "m3"));
      AutoTest.addScript(DevilFruits.id("vfx_supa_m4"), new SupaCapture("vfx_supa_", "m4"));
      AutoTest.addScript(DevilFruits.id("vfx_supa_m5"), new SupaCapture("vfx_supa_", "m5"));
      AutoTest.addScript(DevilFruits.id("vfx_supa_m6"), new SupaCapture("vfx_supa_", "m6"));
      AutoTest.addScript(DevilFruits.id("vfx_supa_m6b"), new SupaCapture("vfx_supa_", "m6b"));
      AutoTest.addScript(DevilFruits.id("vfx_supa_hull"), new SupaCapture("vfx_supa_", "hull"));
   }

   private static void addLayers(AddLayers event) {
      for (Model skin : event.getSkins()) {
         if (event.getSkin(skin) instanceof PlayerRenderer r) {
            r.addLayer(new BladeLayer(r));
         }
      }
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && !mc.isPaused()) {
         BladeLimbsClient.tick(mc.level);
      }
   }

   private static void onRenderPlayer(Pre event) {
      if (event.getEntity() instanceof AbstractClientPlayer player && BladeLimbsClient.of(player) != null) {
         PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)event.getRenderer().getModel();
         if (BladeLimbsClient.armOut(player, true)) {
            model.rightSleeve.visible = false;
         }

         if (BladeLimbsClient.armOut(player, false)) {
            model.leftSleeve.visible = false;
         }

         return;
      }
   }

   private SupaClient() {
   }
}
