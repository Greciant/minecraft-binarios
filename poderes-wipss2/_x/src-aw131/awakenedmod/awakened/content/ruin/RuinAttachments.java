package com.awakenedmod.awakened.content.ruin;

import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class RuinAttachments {
   private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "awakened");
   public static final Supplier<AttachmentType<RuinDecay>> DECAY = ATTACHMENT_TYPES.register(
      "ruin_decay", () -> AttachmentType.builder(() -> RuinDecay.NONE).sync(RuinDecay.STREAM_CODEC).build()
   );
   public static final Supplier<AttachmentType<RuinHand>> HAND = ATTACHMENT_TYPES.register(
      "ruin_hand", () -> AttachmentType.builder(() -> RuinHand.NONE).sync(RuinHand.STREAM_CODEC).build()
   );

   private RuinAttachments() {
   }

   public static void register(IEventBus modEventBus) {
      ATTACHMENT_TYPES.register(modEventBus);
   }
}
