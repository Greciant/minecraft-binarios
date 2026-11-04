package com.awakenedmod.awakened.content.limitless;

import com.awakenedmod.awakened.content.limitless.blue.BlueHoldState;
import com.awakenedmod.awakened.content.limitless.domain.VoidBody;
import com.awakenedmod.awakened.content.limitless.purple.PurpleCast;
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
public final class LimitlessAttachments {
   private LimitlessAttachments() {
   }

   @SubscribeEvent
   public static void onRegister(RegisterEvent event) {
      event.register(Keys.ATTACHMENT_TYPES, AwId.of("limitless_blue_hold"), () -> BlueHoldState.TYPE);
      event.register(Keys.ATTACHMENT_TYPES, AwId.of("limitless_red_cast"), () -> LimitlessCast.RED);
      event.register(Keys.ATTACHMENT_TYPES, AwId.of("limitless_purple_cast"), () -> PurpleCast.TYPE);
      event.register(Keys.ATTACHMENT_TYPES, AwId.of("limitless_rct_cast"), () -> LimitlessCast.RCT);
      event.register(Keys.ATTACHMENT_TYPES, AwId.of("limitless_void_body"), () -> VoidBody.TYPE);
   }
}
