package com.awakenedmod.awakened.content.reaper;

import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ReaperAttachments {
   private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "awakened");
   public static final Supplier<AttachmentType<ReaperSoulData>> REAPER_SOULS = ATTACHMENT_TYPES.register(
      "reaper_souls",
      () -> AttachmentType.builder(ReaperSoulData::new).serialize(ReaperSoulData.MAP_CODEC.codec()).sync(ReaperSoulData.STREAM_CODEC).copyOnDeath().build()
   );

   private ReaperAttachments() {
   }

   public static void register(IEventBus modEventBus) {
      ATTACHMENT_TYPES.register(modEventBus);
   }

   public static ReaperSoulData souls(Player player) {
      return (ReaperSoulData)player.getData(REAPER_SOULS);
   }

   public static void add(Player player, long n) {
      ReaperSoulData data = (ReaperSoulData)player.getData(REAPER_SOULS);
      data.add(n);
      player.setData(REAPER_SOULS, data);
   }

   public static void withdraw(Player player, long n) {
      ReaperSoulData data = (ReaperSoulData)player.getData(REAPER_SOULS);
      data.withdraw(n);
      player.setData(REAPER_SOULS, data);
   }

   public static void set(Player player, long n) {
      ReaperSoulData data = (ReaperSoulData)player.getData(REAPER_SOULS);
      data.set(n);
      player.setData(REAPER_SOULS, data);
   }
}
