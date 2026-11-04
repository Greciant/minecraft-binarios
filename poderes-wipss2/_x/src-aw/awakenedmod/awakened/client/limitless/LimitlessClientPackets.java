package com.awakenedmod.awakened.client.limitless;

import com.awakenedmod.awakened.content.limitless.InfinityStopPayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class LimitlessClientPackets {
   private LimitlessClientPackets() {
   }

   public static void register(PayloadRegistrar registrar) {
      registrar.playToClient(
         InfinityStopPayload.TYPE, InfinityStopPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> InfinityStops.accept(payload))
      );
   }
}
