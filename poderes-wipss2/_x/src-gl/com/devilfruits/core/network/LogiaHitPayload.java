package com.devilfruits.core.network;

import com.devilfruits.DevilFruits;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.phys.Vec3;

public record LogiaHitPayload(int victimId, List<LogiaHitPayload.Hit> hits) implements CustomPacketPayload {
   public static final Type<LogiaHitPayload> TYPE = new Type(DevilFruits.id("logia_hit"));
   public static final int MAX_HITS = 4;
   public static final int PASS = 0;
   public static final int PARTIAL = 1;
   public static final int HAKI = 2;
   public static final StreamCodec<FriendlyByteBuf, LogiaHitPayload> STREAM_CODEC = CustomPacketPayload.codec((p, buf) -> {
      buf.writeVarInt(p.victimId);
      buf.writeByte(p.hits.size());

      for (LogiaHitPayload.Hit hit : p.hits) {
         buf.writeVec3(hit.pos());
         buf.writeVec3(hit.dir());
         buf.writeByte(hit.category());
         buf.writeByte(hit.kind());
         buf.writeVarInt(hit.attacker());
         buf.writeFloat(hit.size());
      }
   }, buf -> {
      int victim = buf.readVarInt();
      int n = Math.min(4, buf.readByte());
      List<LogiaHitPayload.Hit> hits = new ArrayList<>(n);

      for (int i = 0; i < n; i++) {
         hits.add(new LogiaHitPayload.Hit(buf.readVec3(), buf.readVec3(), buf.readByte(), buf.readByte(), buf.readVarInt(), buf.readFloat()));
      }

      return new LogiaHitPayload(victim, hits);
   });

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static record Hit(Vec3 pos, Vec3 dir, int category, int kind, int attacker, float size) {
      public boolean partial() {
         return this.kind == 1;
      }

      public boolean haki() {
         return this.kind == 2;
      }
   }
}
