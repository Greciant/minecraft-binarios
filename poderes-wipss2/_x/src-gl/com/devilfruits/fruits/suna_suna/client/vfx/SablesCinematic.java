package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.sound.LoopSound;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.fruits.suna_suna.SandStorm;
import com.devilfruits.fruits.suna_suna.SunaGround;
import com.devilfruits.fruits.suna_suna.SunaSounds;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SablesCinematic extends SandCinematic {
   private static final int SLICES = 28;
   private final double initial;
   private final double speed;
   private final float radius;
   private final int life;
   private double sx;
   private double sy;
   private double sz;
   private double psx;
   private double psy;
   private double psz;
   private int brokeAt = -1;
   private final double[][] ring0 = new double[29][3];
   private final double[][] ring1 = new double[29][3];
   private final float[] shade0 = new float[29];
   private final float[] shade1 = new float[29];
   private final double[] px = new double[8];
   private final double[] py = new double[8];
   private final double[] pz = new double[8];
   private final float[] sax = new float[8];
   private final float[] say = new float[8];
   private final float[] saz = new float[8];
   private final float[] sw = new float[8];
   private final float[] sa = new float[8];

   public SablesCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      Vec3 v = params.dir();
      this.initial = Math.atan2(v.z, v.x);
      this.speed = v.horizontalDistance();
      this.radius = params.power();
      this.life = params.extra();
      Vec3 p = params.pos();
      this.sx = this.psx = p.x;
      this.sy = this.psy = p.y;
      this.sz = this.psz = p.z;
      final SablesCinematic self = this;
      Minecraft.getInstance()
         .getSoundManager()
         .play(new LoopSound(SunaSounds.SABLES_LOOP.get(), null, p, 2.5F, 0.7F, 0, () -> !self.isDone() && self.brokeAt < 0) {
            @Override
            public void tick() {
               super.tick();
               this.x = self.sx;
               this.y = self.sy + 4.0;
               this.z = self.sz;
            }
         });
   }

   @Override
   protected boolean followsCaster() {
      return false;
   }

   public static void blowApart(long seed, Vec3 at) {
      SablesCinematic c = VfxDirector.find(SablesCinematic.class, s -> s.seed == seed);
      if (c != null) {
         c.breakAt(at);
      }
   }

   private void breakAt(Vec3 at) {
      if (this.brokeAt < 0) {
         this.brokeAt = this.age;
         this.sx = this.psx = at.x;
         this.sy = this.psy = at.y;
         this.sz = this.psz = at.z;
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            int tint = lit(level, this.sx, this.sy + 2.0, this.sz);
            int n = VfxSettings.count(this.radius * 10.0F);

            for (int i = 0; i < n; i++) {
               double a = this.random.nextDouble() * (float) (Math.PI * 2);
               double h = this.random.nextDouble() * 20.0 * 0.8;
               double s = 0.25 + this.random.nextDouble() * 0.35;
               double r = ringRadius(h / 20.0, (double)this.radius) * 0.7;
               plume(this.random, this.sx + Math.cos(a) * r, this.sy + h, this.sz + Math.sin(a) * r, Math.cos(a) * s, 0.04, Math.sin(a) * s, tint)
                  .size(2.2F + this.random.nextFloat());
               if (i % 2 == 0) {
                  grains(
                        this.random, this.sx + Math.cos(a) * r, this.sy + h, this.sz + Math.sin(a) * r, Math.cos(a) * s * 1.4, 0.1, Math.sin(a) * s * 1.4, tint
                     )
                     .size(1.4F);
               }
            }
         }
      }
   }

   private float size(float t) {
      float env = SandStorm.envelope(t, this.life);
      return this.brokeAt < 0 ? Math.max(0.12F, env) : Math.max(0.0F, 0.7F * (1.0F - ((float)(this.age - this.brokeAt) + 0.0F) / 12.0F));
   }

   private static double ringRadius(double f, double r) {
      return r * (0.3 + 0.88 * Math.pow(Mth.clamp(f, 0.0, 1.0), 1.25));
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.psx = this.sx;
      this.psy = this.sy;
      this.psz = this.sz;
      if (this.brokeAt >= 0) {
         if (this.age - this.brokeAt > 12) {
            this.finish();
         }
      } else if (this.age > this.life + 40) {
         this.breakAt(new Vec3(this.sx, this.sy, this.sz));
      } else {
         int k = this.age - 1;
         if (k < this.life) {
            double h = SandStorm.heading(this.initial, this.seed, k);
            this.sx = this.sx + Math.cos(h) * this.speed;
            this.sz = this.sz + Math.sin(h) * this.speed;
            double g = SunaGround.surface(level, this.sx, this.sy, this.sz, 3, 6);
            if (!Double.isNaN(g)) {
               this.sy = g;
            }
         }

         float s = this.size((float)k);
         float r = this.radius * s;
         int tint = lit(level, this.sx, this.sy + 2.0, this.sz);
         int n = VfxSettings.count(5.0F + 7.0F * s);

         for (int i = 0; i < n; i++) {
            double f = this.random.nextDouble();
            double h = f * 20.0 * (double)s;
            double rr = ringRadius(f, (double)r) * (0.75 + this.random.nextDouble() * 0.45);
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double tang = 0.3 + this.random.nextDouble() * 0.2;
            plume(
                  this.random,
                  this.sx + Math.cos(a) * rr,
                  this.sy + h,
                  this.sz + Math.sin(a) * rr,
                  -Math.sin(a) * tang - Math.cos(a) * 0.05,
                  0.12 + this.random.nextDouble() * 0.1,
                  Math.cos(a) * tang - Math.sin(a) * 0.05,
                  tint
               )
               .size((1.6F + 1.4F * (float)f) * (0.5F + 0.5F * s))
               .life(0.7F);
         }

         int foot = VfxSettings.count(2.0F + 3.0F * s);

         for (int i = 0; i < foot; i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double d = (double)r * (0.35 + this.random.nextDouble() * 0.35);
            plume(
                  this.random,
                  this.sx + Math.cos(a) * d,
                  this.sy + 0.3,
                  this.sz + Math.sin(a) * d,
                  Math.cos(a) * 0.22 - Math.sin(a) * 0.15,
                  0.04,
                  Math.sin(a) * 0.22 + Math.cos(a) * 0.15,
                  tint
               )
               .size(1.8F + s);
            grains(
                  this.random,
                  this.sx + Math.cos(a) * d,
                  this.sy + 0.5 + this.random.nextDouble() * 3.0,
                  this.sz + Math.sin(a) * d,
                  Math.cos(a) * 0.35 - Math.sin(a) * 0.3,
                  0.2,
                  Math.sin(a) * 0.35 + Math.cos(a) * 0.3,
                  tint
               )
               .size(1.3F);
         }

         if (this.age % 3 == 0) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double d = (double)r * (1.2 + this.random.nextDouble() * 0.8);
            haze(
                  this.random,
                  this.sx + Math.cos(a) * d,
                  this.sy + 1.0 + this.random.nextDouble() * 6.0,
                  this.sz + Math.sin(a) * d,
                  -Math.sin(a) * 0.08,
                  0.01,
                  Math.cos(a) * 0.08,
                  tint
               )
               .size(1.2F + s)
               .alpha(0.6F);
         }

         shadow(level, this.sx, this.sy + 0.5, this.sz, r * 3.0F, 0.9F * s);
         if (this.age % 10 == 0) {
            CameraEffects.shakeAt(new Vec3(this.sx, this.sy, this.sz), 0.15F * s, (double)(this.radius * 4.0F));
         }

         Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
         double dist = Math.sqrt(Mth.square(cam.x - this.sx) + Mth.square(cam.z - this.sz));
         if (cam.y < this.sy + 20.0 * (double)s) {
            float near = (float)Mth.clamp(1.0 - (dist - (double)r) / (double)(r * 4.0F), 0.0, 1.0) * s;
            if (near > 0.01F) {
               VfxPost.grade(15915174, 0.35F * near);
               VfxPost.vignette(0.25F * near);
            }

            if (dist < (double)r * 1.2) {
               VfxPost.grade(14728320, 0.6F * s);

               for (int i = 0; i < VfxSettings.count(3.0F); i++) {
                  Vec3 d = jitter(this.random, Vec3.ZERO, 1.0F).scale(1.5 + this.random.nextDouble() * 3.0);
                  plume(this.random, cam.x + d.x, cam.y + d.y, cam.z + d.z, -d.z * 0.06, 0.02, d.x * 0.06, tint).size(1.5F).alpha(0.7F);
               }
            }
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      float pt = f.partial;
      float t = (float)this.age + pt;
      float s = this.size((float)(this.age - 1) + pt);
      if (!(s <= 0.02F)) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            double cx = Mth.lerp((double)pt, this.psx, this.sx);
            double cy = Mth.lerp((double)pt, this.psy, this.sy);
            double cz = Mth.lerp((double)pt, this.psz, this.sz);
            float r = this.radius * s;
            float flung = this.brokeAt < 0 ? 0.0F : ((float)(this.age - this.brokeAt) + pt) / 12.0F;
            int tint = lit(level, cx, cy + 2.0, cz);
            float height = 20.0F * s;
            if (flung < 1.0F) {
               this.twister(f, MeshTex.SAND_SHEET, cx, cy, cz, r * (1.0F + flung * 0.6F), height, t, 1.0F, 0.0F, scale(tint, 1.04F), 1.0F - flung);
               this.twister(
                  f, MeshTex.SAND_STREAM, cx, cy, cz, r * 1.18F * (1.0F + flung * 1.4F), height * 1.02F, t * 1.4F, 0.6F, 0.7F, tint, 0.7F * (1.0F - flung)
               );
            }

            for (int k = 0; k < 7; k++) {
               float f0 = 0.68F + 0.32F * (float)(k * 3 % 7) / 6.0F;
               float rad = top(r) * (1.12F + 0.22F * (float)(k % 3)) * (1.0F + flung * 2.0F);
               float a0 = (float)k * (float) (Math.PI * 2) / 7.0F + t * (0.1F + 0.015F * (float)k);
               this.shred(
                  f,
                  cx,
                  cy + (double)(height * f0),
                  cz,
                  rad,
                  a0,
                  0.8F + 0.3F * (float)(k % 2),
                  0.9F + 0.45F * (float)(k % 3),
                  0.6F * Mth.sin((float)k * 1.7F),
                  t,
                  tint,
                  0.8F * (1.0F - flung),
                  true
               );
            }

            for (int k = 0; k < 8; k++) {
               float rad = r * (0.45F + 0.13F * (float)(k % 4)) * (1.0F + flung * 2.0F);
               float a0 = (float)k * (float) (Math.PI * 2) / 8.0F + t * (0.16F - 0.01F * (float)k);
               this.shred(
                  f,
                  cx,
                  cy + 0.25 + 0.3 * (double)(k % 3),
                  cz,
                  rad,
                  a0,
                  1.0F + 0.35F * (float)(k % 2),
                  0.9F + 0.5F * (float)(k % 2),
                  0.0F,
                  t,
                  scale(tint, 0.95F),
                  0.8F * (1.0F - flung),
                  false
               );
            }
         }
      }
   }

   private static float top(float r) {
      return r * 1.15F;
   }

   private static float profile(float r, float f) {
      return f > 1.0F ? top(r) * (1.0F - (f - 1.0F) * 2.6F) : r * (0.24F + 0.4F * f + 0.51F * f * f * f);
   }

   private void axis(double[] out, double cx, double cz, float r, float f, float t) {
      float bend = r * 0.35F;
      out[0] = cx + (double)(Mth.sin(f * 2.6F + t * 0.035F) * bend * f) + (double)(Mth.sin(t * 0.021F + 1.3F) * bend * 0.4F * f * f);
      out[2] = cz + (double)(Mth.cos(f * 2.2F + t * 0.03F + 0.8F) * bend * f);
   }

   private void twister(VfxFrame f, MeshTex tex, double cx, double cy, double cz, float r, float height, float t, float spin, float holes, int rgb, float alpha) {
      if (!(alpha <= 0.01F) && f.allowMesh()) {
         BufferBuilder b = f.mesh(tex);
         int bands = 20;
         double[] ax = new double[3];

         for (int l = 0; l <= bands; l++) {
            float fr = (float)l / (float)bands * 1.12F;
            this.axis(ax, cx, cz, r, Math.min(1.0F, fr), t);
            float y = (float)(cy + (double)(height * (fr > 1.0F ? 1.0F + (fr - 1.0F) * 0.45F : fr)));
            float rad = profile(r, fr);

            for (int i = 0; i <= 28; i++) {
               float a = (float)i * (float) (Math.PI * 2) / 28.0F;
               float lump = 1.0F + 0.13F * SandNoise.lumpy(a * 1.6F + (float)(this.seed & 15L), fr * 4.5F - t * 0.06F, t * 0.04F);
               double[] q = (l & 1) == 0 ? this.ring0[i] : this.ring1[i];
               q[0] = ax[0] + (double)(Mth.cos(a) * rad * lump);
               q[1] = (double)(y + SandNoise.noise(a * 2.0F, fr * 6.0F, t * 0.05F) * 0.25F);
               q[2] = ax[2] + (double)(Mth.sin(a) * rad * lump);
               float ny = fr > 1.0F ? 0.8F : 0.2F - 0.3F * Math.min(1.0F, fr);
               float k = SandDraw.shade(Mth.cos(a), ny, Mth.sin(a)) * (0.9F + 0.15F * lump);
               ((l & 1) == 0 ? this.shade0 : this.shade1)[i] = k;
            }

            if (l != 0) {
               double[][] lo = (l & 1) == 0 ? this.ring1 : this.ring0;
               double[][] hi = (l & 1) == 0 ? this.ring0 : this.ring1;
               float f0 = (float)(l - 1) / (float)bands * 1.12F;
               float f1 = fr;
               float around = (float)Math.max(2, Math.round((float) (Math.PI * 2) * profile(r, f0) / 3.5F));
               float a0 = alpha * (tex.solid ? 1.0F : fade(Math.min(1.0F, f0)));
               float a1 = alpha * (tex.solid ? 1.0F : fade(Math.min(1.0F, fr)));
               float d0 = holes * 0.4F + 0.18F + 0.45F * Math.max(0.0F, 1.0F - f0 * 6.0F) + 0.6F * Math.max(0.0F, (f0 - 0.95F) * 6.0F);
               float d1 = holes * 0.4F + 0.18F + 0.45F * Math.max(0.0F, 1.0F - fr * 6.0F) + 0.6F * Math.max(0.0F, (fr - 0.95F) * 6.0F);
               float[] slo = (l & 1) == 0 ? this.shade1 : this.shade0;
               float[] shi = (l & 1) == 0 ? this.shade0 : this.shade1;

               for (int i = 0; i < 28; i++) {
                  float u0 = (float)i / 28.0F * around;
                  float u1 = (float)(i + 1) / 28.0F * around;
                  float w0 = -f0 * 2.2F - t * 0.05F * spin;
                  float w1 = -f1 * 2.2F - t * 0.05F * spin;
                  float v0 = f0 * height * 0.09F - t * 0.04F * spin;
                  float v1 = f1 * height * 0.09F - t * 0.04F * spin;
                  float g0 = 0.9F + 0.16F * Math.min(1.0F, f0);
                  float g1 = 0.9F + 0.16F * Math.min(1.0F, f1);
                  SandDraw.quadC(
                     b,
                     f,
                     lo[i],
                     lo[i + 1],
                     hi[i + 1],
                     hi[i],
                     u0 + w0,
                     v0,
                     u1 + w0,
                     v0,
                     u1 + w1,
                     v1,
                     u0 + w1,
                     v1,
                     MeshWriter.argb(scale(rgb, slo[i] * g0), a0),
                     MeshWriter.argb(scale(rgb, slo[i + 1] * g0), a0),
                     MeshWriter.argb(scale(rgb, shi[i + 1] * g1), a1),
                     MeshWriter.argb(scale(rgb, shi[i] * g1), a1),
                     d0,
                     d0,
                     d1,
                     d1
                  );
               }
            }
         }
      }
   }

   private void shred(
      VfxFrame f, double cx, double y, double cz, float rad, float a0, float span, float width, float tilt, float t, int rgb, float alpha, boolean upright
   ) {
      if (!(alpha <= 0.01F)) {
         int m = 8;

         for (int i = 0; i < m; i++) {
            float q = (float)i / (float)(m - 1);
            float a = a0 + q * span;
            this.px[i] = cx + (double)(Mth.cos(a) * rad);
            this.py[i] = y + (double)(Mth.sin(q * (float) Math.PI) * tilt) + (double)(Mth.sin(t * 0.2F + a) * 0.15F);
            this.pz[i] = cz + (double)(Mth.sin(a) * rad);
            if (upright) {
               this.sax[i] = 0.0F;
               this.say[i] = 1.0F;
               this.saz[i] = 0.0F;
            } else {
               this.sax[i] = Mth.cos(a);
               this.say[i] = 0.25F;
               this.saz[i] = Mth.sin(a);
            }

            this.sw[i] = width * Mth.sin(q * (float) Math.PI) * (0.6F + 0.4F * Mth.sin(t * 0.3F + a * 2.0F));
            this.sa[i] = alpha * Math.min(1.0F, Mth.sin(q * (float) Math.PI) * 2.5F);
         }

         SandDraw.band(f, MeshTex.SAND_STREAM, m, this.px, this.py, this.pz, this.sax, this.say, this.saz, this.sw, this.sa, -t * 0.5F + a0, 0.45F, rgb, true);
      }
   }

   private static float fade(float f) {
      return Math.min(1.0F, f * 5.0F + 0.3F) * Math.min(1.0F, (1.0F - f) * 3.0F + 0.15F);
   }

   @Override
   public double x() {
      return this.sx;
   }

   @Override
   public double y() {
      return this.sy + 6.0;
   }

   @Override
   public double z() {
      return this.sz;
   }

   @Override
   public double cullDistance() {
      return 192.0;
   }
}
