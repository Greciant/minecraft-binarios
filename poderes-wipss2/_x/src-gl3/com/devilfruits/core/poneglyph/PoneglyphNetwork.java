package com.devilfruits.core.poneglyph;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.poneglyph.PoneglyphClientPackets;
import com.devilfruits.core.network.DFNetwork;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PoneglyphNetwork {
   public static void register(PayloadRegistrar registrar) {
      registrar.playToServer(PoneglyphNetwork.RubHeldPayload.TYPE, PoneglyphNetwork.RubHeldPayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer player) {
            RubbingService.heartbeat(player, payload.pos());
         }
      });
      registrar.playToClient(
         PoneglyphNetwork.RubStatePayload.TYPE, PoneglyphNetwork.RubStatePayload.STREAM_CODEC, (payload, ctx) -> PoneglyphClientPackets.rubState(payload)
      );
      registrar.playToClient(
         PoneglyphNetwork.DeskStatePayload.TYPE, PoneglyphNetwork.DeskStatePayload.STREAM_CODEC, (payload, ctx) -> PoneglyphClientPackets.deskState(payload)
      );
      registrar.playToClient(
         PoneglyphNetwork.PoneglyphSyncPayload.TYPE, PoneglyphNetwork.PoneglyphSyncPayload.STREAM_CODEC, (payload, ctx) -> PoneglyphClientPackets.sync(payload)
      );
      registrar.playToClient(
         PoneglyphNetwork.KnowledgeToastPayload.TYPE,
         PoneglyphNetwork.KnowledgeToastPayload.STREAM_CODEC,
         (payload, ctx) -> PoneglyphClientPackets.toast(payload)
      );
   }

   public static void sync(ServerPlayer player) {
      List<Integer> perm = new ArrayList<>();

      for (int v : Cipher.active(player.server)) {
         perm.add(v);
      }

      List<PoneglyphNetwork.PoneglyphInfo> infos = new ArrayList<>();

      for (Entry<ResourceLocation, PoneglyphData.Poneglyph> e : PoneglyphData.poneglyphs().entrySet()) {
         infos.add(new PoneglyphNetwork.PoneglyphInfo(e.getKey(), e.getValue().knowledge(), e.getValue().loreKey(e.getKey()), e.getValue().carvingVariant()));
      }

      DFNetwork.sendTo(
         player, new PoneglyphNetwork.PoneglyphSyncPayload(PoneglyphConfig.CIPHER_MODE.get() == PoneglyphConfig.CipherMode.WORLDBOUND, perm, infos)
      );
   }

   private PoneglyphNetwork() {
   }

   public static record DeskStatePayload(
      int containerId,
      int status,
      int round,
      int rounds,
      String clue,
      List<String> options,
      int mistakes,
      int maxMistakes,
      int lockTicks,
      String hint,
      int reveal,
      int perks,
      int literacy,
      int xp,
      int xpFrom,
      int xpTo,
      int event,
      int eventIndex
   ) implements CustomPacketPayload {
      public static final int PERK_HINT_READY = 1;
      public static final int PERK_REVEAL_READY = 2;
      public static final int PERK_HINT = 4;
      public static final int PERK_REVEAL = 8;
      public static final Type<PoneglyphNetwork.DeskStatePayload> TYPE = new Type(DevilFruits.id("poneglyph/desk_state"));
      public static final StreamCodec<FriendlyByteBuf, PoneglyphNetwork.DeskStatePayload> STREAM_CODEC = StreamCodec.of(
         (buf, p) -> {
            buf.writeVarInt(p.containerId);
            buf.writeVarInt(p.status);
            buf.writeVarInt(p.round);
            buf.writeVarInt(p.rounds);
            buf.writeUtf(p.clue, 64);
            buf.writeVarInt(p.options.size());
            p.options.forEach(o -> buf.writeUtf(o, 64));
            buf.writeVarInt(p.mistakes);
            buf.writeVarInt(p.maxMistakes);
            buf.writeVarInt(p.lockTicks);
            buf.writeUtf(p.hint, 8);
            buf.writeVarInt(p.reveal + 1);
            buf.writeVarInt(p.perks);
            buf.writeVarInt(p.literacy);
            buf.writeVarInt(p.xp);
            buf.writeVarInt(p.xpFrom);
            buf.writeVarInt(p.xpTo);
            buf.writeVarInt(p.event);
            buf.writeVarInt(p.eventIndex + 1);
         },
         buf -> {
            int id = buf.readVarInt();
            int status = buf.readVarInt();
            int round = buf.readVarInt();
            int rounds = buf.readVarInt();
            String clue = buf.readUtf(64);
            int n = Math.min(buf.readVarInt(), 32);
            List<String> options = new ArrayList<>(n);

            for (int i = 0; i < n; i++) {
               options.add(buf.readUtf(64));
            }

            return new PoneglyphNetwork.DeskStatePayload(
               id,
               status,
               round,
               rounds,
               clue,
               options,
               buf.readVarInt(),
               buf.readVarInt(),
               buf.readVarInt(),
               buf.readUtf(8),
               buf.readVarInt() - 1,
               buf.readVarInt(),
               buf.readVarInt(),
               buf.readVarInt(),
               buf.readVarInt(),
               buf.readVarInt(),
               buf.readVarInt(),
               buf.readVarInt() - 1
            );
         }
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public static record KnowledgeToastPayload(ResourceLocation knowledge, String nameKey, ResourceLocation icon) implements CustomPacketPayload {
      public static final Type<PoneglyphNetwork.KnowledgeToastPayload> TYPE = new Type(DevilFruits.id("poneglyph/knowledge_toast"));
      public static final StreamCodec<ByteBuf, PoneglyphNetwork.KnowledgeToastPayload> STREAM_CODEC = StreamCodec.composite(
         ResourceLocation.STREAM_CODEC,
         PoneglyphNetwork.KnowledgeToastPayload::knowledge,
         ByteBufCodecs.STRING_UTF8,
         PoneglyphNetwork.KnowledgeToastPayload::nameKey,
         ResourceLocation.STREAM_CODEC,
         PoneglyphNetwork.KnowledgeToastPayload::icon,
         PoneglyphNetwork.KnowledgeToastPayload::new
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public static record PoneglyphInfo(ResourceLocation id, ResourceLocation knowledge, String loreKey, int carving) {
      static final StreamCodec<ByteBuf, PoneglyphNetwork.PoneglyphInfo> STREAM_CODEC = StreamCodec.composite(
         ResourceLocation.STREAM_CODEC,
         PoneglyphNetwork.PoneglyphInfo::id,
         ResourceLocation.STREAM_CODEC,
         PoneglyphNetwork.PoneglyphInfo::knowledge,
         ByteBufCodecs.STRING_UTF8,
         PoneglyphNetwork.PoneglyphInfo::loreKey,
         ByteBufCodecs.VAR_INT,
         PoneglyphNetwork.PoneglyphInfo::carving,
         PoneglyphNetwork.PoneglyphInfo::new
      );
   }

   public static record PoneglyphSyncPayload(boolean worldbound, List<Integer> permutation, List<PoneglyphNetwork.PoneglyphInfo> poneglyphs)
      implements CustomPacketPayload {
      public static final Type<PoneglyphNetwork.PoneglyphSyncPayload> TYPE = new Type(DevilFruits.id("poneglyph/sync"));
      public static final StreamCodec<ByteBuf, PoneglyphNetwork.PoneglyphSyncPayload> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.BOOL,
         PoneglyphNetwork.PoneglyphSyncPayload::worldbound,
         ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(26)),
         PoneglyphNetwork.PoneglyphSyncPayload::permutation,
         PoneglyphNetwork.PoneglyphInfo.STREAM_CODEC.apply(ByteBufCodecs.list()),
         PoneglyphNetwork.PoneglyphSyncPayload::poneglyphs,
         PoneglyphNetwork.PoneglyphSyncPayload::new
      );

      public int[] permutationArray() {
         return this.permutation.stream().mapToInt(Integer::intValue).toArray();
      }

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public static record RubHeldPayload(BlockPos pos) implements CustomPacketPayload {
      public static final Type<PoneglyphNetwork.RubHeldPayload> TYPE = new Type(DevilFruits.id("poneglyph/rub_held"));
      public static final StreamCodec<ByteBuf, PoneglyphNetwork.RubHeldPayload> STREAM_CODEC = BlockPos.STREAM_CODEC
         .map(PoneglyphNetwork.RubHeldPayload::new, PoneglyphNetwork.RubHeldPayload::pos);

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public static record RubStatePayload(BlockPos pos, int cell, int carving, int duration, int state) implements CustomPacketPayload {
      public static final int START = 0;
      public static final int DONE = 1;
      public static final int CANCEL = 2;
      public static final Type<PoneglyphNetwork.RubStatePayload> TYPE = new Type(DevilFruits.id("poneglyph/rub_state"));
      public static final StreamCodec<ByteBuf, PoneglyphNetwork.RubStatePayload> STREAM_CODEC = StreamCodec.composite(
         BlockPos.STREAM_CODEC,
         PoneglyphNetwork.RubStatePayload::pos,
         ByteBufCodecs.VAR_INT,
         PoneglyphNetwork.RubStatePayload::cell,
         ByteBufCodecs.VAR_INT,
         PoneglyphNetwork.RubStatePayload::carving,
         ByteBufCodecs.VAR_INT,
         PoneglyphNetwork.RubStatePayload::duration,
         ByteBufCodecs.VAR_INT,
         PoneglyphNetwork.RubStatePayload::state,
         PoneglyphNetwork.RubStatePayload::new
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
