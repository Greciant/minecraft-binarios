package com.devilfruits.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.animation.AnimationLoader;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.fx.CoreClientFx;
import com.devilfruits.client.gacha.CasketClient;
import com.devilfruits.client.haki.HakiClient;
import com.devilfruits.client.hud.DFHud;
import com.devilfruits.client.input.DFKeys;
import com.devilfruits.client.limb.LimbClient;
import com.devilfruits.client.logia.LogiaClient;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.client.particle.DFParticleProviders;
import com.devilfruits.client.poneglyph.PoneglyphClient;
import com.devilfruits.client.render.DebrisRenderer;
import com.devilfruits.client.seastone.SeastoneArmorClient;
import com.devilfruits.client.vfx.render.VfxShaders;
import com.devilfruits.core.ability.AbilityRegistry;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModules;
import com.devilfruits.mobs.pirate.client.PiratesClient;
import com.devilfruits.registry.DFDataComponents;
import com.devilfruits.registry.DFEntities;
import com.devilfruits.registry.DFItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(
   value = "devilfruits",
   dist = {Dist.CLIENT}
)
public final class DevilFruitsClient {
   public DevilFruitsClient(IEventBus modBus, ModContainer container) {
      container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
      modBus.addListener(DFKeys::register);
      modBus.addListener(DFHud::registerLayers);
      modBus.addListener(DFParticleProviders::register);
      modBus.addListener(VfxShaders::onRegister);
      modBus.addListener(DevilFruitsClient::registerRenderers);
      modBus.addListener(DevilFruitsClient::registerReloadListeners);
      modBus.addListener(DevilFruitsClient::clientSetup);
      CoreClientFx.register();
      HakiClient.init(modBus, container);
      PiratesClient.init(modBus);
      PoneglyphClient.init(modBus);
      SeastoneArmorClient.init(modBus);
      CasketClient.init(modBus);
      LimbClient.init(modBus);
      LogiaClient.register();
      AnimationController.addClipSource(GeoResources::animation);

      for (FruitModule module : FruitModules.all()) {
         module.registerClient(modBus);
      }

      if (AutoTest.ENABLED) {
         NeoForge.EVENT_BUS.register(AutoTest.class);
      }
   }

   private static void registerRenderers(RegisterRenderers event) {
      event.registerEntityRenderer(DFEntities.DEBRIS.get(), DebrisRenderer::new);
   }

   private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener(AnimationLoader.INSTANCE);
      event.registerReloadListener(GeoResources.MODELS);
      event.registerReloadListener(GeoResources.ANIMATIONS);
   }

   private static void clientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(
         () -> ItemProperties.register(
               (Item)DFItems.ABILITY_ICON.get(),
               DevilFruits.id("ability"),
               (stack, level, entity, seed) -> (float)AbilityRegistry.iconIndex((ResourceLocation)stack.get((DataComponentType)DFDataComponents.ABILITY.get()))
            )
      );
   }
}
