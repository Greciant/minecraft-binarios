package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.core.AwId;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;

@EventBusSubscriber(
   modid = "awakened",
   bus = Bus.MOD
)
public final class DenseAttachments {
   private DenseAttachments() {
   }

   @SubscribeEvent
   public static void onRegister(RegisterEvent event) {
      event.register(Keys.ATTACHMENT_TYPES, AwId.of("graviton_dense"), () -> DenseVisual.TYPE);
   }
}
