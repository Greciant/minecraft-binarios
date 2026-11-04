package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.fruits.suna_suna.SunaSounds;
import com.devilfruits.fruits.suna_suna.ability.DesertSpadaAbility;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SpadaCinematic extends SandCinematic {
   private static final float LENGTH = 3.6F;
   private static final float HEIGHT = 3.9F;
   private static final int ROWS = 10;
   private static final int COLS = 8;
   private static final int WAKE = 12;
   private static final int PEEL = 8;
   private final Vec3 step;
   private final int steps;
   private final Vec3 dir;
   private final Vec3 side;
   private final float width;
   private final boolean cuts;
   private final int lifetime;
   private double bx;
   private double by;
   private double bz;
   private double pbx;
   private double pby;
   private double pbz;
   private final double startX;
   private final double startZ;
   private int stoppedAt = -1;
   private final double[][] grid = new double[99][3];
   private final float[] dis = new float[99];
   private final double[] px = new double[12];
   private final double[] py = new double[12];
   private final double[] pz = new double[12];
   private final float[] ax = new float[12];
   private final float[] ay = new float[12];
   private final float[] az = new float[12];
   private final float[] w = new float[12];
   private final float[] a = new float[12];

   public SpadaCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      Vec3 velocity = params.dir();
      this.steps = DesertSpadaAbility.steps(velocity);
      this.step = velocity.scale(1.0 / (double)this.steps);
      this.dir = velocity.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : velocity.normalize();
      this.side = new Vec3(-this.dir.z, 0.0, this.dir.x);
      this.width = Math.max(0.6F, Math.abs(params.power()));
      this.cuts = params.power() > 0.0F;
      this.lifetime = params.extra();
      Vec3 s = params.pos();
      this.bx = this.pbx = s.x;
      this.by = this.pby = s.y;
      this.bz = this.pbz = s.z;
      this.startX = s.x;
      this.startZ = s.z;
   }

   @Override
   protected boolean followsCaster() {
      return false;
   }

   public static void stop(long seed, Vec3 at) {
      SpadaCinematic c = VfxDirector.find(SpadaCinematic.class, s -> s.seed == seed);
      if (c != null) {
         c.stopAt(at);
      }
   }

   private void stopAt(Vec3 at) {
      if (this.stoppedAt < 0) {
         this.stoppedAt = this.age;
         this.bx = at.x;
         this.by = at.y;
         this.bz = at.z;
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            int tint = lit(level, this.bx, this.by + 1.0, this.bz);
            burst(this.random, new Vec3(this.bx, this.by + 0.6, this.bz), new Vec3(0.0, 1.0, 0.0), 0.7F, 0.35F, 22.0F, 8.0F, tint);

            for (int i = 0; i < VfxSettings.count(4.0F); i++) {
               plume(
                     this.random,
                     this.bx + this.spread(0.8),
                     this.by + 0.5 + this.random.nextDouble(),
                     this.bz + this.spread(0.8),
                     this.spread(0.06),
                     0.03,
                     this.spread(0.06),
                     tint
                  )
                  .size(1.2F);
            }

            decal(this.random, this.bx, this.by, this.bz, 3.2F, tint);
         }
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.pbx = this.bx;
      this.pby = this.by;
      this.pbz = this.bz;
      if (this.stoppedAt >= 0) {
         if (this.age - this.stoppedAt > 6) {
            this.finish();
         }
      } else if (this.age > this.lifetime + 10) {
         this.stopAt(new Vec3(this.bx, this.by, this.bz));
      } else {
         for (int i = 0; i < this.steps; i++) {
            Vec3 pos = new Vec3(this.bx, this.by, this.bz);
            Vec3 next = pos.add(this.step);
            double ground = DesertSpadaAbility.next(level, pos, next, this.cuts);
            if (Double.isNaN(ground) || this.cuts && DesertSpadaAbility.resisted(level, next.x, ground, next.z)) {
               this.stopAt(pos);
               return;
            }

            this.bx = next.x;
            this.by = ground;
            this.bz = next.z;
         }

         int tint = lit(level, this.bx, this.by + 1.0, this.bz);
         int n = VfxSettings.count(22.0F);

         for (int i = 0; i < n; i++) {
            double s = this.random.nextBoolean() ? 1.0 : -1.0;
            double at = (double)this.width * (0.15 + this.random.nextDouble() * 0.3);
            double back = this.random.nextDouble() * 1.5;
            double out = 0.12 + this.random.nextDouble() * 0.22;
            grains(
                  this.random,
                  this.bx + this.side.x * s * at - this.dir.x * back,
                  this.by + 0.2 + this.random.nextDouble() * 1.6,
                  this.bz + this.side.z * s * at - this.dir.z * back,
                  this.side.x * s * out + this.dir.x * 0.12,
                  0.2 + this.random.nextDouble() * 0.3,
                  this.side.z * s * out + this.dir.z * 0.12,
                  tint
               )
               .size(1.2F);
         }

         for (int k = -1; k <= 1; k += 2) {
            for (int j = 0; j < VfxSettings.count(4.0F); j++) {
               double back = 0.3 + this.random.nextDouble() * 1.4;
               puff(
                     this.random,
                     this.bx + this.side.x * (double)k * (double)this.width * 0.55 - this.dir.x * back,
                     this.by + 0.3 + this.random.nextDouble() * 0.6,
                     this.bz + this.side.z * (double)k * (double)this.width * 0.55 - this.dir.z * back,
                     this.side.x * (double)k * (0.08 + this.random.nextDouble() * 0.1),
                     0.05 + this.random.nextDouble() * 0.06,
                     this.side.z * (double)k * (0.08 + this.random.nextDouble() * 0.1),
                     tint
                  )
                  .size(1.3F + this.random.nextFloat() * 0.7F);
            }
         }

         for (int j = 0; j < VfxSettings.count(2.0F); j++) {
            double k = this.random.nextDouble();
            plume(
                  this.random,
                  Mth.lerp(k, this.pbx, this.bx) - this.dir.x * 1.5 + this.spread(0.6),
                  this.by + 0.4 + this.random.nextDouble() * 1.2,
                  Mth.lerp(k, this.pbz, this.bz) - this.dir.z * 1.5 + this.spread(0.6),
                  this.spread(0.03),
                  0.025,
                  this.spread(0.03),
                  tint
               )
               .size(1.5F + this.random.nextFloat() * 0.6F)
               .alpha(0.85F);
         }

         if (this.age % 2 == 0) {
            decal(this.random, this.bx - this.dir.x, this.by, this.bz - this.dir.z, 2.6F, tint);
         }

         for (int i = 0; i < VfxSettings.count(4.0F); i++) {
            double hh = this.random.nextDouble();
            double xx = finX((float)hh, 1.0F);
            grains(
               this.random,
               this.bx + this.dir.x * xx + this.spread(0.1),
               this.by - 0.5 + hh * 4.4F,
               this.bz + this.dir.z * xx + this.spread(0.1),
               this.dir.x * 0.35 + this.side.x * this.spread(0.2),
               0.08 + this.random.nextDouble() * 0.15,
               this.dir.z * 0.35 + this.side.z * this.spread(0.2),
               tint
            );
         }

         if (this.age % 3 == 0) {
            streak(
               this.bx + this.dir.x * 0.6,
               this.by + 1.0 + this.random.nextDouble() * 1.5,
               this.bz + this.dir.z * 0.6,
               this.dir.x * 0.9 + this.side.x * this.spread(0.3),
               0.05,
               this.dir.z * 0.9 + this.side.z * this.spread(0.3),
               tint
            );
         }

         if (this.age % 3 == 1) {
            SoundDesign.play(new Vec3(this.bx, this.by + 0.8, this.bz), SunaSounds.SPADA_CUT, 0.9F, 0.85F + this.random.nextFloat() * 0.2F);
         }
      }
   }

   private static double finX(float h, float s) {
      double front = 0.9 - 2.1 * Math.pow((double)h, 1.5);
      double back = -2.649999904632568 + 1.4499999046325684 * Math.pow((double)h, 0.6) - 0.35 * Math.sin((double)h * Math.PI);
      return back + (front - back) * (double)s;
   }

   @Override
   public void render(VfxFrame f) {
      float pt = f.partial;
      float t = (float)this.age + pt;
      float sink = this.stoppedAt < 0 ? 0.0F : Mth.clamp(((float)(this.age - this.stoppedAt) + pt) / 6.0F, 0.0F, 1.0F);
      if (!(sink >= 1.0F)) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            double gx = Mth.lerp((double)pt, this.pbx, this.bx);
            double gy = Mth.lerp((double)pt, this.pby, this.by);
            double gz = Mth.lerp((double)pt, this.pbz, this.bz);
            int tint = lit(level, gx, gy + 1.0, gz);
            float grow = Math.min(1.0F, t / 2.5F);
            float h = 3.9F * grow * (1.0F - sink * 0.9F) * (0.96F + 0.04F * Mth.sin(t * 1.4F));
            double foot = gy - 0.5 - (double)sink * 1.5;
            float thick = 0.16F;
            if (f.allowMesh()) {
               BufferBuilder b = f.mesh(MeshTex.SAND_SHEET);
               BufferBuilder over = f.mesh(MeshTex.SAND_STREAM);
               float wob = t * 0.22F + (float)(this.seed & 31L);

               for (int k = -1; k <= 1; k += 2) {
                  for (int r = 0; r <= 10; r++) {
                     float hh = (float)r / 10.0F;

                     for (int c = 0; c <= 8; c++) {
                        float s = (float)c / 8.0F;
                        double xx = finX(hh, s);
                        float edge = Math.min(Math.min(s, 1.0F - s), 1.0F - hh);
                        float swell = SandNoise.lumpy(s * 2.2F + wob, hh * 2.6F, wob * 0.7F) * (0.22F + 0.45F * (1.0F - Math.min(1.0F, edge * 3.0F)));
                        double th = (double)thick
                           * (1.0 - (double)hh * 0.8)
                           * (double)k
                           * (1.0 + 0.6 * (double)SandNoise.noise(s * 3.0F, hh * 3.0F + wob, 5.0F));
                        double bulge = th * (0.4 + 0.6 * Math.sin((double)s * Math.PI));
                        double[] p = this.grid[r * 9 + c];
                        p[0] = gx + this.dir.x * (xx + (double)(swell * (float)(s < 0.5F ? -1 : 1))) + this.side.x * bulge;
                        p[1] = foot + (double)hh * ((double)h + 0.5) + (hh > 0.85F ? (double)swell * 0.6 : 0.0);
                        p[2] = gz + this.dir.z * (xx + (double)(swell * (float)(s < 0.5F ? -1 : 1))) + this.side.z * bulge;
                        this.dis[r * 9 + c] = 0.2F + 0.6F * (1.0F - Math.min(1.0F, edge * 4.0F)) + 0.15F * hh + 0.12F * sink;
                     }
                  }

                  for (int r = 0; r < 10; r++) {
                     for (int c = 0; c < 8; c++) {
                        int i00 = r * 9 + c;
                        int i01 = i00 + 1;
                        int i10 = i00 + 8 + 1;
                        int i11 = i10 + 1;
                        double[] p00 = this.grid[i00];
                        double[] p01 = this.grid[i01];
                        double[] p11 = this.grid[i11];
                        double[] p10 = this.grid[i10];
                        float u0 = (float)c / 8.0F * 1.8F + t * 0.32F;
                        float u1 = (float)(c + 1) / 8.0F * 1.8F + t * 0.32F;
                        float v0 = (float)r / 10.0F * 1.8F;
                        float v1 = (float)(r + 1) / 10.0F * 1.8F;
                        int col = scale(tint, 0.72F + 0.35F * ((float)r + 0.5F) / 10.0F);
                        SandDraw.quadD(b, f, p00, p01, p11, p10, u0, v0, u1, v1, col, 1.0F, 1.0F, this.dis[i00], this.dis[i01], this.dis[i11], this.dis[i10]);
                        float o0 = (float)c / 8.0F * 2.4F + t * 0.9F;
                        float o1 = (float)(c + 1) / 8.0F * 2.4F + t * 0.9F;
                        float a0 = 0.85F * (1.0F - 0.6F * this.dis[i00]);
                        float a1 = 0.85F * (1.0F - 0.6F * this.dis[i10]);
                        SandDraw.quad(over, f, p00, p01, p11, p10, o0, (float)r / 10.0F, o1, (float)(r + 1) / 10.0F, scale(tint, 1.06F), a0, a1);
                     }
                  }
               }
            }

            for (int k = -1; k <= 1; k += 2) {
               for (int j = 0; j < 2; j++) {
                  int m = 7;
                  float hh = 0.3F + 0.3F * (float)j;

                  for (int i = 0; i < m; i++) {
                     float q = (float)i / (float)(m - 1);
                     double xx = finX(hh, 1.0F - q) - (double)q * 0.8;
                     double out = (double)k
                        * (0.12 + (double)q * (0.35 + 0.25 * (double)j) + 0.08 * (double)SandNoise.noise(q * 3.0F + t * 0.3F, (float)j, (float)k));
                     this.px[i] = gx + this.dir.x * xx + this.side.x * out;
                     this.py[i] = foot + (double)hh * ((double)h + 0.5) + (double)(Mth.sin(t * 0.5F + q * 4.0F + (float)j) * 0.12F);
                     this.pz[i] = gz + this.dir.z * xx + this.side.z * out;
                     this.w[i] = (0.25F + 0.3F * (1.0F - Math.abs(q - 0.4F))) * grow;
                     this.a[i] = Math.min(1.0F, q * 5.0F + 0.1F) * (1.0F - q) * 0.75F * (1.0F - sink);
                  }

                  SandDraw.stream(f, m, this.px, this.py, this.pz, this.w, this.a, -t * 0.8F + (float)j + (float)k, 0.35F, scale(tint, 0.95F));
               }
            }

            for (int k = 0; k < 5; k++) {
               float s0 = 0.15F + (float)k * 0.18F;
               float hh = 0.92F - 0.12F * (float)Math.abs(k - 2);
               double x0 = finX(hh, s0);
               int m = 6;

               for (int i = 0; i < m; i++) {
                  float q = (float)i / (float)(m - 1);
                  this.px[i] = gx
                     + this.dir.x * (x0 - (double)q * (1.2 + 0.2 * (double)k))
                     + this.side.x * (double)Mth.sin(t * 0.7F + (float)k + q * 3.0F) * 0.15F * (double)q;
                  this.py[i] = foot + (double)hh * ((double)h + 0.5) + (double)q * 0.35 + (double)(Mth.sin(t * 0.4F + (float)k * 1.7F + q * 2.0F) * 0.1F);
                  this.pz[i] = gz
                     + this.dir.z * (x0 - (double)q * (1.2 + 0.2 * (double)k))
                     + this.side.z * (double)Mth.sin(t * 0.7F + (float)k + q * 3.0F) * 0.15F * (double)q;
                  this.w[i] = 0.08F + 0.22F * q;
                  this.a[i] = (1.0F - q) * 0.75F * grow * (1.0F - sink);
               }

               SandDraw.stream(f, m, this.px, this.py, this.pz, this.w, this.a, -t * 0.9F + (float)k * 0.4F, 0.4F, tint);
            }

            int ne = 7;

            for (int i = 0; i < ne; i++) {
               float hh = (float)i / (float)(ne - 1);
               double xx = finX(hh, 1.0F) + 0.04;
               this.px[i] = gx + this.dir.x * xx;
               this.py[i] = foot + (double)hh * ((double)h + 0.5) + 0.02;
               this.pz[i] = gz + this.dir.z * xx;
               this.w[i] = 0.09F * (1.0F - hh * 0.6F);
               this.a[i] = 0.9F * Math.min(1.0F, (1.0F - hh) * 6.0F + 0.2F);
            }

            SandDraw.stream(f, ne, this.px, this.py, this.pz, this.w, this.a, t * 0.5F, 0.4F, scale(tint, 1.15F));

            for (int k = 0; k < 4; k++) {
               float hh = 0.2F + (float)k * 0.2F;
               double x0 = finX(hh, 0.0F);
               int m = 7;

               for (int i = 0; i < m; i++) {
                  float q = (float)i / (float)(m - 1);
                  double back = x0 - (double)q * (1.6 + (double)k * 0.3);
                  this.px[i] = gx + this.dir.x * back + this.side.x * (double)Mth.sin(t * 0.6F + (float)k + q * 4.0F) * 0.12 * (double)q;
                  this.py[i] = foot + (double)hh * ((double)h + 0.5) - (double)(q * q) * 0.9;
                  this.pz[i] = gz + this.dir.z * back + this.side.z * (double)Mth.sin(t * 0.6F + (float)k + q * 4.0F) * 0.12 * (double)q;
                  this.w[i] = 0.12F + 0.25F * q;
                  this.a[i] = (1.0F - q) * 0.7F * (1.0F - sink);
               }

               SandDraw.stream(f, m, this.px, this.py, this.pz, this.w, this.a, -t * 0.6F + (float)k * 0.3F, 0.3F, tint);
            }

            for (int k = -1; k <= 1; k += 2) {
               for (int j = 0; j < 2; j++) {
                  int m = 9;
                  double out0 = (double)this.width * 0.35;

                  for (int i = 0; i < m; i++) {
                     float q = (float)i / (float)(m - 1);
                     double back = 0.2 - (double)q * (2.4 + (double)j * 0.8);
                     double out = (double)k * (out0 + (double)q * (1.4 + (double)j * 0.7));
                     double up = Math.sin((double)q * Math.PI * 0.85) * (1.6 + (double)j * 0.6) * (double)grow * (double)(1.0F - sink);
                     this.px[i] = gx + this.dir.x * back + this.side.x * out;
                     this.py[i] = gy + 0.2 + up;
                     this.pz[i] = gz + this.dir.z * back + this.side.z * out;
                     this.w[i] = (0.12F + 0.42F * q) * grow;
                     this.a[i] = Math.min(1.0F, q * 6.0F) * (1.0F - q) * 0.85F * (1.0F - sink);
                  }

                  SandDraw.stream(f, m, this.px, this.py, this.pz, this.w, this.a, -t * 0.7F + (float)j + (float)k, 0.32F, tint);
               }
            }

            for (int k = -1; k <= 1; k += 2) {
               float sx = (float)(this.side.x * 0.6 * (double)k);
               float sz = (float)(this.side.z * 0.6 * (double)k);
               float tl = Mth.sqrt(sx * sx + 0.64000005F + sz * sz);

               for (int i = 0; i < 8; i++) {
                  float q = (float)i / 7.0F;
                  double back = 0.7 - (double)q * 3.6;
                  double out = (double)k * ((double)this.width * 0.3 + (double)q * 0.9);
                  float lift = (0.95F - 0.35F * q) * grow * (1.0F - sink);
                  this.px[i] = gx + this.dir.x * back + this.side.x * out + (double)(sx / tl * lift);
                  this.py[i] = gy + (double)(0.8F / tl * lift) + (double)(Mth.sin(t * 0.8F + q * 5.0F + (float)k) * 0.05F);
                  this.pz[i] = gz + this.dir.z * back + this.side.z * out + (double)(sz / tl * lift);
                  this.ax[i] = sx / tl;
                  this.ay[i] = 0.8F / tl;
                  this.az[i] = sz / tl;
                  this.w[i] = lift;
                  this.a[i] = Math.min(1.0F, q * 5.0F + 0.2F) * (1.0F - q) * 0.85F;
               }

               SandDraw.band(
                  f, MeshTex.SAND_STREAM, 8, this.px, this.py, this.pz, this.ax, this.ay, this.az, this.w, this.a, -t * 0.4F + (float)k, 0.45F, tint, true
               );
            }

            double run = Math.sqrt(Mth.square(gx - this.startX) + Mth.square(gz - this.startZ));
            double length = Math.min(7.0, run + 0.5);
            if (length > 0.4) {
               for (int i = 0; i < 12; i++) {
                  float q = (float)i / 11.0F;
                  this.px[i] = gx - this.dir.x * ((double)q * length - 0.5);
                  this.py[i] = gy + 0.06;
                  this.pz[i] = gz - this.dir.z * ((double)q * length - 0.5);
                  this.ax[i] = (float)this.side.x;
                  this.ay[i] = 0.0F;
                  this.az[i] = (float)this.side.z;
                  this.w[i] = this.width * (0.4F + 0.45F * q);
                  this.a[i] = (1.0F - q) * 0.9F * (1.0F - sink);
               }

               SandDraw.band(f, MeshTex.SAND_STREAM, 12, this.px, this.py, this.pz, this.ax, this.ay, this.az, this.w, this.a, -t * 0.3F, 0.5F, tint, true);
            }
         }
      }
   }

   @Override
   public double x() {
      return this.bx;
   }

   @Override
   public double y() {
      return this.by + 1.0;
   }

   @Override
   public double z() {
      return this.bz;
   }

   @Override
   public double cullDistance() {
      return 128.0;
   }
}
