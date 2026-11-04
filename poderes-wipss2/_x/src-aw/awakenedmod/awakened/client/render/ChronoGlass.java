package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ChronoGlass {
   public static final int GOLD = 15123274;
   public static final int HOT = 16774088;
   private static final int INK = 7229464;
   private static final int FILL = 16773586;
   static final Lab.Value DETAIL = Lab.CLIENT.def("chrono.glass.detail", 2.0, 1.0, 4.0, 1.0);
   static final Lab.Value FILL_ALPHA = Lab.CLIENT.def("chrono.glass.fill", 0.035, 0.0, 0.5, 0.005);
   static final Lab.Value FILL_FRESNEL = Lab.CLIENT.def("chrono.glass.fill_fresnel", 0.12, 0.0, 1.0, 0.01);
   static final Lab.Value FILL_GLINT = Lab.CLIENT.def("chrono.glass.fill_glint", 0.3, 0.0, 1.0, 0.01);
   static final Lab.Value FACET_VARY = Lab.CLIENT.def("chrono.glass.facet_vary", 0.025, 0.0, 0.2, 0.005);
   static final Lab.Value EDGE_WIDTH = Lab.CLIENT.def("chrono.glass.edge_width", 0.035, 0.005, 0.2, 0.005);
   static final Lab.Value EDGE_GLOW = Lab.CLIENT.def("chrono.glass.edge_glow", 0.08, 0.0, 1.0, 0.01);
   static final Lab.Value EDGE_FRESNEL = Lab.CLIENT.def("chrono.glass.edge_fresnel", 0.3, 0.0, 1.0, 0.01);
   static final Lab.Value EDGE_GLINT = Lab.CLIENT.def("chrono.glass.edge_glint", 0.7, 0.0, 1.0, 0.01);
   static final Lab.Value EDGE_INK = Lab.CLIENT.def("chrono.glass.edge_ink", 0.16, 0.0, 1.0, 0.01);
   static final Lab.Value GLINT_POWER = Lab.CLIENT.def("chrono.glass.glint_power", 18.0, 1.0, 128.0, 1.0);
   static final Lab.Value FRESNEL_POWER = Lab.CLIENT.def("chrono.glass.fresnel_power", 2.5, 0.5, 8.0, 0.1);
   static final Lab.Value CRACK_FRINGE = Lab.CLIENT.def("chrono.glass.crack_fringe", 3.0, 1.0, 8.0, 0.1);
   static final Lab.Value CRACK_ALPHA = Lab.CLIENT.def("chrono.glass.crack_alpha", 0.9, 0.0, 1.0, 0.01);
   static final Lab.Value CRACK_INK = Lab.CLIENT.def("chrono.glass.crack_ink", 0.45, 0.0, 1.0, 0.01);
   static final Lab.Value SHARD_SPEED = Lab.CLIENT.def("chrono.glass.shard_speed", 0.22, 0.0, 2.0, 0.01);
   static final Lab.Value SHARD_GRAVITY = Lab.CLIENT.def("chrono.glass.shard_gravity", 0.035, 0.0, 0.3, 0.005);
   static final Lab.Value SHARD_SPIN = Lab.CLIENT.def("chrono.glass.shard_spin", 0.25, 0.0, 2.0, 0.01);
   static final Lab.Value SHARD_FILL = Lab.CLIENT.def("chrono.glass.shard_fill", 0.2, 0.0, 1.0, 0.01);
   static final Lab.Value PLANAR_WIDTH = Lab.CLIENT.def("chrono.glass.planar_width", 0.035, 0.002, 0.2, 0.002);
   static final Lab.Value BURST_SHARDS = Lab.CLIENT.def("chrono.glass.burst_shards", 7.0, 0.0, 24.0, 1.0);
   private static final Vector3f LIGHT = new Vector3f(0.35F, 0.85F, 0.4F).normalize();
   private static final float LIFT = 1.004F;
   private static final float EDGE_ON = 0.35F;
   private static final float EDGE_RADIUS = 9.0F;
   private static final Map<Integer, ChronoGlass.Icosphere> SPHERES = new ConcurrentHashMap<>();
   private static final int WEB_CACHE = 96;
   private static final Map<Long, CrackWeb> WEBS = new LinkedHashMap<Long, CrackWeb>(16, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<Long, CrackWeb> eldest) {
         return this.size() > 96;
      }
   };

   private ChronoGlass() {
   }

   public static void preloadLabKeys() {
   }

   public static void dome(Pose pose, VertexConsumer vc, ChronoGlass.Layer layer, Vec3 c, float radius, float alpha) {
      if (!(alpha <= 0.001F)) {
         ChronoGlass.Icosphere s = sphere();
         Vector3f view = new Vector3f();
         float cx = (float)c.x;
         float cy = (float)c.y;
         float cz = (float)c.z;
         if (layer == ChronoGlass.Layer.BODY) {
            float fill = FILL_ALPHA.getF();
            float fres = FILL_FRESNEL.getF();
            float glint = FILL_GLINT.getF();
            float vary = FACET_VARY.getF();
            float fp = FRESNEL_POWER.getF();
            float gp = GLINT_POWER.getF();

            for (int f = 0; f < s.faces.length / 3; f++) {
               float nx = s.normals[f * 3];
               float ny = s.normals[f * 3 + 1];
               float nz = s.normals[f * 3 + 2];
               view.set(cx + nx * radius, cy + ny * radius, cz + nz * radius).normalize();
               float a = alpha * (fill + fres * fresnel(view, nx, ny, nz, fp) + glint * glint(view, nx, ny, nz, gp) + vary * (hash01(f * 7919) - 0.5F) * 2.0F);
               int color = argb(a, 16773586);

               for (int k = 0; k < 3; k++) {
                  int v = s.faces[f * 3 + k] * 3;
                  vc.addVertex(pose, cx + s.verts[v] * radius, cy + s.verts[v + 1] * radius, cz + s.verts[v + 2] * radius).setColor(color);
               }
            }
         }

         float width = EDGE_WIDTH.getF() * Math.max(1.0F, radius / 9.0F);
         float glow = EDGE_GLOW.getF();
         float fres = EDGE_FRESNEL.getF();
         float glint = EDGE_GLINT.getF();
         float ink = EDGE_INK.getF();
         float fp = FRESNEL_POWER.getF();
         float gp = GLINT_POWER.getF();
         float lift = radius * 1.004F;

         for (int e = 0; e < s.edges.length / 2; e++) {
            int i = s.edges[e * 2] * 3;
            int j = s.edges[e * 2 + 1] * 3;
            float ax = cx + s.verts[i] * lift;
            float ay = cy + s.verts[i + 1] * lift;
            float az = cz + s.verts[i + 2] * lift;
            float bx = cx + s.verts[j] * lift;
            float by = cy + s.verts[j + 1] * lift;
            float bz = cz + s.verts[j + 2] * lift;
            float mx = s.verts[i] + s.verts[j];
            float my = s.verts[i + 1] + s.verts[j + 1];
            float mz = s.verts[i + 2] + s.verts[j + 2];
            float ml = Mth.sqrt(mx * mx + my * my + mz * mz);
            mx /= ml;
            my /= ml;
            mz /= ml;
            view.set((ax + bx) * 0.5F, (ay + by) * 0.5F, (az + bz) * 0.5F).normalize();
            if (layer == ChronoGlass.Layer.BODY) {
               billboard(pose, vc, ax, ay, az, bx, by, bz, width, argb(alpha * ink, 7229464), false);
            } else {
               float a = alpha * (glow + fres * fresnel(view, mx, my, mz, fp) + glint * glint(view, mx, my, mz, gp));
               billboard(pose, vc, ax, ay, az, bx, by, bz, width, argb(a, 16774088), true);
            }
         }
      }
   }

   public static void planarCrack(Pose pose, VertexConsumer vc, ChronoGlass.Layer layer, Vec3 c, Vec3 normal, float size, int seed, float growth, float alpha) {
      planarCrack(pose, vc, layer, c, normal, size, 6, seed, growth, alpha);
   }

   public static void planarCrack(
      Pose pose, VertexConsumer vc, ChronoGlass.Layer layer, Vec3 c, Vec3 normal, float size, int rays, int seed, float growth, float alpha
   ) {
      if (!(growth <= 0.0F) && !(alpha <= 0.001F)) {
         CrackWeb web = web(seed, new CrackWeb.Shape(Math.max(1, rays), size, (float) (Math.PI * 2), 0.35F, 3, 0.35F, 1));
         Vector3f n = new Vector3f((float)normal.x, (float)normal.y, (float)normal.z);
         if (n.lengthSquared() < 1.0E-6F) {
            n.set(0.0F, 0.0F, 1.0F);
         }

         n.normalize();
         Vector3f u = new Vector3f();
         Vector3f v = new Vector3f();
         basis(n, u, v);
         Vector3f a = new Vector3f();
         Vector3f b = new Vector3f();
         float cx = (float)c.x;
         float cy = (float)c.y;
         float cz = (float)c.z;
         float width = PLANAR_WIDTH.getF();

         for (int i = 0; i < web.count; i++) {
            float shown = web.shown(i, growth);
            if (!(shown <= 0.0F)) {
               int k = i * 7;
               float x1 = Mth.lerp(shown, web.data[k], web.data[k + 2]);
               float y1 = Mth.lerp(shown, web.data[k + 1], web.data[k + 3]);
               a.set(u).mul(web.data[k]).fma(web.data[k + 1], v).add(cx, cy, cz);
               b.set(u).mul(x1).fma(y1, v).add(cx, cy, cz);
               crackLine(pose, vc, layer, a, b, n, width * web.data[k + 6], alpha);
            }
         }
      }
   }

   public static void burst(Pose pose, VertexConsumer vc, ChronoGlass.Layer layer, Vec3 c, Vec3 dir, float size, int seed, float t, float life) {
      float progress = Mth.clamp(t / Math.max(1.0F, life), 0.0F, 1.0F);
      int count = BURST_SHARDS.getI();
      if (!(progress >= 1.0F) && count > 0) {
         Vector3f n = new Vector3f((float)dir.x, (float)dir.y, (float)dir.z);
         if (n.lengthSquared() < 1.0E-6F) {
            n.set(0.0F, 0.0F, 1.0F);
         }

         n.normalize();
         Vector3f u = new Vector3f();
         Vector3f v = new Vector3f();
         basis(n, u, v);
         float alpha = (float)Math.pow((double)(1.0F - progress), 1.5);
         float speed = SHARD_SPEED.getF() * 0.6F;
         ChronoGlass.Shard shard = new ChronoGlass.Shard();

         for (int i = 0; i < count; i++) {
            int h = seed * 31 + i * 7919;
            float angle = (float) (Math.PI * 2) * ((float)i + hash01(h)) / (float)count;
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            float piece = size * (0.18F + 0.18F * hash01(h + 1));
            float out = speed * (0.4F + hash01(h + 2));
            shard.velocity.set(u).mul(cos * out).fma(sin * out, v).fma(out * (0.6F + hash01(h + 3)), n);
            shard.axis.set(hash01(h + 4) - 0.5F, hash01(h + 5) - 0.5F, hash01(h + 6) - 0.5F);
            shard.spin = SHARD_SPIN.getF() * 2.0F * (0.4F + hash01(h + 7)) * t;
            float r0 = size * 0.25F;

            for (int k = 0; k < 3; k++) {
               float ang = angle + (float)(k - 1) * 0.9F + (hash01(h + 8 + k) - 0.5F) * 0.4F;
               float rr = k == 1 ? r0 + piece : r0;
               shard.corners[k].set(u).mul(Mth.cos(ang) * rr).fma(Mth.sin(ang) * rr, v);
            }

            shard.place(t, SHARD_GRAVITY.getF(), 1.0F - 0.4F * progress, (float)c.x, (float)c.y, (float)c.z);
            drawShard(pose, vc, layer, shard, alpha, SHARD_FILL.getF(), PLANAR_WIDTH.getF(), GLINT_POWER.getF());
         }
      }
   }

   public static void cut(Pose pose, VertexConsumer vc, ChronoGlass.Layer layer, Vec3 a, Vec3 b, float width, float alpha) {
      if (!(alpha <= 0.001F) && !(a.distanceToSqr(b) < 1.0E-4)) {
         float ax = (float)a.x;
         float ay = (float)a.y;
         float az = (float)a.z;
         float bx = (float)b.x;
         float by = (float)b.y;
         float bz = (float)b.z;
         float a0 = alpha * CRACK_ALPHA.getF();
         if (layer == ChronoGlass.Layer.BODY) {
            billboard(pose, vc, ax, ay, az, bx, by, bz, width * 1.6F, argb(alpha * CRACK_INK.getF(), 7229464), true);
         } else {
            billboard(pose, vc, ax, ay, az, bx, by, bz, width * CRACK_FRINGE.getF(), argb(a0 * 0.3F, 15123274), true);
            billboard(pose, vc, ax, ay, az, bx, by, bz, width, argb(a0, 16774088), true);
         }
      }
   }

   private static void crackLine(Pose pose, VertexConsumer vc, ChronoGlass.Layer layer, Vector3f a, Vector3f b, Vector3f n, float width, float alpha) {
      float dx = b.x - a.x;
      float dy = b.y - a.y;
      float dz = b.z - a.z;
      float sx = n.y * dz - n.z * dy;
      float sy = n.z * dx - n.x * dz;
      float sz = n.x * dy - n.y * dx;
      float sl = Mth.sqrt(sx * sx + sy * sy + sz * sz);
      if (!(sl < 1.0E-6F)) {
         sx /= sl;
         sy /= sl;
         sz /= sl;
         float mx = (a.x + b.x) * 0.5F;
         float my = (a.y + b.y) * 0.5F;
         float mz = (a.z + b.z) * 0.5F;
         float ml = Mth.sqrt(mx * mx + my * my + mz * mz);
         float nl = n.length();
         float facing = !(ml < 1.0E-6F) && !(nl < 1.0E-6F) ? Math.abs(n.x * mx + n.y * my + n.z * mz) / (ml * nl) : 1.0F;
         if (facing < 0.35F) {
            float cx = dy * mz - dz * my;
            float cy = dz * mx - dx * mz;
            float cz = dx * my - dy * mx;
            float cl = Mth.sqrt(cx * cx + cy * cy + cz * cz);
            if (cl > 1.0E-6F) {
               if (cx * sx + cy * sy + cz * sz < 0.0F) {
                  cl = -cl;
               }

               float t = 1.0F - facing / 0.35F;
               t = t * t * (3.0F - 2.0F * t);
               sx = Mth.lerp(t, sx, cx / cl);
               sy = Mth.lerp(t, sy, cy / cl);
               sz = Mth.lerp(t, sz, cz / cl);
               float rl = Mth.sqrt(sx * sx + sy * sy + sz * sz);
               if (rl > 1.0E-6F) {
                  sx /= rl;
                  sy /= rl;
                  sz /= rl;
               }
            }
         }

         float a0 = alpha * CRACK_ALPHA.getF();
         if (layer == ChronoGlass.Layer.BODY) {
            ribbon(pose, vc, a, b, sx, sy, sz, width * 1.6F, argb(alpha * CRACK_INK.getF(), 7229464), false);
         } else {
            ribbon(pose, vc, a, b, sx, sy, sz, width * CRACK_FRINGE.getF(), argb(a0 * 0.3F, 15123274), true);
            ribbon(pose, vc, a, b, sx, sy, sz, width, argb(a0, 16774088), true);
         }
      }
   }

   private static void drawShard(
      Pose pose, VertexConsumer vc, ChronoGlass.Layer layer, ChronoGlass.Shard shard, float alpha, float fill, float width, float glintPower
   ) {
      Vector3f[] p = shard.placed;
      if (layer == ChronoGlass.Layer.BODY) {
         Vector3f n = shard.normal();
         Vector3f view = new Vector3f(p[0]).add(p[1]).add(p[2]).div(3.0F).normalize();
         float glint = glint(view, n.x, n.y, n.z, glintPower);
         int color = argb(alpha * (fill + 0.5F * glint), 16773586);

         for (Vector3f q : p) {
            vc.addVertex(pose, q.x, q.y, q.z).setColor(color);
         }
      } else {
         int color = argb(alpha * 0.8F, 16774088);

         for (int k = 0; k < 3; k++) {
            Vector3f a = p[k];
            Vector3f b = p[(k + 1) % 3];
            billboard(pose, vc, a.x, a.y, a.z, b.x, b.y, b.z, width, color, true);
         }
      }
   }

   public static void billboard(
      Pose pose, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz, float width, int color, boolean twoSided
   ) {
      float dx = bx - ax;
      float dy = by - ay;
      float dz = bz - az;
      float mx = (ax + bx) * 0.5F;
      float my = (ay + by) * 0.5F;
      float mz = (az + bz) * 0.5F;
      float sx = dy * mz - dz * my;
      float sy = dz * mx - dx * mz;
      float sz = dx * my - dy * mx;
      float sl = Mth.sqrt(sx * sx + sy * sy + sz * sz);
      if (!(sl < 1.0E-6F)) {
         float h = width * 0.5F / sl;
         quad(pose, vc, ax, ay, az, bx, by, bz, sx * h, sy * h, sz * h, color, twoSided);
      }
   }

   private static void ribbon(Pose pose, VertexConsumer vc, Vector3f a, Vector3f b, float sx, float sy, float sz, float width, int color, boolean twoSided) {
      float h = width * 0.5F;
      quad(pose, vc, a.x, a.y, a.z, b.x, b.y, b.z, sx * h, sy * h, sz * h, color, twoSided);
   }

   private static void quad(
      Pose pose, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz, float sx, float sy, float sz, int color, boolean twoSided
   ) {
      tri(pose, vc, ax - sx, ay - sy, az - sz, ax + sx, ay + sy, az + sz, bx + sx, by + sy, bz + sz, color);
      tri(pose, vc, ax - sx, ay - sy, az - sz, bx + sx, by + sy, bz + sz, bx - sx, by - sy, bz - sz, color);
      if (twoSided) {
         tri(pose, vc, ax - sx, ay - sy, az - sz, bx + sx, by + sy, bz + sz, ax + sx, ay + sy, az + sz, color);
         tri(pose, vc, ax - sx, ay - sy, az - sz, bx - sx, by - sy, bz - sz, bx + sx, by + sy, bz + sz, color);
      }
   }

   private static void tri(Pose pose, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, int color) {
      vc.addVertex(pose, ax, ay, az).setColor(color);
      vc.addVertex(pose, bx, by, bz).setColor(color);
      vc.addVertex(pose, cx, cy, cz).setColor(color);
   }

   private static float fresnel(Vector3f view, float nx, float ny, float nz, float power) {
      float d = Math.abs(view.x * nx + view.y * ny + view.z * nz);
      return (float)Math.pow((double)(1.0F - Math.min(1.0F, d)), (double)power);
   }

   private static float glint(Vector3f view, float nx, float ny, float nz, float power) {
      float d = view.x * nx + view.y * ny + view.z * nz;
      float rx = view.x - 2.0F * d * nx;
      float ry = view.y - 2.0F * d * ny;
      float rz = view.z - 2.0F * d * nz;
      float l = Math.max(0.0F, rx * LIGHT.x + ry * LIGHT.y + rz * LIGHT.z);
      return (float)Math.pow((double)l, (double)power);
   }

   private static void basis(Vector3f n, Vector3f u, Vector3f v) {
      if (Math.abs(n.y) < 0.9F) {
         u.set(0.0F, 1.0F, 0.0F);
      } else {
         u.set(1.0F, 0.0F, 0.0F);
      }

      u.cross(n).normalize();
      v.set(n).cross(u).normalize();
   }

   private static float hash01(int h) {
      return CrackWeb.hash01(h);
   }

   private static int argb(float alpha, int rgb) {
      int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
      return a << 24 | rgb & 16777215;
   }

   private static CrackWeb web(int seed, CrackWeb.Shape shape) {
      long key = (long)seed << 32 | (long)shape.hashCode() & 4294967295L;
      return WEBS.computeIfAbsent(key, k -> CrackWeb.grow((long)seed, shape, 0.0F));
   }

   private static ChronoGlass.Icosphere sphere() {
      return icosphere(DETAIL.getI());
   }

   public static ChronoGlass.Icosphere icosphere(int detail) {
      return SPHERES.computeIfAbsent(Mth.clamp(detail, 1, 6), ChronoGlass.Icosphere::new);
   }

   public static final class Icosphere {
      public final float[] verts;
      public final int[] faces;
      public final int[] edges;
      public final float[] normals;

      Icosphere(int detail) {
         List<float[]> points = new ArrayList<>();
         float t = (1.0F + Mth.sqrt(5.0F)) / 2.0F;
         float[][] base = new float[][]{
            {-1.0F, t, 0.0F},
            {1.0F, t, 0.0F},
            {-1.0F, -t, 0.0F},
            {1.0F, -t, 0.0F},
            {0.0F, -1.0F, t},
            {0.0F, 1.0F, t},
            {0.0F, -1.0F, -t},
            {0.0F, 1.0F, -t},
            {t, 0.0F, -1.0F},
            {t, 0.0F, 1.0F},
            {-t, 0.0F, -1.0F},
            {-t, 0.0F, 1.0F}
         };

         for (float[] p : base) {
            points.add(unit(p[0], p[1], p[2]));
         }

         int[] tris = new int[]{
            0,
            11,
            5,
            0,
            5,
            1,
            0,
            1,
            7,
            0,
            7,
            10,
            0,
            10,
            11,
            1,
            5,
            9,
            5,
            11,
            4,
            11,
            10,
            2,
            10,
            7,
            6,
            7,
            1,
            8,
            3,
            9,
            4,
            3,
            4,
            2,
            3,
            2,
            6,
            3,
            6,
            8,
            3,
            8,
            9,
            4,
            9,
            5,
            2,
            4,
            11,
            6,
            2,
            10,
            8,
            6,
            7,
            9,
            8,
            1
         };

         for (int d = 0; d < detail; d++) {
            Map<Long, Integer> mids = new HashMap<>();
            int[] next = new int[tris.length * 4];
            int o = 0;

            for (int f = 0; f < tris.length; f += 3) {
               int a = tris[f];
               int b = tris[f + 1];
               int c = tris[f + 2];
               int ab = mid(points, mids, a, b);
               int bc = mid(points, mids, b, c);
               int ca = mid(points, mids, c, a);
               int[] quad = new int[]{a, ab, ca, b, bc, ab, c, ca, bc, ab, bc, ca};
               System.arraycopy(quad, 0, next, o, quad.length);
               o += quad.length;
            }

            tris = next;
         }

         this.verts = new float[points.size() * 3];

         for (int i = 0; i < points.size(); i++) {
            System.arraycopy(points.get(i), 0, this.verts, i * 3, 3);
         }

         this.faces = tris;
         this.normals = new float[this.faces.length];
         Map<Long, Boolean> seen = new HashMap<>();
         List<Integer> edgeList = new ArrayList<>();

         for (int f = 0; f < this.faces.length; f += 3) {
            float[] n = unit(
               this.verts[this.faces[f] * 3] + this.verts[this.faces[f + 1] * 3] + this.verts[this.faces[f + 2] * 3],
               this.verts[this.faces[f] * 3 + 1] + this.verts[this.faces[f + 1] * 3 + 1] + this.verts[this.faces[f + 2] * 3 + 1],
               this.verts[this.faces[f] * 3 + 2] + this.verts[this.faces[f + 1] * 3 + 2] + this.verts[this.faces[f + 2] * 3 + 2]
            );
            System.arraycopy(n, 0, this.normals, f, 3);

            for (int k = 0; k < 3; k++) {
               int a = this.faces[f + k];
               int b = this.faces[f + (k + 1) % 3];
               long key = (long)Math.min(a, b) << 32 | (long)Math.max(a, b);
               if (seen.putIfAbsent(key, Boolean.TRUE) == null) {
                  edgeList.add(a);
                  edgeList.add(b);
               }
            }
         }

         this.edges = edgeList.stream().mapToInt(Integer::intValue).toArray();
      }

      private static int mid(List<float[]> points, Map<Long, Integer> mids, int a, int b) {
         long key = (long)Math.min(a, b) << 32 | (long)Math.max(a, b);
         Integer known = mids.get(key);
         if (known != null) {
            return known;
         } else {
            float[] p = points.get(a);
            float[] q = points.get(b);
            points.add(unit(p[0] + q[0], p[1] + q[1], p[2] + q[2]));
            mids.put(key, points.size() - 1);
            return points.size() - 1;
         }
      }

      private static float[] unit(float x, float y, float z) {
         float l = Mth.sqrt(x * x + y * y + z * z);
         return new float[]{x / l, y / l, z / l};
      }
   }

   public static enum Layer {
      BODY,
      GLOW;

      public FxLayer fx() {
         return this == BODY ? FxLayer.PAINT : FxLayer.LIGHT;
      }
   }

   private static final class Shard {
      final Vector3f velocity = new Vector3f();
      final Vector3f axis = new Vector3f();
      float spin;
      final Vector3f[] corners = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f()};
      final Vector3f[] placed = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f()};
      private final Quaternionf turn = new Quaternionf();
      private final Vector3f centre = new Vector3f();

      void place(float t, float gravity, float scale, float cx, float cy, float cz) {
         this.centre.set(this.corners[0]).add(this.corners[1]).add(this.corners[2]).div(3.0F);
         if (this.axis.lengthSquared() < 1.0E-6F) {
            this.axis.set(0.0F, 1.0F, 0.0F);
         }

         this.turn.identity().rotateAxis(this.spin, this.axis.normalize());
         float fx = this.centre.x + this.velocity.x * t + cx;
         float fy = this.centre.y + this.velocity.y * t - 0.5F * gravity * t * t + cy;
         float fz = this.centre.z + this.velocity.z * t + cz;

         for (int k = 0; k < 3; k++) {
            this.placed[k].set(this.corners[k]).sub(this.centre).mul(scale);
            this.turn.transform(this.placed[k]);
            this.placed[k].add(fx, fy, fz);
         }
      }

      Vector3f normal() {
         Vector3f e1 = new Vector3f(this.placed[1]).sub(this.placed[0]);
         Vector3f e2 = new Vector3f(this.placed[2]).sub(this.placed[0]);
         Vector3f n = e1.cross(e2);
         return n.lengthSquared() < 1.0E-12F ? n.set(0.0F, 1.0F, 0.0F) : n.normalize();
      }
   }
}
