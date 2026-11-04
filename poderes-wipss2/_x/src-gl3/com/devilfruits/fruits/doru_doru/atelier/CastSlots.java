package com.devilfruits.fruits.doru_doru.atelier;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class CastSlots {
   public static final int SLOTS = 3;
   public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "devilfruits");
   private static final Codec<CastSlots> CODEC = Codec.list(CastData.CODEC.optionalFieldOf("cast").codec()).xmap(CastSlots::of, CastSlots::list);
   private static final StreamCodec<ByteBuf, CastSlots> STREAM_CODEC = ByteBufCodecs.optional(CastData.STREAM_CODEC)
      .apply(ByteBufCodecs.list(3))
      .map(CastSlots::of, CastSlots::list);
   public static final Supplier<AttachmentType<CastSlots>> TYPE = ATTACHMENTS.register(
      "doru_doru/casts", () -> AttachmentType.builder(CastSlots::new).serialize(CODEC).copyOnDeath().sync((holder, to) -> holder == to, STREAM_CODEC).build()
   );
   private final CastData[] slots = new CastData[3];

   public static void init(IEventBus modBus) {
      ATTACHMENTS.register(modBus);
   }

   public static CastSlots of(Player player) {
      return (CastSlots)player.getData(TYPE);
   }

   @Nullable
   public CastData get(int slot) {
      return slot >= 0 && slot < 3 ? this.slots[slot] : null;
   }

   public void set(int slot, @Nullable CastData data) {
      if (slot >= 0 && slot < 3) {
         this.slots[slot] = data;
      }
   }

   private static CastSlots of(List<Optional<CastData>> list) {
      CastSlots s = new CastSlots();

      for (int i = 0; i < Math.min(3, list.size()); i++) {
         s.slots[i] = list.get(i).orElse(null);
      }

      return s;
   }

   private List<Optional<CastData>> list() {
      List<Optional<CastData>> out = new ArrayList<>(3);

      for (CastData c : this.slots) {
         out.add(Optional.ofNullable(c));
      }

      return out;
   }
}
