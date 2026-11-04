package com.devilfruits.fruits.doru_doru.atelier;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public record SculptRequest(
   SculptRequest.Tool tool,
   int kind,
   List<Vec3> points,
   float[] params,
   float shell,
   SculptRequest.Symmetry symmetry,
   int folds,
   Vec3 origin,
   int color,
   float viscosity
) {
   public static final int MAX_POINTS = 64;
   private static final StreamCodec<ByteBuf, Vec3> VEC = StreamCodec.of((buf, v) -> {
      buf.writeDouble(v.x);
      buf.writeDouble(v.y);
      buf.writeDouble(v.z);
   }, buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
   public static final StreamCodec<ByteBuf, SculptRequest> STREAM_CODEC = StreamCodec.of(
      (buf, r) -> {
         ByteBufCodecs.VAR_INT.encode(buf, r.tool.ordinal());
         ByteBufCodecs.VAR_INT.encode(buf, r.kind);
         ByteBufCodecs.VAR_INT.encode(buf, r.points.size());

         for (Vec3 p : r.points) {
            VEC.encode(buf, p);
         }

         for (int i = 0; i < 4; i++) {
            buf.writeFloat(i < r.params.length ? r.params[i] : 0.0F);
         }

         buf.writeFloat(r.shell);
         ByteBufCodecs.VAR_INT.encode(buf, r.symmetry.ordinal());
         ByteBufCodecs.VAR_INT.encode(buf, r.folds);
         VEC.encode(buf, r.origin);
         ByteBufCodecs.VAR_INT.encode(buf, r.color);
         buf.writeFloat(r.viscosity);
      },
      buf -> {
         SculptRequest.Tool tool = SculptRequest.Tool.values()[Math.floorMod((Integer)ByteBufCodecs.VAR_INT.decode(buf), SculptRequest.Tool.values().length)];
         int kind = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
         int n = Math.min(64, Math.max(0, (Integer)ByteBufCodecs.VAR_INT.decode(buf)));
         List<Vec3> points = new ArrayList<>(n);

         for (int i = 0; i < n; i++) {
            points.add((Vec3)VEC.decode(buf));
         }

         float[] params = new float[4];

         for (int i = 0; i < 4; i++) {
            params[i] = buf.readFloat();
         }

         float shell = buf.readFloat();
         SculptRequest.Symmetry symmetry = SculptRequest.Symmetry.values()[Math.floorMod(
            (Integer)ByteBufCodecs.VAR_INT.decode(buf), SculptRequest.Symmetry.values().length
         )];
         int folds = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
         Vec3 origin = (Vec3)VEC.decode(buf);
         int color = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
         float viscosity = buf.readFloat();
         return new SculptRequest(tool, kind, points, params, shell, symmetry, folds, origin, color, viscosity);
      }
   );

   public float param(int i) {
      return i < this.params.length ? this.params[i] : 0.0F;
   }

   public ShapeKind shape() {
      return ShapeKind.of(this.kind);
   }

   public boolean sane() {
      for (Vec3 p : this.points) {
         if (!Double.isFinite(p.x) || !Double.isFinite(p.y) || !Double.isFinite(p.z)) {
            return false;
         }
      }

      for (float f : this.params) {
         if (!Float.isFinite(f)) {
            return false;
         }
      }

      return Float.isFinite(this.shell)
         && Float.isFinite(this.viscosity)
         && Double.isFinite(this.origin.x)
         && Double.isFinite(this.origin.y)
         && Double.isFinite(this.origin.z);
   }

   public static enum Symmetry {
      NONE,
      MIRROR_X,
      MIRROR_Z,
      MIRROR_XZ,
      RADIAL;
   }

   public static enum Tool {
      SHAPE,
      BRUSH,
      CAST,
      DISSOLVE;
   }
}
