package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class SoulFlames {
   public static void tongues(RandomSource r, Vec3 at, double radius, int count, float scale) {
      for (int i = 0; i < VfxSettings.count((float)count); i++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         double d = Math.sqrt(r.nextDouble()) * radius;
         float size = scale * (0.75F + r.nextFloat() * 0.5F);
         VfxParticles.emit(
               YomiLook.FLAME,
               at.x + Math.cos(a) * d,
               at.y + (double)size * 0.42,
               at.z + Math.sin(a) * d,
               (double)(r.nextFloat() - 0.5F) * 0.02,
               0.015 + (double)r.nextFloat() * 0.03,
               (double)(r.nextFloat() - 0.5F) * 0.02
            )
            .size(size)
            .rotation(0.0F);
      }
   }

   public static void ring(RandomSource r, Vec3 at, double radius, int count, float scale) {
      int n = VfxSettings.count((float)count);

      for (int i = 0; i < n; i++) {
         double a = ((double)i + r.nextDouble() * 0.6) / (double)n * Math.PI * 2.0;
         double d = radius * (0.85 + r.nextDouble() * 0.3);
         float size = scale * (0.7F + r.nextFloat() * 0.6F);
         VfxParticles.emit(
               YomiLook.FLAME_STILL, at.x + Math.cos(a) * d, at.y + (double)size * 0.42, at.z + Math.sin(a) * d, Math.cos(a) * 0.02, 0.01, Math.sin(a) * 0.02
            )
            .size(size)
            .rotation(0.0F);
      }
   }

   public static void wispTrail(RandomSource r, Vec3 a, Vec3 b, float perBlock, float scale) {
      double len = a.distanceTo(b);
      int n = VfxSettings.count((float)(len * (double)perBlock));

      for (int i = 0; i < n; i++) {
         Vec3 p = a.lerp(b, r.nextDouble());
         VfxParticles.emit(
               YomiLook.WISP,
               p.x + (double)(r.nextFloat() - 0.5F) * 0.3,
               p.y + (double)(r.nextFloat() - 0.5F) * 0.3,
               p.z + (double)(r.nextFloat() - 0.5F) * 0.3,
               (double)(r.nextFloat() - 0.5F) * 0.03,
               0.01 + (double)r.nextFloat() * 0.03,
               (double)(r.nextFloat() - 0.5F) * 0.03
            )
            .size(scale * (0.7F + r.nextFloat() * 0.6F))
            .rotation(0.0F);
      }
   }

   public static void burst(RandomSource r, Vec3 at, float scale) {
      VfxParticles.emit(YomiLook.SOUL_FLASH, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(scale);

      for (int i = 0; i < VfxSettings.count(10.0F * scale); i++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         double up = r.nextDouble() * 0.12;
         VfxParticles.emit(YomiLook.WISP, at.x, at.y, at.z, Math.cos(a) * 0.12, up, Math.sin(a) * 0.12).size(scale).rotation(0.0F);
      }
   }

   private SoulFlames() {
   }

   public static final class Web {
      private static final int MAX_POINTS = 10;
      private final float[][] xs;
      private final float[][] zs;
      private final float[] start;
      private final int[] points;
      private final double cx;
      private final double cy;
      private final double cz;
      private final float grow;
      private final float life;
      private static final float[] X = new float[10];
      private static final float[] Y = new float[10];
      private static final float[] Z = new float[10];
      private static final float[] W = new float[10];
      private static final float[] H = new float[10];
      private static final float[] A = new float[10];

      public Web(long seed, Vec3 center, double radius, int branches, float grow, float life) {
         RandomSource r = RandomSource.create(seed);
         int total = branches * 3;
         this.xs = new float[total][10];
         this.zs = new float[total][10];
         this.start = new float[total];
         this.points = new int[total];
         this.cx = center.x;
         this.cy = center.y;
         this.cz = center.z;
         this.grow = grow;
         this.life = life;
         int n = 0;

         for (int b = 0; b < branches && n < total; b++) {
            double a = ((double)b + r.nextDouble() * 0.5) / (double)branches * Math.PI * 2.0;
            n = this.branch(r, n, 0.0F, 0.0F, a, radius * (0.7 + r.nextDouble() * 0.3), 0.0F, 2);
         }
      }

      private int branch(RandomSource r, int slot, float x, float z, double angle, double length, float delay, int depth) {
         if (slot >= this.xs.length) {
            return slot;
         } else {
            int me = slot++;
            int n = Mth.clamp((int)Math.round(length / 0.7) + 1, 3, 10);
            float px = x;
            float pz = z;
            double a = angle;

            for (int i = 0; i < n; i++) {
               this.xs[me][i] = px;
               this.zs[me][i] = pz;
               a += (r.nextDouble() - 0.5) * 0.9;
               double step = length / (double)(n - 1);
               px += (float)(Math.cos(a) * step);
               pz += (float)(Math.sin(a) * step);
            }

            this.points[me] = n;
            this.start[me] = delay;
            if (depth > 0) {
               for (int k = 0; k < 2 && slot < this.xs.length; k++) {
                  if (r.nextFloat() < 0.75F) {
                     int at = 1 + r.nextInt(Math.max(1, n - 2));
                     double fork = a + (double)(r.nextBoolean() ? 1 : -1) * (0.5 + r.nextDouble() * 0.6);
                     slot = this.branch(
                        r, slot, this.xs[me][at], this.zs[me][at], fork, length * (0.4 + r.nextDouble() * 0.25), delay + (float)at / (float)n * 0.6F, depth - 1
                     );
                  }
               }
            }

            return slot;
         }
      }

      public void burn(RandomSource r, float t, float rate) {
         if (!(t < 0.0F) && !(t > this.life)) {
            float burn = t > this.life - 10.0F ? (this.life - t) / 10.0F : 1.0F;
            int n = VfxSettings.count(rate * burn);

            for (int i = 0; i < n; i++) {
               int b = r.nextInt(this.points.length);
               int count = this.points[b];
               if (count >= 2) {
                  float g = Mth.clamp((t / this.grow - this.start[b]) / (1.0F - Math.min(0.8F, this.start[b])), 0.0F, 1.0F);
                  float along = r.nextFloat() * g * (float)(count - 1);
                  int k = Math.min(count - 2, (int)along);
                  float f = along - (float)k;
                  double x = this.cx + (double)Mth.lerp(f, this.xs[b][k], this.xs[b][k + 1]);
                  double z = this.cz + (double)Mth.lerp(f, this.zs[b][k], this.zs[b][k + 1]);
                  float tip = 1.0F - along / (float)(count - 1);
                  SoulFire.lick(x, this.cy + 0.15, z, 0.0, 0.03 + (double)r.nextFloat() * 0.03, 0.0, (0.45F + 0.4F * tip) * burn, r.nextFloat() < 0.4F, false);
               }
            }
         }
      }

      public void render(VfxFrame f, float t, float width) {
         if (!(t < 0.0F) && !(t > this.life) && f.allowMesh()) {
            float burn = t > this.life - 10.0F ? (this.life - t) / 10.0F : 1.0F;

            for (int b = 0; b < this.points.length; b++) {
               int n = this.points[b];
               if (n >= 2) {
                  float g = Mth.clamp((t / this.grow - this.start[b]) / (1.0F - Math.min(0.8F, this.start[b])), 0.0F, 1.0F);
                  int shown = Math.max(0, Math.min(n, (int)Math.ceil((double)(g * (float)(n - 1))) + 1));
                  if (shown >= 2) {
                     for (int i = 0; i < shown; i++) {
                        float wx = (float)this.cx + this.xs[b][i];
                        float wz = (float)this.cz + this.zs[b][i];
                        X[i] = f.rx((double)wx);
                        Y[i] = f.ry(this.cy + 0.06);
                        Z[i] = f.rz((double)wz);
                        float tip = 1.0F - (float)i / (float)(n - 1);
                        W[i] = width * (0.35F + 0.65F * tip) * (0.85F + 0.15F * Mth.sin(t * 0.9F + (float)i * 1.7F + (float)b));
                        H[i] = (0.75F + 0.35F * tip) * burn;
                        A[i] = burn * VfxWorld.nearFade(X[i], Y[i], Z[i]);
                     }

                     MeshWriter.strip(f.mesh(MeshTex.FLAME_RIBBON), shown, X, Y, Z, W, H, A, -t * 0.35F, 0.5F, 16777215, 3, 2.2F);
                  }
               }
            }
         }
      }
   }
}
