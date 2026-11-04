package com.awakenedmod.awakened.content.command;

import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class CommandAttachments {
   private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "awakened");
   public static final Supplier<AttachmentType<CommandData>> RULES = ATTACHMENT_TYPES.register(
      "command_rules",
      () -> AttachmentType.builder(() -> CommandData.EMPTY).serialize(CommandData.MAP_CODEC.codec()).sync(CommandData.STREAM_CODEC).copyOnDeath().build()
   );

   private CommandAttachments() {
   }

   public static void register(IEventBus modEventBus) {
      ATTACHMENT_TYPES.register(modEventBus);
   }
}
