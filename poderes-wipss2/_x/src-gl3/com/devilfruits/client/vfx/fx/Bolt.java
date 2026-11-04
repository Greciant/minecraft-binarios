package com.devilfruits.client.vfx.fx;

import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.util.Mth;

public final class Bolt {
   private static final int DEPTH = 5;
   private static final int MAIN = 33;
   private static final int FORK_DEPTH = 4;
   private static final int FORK = 17;
   public static final int MAX_FORKS = 5;
   private final float[] x = new float[118];
   private final float[] y = new float[this.x.length];
   private final float[] z = new float[this.x.length];
   private double ox;
   private double oy;
   private double oz;
   private int forks;
   private float length;
   private static final float[] SX = new float[33];
   private static final float[] SY = new float[33];
   private static final float[] SZ = new float[33];
   private static final float[] SW = new float[33];
   private static final float[] SH = new float[33];
   private static final float[] SA = new float[33];

   public Bolt build(double ax, double ay, double az, double bx, double by, double bz, float rough, int forkCount, long seed) {
      this.ox = ax;
      this.oy = ay;
      this.oz = az;
      float dx = (float)(bx - ax);
      float dy = (float)(by - ay);
      float dz = (float)(bz - az);
      this.length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
      long s = seed * -7046029254386353131L + 1L;
      this.x[0] = this.y[0] = this.z[0] = 0.0F;
      this.x[32] = dx;
      this.y[32] = dy;
      this.z[32] = dz;
      s = this.displace(0, 33, rough * this.length, s);
      this.forks = Math.min(5, forkCount);

      for (int f = 0; f < this.forks; f++) {
         s = next(s);
         int from = Math.min(29, 3 + (int)(unit(s) * 33.0F * 0.7F));
         int base = 33 + f * 17;
         this.x[base] = this.x[from];
         this.y[base] = this.y[from];
         this.z[base] = this.z[from];
         int a = Math.max(0, from - 2);
         int b = Math.min(32, from + 2);
         float tx = this.x[b] - this.x[a];
         float ty = this.y[b] - this.y[a];
         float tz = this.z[b] - this.z[a];
         float tl = Mth.sqrt(tx * tx + ty * ty + tz * tz) + 1.0E-5F;
         s = next(s);
         float rx = signed(s);
         float ry = signed(s >>> 21);
         float rz = signed(s >>> 42);
         float len = this.length * (0.16F + 0.22F * unit(s >>> 11));
         float fx = tx / tl + rx * 0.9F;
         float fy = ty / tl + ry * 0.9F;
         float fz = tz / tl + rz * 0.9F;
         float fl = Mth.sqrt(fx * fx + fy * fy + fz * fz) + 1.0E-5F;
         this.x[base + 17 - 1] = this.x[from] + fx / fl * len;
         this.y[base + 17 - 1] = this.y[from] + fy / fl * len;
         this.z[base + 17 - 1] = this.z[from] + fz / fl * len;
         s = this.displace(base, 17, rough * len * 1.2F, s);
      }

      return this;
   }

   private long displace(int base, int count, float disp, long s) {
      float dx = this.x[base + count - 1] - this.x[base];
      float dy = this.y[base + count - 1] - this.y[base];
      float dz = this.z[base + count - 1] - this.z[base];
      float dl = Mth.sqrt(dx * dx + dy * dy + dz * dz) + 1.0E-5F;
      dx /= dl;
      dy /= dl;
      dz /= dl;

      for (int step = (count - 1) / 2; step >= 1; step /= 2) {
         for (int i = step; i < count - 1; i += step * 2) {
            int p = base + i;
            int a = p - step;
            int b = p + step;
            s = next(s);
            float rx = signed(s);
            float ry = signed(s >>> 21);
            float rz = signed(s >>> 42);
            float along = rx * dx + ry * dy + rz * dz;
            rx -= along * dx;
            ry -= along * dy;
            rz -= along * dz;
            this.x[p] = (this.x[a] + this.x[b]) * 0.5F + rx * disp;
            this.y[p] = (this.y[a] + this.y[b]) * 0.5F + ry * disp;
            this.z[p] = (this.z[a] + this.z[b]) * 0.5F + rz * disp;
         }

         disp *= 0.56F;
      }

      return s;
   }

   public float length() {
      return this.length;
   }

   public void point(float along, double[] out) {
      int i = Mth.clamp(Math.round(along * 32.0F), 0, 32);
      out[0] = this.ox + (double)this.x[i];
      out[1] = this.oy + (double)this.y[i];
      out[2] = this.oz + (double)this.z[i];
   }

   public void render(VfxFrame f, float width, float alpha, float heat) {
      if (!(alpha <= 0.01F)) {
         float rx = f.rx(this.ox);
         float ry = f.ry(this.oy);
         float rz = f.rz(this.oz);
         this.strip(f, 0, 33, rx, ry, rz, width, alpha, heat, 0.25F);

         for (int k = 0; k < this.forks; k++) {
            this.strip(f, 33 + k * 17, 17, rx, ry, rz, width * 0.55F, alpha * 0.85F, heat * 0.95F, 0.9F);
         }
      }
   }

   private void strip(VfxFrame f, int base, int count, float rx, float ry, float rz, float width, float alpha, float heat, float taper) {
      if (f.allowMesh()) {
         float time = f.time;

         for (int i = 0; i < count; i++) {
            float t = (float)i / (float)(count - 1);
            SX[i] = rx + this.x[base + i];
            SY[i] = ry + this.y[base + i];
            SZ[i] = rz + this.z[base + i];
            SW[i] = width * Math.max(0.08F, 1.0F - taper * t);
            SH[i] = heat * 0.82F;
            SA[i] = alpha * VfxWorld.nearFade(SX[i], SY[i], SZ[i]);
         }

         BufferBuilder b = f.mesh(MeshTex.LANCE);
         MeshWriter.strip(b, count, SX, SY, SZ, SW, SH, SA, -time * 0.9F, 0.14F, 16777215, 1, 2.4F);

         for (int i = 0; i < count; i++) {
            SW[i] = SW[i] * 0.26F;
            SH[i] = heat * 1.25F;
         }

         MeshWriter.strip(b, count, SX, SY, SZ, SW, SH, SA, -time * 1.4F, 0.14F, 16777215, 1, 3.6F);
      }
   }

   private static long next(long s) {
      s ^= s << 13;
      s ^= s >>> 7;
      return s ^ s << 17;
   }

   private static float unit(long s) {
      return (float)((int)(s & 16777215L)) / 1.6777216E7F;
   }

   private static float signed(long s) {
      return unit(s) * 2.0F - 1.0F;
   }
}
