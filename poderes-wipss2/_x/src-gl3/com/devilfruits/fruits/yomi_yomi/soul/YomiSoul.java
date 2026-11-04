package com.devilfruits.fruits.yomi_yomi.soul;

import com.devilfruits.registry.DFAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentType;

public record YomiSoul(int ticksLeft, Optional<UUID> corpse, ResourceKey<Level> dimension, Vec3 body, boolean pending) {
   public static final YomiSoul NONE = new YomiSoul(0, Optional.empty(), Level.OVERWORLD, Vec3.ZERO, false);
   private static final Codec<Vec3> VEC = Codec.DOUBLE
      .listOf(3, 3)
      .xmap(l -> new Vec3((Double)l.get(0), (Double)l.get(1), (Double)l.get(2)), v -> List.of(v.x, v.y, v.z));
   public static final Codec<YomiSoul> CODEC = RecordCodecBuilder.create(
      i -> i.group(
               Codec.INT.optionalFieldOf("ticks_left", 0).forGetter(YomiSoul::ticksLeft),
               UUIDUtil.CODEC.optionalFieldOf("corpse").forGetter(YomiSoul::corpse),
               ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension", Level.OVERWORLD).forGetter(YomiSoul::dimension),
               VEC.optionalFieldOf("body", Vec3.ZERO).forGetter(YomiSoul::body),
               Codec.BOOL.optionalFieldOf("pending", false).forGetter(YomiSoul::pending)
            )
            .apply(i, YomiSoul::new)
   );
   public static final StreamCodec<ByteBuf, YomiSoul> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      YomiSoul::ticksLeft,
      ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
      YomiSoul::corpse,
      ResourceKey.streamCodec(Registries.DIMENSION),
      YomiSoul::dimension,
      ByteBufCodecs.DOUBLE.apply(ByteBufCodecs.list(3)).map(l -> new Vec3((Double)l.get(0), (Double)l.get(1), (Double)l.get(2)), v -> List.of(v.x, v.y, v.z)),
      YomiSoul::body,
      ByteBufCodecs.BOOL,
      YomiSoul::pending,
      YomiSoul::new
   );
   public static final Supplier<AttachmentType<YomiSoul>> ATTACHMENT = DFAttachments.ATTACHMENTS
      .register(
         "yomi_yomi/soul", () -> AttachmentType.builder(() -> NONE).serialize(CODEC).copyOnDeath().sync((holder, to) -> holder == to, STREAM_CODEC).build()
      );

   public static void init() {
   }

   public boolean active() {
      return this.corpse.isPresent();
   }

   public YomiSoul withTicks(int ticks) {
      return new YomiSoul(ticks, this.corpse, this.dimension, this.body, this.pending);
   }

   public YomiSoul respawned() {
      return new YomiSoul(this.ticksLeft, this.corpse, this.dimension, this.body, false);
   }

   public static YomiSoul of(Player player) {
      return player.hasData(ATTACHMENT) ? (YomiSoul)player.getData(ATTACHMENT) : NONE;
   }

   @Nullable
   public UUID corpseId() {
      return this.corpse.orElse(null);
   }
}
