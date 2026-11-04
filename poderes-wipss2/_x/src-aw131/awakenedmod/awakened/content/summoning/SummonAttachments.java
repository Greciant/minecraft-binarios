package com.awakenedmod.awakened.content.summoning;

import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class SummonAttachments {
   private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "awakened");
   public static final Supplier<AttachmentType<SoulData>> SOULS = ATTACHMENT_TYPES.register(
      "souls", () -> AttachmentType.builder(SoulData::new).serialize(SoulData.MAP_CODEC.codec()).sync(SoulData.STREAM_CODEC).copyOnDeath().build()
   );
   public static final Supplier<AttachmentType<MinionData>> MINION = ATTACHMENT_TYPES.register(
      "minion", () -> AttachmentType.builder(MinionData::new).serialize(MinionData.MAP_CODEC.codec()).sync(MinionData.STREAM_CODEC).build()
   );

   private SummonAttachments() {
   }

   public static void register(IEventBus modEventBus) {
      ATTACHMENT_TYPES.register(modEventBus);
   }

   public static SoulData souls(Player player) {
      return (SoulData)player.getData(SOULS);
   }

   public static MinionData minion(Entity entity) {
      return entity.hasData(MINION) ? (MinionData)entity.getData(MINION) : MinionData.NONE;
   }
}
