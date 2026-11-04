package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SyncTeamPayload(boolean enabled, boolean inTeam, boolean friendlyFire, boolean youLead, List<SyncTeamPayload.Member> members, List<String> invites)
   implements CustomPacketPayload {
   public static final SyncTeamPayload NONE = none(List.of());
   public static final SyncTeamPayload OFF = new SyncTeamPayload(false, false, true, false, List.of(), List.of());
   public static final Type<SyncTeamPayload> TYPE = new Type(AwId.of("sync_team"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SyncTeamPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      SyncTeamPayload::enabled,
      ByteBufCodecs.BOOL,
      SyncTeamPayload::inTeam,
      ByteBufCodecs.BOOL,
      SyncTeamPayload::friendlyFire,
      ByteBufCodecs.BOOL,
      SyncTeamPayload::youLead,
      SyncTeamPayload.Member.STREAM_CODEC.apply(ByteBufCodecs.list()),
      SyncTeamPayload::members,
      ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
      SyncTeamPayload::invites,
      SyncTeamPayload::new
   );

   public static SyncTeamPayload none(List<String> invites) {
      return new SyncTeamPayload(true, false, true, false, List.of(), invites);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static record Member(String name, UUID id, boolean leader, boolean online) {
      public static final StreamCodec<RegistryFriendlyByteBuf, SyncTeamPayload.Member> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8,
         SyncTeamPayload.Member::name,
         UUIDUtil.STREAM_CODEC,
         SyncTeamPayload.Member::id,
         ByteBufCodecs.BOOL,
         SyncTeamPayload.Member::leader,
         ByteBufCodecs.BOOL,
         SyncTeamPayload.Member::online,
         SyncTeamPayload.Member::new
      );
   }
}
