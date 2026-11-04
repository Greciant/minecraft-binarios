package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.fruits.suna_suna.SunaGround;
import com.devilfruits.fruits.suna_suna.SunaSounds;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class TsunamiCinematic extends SandCinematic {
   private static final float STEP = 0.5F;
   private static final int CRASH_TICKS = 10;
   private static final int FRONT_ROWS = 10;
   private static final int BACK_ROWS = 6;
   private static final float BACK_SLOPE = 1.7F;
   private final Vec3 velocity;
   private final Vec3 dir;
   private final Vec3 side;
   private final float width;
   private final float height;
   private final int lifetime;
   private final int columns;
   private final double[] base;
   private final double[] prevBase;
   private double wx;
   private double wy;
   private double wz;
   private double pwx;
   private double pwy;
   private double pwz;
   private int crashedAt = -1;
   private final double[][] feet;
   private final float[] heights;
   private final double[] q00 = new double[3];
   private final double[] q01 = new double[3];
   private final double[] q11 = new double[3];
   private final double[] q10 = new double[3];
   private final double[] px = new double[64];
   private final double[] py = new double[64];
   private final double[] pz = new double[64];
   private final float[] w = new float[64];
   private final float[] a = new float[64];

   public TsunamiCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.velocity = params.dir();
      this.dir = this.velocity.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : this.velocity.normalize();
      this.side = new Vec3(-this.dir.z, 0.0, this.dir.x);
      this.width = params.power();
      this.height = (float)Math.max(1, params.extra() & 15) / 2.0F;
      this.lifetime = params.extra() >> 4;
      this.columns = Mth.clamp(Mth.ceil(this.width / 0.5F), 3, 60);
      this.base = new double[this.columns];
      this.prevBase = new double[this.columns];
      this.feet = new double[this.columns + 1][3];
      this.heights = new float[this.columns + 1];
      Vec3 s = params.pos();
      this.wx = this.pwx = s.x;
      this.wy = this.pwy = s.y;
      this.wz = this.pwz = s.z;
      this.ground(level);
      System.arraycopy(this.base, 0, this.prevBase, 0, this.columns);
   }

   @Override
   protected boolean followsCaster() {
      return false;
   }

   public static void crash(long seed) {
      TsunamiCinematic c = VfxDirector.find(TsunamiCinematic.class, s -> s.seed == seed);
      if (c != null) {
         c.crash();
      }
   }

   private void crash() {
      if (this.crashedAt < 0) {
         this.crashedAt = this.age;
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            int tint = lit(level, this.wx, this.wy + 1.0, this.wz);
            int n = VfxSettings.count(this.width * 1.6F);

            for (int i = 0; i < n; i++) {
               double s = this.random.nextDouble() * 2.0 - 1.0;
               Vec3 p = new Vec3(this.wx, this.wy, this.wz).add(this.side.scale(s * (double)this.width * 0.5)).add(this.dir.scale(0.5));
               plume(
                     this.random,
                     p.x,
                     p.y + this.random.nextDouble() * (double)this.height,
                     p.z,
                     this.dir.x * 0.2 + this.spread(0.08),
                     0.03 + this.random.nextDouble() * 0.05,
                     this.dir.z * 0.2 + this.spread(0.08),
                     tint
                  )
                  .size(1.4F + this.height * 0.25F);
               if (i % 2 == 0) {
                  grains(
                        this.random,
                        p.x,
                        p.y + this.random.nextDouble() * (double)this.height,
                        p.z,
                        this.dir.x * 0.3 + this.spread(0.15),
                        0.1 + this.random.nextDouble() * 0.2,
                        this.dir.z * 0.3 + this.spread(0.15),
                        tint
                     )
                     .size(1.2F);
               }
            }

            SoundDesign.play(
               new Vec3(this.wx, this.wy + 1.0, this.wz), SunaSounds.TSUNAMI_CRASH, 1.6F, 0.8F, SoundDesign.layer(SunaSounds.LAYER_SAND, 1.0F, 0.6F)
            );
         }
      }
   }

   private void ground(ClientLevel level) {
      int up = Mth.ceil((double)this.height * 0.6);

      for (int c = 0; c < this.columns; c++) {
         double s = ((double)c + 0.5) / (double)this.columns * 2.0 - 1.0;
         double cx = this.wx + this.side.x * s * (double)this.width * 0.5;
         double cz = this.wz + this.side.z * s * (double)this.width * 0.5;
         double g = SunaGround.surface(level, cx, this.wy, cz, up, 4);
         this.base[c] = Double.isNaN(g) ? this.wy : g;
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.pwx = this.wx;
      this.pwy = this.wy;
      this.pwz = this.wz;
      System.arraycopy(this.base, 0, this.prevBase, 0, this.columns);
      if (this.crashedAt >= 0) {
         if (this.age - this.crashedAt > 10) {
            this.finish();
         }
      } else if (this.age > this.lifetime + 20) {
         this.crash();
      } else {
         double nx = this.wx + this.velocity.x;
         double nz = this.wz + this.velocity.z;
         double g = SunaGround.surface(level, nx, this.wy, nz, Mth.ceil((double)this.height * 0.6), 4);
         if (Double.isNaN(g)) {
            this.crash();
         } else {
            this.wx = nx;
            this.wy = g;
            this.wz = nz;
            this.ground(level);
            int tint = lit(level, this.wx, this.wy + 1.0, this.wz);
            float grow = Math.min(1.0F, (float)this.age / 6.0F);
            int n = VfxSettings.count(this.width * 0.9F);

            for (int i = 0; i < n; i++) {
               int c = this.random.nextInt(this.columns);
               double s = ((double)c + 0.5) / (double)this.columns * 2.0 - 1.0;
               double h = (double)(this.columnHeight(s, (float)this.age) * grow);
               Vec3 p = new Vec3(this.wx, this.base[c], this.wz).add(this.side.scale(s * (double)this.width * 0.5)).add(this.dir.scale((double)lean(1.0F) * h));
               if (i % 2 == 0) {
                  plume(
                        this.random,
                        p.x,
                        p.y + h * 0.95,
                        p.z,
                        this.dir.x * 0.2 + this.spread(0.05),
                        0.05 + this.random.nextDouble() * 0.06,
                        this.dir.z * 0.2 + this.spread(0.05),
                        tint
                     )
                     .size(1.3F + this.height * 0.22F)
                     .life(0.8F);
               } else {
                  grains(
                        this.random,
                        p.x,
                        p.y + h,
                        p.z,
                        this.dir.x * 0.32 + this.spread(0.08),
                        0.1 + this.random.nextDouble() * 0.15,
                        this.dir.z * 0.32 + this.spread(0.08),
                        tint
                     )
                     .size(1.2F);
               }

               if (i % 4 == 0) {
                  streak(p.x, p.y + h, p.z, this.dir.x * 0.6 + this.spread(0.1), 0.05, this.dir.z * 0.6 + this.spread(0.1), tint);
               }
            }

            int m = VfxSettings.count(this.width * 0.25F);

            for (int i = 0; i < m; i++) {
               int cx = this.random.nextInt(this.columns);
               double sx = ((double)cx + 0.5) / (double)this.columns * 2.0 - 1.0;
               Vec3 px = new Vec3(this.wx, this.base[cx], this.wz).add(this.side.scale(sx * (double)this.width * 0.5));
               puff(this.random, px.x + this.dir.x * 0.8, px.y + 0.3, px.z + this.dir.z * 0.8, this.dir.x * 0.25, 0.02, this.dir.z * 0.25, tint).size(1.1F);
               if (i % 2 == 0) {
                  haze(
                        this.random,
                        px.x - this.dir.x * (double)this.height * 1.4,
                        px.y + 0.6 + this.random.nextDouble() * (double)this.height * 0.5,
                        px.z - this.dir.z * (double)this.height * 1.4,
                        this.spread(0.03),
                        0.015,
                        this.spread(0.03),
                        tint
                     )
                     .size(0.5F + this.height * 0.1F)
                     .alpha(0.55F);
               }
            }

            if (this.age % 8 == 1) {
               SoundDesign.play(new Vec3(this.wx, this.wy + 1.0, this.wz), SunaSounds.TSUNAMI_LOOP, 1.4F, 0.7F + this.random.nextFloat() * 0.1F);
               CameraEffects.shakeAt(new Vec3(this.wx, this.wy, this.wz), 0.14F, (double)(this.width + 8.0F));
            }
         }
      }
   }

   private float columnHeight(double s, float t) {
      double a2 = s * s;
      double profile = 1.0 - 0.85 * a2 * a2 * a2;
      double ripple = 1.0 + 0.06 * Math.sin((double)t * 0.31 + s * 11.0) + 0.03 * Math.sin((double)t * 0.57 + s * 23.0);
      return (float)((double)this.height * profile * ripple);
   }

   private static float lean(float h) {
      float l = (float)Math.pow((double)h, 2.4) * 0.45F;
      return h > 0.72F ? l + (float)Math.pow((double)(h - 0.72F), 1.5) * 1.8F : l;
   }

   private float end(int c) {
      float s = Math.abs((float)c / (float)this.columns * 2.0F - 1.0F);
      return Mth.clamp((1.0F - s) / 0.18F, 0.0F, 1.0F);
   }

   private static float crumble(float f, float e) {
      return Math.min(0.85F, 0.05F + 0.6F * Mth.clamp((f - 0.82F) / 0.18F, 0.0F, 1.0F) + 0.7F * (1.0F - e));
   }

   private void lump(double[] p, int c, float f, float t, float h) {
      float k = Math.min(1.0F, h / 2.0F);
      float n = SandNoise.lumpy((float)c * 0.55F + (float)(this.seed & 63L), f * 3.2F - t * 0.12F, t * 0.04F);
      float m = SandNoise.noise((float)c * 0.9F + 31.0F, f * 5.0F - t * 0.2F, t * 0.05F + 7.0F);
      p[0] += this.dir.x * (double)n * 0.45 * (double)k + this.side.x * (double)m * 0.12;
      p[1] += (double)m * 0.18 * (double)k;
      p[2] += this.dir.z * (double)n * 0.45 * (double)k + this.side.z * (double)m * 0.12;
   }

   private double foot(int c, float pt) {
      int i0 = Math.max(0, c - 1);
      int i1 = Math.min(this.columns - 1, c);
      return (Mth.lerp((double)pt, this.prevBase[i0], this.base[i0]) + Mth.lerp((double)pt, this.prevBase[i1], this.base[i1])) * 0.5;
   }

   private void front(double[] out, double[] foot, float h, float f, float topple) {
      double y = (double)(f * h - (f > 0.88F ? (f - 0.88F) * h * 0.9F : 0.0F));
      double fwd = (double)(lean(f) * h) + (double)topple * 1.6 * (double)f;
      out[0] = foot[0] + this.dir.x * fwd;
      out[1] = foot[1] + y;
      out[2] = foot[2] + this.dir.z * fwd;
   }

   private void back(double[] out, double[] foot, float h, float b, float topple) {
      double y = (double)h * 0.84 * (double)(1.0F - b * b * (3.0F - 2.0F * b));
      double fwd = (double)(lean(0.82F) * h - b * h * 1.7F) + (double)topple * 1.3 * (double)(1.0F - b);
      out[0] = foot[0] + this.dir.x * fwd;
      out[1] = foot[1] + y;
      out[2] = foot[2] + this.dir.z * fwd;
   }

   @Override
   public void render(VfxFrame f) {
      float pt = f.partial;
      float t = (float)this.age + pt;
      float shrink = this.crashedAt < 0 ? Math.min(1.0F, t / 6.0F) : Math.max(0.0F, 1.0F - ((float)(this.age - this.crashedAt) + pt) / 10.0F);
      if (!(shrink <= 0.02F)) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            double cx = Mth.lerp((double)pt, this.pwx, this.wx);
            double cz = Mth.lerp((double)pt, this.pwz, this.wz);
            int tint = lit(level, cx, Mth.lerp((double)pt, this.pwy, this.wy) + 1.0, cz);
            float topple = this.crashedAt < 0 ? 0.0F : 1.0F - shrink;
            int n = this.columns + 1;

            for (int c = 0; c < n; c++) {
               double s = (double)c / (double)this.columns * 2.0 - 1.0;
               this.feet[c][0] = cx + this.side.x * s * (double)this.width * 0.5;
               this.feet[c][1] = this.foot(c, pt) - 0.3;
               this.feet[c][2] = cz + this.side.z * s * (double)this.width * 0.5;
               this.heights[c] = this.columnHeight(s, t) * shrink + 0.3F;
            }

            if (f.allowMesh()) {
               BufferBuilder b = f.mesh(MeshTex.SAND_SHEET);
               BufferBuilder over = f.mesh(MeshTex.SAND_STREAM);

               for (int c = 0; c < this.columns; c++) {
                  float u0 = (float)c / (float)this.columns * this.width * 0.55F;
                  float u1 = (float)(c + 1) / (float)this.columns * this.width * 0.55F;
                  float e0 = this.end(c);
                  float e1 = this.end(c + 1);

                  for (int r = 0; r < 10; r++) {
                     float f0 = (float)r / 10.0F;
                     float f1 = (float)(r + 1) / 10.0F;
                     this.front(this.q00, this.feet[c], this.heights[c], f0, topple);
                     this.lump(this.q00, c, f0, t, this.heights[c]);
                     this.front(this.q01, this.feet[c + 1], this.heights[c + 1], f0, topple);
                     this.lump(this.q01, c + 1, f0, t, this.heights[c + 1]);
                     this.front(this.q11, this.feet[c + 1], this.heights[c + 1], f1, topple);
                     this.lump(this.q11, c + 1, f1, t, this.heights[c + 1]);
                     this.front(this.q10, this.feet[c], this.heights[c], f1, topple);
                     this.lump(this.q10, c, f1, t, this.heights[c]);
                     float k = 0.7F + 0.4F * f1 * f1;
                     float v0 = f0 * this.height * 0.55F - t * 0.12F;
                     float v1 = f1 * this.height * 0.55F - t * 0.12F;
                     SandDraw.quadD(
                        b,
                        f,
                        this.q00,
                        this.q01,
                        this.q11,
                        this.q10,
                        v0,
                        u0,
                        v1,
                        u1,
                        scale(tint, k),
                        1.0F,
                        1.0F,
                        crumble(f0, e0),
                        crumble(f0, e1),
                        crumble(f1, e1),
                        crumble(f1, e0)
                     );
                     float w0 = f0 * this.height * 0.7F - t * 0.35F;
                     float w1 = f1 * this.height * 0.7F - t * 0.35F;
                     SandDraw.quad(
                        over,
                        f,
                        this.q00,
                        this.q01,
                        this.q11,
                        this.q10,
                        w0,
                        u0 * 0.6F,
                        w1,
                        u1 * 0.6F,
                        scale(tint, 1.05F),
                        0.55F * (1.0F - crumble(f0, e0)),
                        0.55F * (1.0F - crumble(f1, e0))
                     );
                  }

                  for (int r = 0; r < 6; r++) {
                     float b0 = (float)r / 6.0F;
                     float b1 = (float)(r + 1) / 6.0F;
                     this.back(this.q00, this.feet[c], this.heights[c], b0, topple);
                     this.lump(this.q00, c, 1.0F + b0, t, this.heights[c]);
                     this.back(this.q01, this.feet[c + 1], this.heights[c + 1], b0, topple);
                     this.lump(this.q01, c + 1, 1.0F + b0, t, this.heights[c + 1]);
                     this.back(this.q11, this.feet[c + 1], this.heights[c + 1], b1, topple);
                     this.lump(this.q11, c + 1, 1.0F + b1, t, this.heights[c + 1]);
                     this.back(this.q10, this.feet[c], this.heights[c], b1, topple);
                     this.lump(this.q10, c, 1.0F + b1, t, this.heights[c]);
                     float v0 = b0 * this.height * 0.7F + t * 0.06F;
                     float v1 = b1 * this.height * 0.7F + t * 0.06F;
                     float d0 = Math.max(0.06F, 0.55F * (1.0F - Math.min(1.0F, b0 * 3.0F))) * 0.5F + (1.0F - e0) * 0.5F;
                     float d1 = Math.max(0.06F, 0.55F * (1.0F - Math.min(1.0F, b1 * 3.0F))) * 0.5F + (1.0F - e0) * 0.5F;
                     SandDraw.quadD(b, f, this.q00, this.q01, this.q11, this.q10, v0, u0, v1, u1, scale(tint, 1.02F - 0.12F * b1), 1.0F, 1.0F, d0, d0, d1, d1);
                  }
               }
            }

            int m = Math.min(64, n);

            for (int i = 0; i < m; i++) {
               int c = i * (n - 1) / Math.max(1, m - 1);
               this.front(this.q00, this.feet[c], this.heights[c], 1.0F, topple);
               this.px[i] = this.q00[0] + this.dir.x * 0.1;
               this.py[i] = this.q00[1] + 0.05;
               this.pz[i] = this.q00[2] + this.dir.z * 0.1;
               this.w[i] = 0.6F * shrink;
               this.a[i] = 0.9F * Math.min(1.0F, this.heights[c] / Math.max(0.1F, this.height) * 1.5F);
            }

            SandDraw.stream(f, m, this.px, this.py, this.pz, this.w, this.a, t * 0.3F, this.width / (float)m * 0.6F, scale(tint, 1.08F));

            for (int k = 1; k < this.columns; k += 3) {
               this.front(this.q00, this.feet[k], this.heights[k], 0.97F, topple);
               int steps = 7;

               for (int i = 0; i < steps; i++) {
                  float q = (float)i / (float)(steps - 1);
                  this.px[i] = this.q00[0] + this.dir.x * (0.15 + (double)q * 0.4);
                  this.py[i] = this.q00[1] - (double)(q * this.heights[k]) * 0.75;
                  this.pz[i] = this.q00[2] + this.dir.z * (0.15 + (double)q * 0.4);
                  this.w[i] = (0.25F + 0.3F * q) * shrink;
                  this.a[i] = Math.min(1.0F, (1.0F - q) * 2.5F) * 0.75F;
               }

               SandDraw.stream(f, steps, this.px, this.py, this.pz, this.w, this.a, t * 0.6F + (float)k, 0.3F, tint);
            }
         }
      }
   }

   @Override
   public double x() {
      return this.wx;
   }

   @Override
   public double y() {
      return this.wy + (double)this.height * 0.5;
   }

   @Override
   public double z() {
      return this.wz;
   }

   @Override
   public double cullDistance() {
      return 128.0;
   }
}
