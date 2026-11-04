package com.devilfruits.fruits.doru_doru.atelier;

import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.ability.WaxToolsAbility;
import com.devilfruits.fruits.doru_doru.client.atelier.PourClient;
import com.devilfruits.fruits.doru_doru.coating.WaxCoating;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class DoruNetwork {
   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("10");
      registrar.playToServer(DoruNetwork.ToolChoicePayload.TYPE, DoruNetwork.ToolChoicePayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer player) {
            WaxToolsAbility.choose(player, payload.tool());
         }
      });
      registrar.playToServer(DoruNetwork.StrugglePayload.TYPE, DoruNetwork.StrugglePayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer player) {
            WaxCoating.struggle(player);
         }
      });
      registrar.playToServer(DoruNetwork.ShakePayload.TYPE, DoruNetwork.ShakePayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer player) {
            WaxCoating.shake(player);
         }
      });
      registrar.playToServer(DoruNetwork.SculptPayload.TYPE, DoruNetwork.SculptPayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer player) {
            BuildEngine.handle(player, payload.request());
         }
      });
      registrar.playToServer(DoruNetwork.CommandPayload.TYPE, DoruNetwork.CommandPayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer player) {
            BuildEngine.command(player, payload.action());
         }
      });
      registrar.playToClient(DoruNetwork.PourPayload.TYPE, DoruNetwork.PourPayload.STREAM_CODEC, (payload, ctx) -> PourClient.handle(payload));
   }

   private DoruNetwork() {
   }

   public static record CommandPayload(int action) implements CustomPacketPayload {
      public static final int UNDO = 0;
      public static final int REDO = 1;
      public static final int STOP = 2;
      public static final Type<DoruNetwork.CommandPayload> TYPE = new Type(DoruDoru.id("sculpt_command"));
      public static final StreamCodec<ByteBuf, DoruNetwork.CommandPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
         .map(DoruNetwork.CommandPayload::new, DoruNetwork.CommandPayload::action);

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public static record PourPayload(int entity, Vec3 hands, int rate, long start, int total, int color, long[] path, boolean end, boolean carve)
      implements CustomPacketPayload {
      public static final int PATH = 96;
      public static final Type<DoruNetwork.PourPayload> TYPE = new Type(DoruDoru.id("pour"));
      public static final StreamCodec<ByteBuf, DoruNetwork.PourPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
         ByteBufCodecs.VAR_INT.encode(buf, p.entity);
         buf.writeBoolean(p.end);
         buf.writeBoolean(p.carve);
         if (!p.end) {
            buf.writeDouble(p.hands.x);
            buf.writeDouble(p.hands.y);
            buf.writeDouble(p.hands.z);
            ByteBufCodecs.VAR_INT.encode(buf, p.rate);
            buf.writeLong(p.start);
            ByteBufCodecs.VAR_INT.encode(buf, p.total);
            ByteBufCodecs.VAR_INT.encode(buf, p.color);
            ByteBufCodecs.VAR_INT.encode(buf, p.path.length);

            for (long k : p.path) {
               buf.writeLong(k);
            }
         }
      }, buf -> {
         int entity = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
         boolean end = buf.readBoolean();
         boolean carve = buf.readBoolean();
         if (end) {
            return new DoruNetwork.PourPayload(entity, Vec3.ZERO, 0, 0L, 0, 0, new long[0], true, carve);
         } else {
            Vec3 hands = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            int rate = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
            long start = buf.readLong();
            int total = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
            int color = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
            int n = Math.min(96, Math.max(0, (Integer)ByteBufCodecs.VAR_INT.decode(buf)));
            long[] path = new long[n];

            for (int i = 0; i < n; i++) {
               path[i] = buf.readLong();
            }

            return new DoruNetwork.PourPayload(entity, hands, rate, start, total, color, path, false, carve);
         }
      });

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public static record SculptPayload(SculptRequest request) implements CustomPacketPayload {
      public static final Type<DoruNetwork.SculptPayload> TYPE = new Type(DoruDoru.id("sculpt"));
      public static final StreamCodec<ByteBuf, DoruNetwork.SculptPayload> STREAM_CODEC = SculptRequest.STREAM_CODEC
         .map(DoruNetwork.SculptPayload::new, DoruNetwork.SculptPayload::request);

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public static record ShakePayload() implements CustomPacketPayload {
      public static final Type<DoruNetwork.ShakePayload> TYPE = new Type(DoruDoru.id("shake"));
      public static final StreamCodec<ByteBuf, DoruNetwork.ShakePayload> STREAM_CODEC = StreamCodec.unit(new DoruNetwork.ShakePayload());

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public static record StrugglePayload() implements CustomPacketPayload {
      public static final Type<DoruNetwork.StrugglePayload> TYPE = new Type(DoruDoru.id("struggle"));
      public static final StreamCodec<ByteBuf, DoruNetwork.StrugglePayload> STREAM_CODEC = StreamCodec.unit(new DoruNetwork.StrugglePayload());

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public static record ToolChoicePayload(int tool) implements CustomPacketPayload {
      public static final Type<DoruNetwork.ToolChoicePayload> TYPE = new Type(DoruDoru.id("tool_choice"));
      public static final StreamCodec<ByteBuf, DoruNetwork.ToolChoicePayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
         .map(DoruNetwork.ToolChoicePayload::new, DoruNetwork.ToolChoicePayload::tool);

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
