package com.awakenedmod.awakened.client.render.limitless;

import java.util.Random;
import org.joml.Vector3f;

public final class StarField {
   private final int count;
   private final float[] dir;
   private final float[] u;
   private final float[] v;
   private final float[] size;
   private final float[] bright;
   private final float[] phase;
   private final int[] rgb;

   private StarField(int count) {
      this.count = count;
      this.dir = new float[count * 3];
      this.u = new float[count * 3];
      this.v = new float[count * 3];
      this.size = new float[count];
      this.bright = new float[count];
      this.phase = new float[count];
      this.rgb = new int[count];
   }

   public static StarField build(
      long seed, int count, StarField.Placement where, float minSize, float maxSize, float sizePower, float minBright, int[] palette, float[] weights
   ) {
      StarField f = new StarField(count);
      Random r = new Random(seed);
      float total = 0.0F;

      for (float w : weights) {
         total += w;
      }

      Vector3f d = new Vector3f();

      for (int i = 0; i < count; i++) {
         where.place(r, d);
         d.normalize();
         Vector3f[] ax = LimitlessMesh.planeAxes(d);
         f.dir[i * 3] = d.x;
         f.dir[i * 3 + 1] = d.y;
         f.dir[i * 3 + 2] = d.z;
         f.u[i * 3] = ax[0].x;
         f.u[i * 3 + 1] = ax[0].y;
         f.u[i * 3 + 2] = ax[0].z;
         f.v[i * 3] = ax[1].x;
         f.v[i * 3 + 1] = ax[1].y;
         f.v[i * 3 + 2] = ax[1].z;
         f.size[i] = minSize + (maxSize - minSize) * (float)Math.pow((double)r.nextFloat(), (double)sizePower);
         f.bright[i] = minBright + (1.0F - minBright) * r.nextFloat();
         f.phase[i] = r.nextFloat() * (float) (Math.PI * 2);
         float pick = r.nextFloat() * total;

         int c;
         for (c = 0; c < palette.length - 1 && pick > weights[c]; c++) {
            pick -= weights[c];
         }

         f.rgb[i] = palette[c] & 16777215;
      }

      return f;
   }

   public static StarField.Placement uniform() {
      return (r, out) -> {
         float z = r.nextFloat() * 2.0F - 1.0F;
         float a = r.nextFloat() * (float) (Math.PI * 2);
         float s = (float)Math.sqrt((double)(1.0F - z * z));
         out.set(s * (float)Math.cos((double)a), z, s * (float)Math.sin((double)a));
      };
   }

   public static StarField.Placement band(Vector3f pole, float width, float inBand) {
      Vector3f[] ax = LimitlessMesh.planeAxes(new Vector3f(pole).normalize());
      Vector3f p = new Vector3f(pole).normalize();
      StarField.Placement even = uniform();
      return (r, out) -> {
         if (r.nextFloat() >= inBand) {
            even.place(r, out);
         } else {
            float a = r.nextFloat() * (float) (Math.PI * 2);
            float lat = (float)r.nextGaussian() * width;
            float cl = (float)Math.cos((double)lat);
            float sl = (float)Math.sin((double)lat);
            float ca = (float)Math.cos((double)a) * cl;
            float sa = (float)Math.sin((double)a) * cl;
            out.set(ax[0].x * ca + ax[1].x * sa + p.x * sl, ax[0].y * ca + ax[1].y * sa + p.y * sl, ax[0].z * ca + ax[1].z * sa + p.z * sl);
         }
      };
   }

   public int count() {
      return this.count;
   }

   public Vector3f direction(int i, Vector3f out) {
      return out.set(this.dir[i * 3], this.dir[i * 3 + 1], this.dir[i * 3 + 2]);
   }

   public void draw(LimitlessMesh m, VoidSky sky, float time, float alpha, float twinkle, float scale, float inset) {
      if (!(alpha <= 0.0F)) {
         Vector3f c = new Vector3f();
         Vector3f[] rim = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};

         for (int i = 0; i < this.count; i++) {
            int k = i * 3;
            sky.at(this.dir[k], this.dir[k + 1], this.dir[k + 2], inset, c);
            float h = this.size[i] * scale * sky.span(c);
            float tw = 1.0F - twinkle * (0.5F + 0.5F * (float)Math.sin((double)(time * 0.15F + this.phase[i])));
            int a = Math.round(255.0F * alpha * this.bright[i] * tw);
            if (a > 0) {
               int core = a << 24 | this.rgb[i];
               int edge = this.rgb[i];

               for (int j = 0; j < 6; j++) {
                  float ang = (float)j * (float) (Math.PI / 3);
                  float cs = (float)Math.cos((double)ang) * h;
                  float sn = (float)Math.sin((double)ang) * h;
                  rim[j]
                     .set(c.x + this.u[k] * cs + this.v[k] * sn, c.y + this.u[k + 1] * cs + this.v[k + 1] * sn, c.z + this.u[k + 2] * cs + this.v[k + 2] * sn);
               }

               for (int j = 0; j < 6; j++) {
                  m.tri(c, core, rim[j], edge, rim[(j + 1) % 6], edge);
               }
            }
         }
      }
   }

   @FunctionalInterface
   public interface Placement {
      void place(Random var1, Vector3f var2);
   }
}
