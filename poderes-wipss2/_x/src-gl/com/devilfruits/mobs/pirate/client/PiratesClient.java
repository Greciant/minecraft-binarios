package com.devilfruits.mobs.pirate.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.mobs.pirate.Pirates;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class PiratesClient {
   public static void init(IEventBus modBus) {
      PirateClientFx.register();
      modBus.addListener(PiratesClient::registerRenderers);
      modBus.addListener(PirateClientFx::registerParticles);
      modBus.addListener(PiratesClient::clientSetup);
      modBus.addListener(PiratesClient::clientExtensions);
      NeoForge.EVENT_BUS.addListener(PirateGunClient::onInteraction);
      AutoTest.addScript(DevilFruits.id("pirates"), new PirateAutotest());
      AutoTest.addScript(DevilFruits.id("vfx_gun_death"), new GunDeathCheck());
      AutoTest.addScript(DevilFruits.id("vfx_weapon_icons"), new WeaponIconCheck());
   }

   private static void clientExtensions(RegisterClientExtensionsEvent event) {
      event.registerItem(PirateGunClient.EXTENSIONS, new Item[]{(Item)Pirates.FLINTLOCK_PISTOL.get(), (Item)Pirates.MUSKET.get()});
   }

   private static void registerRenderers(RegisterRenderers event) {
      event.registerEntityRenderer(Pirates.DECKHAND.get(), ctx -> new PirateRenderer(ctx, "deckhand", 4, "vindicator", true));
      event.registerEntityRenderer(Pirates.GUNNER.get(), ctx -> new PirateRenderer(ctx, "gunner", 4, "pillager", false));
      event.registerEntityRenderer(Pirates.CAPTAIN.get(), ctx -> new PirateRenderer(ctx, "captain", 2, "evoker", true));
      event.registerEntityRenderer(Pirates.MUSKET_BALL_ENTITY.get(), ThrownItemRenderer::new);
   }

   private static void clientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(() -> {
         ClampedItemPropertyFunction loaded = (stack, level, entity, seed) -> PirateGunClient.loaded(stack, entity) ? 1.0F : 0.0F;
         ClampedItemPropertyFunction reload = (stack, level, entity, seed) -> PirateGunClient.reload(stack, entity);

         for (Item gun : new Item[]{(Item)Pirates.FLINTLOCK_PISTOL.get(), (Item)Pirates.MUSKET.get()}) {
            ItemProperties.register(gun, DevilFruits.id("loaded"), loaded);
            ItemProperties.register(gun, DevilFruits.id("reload"), reload);
         }
      });
   }

   private PiratesClient() {
   }
}
