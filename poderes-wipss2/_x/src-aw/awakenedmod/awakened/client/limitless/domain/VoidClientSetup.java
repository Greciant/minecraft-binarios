package com.awakenedmod.awakened.client.limitless.domain;

import com.awakenedmod.awakened.content.AwakenedEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT},
   bus = Bus.MOD
)
public final class VoidClientSetup {
   private VoidClientSetup() {
   }

   @SubscribeEvent
   public static void onRenderers(RegisterRenderers event) {
      event.registerEntityRenderer(AwakenedEntities.LIMITLESS_VOID_ANCHOR.get(), NoopRenderer::new);
   }

   @SubscribeEvent
   public static void onGuiLayers(RegisterGuiLayersEvent event) {
      event.registerAboveAll(VoidIntro.LAYER, new VoidLetterbox());
   }
}
