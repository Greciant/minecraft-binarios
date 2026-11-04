package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.fruits.suna_suna.SunaGround;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class TsunamiChargeCinematic extends SandCinematic {
   private static final int SEG = 16;
   private static final int SPOUTS = 6;
   private final int charge;
   private int stopped = -1;
   private final double[] gy = new double[17];
   private final double[] px = new double[10];
   private final double[] py = new double[10];
   private final double[] pz = new double[10];
   private final float[] w = new float[10];
   private final float[] a = new float[10];
   private final double[] q00 = new double[3];
   private final double[] q01 = new double[3];
   private final double[] q11 = new double[3];
   private final double[] q10 = new double[3];

   public TsunamiChargeCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.charge = Math.max(1, params.extra());
   }

   @Nullable
   public static TsunamiChargeCinematic of(Entity e) {
      return VfxDirector.find(TsunamiChargeCinematic.class, c -> c.follows(e) && c.stopped < 0);
   }

   public void stop() {
      if (this.stopped < 0) {
         this.stopped = this.age;
      }
   }

   private float progress(float t) {
      float c = Math.min(1.0F, t / (float)this.charge);
      return this.stopped < 0 ? c : c * Math.max(0.0F, 1.0F - (t - (float)this.stopped) / 5.0F);
   }

   private Vec3 arc(Entity e, float pt, double s, double dist) {
      float yaw = Mth.lerp(pt, e.yRotO, e.getYRot()) * (float) (Math.PI / 180.0);
      double fx = (double)(-Mth.sin(yaw));
      double fz = (double)Mth.cos(yaw);
      double ang = s * 1.15;
      double lx = fx * Math.cos(ang) - fz * Math.sin(ang);
      double lz = fz * Math.cos(ang) + fx * Math.sin(ang);
      return new Vec3(Mth.lerp((double)pt, e.xo, e.getX()) + lx * dist, Mth.lerp((double)pt, e.yo, e.getY()), Mth.lerp((double)pt, e.zo, e.getZ()) + lz * dist);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      if (e != null && this.age <= this.charge + 200 && (this.stopped < 0 || this.age - this.stopped <= 5)) {
         for (int i = 0; i <= 16; i++) {
            Vec3 p = this.arc(e, 1.0F, (double)i / 16.0 * 2.0 - 1.0, 2.8);
            double g = SunaGround.surface(level, p.x, e.getY(), p.z, 2, 3);
            this.gy[i] = Double.isNaN(g) ? e.getY() : g;
         }

         if (this.stopped < 0) {
            float c = this.progress((float)this.age);
            int tint = lit(level, this.x, this.y + 1.0, this.z);
            int n = VfxSettings.count(2.0F + 6.0F * c);

            for (int i = 0; i < n; i++) {
               double s = this.random.nextDouble() * 2.0 - 1.0;
               Vec3 p = this.arc(e, 1.0F, s, 2.4 + this.random.nextDouble() * 1.0);
               double g = this.gy[Mth.clamp((int)Math.round((s + 1.0) / 2.0 * 16.0), 0, 16)];
               if (i % 3 == 0) {
                  puff(this.random, p.x, g + 0.15, p.z, this.spread(0.04), 0.04 + 0.04 * (double)c, this.spread(0.04), tint).size(0.9F + 0.6F * c);
               } else {
                  grains(
                     this.random,
                     p.x,
                     g + 0.1 + this.random.nextDouble() * (double)c * 1.6,
                     p.z,
                     this.spread(0.06),
                     0.12 + this.random.nextDouble() * 0.2 * (double)c,
                     this.spread(0.06),
                     tint
                  );
               }
            }

            if (this.local() && c >= 1.0F && this.age % 4 == 0) {
               CameraDirector.shake(0.06F, 0.2F, 0.6F, 6.0F);
            }
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      Entity e = this.caster();
      float t = (float)this.age + f.partial;
      float c = this.progress(t);
      if (e != null && !(c <= 0.02F)) {
         float pt = f.entityPartial;
         int tint = lit(e.level(), this.x, this.y + 1.0, this.z);
         if (f.allowMesh()) {
            BufferBuilder b = f.mesh(MeshTex.SAND_SHEET);
            int rows = 4;

            for (int i = 0; i < 16; i++) {
               double s0 = (double)i / 16.0 * 2.0 - 1.0;
               double s1 = (double)(i + 1) / 16.0 * 2.0 - 1.0;

               for (int r = 0; r < rows; r++) {
                  float h0 = (float)r / (float)rows;
                  float h1 = (float)(r + 1) / (float)rows;
                  this.ridge(this.q00, e, pt, s0, h0, c, this.gy[i], t);
                  this.ridge(this.q01, e, pt, s1, h0, c, this.gy[i + 1], t);
                  this.ridge(this.q11, e, pt, s1, h1, c, this.gy[i + 1], t);
                  this.ridge(this.q10, e, pt, s0, h1, c, this.gy[i], t);
                  float u0 = (float)s0 * 2.5F;
                  float u1 = (float)s1 * 2.5F;
                  SandDraw.quad(
                     b,
                     f,
                     this.q00,
                     this.q01,
                     this.q11,
                     this.q10,
                     u0,
                     h0 * 0.8F - t * 0.06F,
                     u1,
                     h1 * 0.8F - t * 0.06F,
                     scale(tint, 0.8F + 0.25F * h1),
                     1.0F,
                     1.0F
                  );
               }
            }
         }

         for (int k = 0; k < 6; k++) {
            double s = ((double)k + 0.5) / 6.0 * 2.0 - 1.0;
            Vec3 base = this.arc(e, pt, s, 2.9);
            double g = this.gy[Mth.clamp((int)Math.round((s + 1.0) / 2.0 * 16.0), 0, 16)];
            float h = c * (1.4F + 0.8F * (float)(k * 7 % 3) / 2.0F) + 0.15F * Mth.sin(t * 0.4F + (float)k);
            int m = 8;

            for (int i = 0; i < m; i++) {
               float q = (float)i / (float)(m - 1);
               this.px[i] = base.x + (double)(Mth.sin(t * 0.3F + (float)k + q * 3.0F) * 0.12F * q);
               this.py[i] = g - 0.2 + (double)q * ((double)h + 0.4);
               this.pz[i] = base.z + (double)(Mth.cos(t * 0.27F + (float)k + q * 3.0F) * 0.12F * q);
               this.w[i] = 0.18F + 0.22F * q;
               this.a[i] = Math.min(1.0F, (1.0F - q) * 3.0F) * 0.8F * Math.min(1.0F, c * 2.0F);
            }

            SandDraw.stream(f, m, this.px, this.py, this.pz, this.w, this.a, -t * 0.4F + (float)k, -0.3F, tint);
         }
      }
   }

   private void ridge(double[] out, Entity e, float pt, double s, float h, float c, double ground, float t) {
      double edge = Math.pow(Math.max(0.0, 1.0 - s * s), 0.5);
      double height = (0.25 + 1.25 * (double)c) * edge * (1.0 + 0.08 * Math.sin((double)t * 0.35 + s * 7.0));
      Vec3 p = this.arc(e, pt, s, 2.8 - 0.9 * (double)(1.0F - h) + 0.25 * (double)h * (double)h);
      out[0] = p.x;
      out[1] = ground - 0.3 + (height + 0.3) * Math.sin((double)h * Math.PI * 0.5);
      out[2] = p.z;
   }
}
