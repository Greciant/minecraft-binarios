package com.awakenedmod.awakened.content.pocket;

import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class RiftAttachments {
   private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "awakened");
   public static final Supplier<AttachmentType<RiftStashData>> STASH = ATTACHMENT_TYPES.register(
      "pocket_stash", () -> AttachmentType.builder(RiftStashData::new).serialize(RiftStashData.MAP_CODEC.codec()).copyOnDeath().build()
   );

   private RiftAttachments() {
   }

   public static void register(IEventBus modEventBus) {
      ATTACHMENT_TYPES.register(modEventBus);
   }

   public static RiftStashData stash(Player player) {
      return (RiftStashData)player.getData(STASH);
   }
}
