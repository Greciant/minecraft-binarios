package com.devilfruits.registry;

import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.haki.PlayerHakiData;
import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class DFAttachments {
   public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "devilfruits");
   public static final Supplier<AttachmentType<PlayerFruitData>> PLAYER_DATA = ATTACHMENTS.register(
      "player_data",
      () -> AttachmentType.builder(PlayerFruitData::new)
            .serialize(PlayerFruitData.CODEC)
            .copyOnDeath()
            .sync((holder, to) -> holder == to, PlayerFruitData.STREAM_CODEC)
            .build()
   );
   public static final Supplier<AttachmentType<PlayerHakiData>> HAKI = ATTACHMENTS.register(
      "haki",
      () -> AttachmentType.builder(PlayerHakiData::new)
            .serialize(PlayerHakiData.CODEC)
            .copyOnDeath()
            .sync((holder, to) -> true, PlayerHakiData.STREAM_CODEC)
            .build()
   );

   public static PlayerFruitData data(Player player) {
      return (PlayerFruitData)player.getData(PLAYER_DATA);
   }

   private DFAttachments() {
   }
}
