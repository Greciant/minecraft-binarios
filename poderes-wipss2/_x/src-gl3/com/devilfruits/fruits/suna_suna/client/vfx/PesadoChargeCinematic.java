package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class PesadoChargeCinematic extends SandCinematic {
   private static final int SPIRALS = 4;
   private static final int STEPS = 14;
   private final int charge;
   private double hx;
   private double hy;
   private double hz;
   private int stopped = -1;
   private final double[] px = new double[14];
   private final double[] py = new double[14];
   private final double[] pz = new double[14];
   private final float[] w = new float[14];
   private final float[] a = new float[14];

   public PesadoChargeCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.charge = Math.max(1, params.extra());
      Entity e = this.caster(level);
      Vec3 h = e != null ? SandGatherEffect.hand(e, 1.0F) : new Vec3(this.x, this.y + 1.3, this.z);
      this.hx = h.x;
      this.hy = h.y;
      this.hz = h.z;
   }

   @Nullable
   public static PesadoChargeCinematic of(Entity e) {
      return VfxDirector.find(PesadoChargeCinematic.class, c -> c.follows(e) && c.stopped < 0);
   }

   public void stop() {
      if (this.stopped < 0) {
         this.stopped = this.age;
      }
   }

   private float progress(float t) {
      float p = Math.min(1.0F, t / (float)this.charge);
      return this.stopped < 0 ? p : p * Math.max(0.0F, 1.0F - (t - (float)this.stopped) / 4.0F);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      if (e != null && this.age <= this.charge + 300 && (this.stopped < 0 || this.age - this.stopped <= 4)) {
         Vec3 h = SandGatherEffect.hand(e, 1.0F);
         this.hx = h.x;
         this.hy = h.y;
         this.hz = h.z;
         if (this.stopped < 0) {
            float p = this.progress((float)this.age);
            boolean fp = this.firstPersonCaster();
            int tint = lit(level, this.hx, this.hy, this.hz);
            int n = VfxSettings.count(fp ? 1.0F + 2.0F * p : 2.0F + 5.0F * p);

            for (int i = 0; i < n; i++) {
               Vec3 d = jitter(this.random, Vec3.ZERO, 1.0F);
               double r = (fp ? 1.2 : 1.4) + this.random.nextDouble() * 1.0;
               double sx = this.hx + d.x * r;
               double sy = this.hy + d.y * r * 0.7;
               double sz = this.hz + d.z * r;
               if (i % 3 == 2) {
                  puff(this.random, sx, sy, sz, (this.hx - sx) * 0.1, (this.hy - sy) * 0.1, (this.hz - sz) * 0.1, tint).size(0.35F).life(0.7F);
               } else {
                  grains(this.random, sx, sy, sz, (this.hx - sx) * 0.16, (this.hy - sy) * 0.16 + 0.03, (this.hz - sz) * 0.16, tint).life(0.5F);
               }
            }

            if (!Double.isNaN(this.groundY) && this.hy - this.groundY < 3.5 && !fp) {
               int m = VfxSettings.count(1.0F + 2.0F * p);

               for (int ix = 0; ix < m; ix++) {
                  double a0 = this.random.nextDouble() * (float) (Math.PI * 2);
                  double d0 = 0.6 + this.random.nextDouble() * 1.2;
                  double sx = this.hx + Math.cos(a0) * d0;
                  double sz = this.hz + Math.sin(a0) * d0;
                  grains(this.random, sx, this.groundY + 0.1, sz, (this.hx - sx) * 0.08, 0.16 + 0.1 * (double)p, (this.hz - sz) * 0.08, tint).life(0.6F);
               }

               if (this.age % 3 == 0) {
                  puff(this.random, this.x + this.spread(0.8), this.groundY + 0.15, this.z + this.spread(0.8), this.spread(0.03), 0.02, this.spread(0.03), tint)
                     .size(0.6F)
                     .alpha(0.6F);
               }
            }

            if (this.local() && p >= 1.0F) {
               if (this.age % 4 == 0) {
                  CameraDirector.shake(0.04F, 0.2F, 0.4F, 8.0F);
               }

               VfxPost.vignette(0.12F);
            }
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      Entity e = this.caster();
      if (e != null) {
         Vec3 h = SandGatherEffect.hand(e, f.entityPartial);
         this.hx = h.x;
         this.hy = h.y;
         this.hz = h.z;
      }

      float t = (float)this.age + f.partial;
      float p = this.progress(t);
      if (!(p <= 0.01F)) {
         boolean fp = this.firstPersonCaster();
         float k = fp ? 0.55F : 1.0F;
         int tint = e != null ? lit(e.level(), this.hx, this.hy, this.hz) : 16777215;
         float r = (0.07F + 0.2F * p) * k;
         float tremble = p >= 1.0F && this.stopped < 0 ? 0.012F * Mth.sin(t * 9.1F) : 0.0F;
         double cx = this.hx + (double)tremble;
         double cy = this.hy + (double)tremble * 0.6;
         double cz = this.hz;
         float spin = t * 0.12F;
         SandDraw.billboard(f, VfxSprites.SAND_CLUMP, t * 0.8F + 8.0F, cx, cy, cz, r * 4.2F, -spin * 0.7F, scale(tint, 0.62F), 0.85F * Math.min(1.0F, p * 3.0F));
         SandDraw.billboard(f, VfxSprites.SAND_CLUMP, t * 0.9F, cx, cy, cz, r * 3.4F, spin, scale(tint, 0.86F), Math.min(1.0F, p * 3.0F));
         if (!fp) {
            for (int s = 0; s < 4; s++) {
               float base = (float)s * (float) (Math.PI * 2) / 4.0F + t * 0.12F;

               for (int i = 0; i < 14; i++) {
                  float q = (float)i / 13.0F;
                  float rad = (0.25F + 1.5F * q) * (0.8F + 0.2F * p);
                  float ang = base + q * 3.2F;
                  this.px[i] = cx + (double)(Mth.cos(ang) * rad);
                  this.py[i] = cy - (double)(q * 0.9F) + (double)(Mth.sin(ang * 1.3F) * 0.15F * q);
                  this.pz[i] = cz + (double)(Mth.sin(ang) * rad);
                  this.w[i] = 0.08F + 0.22F * q;
                  this.a[i] = Math.min(1.0F, (1.0F - q) * 2.5F) * Math.min(1.0F, q * 6.0F) * 0.6F * Math.min(1.0F, p * 2.0F);
               }

               SandDraw.stream(f, 14, this.px, this.py, this.pz, this.w, this.a, -t * 0.45F + (float)s, -0.16F, tint);
            }
         }
      }
   }

   @Override
   public double x() {
      return this.hx;
   }

   @Override
   public double y() {
      return this.hy;
   }

   @Override
   public double z() {
      return this.hz;
   }
}
