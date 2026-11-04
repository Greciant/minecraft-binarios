package com.devilfruits.fruits.kage_kage.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.render.EntityShadows;
import com.devilfruits.fruits.kage_kage.KageEntities;
import com.devilfruits.fruits.kage_kage.KageItems;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.client.dev.KageAutotest;
import com.devilfruits.fruits.kage_kage.client.dev.KageCapture;
import com.devilfruits.fruits.kage_kage.client.vfx.KageRender;
import com.devilfruits.fruits.kage_kage.client.vfx.SilhouetteRim;
import com.devilfruits.fruits.kage_kage.item.MoriaScissorsItem;
import com.devilfruits.fruits.kage_kage.shadow.Shadows;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

public final class KageClient {
   public static void init(IEventBus modBus) {
      EntityShadows.hideWhen(Shadows::isShadowless);
      KageClientFx.register();
      modBus.addListener(
         e -> e.enqueueWork(
               () -> ItemProperties.register(
                     (Item)KageItems.MORIA_SCISSORS.get(),
                     DevilFruits.id("kage_split"),
                     (stack, level, entity, seed) -> MoriaScissorsItem.isSplit(stack) ? 1.0F : 0.0F
                  )
            )
      );
      modBus.addListener(e -> {
         e.registerEntityRenderer(KageEntities.DOPPELMAN.get(), DoppelmanRenderer::new);
         e.registerEntityRenderer(KageEntities.SHADOW_ZOMBIE.get(), ShadowZombieRenderer::new);
         e.registerEntityRenderer(KageEntities.HOLLOW_CORPSE.get(), HollowCorpseRenderer::new);
      });
      modBus.addListener(e -> e.registerAbove(VanillaGuiLayers.CROSSHAIR, KageKage.id("status"), KageHud::render));
      modBus.addListener(KageKeys::register);
      modBus.addListener(e -> e.enqueueWork(SilhouetteRim::enableStencil));
      modBus.addListener(e -> e.registerItem(new IClientItemExtensions() {
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
               return ScissorsItemRenderer.get();
            }
         }, new Item[]{(Item)KageItems.MORIA_SCISSORS.get(), (Item)KageItems.MORIA_BLADE.get()}));
      NeoForge.EVENT_BUS.addListener(KageKeys::tick);
      AnimationController.addPoseLayer(new DualGuardPose());
      NeoForge.EVENT_BUS.addListener(e -> KageRender.onRenderStage(e));
      NeoForge.EVENT_BUS.addListener(e -> KageRender.clear());
      AutoTest.addScript(KageKage.ID, new KageAutotest());
      AutoTest.addScript(DevilFruits.id("vfx_kage_m3"), new KageCapture("vfx_kage_"));
      AutoTest.addScript(DevilFruits.id("vfx_kage_spear"), new KageCapture("vfx_kspear_", "spear"));
      AutoTest.addScript(DevilFruits.id("vfx_kage_m4"), new KageCapture("vfx_kage4_", "m4"));
      AutoTest.addScript(DevilFruits.id("vfx_kage_trap"), new KageCapture("vfx_ktrap_", "trap"));
      AutoTest.addScript(DevilFruits.id("vfx_kage_m5"), new KageCapture("vfx_kage5_", "m5"));
      AutoTest.addScript(DevilFruits.id("vfx_kage_m6"), new KageCapture("vfx_kage6_", "m6"));
      AutoTest.addScript(DevilFruits.id("vfx_kage_zombie"), new KageCapture("vfx_kage6_", "zombie"));
      AutoTest.addScript(DevilFruits.id("vfx_kage_blades"), new KageCapture("vfx_kblades_", "blades"));
      AutoTest.addScript(DevilFruits.id("vfx_kage_fixes"), new KageCapture("vfx_kblades_", "fixes"));
      AutoTest.addScript(DevilFruits.id("vfx_kage_rims"), new KageCapture("vfx_krims_", "rims"));
   }

   private KageClient() {
   }
}
