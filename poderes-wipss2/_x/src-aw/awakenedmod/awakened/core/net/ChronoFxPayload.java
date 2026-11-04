package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public record ChronoFxPayload(ChronoFxPayload.Kind kind, int entityId, double x, double y, double z, float radius, int duration, int seed, List<Vec3> points)
   implements CustomPacketPayload {
   public static final Type<ChronoFxPayload> TYPE = new Type(AwId.of("chrono_fx"));
   public static final int MAX_POINTS = 20;
   public static final float MAX_RADIUS = 64.0F;
   public static final int MAX_DURATION = 600;
   private static final int POINT_BYTES = 24;
   public static final StreamCodec<RegistryFriendlyByteBuf, ChronoFxPayload> STREAM_CODEC = StreamCodec.of(ChronoFxPayload::write, ChronoFxPayload::read);

   public ChronoFxPayload(ChronoFxPayload.Kind kind, int entityId, double x, double y, double z, float radius, int duration, int seed, List<Vec3> points) {
      points = List.copyOf(points.size() > 20 ? points.subList(0, 20) : points);
      this.kind = kind;
      this.entityId = entityId;
      this.x = x;
      this.y = y;
      this.z = z;
      this.radius = radius;
      this.duration = duration;
      this.seed = seed;
      this.points = points;
   }

   public Vec3 pos() {
      return new Vec3(this.x, this.y, this.z);
   }

   public boolean finite() {
      if (Double.isFinite(this.x) && Double.isFinite(this.y) && Double.isFinite(this.z) && Float.isFinite(this.radius)) {
         for (Vec3 v : this.points) {
            if (!Double.isFinite(v.x) || !Double.isFinite(v.y) || !Double.isFinite(v.z)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static void write(RegistryFriendlyByteBuf buf, ChronoFxPayload p) {
      buf.writeVarInt(p.kind.ordinal());
      buf.writeVarInt(p.entityId);
      buf.writeDouble(p.x);
      buf.writeDouble(p.y);
      buf.writeDouble(p.z);
      buf.writeFloat(p.radius);
      buf.writeVarInt(p.duration);
      buf.writeInt(p.seed);
      buf.writeVarInt(p.points.size());

      for (Vec3 v : p.points) {
         buf.writeDouble(v.x);
         buf.writeDouble(v.y);
         buf.writeDouble(v.z);
      }
   }

   private static ChronoFxPayload read(RegistryFriendlyByteBuf buf) {
      ChronoFxPayload.Kind[] kinds = ChronoFxPayload.Kind.values();
      int ordinal = buf.readVarInt();
      ChronoFxPayload.Kind kind = ordinal >= 0 && ordinal < kinds.length ? kinds[ordinal] : ChronoFxPayload.Kind.UNKNOWN;
      int entityId = buf.readVarInt();
      double x = buf.readDouble();
      double y = buf.readDouble();
      double z = buf.readDouble();
      float rawRadius = buf.readFloat();
      int duration = Mth.clamp(buf.readVarInt(), 0, 600);
      int seed = buf.readInt();
      int count = Mth.clamp(buf.readVarInt(), 0, buf.readableBytes() / 24);
      boolean finite = Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
      List<Vec3> points = new ArrayList<>(Math.min(count, 20));

      for (int i = 0; i < count; i++) {
         double px = buf.readDouble();
         double py = buf.readDouble();
         double pz = buf.readDouble();
         if (Double.isFinite(px) && Double.isFinite(py) && Double.isFinite(pz)) {
            if (points.size() < 20) {
               points.add(new Vec3(px, py, pz));
            }
         } else {
            finite = false;
         }
      }

      if (!finite) {
         return new ChronoFxPayload(ChronoFxPayload.Kind.UNKNOWN, entityId, 0.0, 0.0, 0.0, 0.0F, 0, seed, List.of());
      } else {
         float radius = Float.isFinite(rawRadius) ? Mth.clamp(rawRadius, 0.0F, 64.0F) : 0.0F;
         return new ChronoFxPayload(kind, entityId, x, y, z, radius, duration, seed, points);
      }
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static enum Kind {
      TIME_STOP,
      TIME_STOP_END,
      HELD_IMPACT,
      STASIS_LOCK,
      STASIS_RELEASE,
      REWIND,
      FLICKER,
      TIME_STOP_RESOLVE,
      UNKNOWN;
   }
}
