package com.awakenedmod.awakened.content.summoning;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class MinionData {
   private static final UUID NIL = new UUID(0L, 0L);
   public static final MinionData NONE = new MinionData();
   public static final MapCodec<MinionData> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               UUIDUtil.CODEC.optionalFieldOf("owner", NIL).forGetter(d -> d.owner),
               Codec.STRING.optionalFieldOf("soulType", "").forGetter(d -> d.soulType),
               Codec.FLOAT.optionalFieldOf("powerBonus", 0.0F).forGetter(d -> d.powerBonus),
               Codec.BOOL.optionalFieldOf("conjured", false).forGetter(d -> d.conjured)
            )
            .apply(instance, MinionData::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, MinionData> STREAM_CODEC = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC,
      d -> d.owner,
      ByteBufCodecs.STRING_UTF8,
      d -> d.soulType,
      ByteBufCodecs.FLOAT,
      d -> d.powerBonus,
      ByteBufCodecs.BOOL,
      d -> d.conjured,
      MinionData::new
   );
   private final UUID owner;
   private final String soulType;
   private final float powerBonus;
   private final boolean conjured;

   public MinionData() {
      this(NIL, "", 0.0F, false);
   }

   public MinionData(UUID owner, String soulType) {
      this(owner, soulType, 0.0F, false);
   }

   public MinionData(UUID owner, String soulType, float powerBonus) {
      this(owner, soulType, powerBonus, false);
   }

   public MinionData(UUID owner, String soulType, float powerBonus, boolean conjured) {
      this.owner = owner;
      this.soulType = soulType;
      this.powerBonus = powerBonus;
      this.conjured = conjured;
   }

   public UUID owner() {
      return this.owner;
   }

   public String soulType() {
      return this.soulType;
   }

   public float powerBonus() {
      return this.powerBonus;
   }

   public boolean conjured() {
      return this.conjured;
   }

   public boolean isMinion() {
      return !this.owner.equals(NIL);
   }

   public boolean isOwnedBy(UUID candidate) {
      return this.isMinion() && this.owner.equals(candidate);
   }
}
