package com.awakenedmod.awakened.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.util.Mth;

public final class CrackWeb {
   public static final int STRIDE = 7;
   public final float[] data;
   public final int count;

   private CrackWeb(float[] data, int count) {
      this.data = data;
      this.count = count;
   }

   public static CrackWeb grow(long seed, CrackWeb.Shape shape, float heading) {
      CrackWeb.Builder b = new CrackWeb.Builder(new Random(seed), shape);
      boolean round = shape.spread >= 6.2821856F;
      List<float[]> rays = new ArrayList<>();

      for (int i = 0; i < shape.rays; i++) {
         float slot = ((float)i + 0.2F + 0.6F * b.rng.nextFloat()) / (float)shape.rays;
         float angle = round ? heading + (float) (Math.PI * 2) * slot : heading - shape.spread * 0.5F + shape.spread * slot;
         float length = shape.reach * (0.7F + 0.3F * b.rng.nextFloat());
         rays.add(b.walk(0.0F, 0.0F, angle, 0.0F, length, 1.0F, true));
      }

      int pairs = round ? rays.size() : rays.size() - 1;

      for (int ring = 1; ring <= shape.rings; ring++) {
         float rho = shape.reach * 0.5F * (float)ring / (float)(shape.rings + 1);

         for (int i = 0; i < pairs; i++) {
            if (b.rng.nextFloat() < 0.7F) {
               b.chord(rays.get(i), rays.get((i + 1) % rays.size()), rho * (0.85F + 0.3F * b.rng.nextFloat()));
            }
         }
      }

      return new CrackWeb(b.out(), b.count);
   }

   public float shown(int i, float growth) {
      float t0 = this.data[i * 7 + 4];
      float t1 = this.data[i * 7 + 5];
      if (growth <= t0) {
         return 0.0F;
      } else {
         return t1 <= t0 ? 1.0F : Math.min(1.0F, (growth - t0) / (t1 - t0));
      }
   }

   public static float hash01(int h) {
      h ^= h >>> 16;
      h *= 2146121005;
      h ^= h >>> 15;
      h *= -2073254261;
      h ^= h >>> 16;
      return (float)(h >>> 8) / 1.6777216E7F;
   }

   private static final class Builder {
      final Random rng;
      final CrackWeb.Shape shape;
      float[] data = new float[448];
      int count;

      Builder(Random rng, CrackWeb.Shape shape) {
         this.rng = rng;
         this.shape = shape;
      }

      float[] walk(float x, float y, float angle, float dist, float length, float width, boolean main) {
         int n = Math.max(2, Math.round((float)this.shape.steps * length / this.shape.reach));
         float step = length / (float)n;
         float[] line = new float[(n + 1) * 3];
         line[0] = x;
         line[1] = y;
         line[2] = dist;

         for (int k = 0; k < n; k++) {
            angle += (this.rng.nextFloat() - 0.5F) * 2.0F * this.shape.jag;
            float l = step * (0.7F + 0.6F * this.rng.nextFloat());
            float nx = x + Mth.cos(angle) * l;
            float ny = y + Mth.sin(angle) * l;
            this.add(x, y, nx, ny, dist, dist + l, width * (1.0F - 0.5F * (float)k / (float)n));
            x = nx;
            y = ny;
            dist += l;
            line[(k + 1) * 3] = nx;
            line[(k + 1) * 3 + 1] = ny;
            line[(k + 1) * 3 + 2] = dist;
            if (main && k < n - 1 && this.rng.nextFloat() < this.shape.branch) {
               float side = this.rng.nextBoolean() ? 1.0F : -1.0F;
               this.walk(nx, ny, angle + side * (0.45F + 0.5F * this.rng.nextFloat()), dist, (length - step * (float)(k + 1)) * 0.55F, width * 0.6F, false);
            }
         }

         return line;
      }

      void chord(float[] a, float[] b, float rho) {
         float[] p = at(a, rho);
         float[] q = at(b, rho);
         float mx = (p[0] + q[0]) * 0.5F;
         float my = (p[1] + q[1]) * 0.5F;
         float dx = q[0] - p[0];
         float dy = q[1] - p[1];
         float kink = (this.rng.nextFloat() - 0.5F) * 0.3F;
         mx -= dy * kink;
         my += dx * kink;
         float d = Math.max(p[2], q[2]);
         this.add(p[0], p[1], mx, my, d, d + rho * 0.25F, 0.45F);
         this.add(mx, my, q[0], q[1], d + rho * 0.25F, d + rho * 0.5F, 0.45F);
      }

      static float[] at(float[] line, float rho) {
         int points = line.length / 3;

         for (int k = 1; k < points; k++) {
            float d0 = line[(k - 1) * 3 + 2];
            float d1 = line[k * 3 + 2];
            if (rho <= d1 || k == points - 1) {
               float f = d1 <= d0 ? 0.0F : Mth.clamp((rho - d0) / (d1 - d0), 0.0F, 1.0F);
               return new float[]{Mth.lerp(f, line[(k - 1) * 3], line[k * 3]), Mth.lerp(f, line[(k - 1) * 3 + 1], line[k * 3 + 1]), Mth.lerp(f, d0, d1)};
            }
         }

         return new float[]{line[0], line[1], line[2]};
      }

      void add(float x0, float y0, float x1, float y1, float d0, float d1, float width) {
         if ((this.count + 1) * 7 > this.data.length) {
            float[] grown = new float[this.data.length * 2];
            System.arraycopy(this.data, 0, grown, 0, this.data.length);
            this.data = grown;
         }

         int o = this.count++ * 7;
         float reach = Math.max(1.0E-4F, this.shape.reach);
         this.data[o] = x0;
         this.data[o + 1] = y0;
         this.data[o + 2] = x1;
         this.data[o + 3] = y1;
         this.data[o + 4] = Math.min(1.0F, d0 / reach);
         this.data[o + 5] = Math.min(1.0F, d1 / reach);
         this.data[o + 6] = width;
      }

      float[] out() {
         float[] exact = new float[this.count * 7];
         System.arraycopy(this.data, 0, exact, 0, exact.length);
         return exact;
      }
   }

   public static record Shape(int rays, float reach, float spread, float jag, int steps, float branch, int rings) {
   }
}
