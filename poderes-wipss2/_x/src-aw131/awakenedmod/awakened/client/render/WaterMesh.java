package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class WaterMesh {
   public static final int FOAM_RGB = 15004159;
   public static final int FULL_BRIGHT = 15728880;
   public static final int FOAM_MIN_LIGHT = 10485920;
   private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
   private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
   private static final float FLOW_PER_BLOCK = 0.5F;
   public static final int FACE_UP = 1;
   public static final int FACE_DOWN = 2;
   public static final int FACE_POS_Z = 4;
   public static final int FACE_NEG_Z = 8;
   public static final int FACE_POS_X = 16;
   public static final int FACE_NEG_X = 32;
   public static final int ALL_FACES = 63;

   private WaterMesh() {
   }

   public static int liftLight(int packed, int min) {
      int block = Math.max(packed & 65535, min & 65535);
      int sky = Math.max(packed >>> 16, min >>> 16);
      return sky << 16 | block;
   }

   public static int argb(int rgb, float alpha) {
      int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
      return a << 24 | rgb & 16777215;
   }

   public static int mix(int a, int b, float t) {
      float k = Mth.clamp(t, 0.0F, 1.0F);
      int r = Math.round(Mth.lerp(k, (float)(a >> 16 & 0xFF), (float)(b >> 16 & 0xFF)));
      int g = Math.round(Mth.lerp(k, (float)(a >> 8 & 0xFF), (float)(b >> 8 & 0xFF)));
      int bl = Math.round(Mth.lerp(k, (float)(a & 0xFF), (float)(b & 0xFF)));
      return r << 16 | g << 8 | bl;
   }

   public static float noise01(int seed, int i, int salt) {
      int h = seed * 668265261 ^ i * 374761393 ^ salt * 1640531527;
      h ^= h >>> 15;
      h *= 739982445;
      h ^= h >>> 12;
      h *= 695872825;
      h ^= h >>> 15;
      return (float)(h & 16777215) / 1.6777216E7F;
   }

   static void submit(PoseStack poseStack, VfxDraw.Sink sink, WaterMesh.Shape shape) {
      VfxDraw.vanillaLayer(poseStack, sink, ModRenderTypes.WATER_TRANSLUCENT, shape::build);
   }

   public static void submit(PoseStack poseStack, MultiBufferSource buffers, WaterMesh.Shape shape) {
      shape.build(poseStack.last(), buffers.getBuffer(ModRenderTypes.WATER_TRANSLUCENT));
   }

   public static void box(Pose pose, VertexConsumer vc, WaterMesh.Paint paint, float x0, float y0, float z0, float x1, float y1, float z1) {
      box(pose, vc, paint, x0, y0, z0, x1, y1, z1, 63);
   }

   public static void box(Pose pose, VertexConsumer vc, WaterMesh.Paint paint, float x0, float y0, float z0, float x1, float y1, float z1, int faces) {
      if ((faces & 1) != 0) {
         face(pose, vc, paint, paint.still(), 1.0F, x0, y1, z0, x1 - x0, 0.0F, 0.0F, 0.0F, 0.0F, z1 - z0, 0.0F, 1.0F, 0.0F);
      }

      if ((faces & 2) != 0) {
         face(pose, vc, paint, paint.still(), 1.0F, x0, y0, z1, x1 - x0, 0.0F, 0.0F, 0.0F, 0.0F, z0 - z1, 0.0F, -1.0F, 0.0F);
      }

      if ((faces & 4) != 0) {
         face(pose, vc, paint, paint.flow(), 0.5F, x0, y1, z1, x1 - x0, 0.0F, 0.0F, 0.0F, y0 - y1, 0.0F, 0.0F, 0.0F, 1.0F);
      }

      if ((faces & 8) != 0) {
         face(pose, vc, paint, paint.flow(), 0.5F, x1, y1, z0, x0 - x1, 0.0F, 0.0F, 0.0F, y0 - y1, 0.0F, 0.0F, 0.0F, -1.0F);
      }

      if ((faces & 16) != 0) {
         face(pose, vc, paint, paint.flow(), 0.5F, x1, y1, z1, 0.0F, 0.0F, z0 - z1, 0.0F, y0 - y1, 0.0F, 1.0F, 0.0F, 0.0F);
      }

      if ((faces & 32) != 0) {
         face(pose, vc, paint, paint.flow(), 0.5F, x0, y1, z0, 0.0F, 0.0F, z1 - z0, 0.0F, y0 - y1, 0.0F, -1.0F, 0.0F, 0.0F);
      }
   }

   public static void boxCentered(Pose pose, VertexConsumer vc, WaterMesh.Paint paint, float cx, float cy, float cz, float hx, float hy, float hz) {
      box(pose, vc, paint, cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz);
   }

   public static void tube(Pose pose, VertexConsumer vc, WaterMesh.Paint paint, WaterMesh.Spine spine, int stations, int sides) {
      tube(pose, vc, paint, spine, stations, sides, 0.0F);
   }

   public static void tube(Pose pose, VertexConsumer vc, WaterMesh.Paint paint, WaterMesh.Spine spine, int stations, int sides, float vScroll) {
      tubeArc(pose, vc, paint, spine, stations, sides, 0.0F, 1.0F, vScroll);
   }

   public static void tubeArc(Pose pose, VertexConsumer vc, WaterMesh.Paint paint, WaterMesh.Spine spine, int stations, int sides, float from, float to) {
      tubeArc(pose, vc, paint, spine, stations, sides, from, to, 0.0F);
   }

   public static void tubeArc(
      Pose pose, VertexConsumer vc, WaterMesh.Paint paint, WaterMesh.Spine spine, int stations, int sides, float from, float to, float vScroll
   ) {
      int n = Math.max(2, stations);
      int m = Math.max(3, sides);
      Vector3f[] centres = new Vector3f[n];
      float[] radii = new float[n];

      for (int i = 0; i < n; i++) {
         float t = (float)i / (float)(n - 1);
         centres[i] = spine.center(t);
         radii[i] = Math.max(0.0F, spine.radius(t));
      }

      Vector3f[] normals = new Vector3f[n];
      Vector3f[] binormals = new Vector3f[n];
      Vector3f prevNormal = null;

      for (int i = 0; i < n; i++) {
         Vector3f tan = new Vector3f(centres[Math.min(n - 1, i + 1)]).sub(centres[Math.max(0, i - 1)]);
         if (tan.lengthSquared() < 1.0E-10F) {
            tan.set(0.0F, 0.0F, 1.0F);
         }

         tan.normalize();
         Vector3f nrm;
         if (prevNormal == null) {
            Vector3f ref = Math.abs(tan.y) < 0.9F ? new Vector3f(0.0F, 1.0F, 0.0F) : new Vector3f(1.0F, 0.0F, 0.0F);
            nrm = new Vector3f(ref).cross(tan).normalize();
         } else {
            nrm = new Vector3f(prevNormal).sub(new Vector3f(tan).mul(prevNormal.dot(tan)));
            if (nrm.lengthSquared() < 1.0E-10F) {
               nrm = new Vector3f(prevNormal);
            }

            nrm.normalize();
         }

         normals[i] = nrm;
         binormals[i] = new Vector3f(tan).cross(nrm).normalize();
         prevNormal = nrm;
      }

      float[] along = new float[n];
      along[0] = vScroll - (float)Math.floor((double)vScroll);

      for (int i = 1; i < n; i++) {
         along[i] = along[i - 1] + centres[i].distance(centres[i - 1]);
      }

      TextureAtlasSprite sprite = paint.flow();
      Vector3f a = new Vector3f();
      Vector3f b = new Vector3f();
      Vector3f c = new Vector3f();
      Vector3f d = new Vector3f();
      Vector3f na = new Vector3f();
      Vector3f nb = new Vector3f();
      Vector3f nc = new Vector3f();
      Vector3f nd = new Vector3f();
      float[] uu = new float[2];
      float[] vv = new float[2];

      for (int i = 0; i < n - 1; i++) {
         if (!(radii[i] <= 0.0F) || !(radii[i + 1] <= 0.0F)) {
            float circ = (float)(Math.PI * (double)(radii[i] + radii[i + 1]));
            span(along[i] * 0.5F, along[i + 1] * 0.5F, vv);

            for (int j = 0; j < m; j++) {
               float f0 = (float)j / (float)m;
               float f1 = (float)(j + 1) / (float)m;
               float t0 = Mth.lerp(f0, from, to);
               float t1 = Mth.lerp(f1, from, to);
               span((t0 - from) * circ * 0.5F, (t1 - from) * circ * 0.5F, uu);
               ringPoint(centres[i], normals[i], binormals[i], radii[i], t0, a, na);
               ringPoint(centres[i], normals[i], binormals[i], radii[i], t1, b, nb);
               ringPoint(centres[i + 1], normals[i + 1], binormals[i + 1], radii[i + 1], t1, c, nc);
               ringPoint(centres[i + 1], normals[i + 1], binormals[i + 1], radii[i + 1], t0, d, nd);
               vertex(pose, vc, paint, a, sprite, uu[0], vv[0], na);
               vertex(pose, vc, paint, b, sprite, uu[1], vv[0], nb);
               vertex(pose, vc, paint, c, sprite, uu[1], vv[1], nc);
               vertex(pose, vc, paint, d, sprite, uu[0], vv[1], nd);
            }
         }
      }
   }

   public static void prism(Pose pose, VertexConsumer vc, WaterMesh.Paint paint, Vector3f from, Vector3f to, float r0, float r1, int sides) {
      int stations = Math.max(2, 1 + (int)Math.ceil((double)from.distance(to)));
      tube(pose, vc, paint, WaterMesh.Spine.line(from, to, t -> Mth.lerp(t, r0, r1)), stations, sides);
   }

   public static void column(Pose pose, VertexConsumer vc, WaterMesh.Paint paint, float y0, float y1, float r0, float r1, int sides) {
      prism(pose, vc, paint, new Vector3f(0.0F, y0, 0.0F), new Vector3f(0.0F, y1, 0.0F), r0, r1, sides);
   }

   public static void torus(Pose pose, VertexConsumer vc, WaterMesh.Paint paint, final float ringR, final float tubeR, int segs, int sides) {
      int m = Math.max(3, segs);
      tube(pose, vc, paint, new WaterMesh.Spine() {
         @Override
         public Vector3f center(float t) {
            float a = t * (float) (Math.PI * 2);
            return new Vector3f(Mth.cos(a) * ringR, 0.0F, Mth.sin(a) * ringR);
         }

         @Override
         public float radius(float t) {
            return tubeR;
         }
      }, m + 1, sides);
   }

   public static void blob(Pose pose, VertexConsumer vc, WaterMesh.Paint paint, float cx, float cy, float cz, float r, float stretchY) {
      int rings = 4;
      int segs = 6;
      TextureAtlasSprite sprite = paint.still();
      Vector3f p0 = new Vector3f();
      Vector3f p1 = new Vector3f();
      Vector3f p2 = new Vector3f();
      Vector3f p3 = new Vector3f();
      Vector3f n0 = new Vector3f();
      Vector3f n1 = new Vector3f();
      Vector3f n2 = new Vector3f();
      Vector3f n3 = new Vector3f();
      float uvSpan = Math.min(1.0F, r * 4.0F);

      for (int i = 0; i < 4; i++) {
         float th0 = (float) Math.PI * (float)i / 4.0F;
         float th1 = (float) Math.PI * (float)(i + 1) / 4.0F;

         for (int j = 0; j < 6; j++) {
            float ph0 = (float) (Math.PI * 2) * (float)j / 6.0F;
            float ph1 = (float) (Math.PI * 2) * (float)(j + 1) / 6.0F;
            spherePoint(th0, ph0, n0);
            spherePoint(th1, ph0, n1);
            spherePoint(th1, ph1, n2);
            spherePoint(th0, ph1, n3);
            p0.set(cx + n0.x * r, cy + n0.y * r * stretchY, cz + n0.z * r);
            p1.set(cx + n1.x * r, cy + n1.y * r * stretchY, cz + n1.z * r);
            p2.set(cx + n2.x * r, cy + n2.y * r * stretchY, cz + n2.z * r);
            p3.set(cx + n3.x * r, cy + n3.y * r * stretchY, cz + n3.z * r);
            float u0 = uvSpan * (float)j / 6.0F;
            float u1 = uvSpan * (float)(j + 1) / 6.0F;
            float v0 = uvSpan * (float)i / 4.0F;
            float v1 = uvSpan * (float)(i + 1) / 4.0F;
            vertex(pose, vc, paint, p0, sprite, u0, v0, n0);
            vertex(pose, vc, paint, p1, sprite, u0, v1, n1);
            vertex(pose, vc, paint, p2, sprite, u1, v1, n2);
            vertex(pose, vc, paint, p3, sprite, u1, v0, n3);
         }
      }
   }

   public static void blobAlong(Pose pose, VertexConsumer vc, WaterMesh.Paint paint, Vector3f c, Vector3f dir, float r, float stretch) {
      Pose turned = pose.copy();
      Quaternionf rot = dir.lengthSquared() < 1.0E-10F ? new Quaternionf() : new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, dir.x, dir.y, dir.z);
      turned.pose().translate(c.x, c.y, c.z).rotate(rot);
      turned.normal().rotate(rot);
      blob(turned, vc, paint, 0.0F, 0.0F, 0.0F, r, stretch);
   }

   public static void sheet(
      Pose pose, VertexConsumer vc, WaterMesh.Paint paint, float x0, float z0, float x1, float z1, int nx, int nz, WaterMesh.Height height
   ) {
      int cx = Math.max(1, nx);
      int cz = Math.max(1, nz);
      TextureAtlasSprite sprite = paint.still();
      float dx = (x1 - x0) / (float)cx;
      float dz = (z1 - z0) / (float)cz;
      Vector3f[] p = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
      Vector3f[] nr = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
      float[] uu = new float[2];
      float[] vv = new float[2];

      for (int i = 0; i < cx; i++) {
         float xa = x0 + (float)i * dx;
         float xb = xa + dx;
         span(xa - x0, xb - x0, uu);

         for (int k = 0; k < cz; k++) {
            float za = z0 + (float)k * dz;
            float zb = za + dz;
            span(za - z0, zb - z0, vv);
            sheetPoint(height, xa, za, dx, dz, p[0], nr[0]);
            sheetPoint(height, xa, zb, dx, dz, p[1], nr[1]);
            sheetPoint(height, xb, zb, dx, dz, p[2], nr[2]);
            sheetPoint(height, xb, za, dx, dz, p[3], nr[3]);
            vertex(pose, vc, paint, p[0], sprite, uu[0], vv[0], nr[0]);
            vertex(pose, vc, paint, p[1], sprite, uu[0], vv[1], nr[1]);
            vertex(pose, vc, paint, p[2], sprite, uu[1], vv[1], nr[2]);
            vertex(pose, vc, paint, p[3], sprite, uu[1], vv[0], nr[3]);
         }
      }
   }

   public static void disc(Pose pose, VertexConsumer vc, WaterMesh.Paint paint, float r, int rings, int segs, WaterMesh.Height height) {
      annulus(pose, vc, paint, 0.0F, r, rings, segs, height);
   }

   public static void annulus(Pose pose, VertexConsumer vc, WaterMesh.Paint paint, float rIn, float r, int rings, int segs, WaterMesh.Height height) {
      int nr = Math.max(1, rings);
      int ns = Math.max(3, segs);
      TextureAtlasSprite sprite = paint.still();
      float inner = Mth.clamp(rIn, 0.0F, r);
      float step = (r - inner) / (float)nr;
      Vector3f[] p = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
      Vector3f[] nrm = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
      float[] xs = new float[4];
      float[] zs = new float[4];

      for (int i = 0; i < nr; i++) {
         float ra = inner + (float)i * step;
         float rb = inner + (float)(i + 1) * step;

         for (int j = 0; j < ns; j++) {
            float a0 = (float) (Math.PI * 2) * (float)j / (float)ns;
            float a1 = (float) (Math.PI * 2) * (float)(j + 1) / (float)ns;
            xs[0] = Mth.cos(a0) * ra;
            zs[0] = Mth.sin(a0) * ra;
            xs[1] = Mth.cos(a0) * rb;
            zs[1] = Mth.sin(a0) * rb;
            xs[2] = Mth.cos(a1) * rb;
            zs[2] = Mth.sin(a1) * rb;
            xs[3] = Mth.cos(a1) * ra;
            zs[3] = Mth.sin(a1) * ra;
            float minX = Math.min(Math.min(xs[0], xs[1]), Math.min(xs[2], xs[3]));
            float minZ = Math.min(Math.min(zs[0], zs[1]), Math.min(zs[2], zs[3]));
            float ou = (float)Math.floor((double)(minX + r));
            float ov = (float)Math.floor((double)(minZ + r));

            for (int k = 0; k < 4; k++) {
               sheetPoint(height, xs[k], zs[k], step, step, p[k], nrm[k]);
            }

            for (int k = 0; k < 4; k++) {
               float u = Mth.clamp(xs[k] + r - ou, 0.0F, 1.0F);
               float v = Mth.clamp(zs[k] + r - ov, 0.0F, 1.0F);
               vertex(pose, vc, paint, p[k], sprite, u, v, nrm[k]);
            }
         }
      }
   }

   static void span(float a, float b, float[] out) {
      float base = (float)Math.floor((double)a);
      float s = a - base;
      float e = b - base;
      if (e > 1.0001F) {
         s = 0.0F;
         e = Math.min(1.0F, b - a);
      }

      out[0] = s;
      out[1] = e;
   }

   private static float spanStart(float a, float b) {
      float base = (float)Math.floor((double)a);
      return b - base > 1.0001F ? 0.0F : a - base;
   }

   private static float spanEnd(float a, float b) {
      float base = (float)Math.floor((double)a);
      float e = b - base;
      return e > 1.0001F ? Math.min(1.0F, b - a) : e;
   }

   private static void face(
      Pose pose,
      VertexConsumer vc,
      WaterMesh.Paint paint,
      TextureAtlasSprite sprite,
      float perBlock,
      float ox,
      float oy,
      float oz,
      float eux,
      float euy,
      float euz,
      float evx,
      float evy,
      float evz,
      float nx,
      float ny,
      float nz
   ) {
      float lu = Mth.sqrt(eux * eux + euy * euy + euz * euz);
      float lv = Mth.sqrt(evx * evx + evy * evy + evz * evz);
      if (!(lu < 1.0E-5F) && !(lv < 1.0E-5F)) {
         int tu = Math.max(1, (int)Math.ceil((double)(lu - 0.001F)));
         int tv = Math.max(1, (int)Math.ceil((double)(lv - 0.001F)));
         int argb = paint.argb();
         int light = paint.light();

         for (int i = 0; i < tu; i++) {
            float fa = (float)i / (float)tu;
            float fb = (float)(i + 1) / (float)tu;
            float ua = fa * lu * perBlock;
            float ub = fb * lu * perBlock;
            float u0 = sprite.getU(spanStart(ua, ub));
            float u1 = sprite.getU(spanEnd(ua, ub));
            float ax = ox + eux * fa;
            float ay = oy + euy * fa;
            float az = oz + euz * fa;
            float bx = ox + eux * fb;
            float by = oy + euy * fb;
            float bz = oz + euz * fb;

            for (int k = 0; k < tv; k++) {
               float ga = (float)k / (float)tv;
               float gb = (float)(k + 1) / (float)tv;
               float va = ga * lv * perBlock;
               float vb = gb * lv * perBlock;
               float v0 = sprite.getV(spanStart(va, vb));
               float v1 = sprite.getV(spanEnd(va, vb));
               vertex(pose, vc, argb, light, ax + evx * ga, ay + evy * ga, az + evz * ga, u0, v0, nx, ny, nz);
               vertex(pose, vc, argb, light, ax + evx * gb, ay + evy * gb, az + evz * gb, u0, v1, nx, ny, nz);
               vertex(pose, vc, argb, light, bx + evx * gb, by + evy * gb, bz + evz * gb, u1, v1, nx, ny, nz);
               vertex(pose, vc, argb, light, bx + evx * ga, by + evy * ga, bz + evz * ga, u1, v0, nx, ny, nz);
            }
         }
      }
   }

   private static void ringPoint(Vector3f c, Vector3f n, Vector3f b, float r, float t, Vector3f out, Vector3f outNormal) {
      float a = t * (float) (Math.PI * 2);
      float cos = Mth.cos(a);
      float sin = Mth.sin(a);
      outNormal.set(n.x * cos + b.x * sin, n.y * cos + b.y * sin, n.z * cos + b.z * sin);
      out.set(c.x + outNormal.x * r, c.y + outNormal.y * r, c.z + outNormal.z * r);
   }

   private static void spherePoint(float theta, float phi, Vector3f out) {
      float st = Mth.sin(theta);
      out.set(st * Mth.cos(phi), Mth.cos(theta), st * Mth.sin(phi));
   }

   private static void sheetPoint(WaterMesh.Height h, float x, float z, float dx, float dz, Vector3f out, Vector3f n) {
      float y = h.at(x, z);
      out.set(x, y, z);
      float ex = Math.max(0.001F, Math.abs(dx) * 0.5F);
      float ez = Math.max(0.001F, Math.abs(dz) * 0.5F);
      float gx = (h.at(x + ex, z) - h.at(x - ex, z)) / (2.0F * ex);
      float gz = (h.at(x, z + ez) - h.at(x, z - ez)) / (2.0F * ez);
      n.set(-gx, 1.0F, -gz).normalize();
   }

   private static void vertex(Pose pose, VertexConsumer vc, WaterMesh.Paint paint, Vector3f p, TextureAtlasSprite sprite, float u, float v, Vector3f n) {
      vertex(pose, vc, paint.argb(), paint.light(), p.x, p.y, p.z, sprite.getU(u), sprite.getV(v), n.x, n.y, n.z);
   }

   private static void vertex(Pose pose, VertexConsumer vc, int argb, int light, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
      vc.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
   }

   @FunctionalInterface
   public interface Height {
      float at(float var1, float var2);
   }

   public static record Paint(int argb, int light, TextureAtlasSprite still, TextureAtlasSprite flow) {
      public static WaterMesh.Paint of(int rgb, float alpha, int light) {
         return new WaterMesh.Paint(WaterMesh.argb(rgb, alpha), light, FluidSpriteCache.getSprite(WaterMesh.STILL), FluidSpriteCache.getSprite(WaterMesh.FLOW));
      }

      public WaterMesh.Paint alpha(float alpha) {
         return new WaterMesh.Paint(WaterMesh.argb(this.argb & 16777215, alpha), this.light, this.still, this.flow);
      }

      public WaterMesh.Paint tint(int rgb) {
         return new WaterMesh.Paint(this.argb & 0xFF000000 | rgb & 16777215, this.light, this.still, this.flow);
      }

      public WaterMesh.Paint light(int packedLight) {
         return new WaterMesh.Paint(this.argb, packedLight, this.still, this.flow);
      }

      public float opacity() {
         return (float)(this.argb >>> 24) / 255.0F;
      }

      public WaterMesh.Paint foam(float alpha) {
         return new WaterMesh.Paint(WaterMesh.argb(15004159, alpha), WaterMesh.liftLight(this.light, 10485920), this.still, this.flow);
      }

      public WaterMesh.Paint sheen(float alpha) {
         return this.foam(alpha).tint(WaterMesh.mix(this.argb & 16777215, 15004159, 0.5F));
      }
   }

   @FunctionalInterface
   public interface Shape {
      void build(Pose var1, VertexConsumer var2);
   }

   public interface Spine {
      Vector3f center(float var1);

      float radius(float var1);

      static WaterMesh.Spine line(final Vector3f from, final Vector3f to, final WaterMesh.Spine.Radius radius) {
         return new WaterMesh.Spine() {
            @Override
            public Vector3f center(float t) {
               return new Vector3f(from).lerp(to, t);
            }

            @Override
            public float radius(float t) {
               return radius.at(t);
            }
         };
      }

      static WaterMesh.Spine arc(final Vector3f from, final Vector3f to, final Vector3f bow, final WaterMesh.Spine.Radius radius) {
         return new WaterMesh.Spine() {
            @Override
            public Vector3f center(float t) {
               float s = 4.0F * t * (1.0F - t);
               return new Vector3f(from).lerp(to, t).add(bow.x * s, bow.y * s, bow.z * s);
            }

            @Override
            public float radius(float t) {
               return radius.at(t);
            }
         };
      }

      @FunctionalInterface
      public interface Radius {
         float at(float var1);
      }
   }
}
