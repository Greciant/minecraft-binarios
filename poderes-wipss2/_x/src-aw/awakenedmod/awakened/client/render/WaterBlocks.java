package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.function.Consumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class WaterBlocks {
   public static final float REST_LIFT = 0.01F;
   private static final double LIGHT_LIFT = 0.2;
   private static final float LOOSE_FLASH = 0.4F;

   private WaterBlocks() {
   }

   public static int lightAt(BlockAndTintGetter level, double x, double y, double z) {
      return LevelRenderer.getLightColor(level, BlockPos.containing(x, y, z));
   }

   public static int lightAbove(BlockAndTintGetter level, double x, double y, double z) {
      return lightAt(level, x, y + 0.2, z);
   }

   public static float snapStep(float f, float snap) {
      float k = Mth.clamp((f - 0.5F) * snap + 0.5F, 0.0F, 1.0F);
      return k * k * (3.0F - 2.0F * k);
   }

   public static float slideFlash(float step) {
      return step * (1.0F - step) * 4.0F;
   }

   public static float flowOffset(float time, float speed, float gap, float phase) {
      float off = time * speed + phase;
      return off - (float)Mth.floor(off / gap) * gap;
   }

   public static float yawOutward(float a) {
      return (float)Math.toDegrees(Math.atan2((double)(-Mth.cos(a)), (double)Mth.sin(a)));
   }

   public static void cube(Pose pose, VertexConsumer vc, WaterMesh.Paint water, WaterMesh.Paint foam, float cx, float cy, float cz, float hs) {
      WaterMesh.boxCentered(pose, vc, water, cx, cy, cz, hs, hs, hs);
      if (foam != null) {
         WaterFramePass.foam(pose, vc, (fp, fvc) -> WaterMesh.boxCentered(fp, fvc, foam, cx, cy, cz, hs, hs, hs));
      }
   }

   public static void tippedCube(
      PoseStack ps, VertexConsumer vc, WaterMesh.Paint water, WaterMesh.Paint foamCap, float px, float py, float pz, float size, float tipDeg
   ) {
      ps.pushPose();
      ps.translate(px, py, pz);
      ps.mulPose(Axis.XP.rotationDegrees(tipDeg));
      float hs = size * 0.5F;
      WaterMesh.box(ps.last(), vc, water, -hs, 0.0F, -size, hs, size, 0.0F);
      if (foamCap != null) {
         WaterFramePass.foam(
            ps.last(), vc, (fp, fvc) -> WaterMesh.box(fp, fvc, foamCap, -hs + 0.01F, size - 0.02F, -size + 0.01F, hs - 0.01F, size + 0.005F, -0.01F, 1)
         );
      }

      ps.popPose();
   }

   public static void tippedCube(
      Pose pose, VertexConsumer vc, WaterMesh.Paint water, WaterMesh.Paint foamCap, float px, float py, float pz, float size, float tipDeg
   ) {
      Pose p = turned(pose, new Vector3f(px, py, pz), new Quaternionf().rotationX(tipDeg * (float) (Math.PI / 180.0)));
      float hs = size * 0.5F;
      WaterMesh.box(p, vc, water, -hs, 0.0F, -size, hs, size, 0.0F);
      if (foamCap != null) {
         WaterFramePass.foam(
            p, vc, (fp, fvc) -> WaterMesh.box(fp, fvc, foamCap, -hs + 0.01F, size - 0.02F, -size + 0.01F, hs - 0.01F, size + 0.005F, -0.01F, 1)
         );
      }
   }

   public static float lipBeat(float phase, int seed, int k) {
      float v = phase + WaterMesh.noise01(seed, k, 71);
      return v - (float)Mth.floor(v);
   }

   public static boolean lipBroke(float from, float to, int seed, int k) {
      float n = WaterMesh.noise01(seed, k, 71);
      return Mth.floor(to + n) != Mth.floor(from + n);
   }

   public static float lipSize(float cube, float beat, float grow) {
      float g = Mth.clamp(beat / grow, 0.0F, 1.0F);
      return cube * g * g * (3.0F - 2.0F * g);
   }

   public static float lipTip(float beat, float grow, float maxDeg) {
      float t = Mth.clamp((beat - grow) / (1.0F - grow), 0.0F, 1.0F);
      return maxDeg * t * t;
   }

   public static void chain(Pose pose, VertexConsumer vc, WaterMesh.Paint water, WaterMesh.Paint foam, WaterBlocks.Chain c) {
      chain(pose, vc, water, foam, c, null);
   }

   public static void chain(Pose pose, VertexConsumer vc, WaterMesh.Paint water, WaterMesh.Paint foam, WaterBlocks.Chain c, WaterBlocks.Fade fade) {
      float gap = Math.max(0.05F, c.gap());
      float len = c.length();
      if (!(len <= 0.01F)) {
         int first = Mth.floor(c.flow() / gap);
         float off = c.flow() - (float)first * gap;
         float eps = Math.min(0.02F, gap / len * 0.25F);
         Quaternionf q = new Quaternionf();
         int i = 0;

         while (true) {
            float d = off + (float)i * gap;
            if (d > len) {
               return;
            }

            int id = i - first;
            float t = d / len;
            float edge = Math.min(1.0F, Math.min(d, len - d) / gap);
            float hs = c.spine().radius(t) * edge;
            label64:
            if (!(hs < 0.01F)) {
               Vector3f p = c.spine().center(t);
               if (c.jitter() > 0.0F) {
                  p.add(
                     c.jitter() * (2.0F * WaterMesh.noise01(c.seed(), id, 11) - 1.0F),
                     c.jitter() * (2.0F * WaterMesh.noise01(c.seed(), id, 12) - 1.0F),
                     c.jitter() * (2.0F * WaterMesh.noise01(c.seed(), id, 13) - 1.0F)
                  );
               }

               WaterMesh.Paint w = water;
               WaterMesh.Paint f = foam;
               if (fade != null) {
                  float k = fade.at(p);
                  if (k <= 0.02F) {
                     break label64;
                  }

                  if (k < 1.0F) {
                     w = water.alpha(water.opacity() * k);
                     f = foam == null ? null : foam.alpha(foam.opacity() * k);
                  }
               }

               WaterMesh.Paint white = f != null && WaterMesh.noise01(c.seed(), id, 14) < c.foamShare() ? f : null;
               if (!c.align() && c.roll() == 0.0F) {
                  cube(pose, vc, w, white, p.x, p.y, p.z, hs);
               } else {
                  q.identity();
                  if (c.align()) {
                     Vector3f tan = c.spine().center(Math.min(1.0F, t + eps)).sub(c.spine().center(Math.max(0.0F, t - eps)));
                     if (tan.lengthSquared() > 1.0E-10F) {
                        q.rotationTo(0.0F, 0.0F, 1.0F, tan.x, tan.y, tan.z);
                     }
                  }

                  if (c.roll() != 0.0F) {
                     q.rotateZ((float) (Math.PI / 180.0) * c.roll() * (2.0F * WaterMesh.noise01(c.seed(), id, 15) - 1.0F));
                  }

                  cube(turned(pose, p, q), vc, w, white, 0.0F, 0.0F, 0.0F, hs);
               }
            }

            i++;
         }
      }
   }

   public static void column(Pose pose, VertexConsumer vc, WaterMesh.Paint water, WaterMesh.Paint foam, WaterBlocks.Column c, WaterMesh.Spine.Radius taper) {
      column(pose, vc, water, foam, c, taper, null);
   }

   public static void column(
      Pose pose, VertexConsumer vc, WaterMesh.Paint water, WaterMesh.Paint foam, WaterBlocks.Column c, WaterMesh.Spine.Radius taper, WaterBlocks.Bend bend
   ) {
      column(pose, vc, water, foam, c, taper, bend, null, 0.0F);
   }

   public static void column(
      Pose pose,
      VertexConsumer vc,
      WaterMesh.Paint water,
      WaterMesh.Paint foam,
      WaterBlocks.Column c,
      WaterMesh.Spine.Radius taper,
      WaterBlocks.Bend bend,
      WaterBlocks.Loosen loosen,
      float drift
   ) {
      Quaternionf q = new Quaternionf();
      Vector3f at = new Vector3f();
      columnCubes(c, taper, bend, cb -> {
         float loose = loosen == null ? 0.0F : loosen.at(cb.ring(), cb.index(), cb.mid() / c.height());
         if (!(loose >= 1.0F)) {
            float out = cb.out() + drift * loose;
            at.set(cb.axisX() + Mth.cos(cb.angle()) * out, 0.0F, cb.axisZ() + Mth.sin(cb.angle()) * out);
            q.rotationY(-cb.angle());
            Pose p = turned(pose, at, q);
            float hs = cb.hs();
            if (loose > 0.0F) {
               looseCube(p, vc, water, foam, cb.mid(), hs * (1.0F - loose), loose);
            } else {
               float yb = cb.bottom();
               float y1 = cb.top();
               WaterMesh.box(p, vc, water, -hs, yb, -hs, hs, y1, hs);
               boolean white = foam != null && (cb.crown() || WaterMesh.noise01(c.seed(), cb.ring() * 31 + cb.index(), 23) < c.foamShare());
               if (white) {
                  WaterFramePass.foam(p, vc, (fp, fvc) -> WaterMesh.box(fp, fvc, foam, -hs, yb, -hs, hs, y1, hs));
               }
            }
         }
      });
   }

   public static void columnCubes(WaterBlocks.Column c, WaterMesh.Spine.Radius taper, WaterBlocks.Bend bend, Consumer<WaterBlocks.ColumnCube> each) {
      float cube = Math.max(0.05F, c.cube());
      float h = c.height();
      if (!(h <= 0.02F)) {
         float climbed = c.time() * c.rise();
         int first = Mth.floor(climbed / cube);
         float off = climbed - (float)first * cube;
         Vector3f at = new Vector3f();
         int j = 0;

         while (true) {
            float y0 = off + (float)(j - 1) * cube;
            if (y0 >= h) {
               return;
            }

            float y1 = Math.min(h, y0 + cube);
            float yb = Math.max(0.0F, y0);
            if (!(y1 - yb < 0.02F)) {
               int id = j - first;
               float mid = (yb + y1) * 0.5F;
               float r = c.radius() * (taper == null ? 1.0F : Math.max(0.0F, taper.at(mid / h)));
               boolean top = y1 >= h - 0.001F;
               float bx = 0.0F;
               float bz = 0.0F;
               if (bend != null) {
                  bend.offset(mid, at.zero());
                  bx = at.x;
                  bz = at.z;
               }

               int n = r < cube * 0.5F ? 1 : Math.max(3, Math.round((float) (Math.PI * 2) * r / cube));
               float spin = c.time() * c.churn() * ((id & 1) == 0 ? 1.0F : -1.0F) + (float) (Math.PI * 2) * WaterMesh.noise01(c.seed(), id, 21);

               for (int i = 0; i < n; i++) {
                  float grow = 0.85F + 0.3F * WaterMesh.noise01(c.seed(), id * 31 + i, 22);
                  each.accept(
                     new WaterBlocks.ColumnCube(
                        id, i, spin + (float) (Math.PI * 2) * (float)i / (float)n, n == 1 ? 0.0F : r, bx, bz, yb, y1, cube * 0.5F * grow, top
                     )
                  );
               }
            }

            j++;
         }
      }
   }

   private static void looseCube(Pose p, VertexConsumer vc, WaterMesh.Paint water, WaterMesh.Paint foam, float y, float hs, float loose) {
      if (!(hs < 0.01F)) {
         WaterMesh.Paint w = water.alpha(water.opacity() * Math.min(1.0F, 2.0F * (1.0F - loose)));
         WaterMesh.box(p, vc, w, -hs, y - hs, -hs, hs, y + hs, hs);
         float flash = 1.0F - loose / 0.4F;
         if (foam != null && flash > 0.0F) {
            WaterMesh.Paint white = foam.alpha(foam.opacity() * flash);
            WaterFramePass.foam(p, vc, (fp, fvc) -> WaterMesh.box(fp, fvc, white, -hs, y - hs, -hs, hs, y + hs, hs));
         }
      }
   }

   public static void ring(
      Pose pose,
      VertexConsumer vc,
      WaterMesh.Paint water,
      WaterMesh.Paint foam,
      float radius,
      float cube,
      float height,
      float bob,
      float foamShare,
      float time,
      int seed
   ) {
      if (!(radius <= 0.01F) && !(height <= 0.02F)) {
         int n = Math.max(6, Math.round((float) (Math.PI * 2) * radius / cube));
         float hs = cube * 0.5F;
         Quaternionf q = new Quaternionf();
         Vector3f at = new Vector3f();

         for (int i = 0; i < n; i++) {
            float a = (float) (Math.PI * 2) * ((float)i + 0.3F * WaterMesh.noise01(seed, i, 31)) / (float)n;
            float hi = height * (0.6F + 0.4F * WaterMesh.noise01(seed, i, 32))
               + bob * Mth.sin(time * 0.3F + (float) (Math.PI * 2) * WaterMesh.noise01(seed, i, 33));
            if (!(hi <= 0.02F)) {
               at.set(Mth.cos(a) * radius, 0.0F, Mth.sin(a) * radius);
               q.rotationY(-a);
               Pose p = turned(pose, at, q);
               WaterMesh.box(p, vc, water, -hs, 0.0F, -hs, hs, hi, hs);
               if (foam != null && WaterMesh.noise01(seed, i, 34) < foamShare) {
                  WaterFramePass.foam(
                     p, vc, (fp, fvc) -> WaterMesh.box(fp, fvc, foam, -hs + 0.01F, hi - 0.02F, -hs + 0.01F, hs - 0.01F, hi + 0.005F, hs - 0.01F, 1)
                  );
               }
            }
         }
      }
   }

   public static WaterCubeGrid puddle(float radius, float cube, float depth, float ragged, int seed) {
      int half = Math.max(0, (int)Math.ceil((double)(radius / cube)));
      WaterCubeGrid g = WaterCubeGrid.centered(2 * half + 1, 2 * half + 1, cube, 0.01F);

      for (int k = 0; k < g.cols(); k++) {
         for (int r = 0; r < g.rows(); r++) {
            float x = g.x(k);
            float z = g.z(r);
            float edge = radius * (1.0F - ragged * WaterMesh.noise01(seed, k * 97 + r, 41));
            if (!(x * x + z * z > edge * edge)) {
               g.set(k, r, depth * (0.6F + 0.4F * WaterMesh.noise01(seed, k * 97 + r, 42)));
            }
         }
      }

      return g;
   }

   private static Pose turned(Pose pose, Vector3f at, Quaternionf q) {
      Pose p = pose.copy();
      p.pose().translate(at.x, at.y, at.z).rotate(q);
      p.normal().rotate(q);
      return p;
   }

   @FunctionalInterface
   public interface Bend {
      void offset(float var1, Vector3f var2);
   }

   public static record Chain(WaterMesh.Spine spine, float length, float gap, float flow, float jitter, float roll, float foamShare, boolean align, int seed) {
      public static WaterBlocks.Chain along(WaterMesh.Spine spine, float length, float gap) {
         return new WaterBlocks.Chain(spine, length, gap, 0.0F, 0.0F, 0.0F, 0.0F, true, 0);
      }

      public WaterBlocks.Chain flowing(float flow) {
         return new WaterBlocks.Chain(this.spine, this.length, this.gap, flow, this.jitter, this.roll, this.foamShare, this.align, this.seed);
      }

      public WaterBlocks.Chain rough(float jitter, float roll) {
         return new WaterBlocks.Chain(this.spine, this.length, this.gap, this.flow, jitter, roll, this.foamShare, this.align, this.seed);
      }

      public WaterBlocks.Chain foamed(float share) {
         return new WaterBlocks.Chain(this.spine, this.length, this.gap, this.flow, this.jitter, this.roll, share, this.align, this.seed);
      }

      public WaterBlocks.Chain aligned(boolean on) {
         return new WaterBlocks.Chain(this.spine, this.length, this.gap, this.flow, this.jitter, this.roll, this.foamShare, on, this.seed);
      }

      public WaterBlocks.Chain seeded(int s) {
         return new WaterBlocks.Chain(this.spine, this.length, this.gap, this.flow, this.jitter, this.roll, this.foamShare, this.align, s);
      }
   }

   public static record Column(float radius, float height, float cube, float rise, float churn, float foamShare, float time, int seed) {
   }

   public static record ColumnCube(int ring, int index, float angle, float out, float axisX, float axisZ, float bottom, float top, float hs, boolean crown) {
      public float mid() {
         return (this.bottom + this.top) * 0.5F;
      }

      public Vector3f center() {
         return new Vector3f(this.axisX + Mth.cos(this.angle) * this.out, this.mid(), this.axisZ + Mth.sin(this.angle) * this.out);
      }
   }

   @FunctionalInterface
   public interface Fade {
      float at(Vector3f var1);
   }

   @FunctionalInterface
   public interface Loosen {
      float at(int var1, int var2, float var3);
   }
}
