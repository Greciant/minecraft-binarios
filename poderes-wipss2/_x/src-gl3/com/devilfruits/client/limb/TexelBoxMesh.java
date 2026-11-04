package com.devilfruits.client.limb;

import com.devilfruits.core.limb.LimbBands;
import com.devilfruits.core.limb.LimbPath;
import com.devilfruits.core.limb.LimbUv;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class TexelBoxMesh {
   private static final int MAX_RINGS = 2048;
   private final float[] ringS = new float[2048];
   private final float[] centre = new float[6144];
   private final float[] across = new float[6144];
   private final float[] depth = new float[6144];
   private final float[] along = new float[6144];
   private final float[] scale = new float[2048];
   private final int[] light = new int[2048];
   private final int[] segFirst = new int[2048];
   private final int[] segLast = new int[2048];
   private float[] segments = new float[0];
   private int segCount;
   private int rings;
   private float length;
   private final Vector3f pos = new Vector3f();
   private final Vector3f tan = new Vector3f();
   private final Vector3f prevPos = new Vector3f();
   private final Vector3f prevTan = new Vector3f();
   private final Vector3f frameN = new Vector3f();
   private final Vector3f nextN = new Vector3f();
   private final Vector3f n = new Vector3f();
   private final Vector3f b = new Vector3f();
   private final Vector3f out = new Vector3f();
   private final Vector3f normal = new Vector3f();
   private final Vector3f normalOut = new Vector3f();
   private final Vector3f tilted = new Vector3f();
   private final float[] k = new float[1];
   public float vShift;
   private final float[] cap = new float[16];

   public int rings() {
      return this.rings;
   }

   public void sweep(LimbPath path, float[] segs, int count, float step, Vector3fc n0, LimbSpec spec, int light0, int light1, int light2) {
      this.segments = segs;
      this.segCount = count;
      this.rings = 0;
      this.length = LimbBands.length(segs, count);
      float handStart = Math.max(0.0F, this.length - 4.0F * spec.fist);
      int need = 1;

      for (int i = 0; i < count; i++) {
         need += Math.max(1, Mth.ceil((segs[i * 4 + 1] - segs[i * 4]) / step));
      }

      if (need > 2048) {
         step *= (float)need / (float)(2048 - count - 1) + 0.01F;
      }

      for (int i = 0; i < count; i++) {
         float s0 = segs[i * 4];
         float s1 = segs[i * 4 + 1];
         int sub = Math.max(1, Mth.ceil((s1 - s0) / step - 0.001F));
         if (i == 0) {
            this.ring(path, s0, n0, spec, handStart, light0, light1, light2);
         }

         this.segFirst[i] = this.rings - 1;

         for (int j = 1; j <= sub && this.rings < 2048; j++) {
            this.ring(path, s0 + (s1 - s0) * (float)j / (float)sub, n0, spec, handStart, light0, light1, light2);
         }

         this.segLast[i] = this.rings - 1;
      }
   }

   private void ring(LimbPath path, float s, Vector3fc n0, LimbSpec spec, float handStart, int l0, int l1, int l2) {
      path.sample(s, this.pos, this.tan);
      if (this.rings == 0) {
         this.frameN.set(n0).fma(-n0.dot(this.tan), this.tan);
         if (this.frameN.lengthSquared() < 1.0E-8F) {
            this.frameN.set(Math.abs(this.tan.x) < 0.9F ? 1.0F : 0.0F, Math.abs(this.tan.x) < 0.9F ? 0.0F : 1.0F, 0.0F);
            this.frameN.fma(-this.frameN.dot(this.tan), this.tan);
         }

         this.frameN.normalize();
      } else {
         LimbPath.transport(this.prevPos, this.prevTan, this.frameN, this.pos, this.tan, this.nextN);
         this.frameN.set(this.nextN);
      }

      this.prevPos.set(this.pos);
      this.prevTan.set(this.tan);
      float f = this.length > 0.0F ? s / this.length : 0.0F;
      float roll = f * (spec.twistTurns * (float) (Math.PI * 2) + spec.spin);
      this.frameN.cross(this.tan, this.b);
      float c = Mth.cos(roll);
      float sn = Mth.sin(roll);
      this.n.set(this.frameN).mul(c).fma(sn, this.b);
      this.b.mul(c).fma(-sn, this.frameN);
      this.k[0] = this.thickness(s, spec, handStart);
      if (spec.shaper != null) {
         spec.shaper.shape(s, this.length, this.pos, this.n, this.b, this.k);
      }

      int r = this.rings++;
      this.ringS[r] = s;
      put(this.centre, r, this.pos);
      put(this.across, r, this.n);
      put(this.depth, r, this.b);
      put(this.along, r, this.tan);
      this.scale[r] = this.k[0];
      this.light[r] = f < 0.5F ? mixLight(l0, l1, f * 2.0F) : mixLight(l1, l2, f * 2.0F - 1.0F);
   }

   private float thickness(float s, LimbSpec spec, float handStart) {
      if (s >= handStart) {
         float swell = Mth.clamp((s - handStart) / Math.max(spec.fist, 0.001F), 0.0F, 1.0F);
         return Mth.lerp(swell, 1.0F, spec.fist);
      } else {
         float mid0 = 4.0F;
         if (!(spec.tension <= 0.0F) && !(handStart <= mid0)) {
            float ramp = Math.min(4.0F, (handStart - mid0) * 0.5F);
            float w = smooth((s - mid0) / ramp) * (1.0F - smooth((s - (handStart - ramp)) / ramp));
            return Mth.lerp(w, 1.0F, Mth.lerp(Mth.clamp(spec.tension, 0.0F, 1.0F), 1.0F, spec.thinnest));
         } else {
            return 1.0F;
         }
      }
   }

   private static float smooth(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return x * x * (3.0F - 2.0F * x);
   }

   private static int mixLight(int a, int b, float t) {
      int lo = Math.round(Mth.lerp(t, (float)(a & 65535), (float)(b & 65535)));
      int hi = Math.round(Mth.lerp(t, (float)(a >>> 16), (float)(b >>> 16)));
      return lo | hi << 16;
   }

   private static void put(float[] dest, int i, Vector3fc v) {
      dest[i * 3] = v.x();
      dest[i * 3 + 1] = v.y();
      dest[i * 3 + 2] = v.z();
   }

   public void write(Pose pose, VertexConsumer vc, LimbUv.Layout uv, int overlay, int color, @Nullable TexelBoxMesh.VertexShaper shaper) {
      this.write(pose, vc, uv, overlay, color, shaper, false);
   }

   public void write(Pose pose, VertexConsumer vc, LimbUv.Layout uv, int overlay, int color, @Nullable TexelBoxMesh.VertexShaper shaper, boolean perTexel) {
      if (this.rings >= 2) {
         float hw = (float)uv.width() * 0.5F + uv.grow();
         float hd = (float)uv.depth() * 0.5F + uv.grow();
         float mx = uv.mirror() ? -1.0F : 1.0F;
         float top = uv.sideTop();

         for (int seg = 0; seg < this.segCount; seg++) {
            float s0 = this.segments[seg * 4];
            float s1 = this.segments[seg * 4 + 1];
            float r0 = this.segments[seg * 4 + 2];
            float r1 = this.segments[seg * 4 + 3];
            float rate = s1 > s0 ? (r1 - r0) / (s1 - s0) : 0.0F;

            for (int a = this.segFirst[seg]; a < this.segLast[seg]; a++) {
               int bIdx = a + 1;
               float va = uv.v(top + r0 + (this.ringS[a] - s0) * rate + this.vShift);
               float vb = uv.v(top + r0 + (this.ringS[bIdx] - s0) * rate + this.vShift);

               for (int face = 0; face < 4; face++) {
                  float u0 = uv.u(uv.sideU(face, 0));
                  float u1 = uv.u(uv.sideU(face, 1));
                  int x0 = LimbUv.Layout.cornerX(face, 0);
                  int z0 = LimbUv.Layout.cornerZ(face, 0);
                  int x1 = LimbUv.Layout.cornerX(face, 1);
                  int z1 = LimbUv.Layout.cornerZ(face, 1);
                  int columns = !perTexel ? 1 : Math.max(1, face != 0 && face != 2 ? uv.width() : uv.depth());

                  for (int c = 0; c < columns; c++) {
                     float t0 = (float)c / (float)columns;
                     float t1 = (float)(c + 1) / (float)columns;
                     float xa = Mth.lerp(t0, (float)x0, (float)x1) * mx * hw;
                     float za = Mth.lerp(t0, (float)z0, (float)z1) * hd;
                     float ua = Mth.lerp(t0, u0, u1);
                     float xb = Mth.lerp(t1, (float)x0, (float)x1) * mx * hw;
                     float zb = Mth.lerp(t1, (float)z0, (float)z1) * hd;
                     float ub = Mth.lerp(t1, u0, u1);
                     this.faceNormal(face, a, mx);
                     this.vertex(pose, vc, a, xa, za, ua, va, overlay, color, uv.grow(), face, shaper);
                     this.vertex(pose, vc, a, xb, zb, ub, va, overlay, color, uv.grow(), face, shaper);
                     this.faceNormal(face, bIdx, mx);
                     this.vertex(pose, vc, bIdx, xb, zb, ub, vb, overlay, color, uv.grow(), face, shaper);
                     this.vertex(pose, vc, bIdx, xa, za, ua, vb, overlay, color, uv.grow(), face, shaper);
                  }
               }
            }
         }

         this.capFace(pose, vc, uv, true, 0, hw, hd, mx, overlay, color, shaper);
         this.capFace(pose, vc, uv, false, this.rings - 1, hw, hd, mx, overlay, color, shaper);
      }
   }

   private void capFace(
      Pose pose,
      VertexConsumer vc,
      LimbUv.Layout uv,
      boolean topCap,
      int r,
      float hw,
      float hd,
      float mx,
      int overlay,
      int color,
      @Nullable TexelBoxMesh.VertexShaper shaper
   ) {
      uv.cap(topCap, this.cap);
      float sign = topCap ? -1.0F : 1.0F;
      this.normal.set(this.along[r * 3], this.along[r * 3 + 1], this.along[r * 3 + 2]).mul(sign);

      for (int i = 0; i < 4; i++) {
         this.vertex(
            pose,
            vc,
            r,
            this.cap[i * 4] * mx * hw,
            this.cap[i * 4 + 1] * hd,
            uv.u(this.cap[i * 4 + 2]),
            uv.v(this.cap[i * 4 + 3]),
            overlay,
            color,
            uv.grow(),
            topCap ? 4 : 5,
            shaper
         );
      }
   }

   private void faceNormal(int face, int r, float mx) {
      float[] axis = face != 0 && face != 2 ? this.depth : this.across;
      float sign = face != 0 && face != 1 ? 1.0F : -1.0F;
      if (face == 0 || face == 2) {
         sign *= mx;
      }

      this.normal.set(axis[r * 3], axis[r * 3 + 1], axis[r * 3 + 2]).mul(sign);
   }

   private void vertex(
      Pose pose,
      VertexConsumer vc,
      int r,
      float x,
      float z,
      float u,
      float v,
      int overlay,
      int color,
      float grow,
      int face,
      @Nullable TexelBoxMesh.VertexShaper shaper
   ) {
      int i = r * 3;
      float k = this.scale[r];
      float ext = r == 0 ? -grow : (r == this.rings - 1 ? grow : 0.0F);
      this.out
         .set(
            this.centre[i] + (this.across[i] * x + this.depth[i] * z) * k + this.along[i] * ext,
            this.centre[i + 1] + (this.across[i + 1] * x + this.depth[i + 1] * z) * k + this.along[i + 1] * ext,
            this.centre[i + 2] + (this.across[i + 2] * x + this.depth[i + 2] * z) * k + this.along[i + 2] * ext
         );
      int c = color;
      Vector3f n = this.normal;
      if (shaper != null) {
         shaper.shape(face, this.ringS[r], this.out);
         n = this.tilted.set(this.normal);
         float bright = shaper.light(this.tilted);
         if (bright != 1.0F) {
            c = color & 0xFF000000
               | (int)((float)(color >> 16 & 0xFF) * bright) << 16
               | (int)((float)(color >> 8 & 0xFF) * bright) << 8
               | (int)((float)(color & 0xFF) * bright);
         }
      }

      Matrix4f m = pose.pose();
      float px = m.m00() * this.out.x + m.m10() * this.out.y + m.m20() * this.out.z + m.m30();
      float py = m.m01() * this.out.x + m.m11() * this.out.y + m.m21() * this.out.z + m.m31();
      float pz = m.m02() * this.out.x + m.m12() * this.out.y + m.m22() * this.out.z + m.m32();
      Vector3f nn = pose.transformNormal(n.x, n.y, n.z, this.normalOut);
      vc.addVertex(px, py, pz, c, u, v, overlay, this.light[r], nn.x, nn.y, nn.z);
   }

   @FunctionalInterface
   public interface RingShaper {
      void shape(float var1, float var2, Vector3f var3, Vector3f var4, Vector3f var5, float[] var6);
   }

   @FunctionalInterface
   public interface VertexShaper {
      void shape(int var1, float var2, Vector3f var3);

      default float light(Vector3f normal) {
         return 1.0F;
      }
   }
}
