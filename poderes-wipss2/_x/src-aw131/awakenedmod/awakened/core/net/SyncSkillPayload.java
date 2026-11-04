package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.data.SkillData;
import com.awakenedmod.awakened.core.skill.SkillFormula;
import com.awakenedmod.awakened.core.skill.SkillSystem;
import com.awakenedmod.awakened.core.skill.Stat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SyncSkillPayload(int level, float xp, float xpForNext, int availablePoints, List<Integer> points) implements CustomPacketPayload {
   public static final Type<SyncSkillPayload> TYPE = new Type(AwId.of("sync_skill"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SyncSkillPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      SyncSkillPayload::level,
      ByteBufCodecs.FLOAT,
      SyncSkillPayload::xp,
      ByteBufCodecs.FLOAT,
      SyncSkillPayload::xpForNext,
      ByteBufCodecs.VAR_INT,
      SyncSkillPayload::availablePoints,
      ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()),
      SyncSkillPayload::points,
      SyncSkillPayload::new
   );

   public static SyncSkillPayload of(SkillData data) {
      List<Integer> points = new ArrayList<>(Stat.count());

      for (Stat stat : Stat.values()) {
         points.add(data.points(stat));
      }

      return new SyncSkillPayload(data.level(), (float)data.xp(), (float)SkillFormula.xpForNext(data.level()), SkillSystem.availablePoints(data), points);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
