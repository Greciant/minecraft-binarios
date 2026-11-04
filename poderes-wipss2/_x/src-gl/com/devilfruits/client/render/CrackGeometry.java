package com.devilfruits.client.render;

import com.devilfruits.core.util.DeterministicRandom;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public final class CrackGeometry {
   public static final int STRIDE = 8;

   public static float[] sphere(long seed, int starts, int steps) {
      DeterministicRandom r = new DeterministicRandom(seed);
      FloatArrayList out = new FloatArrayList(starts * steps * 8 * 2);
      Vector3f p = new Vector3f();
      Vector3f t = new Vector3f();

      for (int s = 0; s < starts; s++) {
         randomUnit(r, p);
         randomTangent(r, p, t);
         sphereWalk(r, out, new Vector3f(p), new Vector3f(t), steps, r.range(0.0F, 0.3F), 1.0F, 0);
      }

      return out.toFloatArray();
   }

   private static void sphereWalk(DeterministicRandom r, FloatArrayList out, Vector3f p, Vector3f t, int steps, float birth, float width, int depth) {
      Vector3f next = new Vector3f();
      float step = 0.2F + r.nextFloat() * 0.08F;

      for (int i = 0; i < steps; i++) {
         float turn = (float)r.nextGaussian() * 0.55F;
         t.rotateAxis(turn, p.x, p.y, p.z).normalize();
         next.set(p).fma(step, t).normalize();
         float b = birth + (1.0F - birth) * ((float)i / (float)steps) * 0.85F;
         add(out, p.x, p.y, p.z, next.x, next.y, next.z, b, width * (1.0F - 0.45F * (float)i / (float)steps));
         t.sub(new Vector3f(next).mul(t.dot(next))).normalize();
         p.set(next);
         if (depth < 2 && i > 0 && r.chance(0.22F)) {
            Vector3f bt = new Vector3f(t).rotateAxis((float)(r.chance(0.5F) ? 1 : -1) * r.range(0.6F, 1.2F), p.x, p.y, p.z);
            sphereWalk(r, out, new Vector3f(p), bt, Math.max(1, (steps - i) / 2), b, width * 0.6F, depth + 1);
         }
      }
   }

   public static float[] web(long seed, int spokes, int rings) {
      DeterministicRandom r = new DeterministicRandom(seed);
      FloatArrayList out = new FloatArrayList(spokes * 40);
      int points = 7;
      float[][] sx = new float[spokes][points + 1];
      float[][] sy = new float[spokes][points + 1];
      float base = r.range(0.0F, (float) (Math.PI * 2));

      for (int i = 0; i < spokes; i++) {
         float angle = base + (float)i * (float) (Math.PI * 2) / (float)spokes + r.signed(1.0995574F / (float)spokes);
         float len = r.range(0.75F, 1.05F);
         sx[i][0] = 0.0F;
         sy[i][0] = 0.0F;

         for (int k = 1; k <= points; k++) {
            float rad = len * (float)k / (float)points;
            float a = angle + r.signed(0.12F);
            sx[i][k] = Mth.cos(a) * rad;
            sy[i][k] = Mth.sin(a) * rad;
            add(out, sx[i][k - 1], sy[i][k - 1], 0.0F, sx[i][k], sy[i][k], 0.0F, rad * 0.8F, 1.25F - 0.6F * (float)k / (float)points);
         }

         if (r.chance(0.6F)) {
            int k = 2 + r.nextInt(points - 3);
            float a = angle + (float)(r.chance(0.5F) ? 1 : -1) * r.range(0.35F, 0.8F);
            float l = r.range(0.1F, 0.22F);
            add(out, sx[i][k], sy[i][k], 0.0F, sx[i][k] + Mth.cos(a) * l, sy[i][k] + Mth.sin(a) * l, 0.0F, (float)k / (float)points, 0.55F);
         }
      }

      for (int ring = 1; ring <= rings; ring++) {
         int k = Mth.clamp(Math.round((float)(ring * points) / ((float)rings + 0.6F)), 1, points);

         for (int i = 0; i < spokes; i++) {
            if (!r.chance(0.18F)) {
               int j = (i + 1) % spokes;
               int kj = Mth.clamp(k + (r.chance(0.3F) ? (r.chance(0.5F) ? 1 : -1) : 0), 1, points);
               float mx = (sx[i][k] + sx[j][kj]) * 0.5F * 0.9F + r.signed(0.03F);
               float my = (sy[i][k] + sy[j][kj]) * 0.5F * 0.9F + r.signed(0.03F);
               float birth = (float)Math.sqrt((double)(mx * mx + my * my)) + 0.1F;
               add(out, sx[i][k], sy[i][k], 0.0F, mx, my, 0.0F, birth, 0.8F);
               add(out, mx, my, 0.0F, sx[j][kj], sy[j][kj], 0.0F, birth, 0.8F);
            }
         }
      }

      return out.toFloatArray();
   }

   public static float[] burst(long seed, int rays, float jag) {
      DeterministicRandom r = new DeterministicRandom(seed);
      FloatArrayList out = new FloatArrayList(rays * 5 * 8);
      float base = r.range(0.0F, (float) (Math.PI * 2));

      for (int i = 0; i < rays; i++) {
         float angle = base + (float)i * (float) (Math.PI * 2) / (float)rays + r.signed(0.5F);
         float len = r.range(0.55F, 1.0F);
         int steps = 3 + r.nextInt(3);
         float px = 0.0F;
         float py = 0.0F;

         for (int k = 1; k <= steps; k++) {
            float rad = len * (float)k / (float)steps;
            float a = angle + r.signed(jag);
            float nx = Mth.cos(a) * rad;
            float ny = Mth.sin(a) * rad;
            add(out, px, py, 0.0F, nx, ny, 0.0F, rad * 0.7F, 1.0F - 0.5F * (float)k / (float)steps);
            px = nx;
            py = ny;
         }
      }

      return out.toFloatArray();
   }

   public static float[] crescent(long seed, float span, int segments) {
      DeterministicRandom r = new DeterministicRandom(seed);
      FloatArrayList out = new FloatArrayList(segments * 8 * 3);
      float px = 0.0F;
      float py = 0.0F;

      for (int i = 0; i <= segments; i++) {
         float f = (float)i / (float)segments;
         float a = (float) (Math.PI / 2) - span / 2.0F + span * f;
         float rad = 1.0F + r.signed(0.05F);
         float nx = Mth.cos(a) * rad;
         float ny = Mth.sin(a) * rad;
         float width = Mth.sin(f * (float) Math.PI);
         if (i > 0) {
            add(out, px, py, 0.0F, nx, ny, 0.0F, 0.0F, 0.35F + width);
            if (r.chance(0.45F)) {
               float ba = a + r.signed(0.9F);
               float bl = r.range(0.12F, 0.3F) * (0.4F + width);
               float dir = r.chance(0.6F) ? 1.0F : -1.0F;
               add(out, nx, ny, 0.0F, nx + Mth.cos(ba) * bl * dir, ny + Mth.sin(ba) * bl * dir, 0.0F, 0.0F, 0.45F);
            }
         }

         px = nx;
         py = ny;
      }

      return out.toFloatArray();
   }

   private static void add(FloatArrayList out, float x1, float y1, float z1, float x2, float y2, float z2, float birth, float width) {
      out.add(x1);
      out.add(y1);
      out.add(z1);
      out.add(x2);
      out.add(y2);
      out.add(z2);
      out.add(Mth.clamp(birth, 0.0F, 1.0F));
      out.add(width);
   }

   private static void randomUnit(DeterministicRandom r, Vector3f out) {
      float z = r.range(-1.0F, 1.0F);
      float a = r.range(0.0F, (float) (Math.PI * 2));
      float s = (float)Math.sqrt((double)(1.0F - z * z));
      out.set(s * Mth.cos(a), s * Mth.sin(a), z);
   }

   private static void randomTangent(DeterministicRandom r, Vector3f normal, Vector3f out) {
      Vector3f any = Math.abs(normal.y) < 0.9F ? new Vector3f(0.0F, 1.0F, 0.0F) : new Vector3f(1.0F, 0.0F, 0.0F);
      out.set(normal).cross(any).normalize();
      out.rotateAxis(r.range(0.0F, (float) (Math.PI * 2)), normal.x, normal.y, normal.z);
   }

   private CrackGeometry() {
   }
}
