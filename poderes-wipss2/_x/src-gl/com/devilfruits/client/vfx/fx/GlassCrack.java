package com.devilfruits.client.vfx.fx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class GlassCrack extends Cinematic {
   private static final int MAX_RADIALS = 16;
   private static final int MAX_RINGS = 4;
   private static final int RADIAL_POINTS = 7;
   private double cx;
   private double cy;
   private double cz;
   private final Vector3f n = new Vector3f(0.0F, 0.0F, 1.0F);
   private float radius;
   private final long seed;
   private int radials = 11;
   private int rings = 3;
   private int growTicks = 3;
   private int holdTicks = 8;
   private int fadeTicks = 8;
   @Nullable
   private Vec3 push;
   private float refraction = 0.55F;
   private int rgb = 15398655;
   private float lineWidth = 0.02F;
   private float sheen = 1.0F;
   private boolean billboard;
   private int followId = -1;
   private double fox;
   private double foy;
   private double foz;
   private float alpha = 1.0F;
   private float burst = 1.0F;
   private final float[] ru = new float[112];
   private final float[] rv = new float[112];
   private final float[] rd = new float[112];
   private final int[] rn = new int[16];
   private final boolean[] ringGap = new boolean[64];
   private final float[] ringBow = new float[64];
   private final float[] bu = new float[64];
   private final float[] bv = new float[64];
   private final float[] bd = new float[32];
   private final float[] ringR = new float[5];
   private final float[] offX = new float[64];
   private final float[] offY = new float[64];
   private final float[] shine = new float[64];
   private final float[] vx = new float[64];
   private final float[] vy = new float[64];
   private final float[] vz = new float[64];
   private final float[] spinX = new float[64];
   private final float[] spinY = new float[64];
   private final float[] spinZ = new float[64];
   private final float[] spinRate = new float[64];
   private int shattered = -1;
   private boolean built;
   private final Vector3f u = new Vector3f();
   private final Vector3f v = new Vector3f();
   private final float[] sx = new float[16];
   private final float[] sy = new float[16];
   private final float[] sz = new float[16];
   private final float[] sw = new float[16];
   private final float[] sh = new float[16];
   private final float[] sa = new float[16];
   private final float[] qx = new float[4];
   private final float[] qy = new float[4];
   private final float[] qz = new float[4];
   private final float[] tw = new float[16];
   private final float[] ta = new float[16];
   private boolean frozen;

   private GlassCrack(Vec3 c, Vec3 normal, float radius, long seed) {
      super(new VfxAnchor(-1, c), VfxParams.NONE);
      this.cx = c.x;
      this.cy = c.y;
      this.cz = c.z;
      if (normal.lengthSqr() > 1.0E-6) {
         this.n.set((float)normal.x, (float)normal.y, (float)normal.z).normalize();
      }

      this.radius = Math.max(0.1F, radius);
      this.seed = seed;
   }

   public static GlassCrack at(Vec3 c, Vec3 normal, float radius, long seed) {
      return new GlassCrack(c, normal, radius, seed);
   }

   public GlassCrack pattern(int radialCount, int ringCount) {
      this.radials = Mth.clamp(radialCount, 4, 16);
      this.rings = Mth.clamp(ringCount, 1, 4);
      return this;
   }

   public GlassCrack grow(int ticks) {
      this.growTicks = Math.max(1, ticks);
      return this;
   }

   public GlassCrack hold(int ticks) {
      this.holdTicks = Math.max(0, ticks);
      return this;
   }

   public GlassCrack fade(int ticks) {
      this.fadeTicks = Math.max(1, ticks);
      return this;
   }

   public GlassCrack shatter(Vec3 push) {
      this.push = push;
      return this;
   }

   public GlassCrack refraction(float r) {
      this.refraction = r;
      return this;
   }

   public GlassCrack tint(int crackRgb) {
      this.rgb = crackRgb;
      return this;
   }

   public GlassCrack lines(float widthFraction) {
      this.lineWidth = widthFraction;
      return this;
   }

   public GlassCrack sheen(float s) {
      this.sheen = s;
      return this;
   }

   public GlassCrack alpha(float a) {
      this.alpha = a;
      return this;
   }

   public GlassCrack burst(float s) {
      this.burst = s;
      return this;
   }

   public GlassCrack billboard() {
      this.billboard = true;
      return this;
   }

   public GlassCrack follow(@Nullable Entity e) {
      if (e != null) {
         this.followId = e.getId();
         this.fox = this.cx - e.getX();
         this.foy = this.cy - e.getY();
         this.foz = this.cz - e.getZ();
      }

      return this;
   }

   public GlassCrack spawn() {
      VfxDirector.spawn(this);
      return this;
   }

   public void moveTo(double x, double y, double z) {
      this.cx = x;
      this.cy = y;
      this.cz = z;
   }

   public void radius(float r) {
      this.radius = Math.max(0.05F, r);
   }

   public void shatterNow(Vec3 push) {
      this.push = push;
      if (this.shattered < 0) {
         this.breakUp();
      }
   }

   public void release() {
      this.holdTicks = Math.min(this.holdTicks, Math.max(0, this.age - this.growTicks));
      this.push = null;
   }

   public boolean broken() {
      return this.shattered >= 0;
   }

   private void build() {
      this.built = true;
      RandomSource r = RandomSource.create(this.seed ^ 1013904242L);
      float base = r.nextFloat() * (float) (Math.PI * 2);
      this.ringR[0] = 0.0F;

      for (int j = 1; j <= this.rings; j++) {
         float f = ((float)j - 0.25F) / ((float)this.rings + 0.35F);
         this.ringR[j] = Mth.clamp(f + (r.nextFloat() - 0.5F) * 0.08F, 0.12F, 0.9F);
      }

      for (int i = 0; i < this.radials; i++) {
         float a = base + (float)i * (float) (Math.PI * 2) / (float)this.radials + (r.nextFloat() - 0.5F) * 0.7F * (float) (Math.PI * 2) / (float)this.radials;
         float ca = Mth.cos(a);
         float sa = Mth.sin(a);
         int k = 0;
         int o = i * 7;
         this.ru[o] = 0.0F;
         this.rv[o] = 0.0F;
         this.rd[o] = 0.0F;
         k++;

         for (int j = 1; j <= this.rings; j++) {
            float rr = this.ringR[j];
            float side = (r.nextFloat() - 0.5F) * 0.07F;
            this.ru[o + k] = ca * rr - sa * side;
            this.rv[o + k] = sa * rr + ca * side;
            k++;
         }

         float tip = 0.78F + r.nextFloat() * 0.22F;
         float last = this.ringR[this.rings];

         for (int s = 1; s <= 2; s++) {
            float rr = last + (tip - last) * (float)s / 2.0F;
            float side = (r.nextFloat() - 0.5F) * 0.09F;
            this.ru[o + k] = ca * rr - sa * side;
            this.rv[o + k] = sa * rr + ca * side;
            k++;
         }

         this.rn[i] = k;

         for (int q = 1; q < k; q++) {
            float du = this.ru[o + q] - this.ru[o + q - 1];
            float dv = this.rv[o + q] - this.rv[o + q - 1];
            this.rd[o + q] = this.rd[o + q - 1] + Mth.sqrt(du * du + dv * dv);
         }

         for (int j = 1; j <= this.rings; j++) {
            this.ringGap[i * 4 + j - 1] = j > 1 && r.nextFloat() < 0.16F;
            this.ringBow[i * 4 + j - 1] = 0.05F + r.nextFloat() * 0.1F;
         }

         int from = 1 + r.nextInt(Math.max(1, k - 2));
         float pu = this.ru[o + from];
         float pv = this.rv[o + from];
         float ba = a + (float)(r.nextBoolean() ? 1 : -1) * (0.4F + r.nextFloat() * 0.5F);
         float bl = 0.1F + r.nextFloat() * 0.18F;
         this.bu[i * 4] = pu;
         this.bv[i * 4] = pv;
         this.bu[i * 4 + 1] = pu + Mth.cos(ba) * bl * 0.55F + (r.nextFloat() - 0.5F) * 0.03F;
         this.bv[i * 4 + 1] = pv + Mth.sin(ba) * bl * 0.55F + (r.nextFloat() - 0.5F) * 0.03F;
         this.bu[i * 4 + 2] = pu + Mth.cos(ba + 0.2F) * bl;
         this.bv[i * 4 + 2] = pv + Mth.sin(ba + 0.2F) * bl;
         this.bd[i * 2] = this.rd[o + from];
         this.bd[i * 2 + 1] = bl;
      }

      for (int s = 0; s < this.radials * this.rings; s++) {
         float a = r.nextFloat() * (float) (Math.PI * 2);
         float m = 0.45F + r.nextFloat() * 0.55F;
         this.offX[s] = Mth.cos(a) * m;
         this.offY[s] = Mth.sin(a) * m;
         this.shine[s] = 0.015F + r.nextFloat() * 0.08F;
         float sx0 = r.nextFloat() - 0.5F;
         float sy0 = r.nextFloat() - 0.5F;
         float sz0 = r.nextFloat() - 0.5F;
         float sl = Mth.sqrt(sx0 * sx0 + sy0 * sy0 + sz0 * sz0) + 1.0E-4F;
         this.spinX[s] = sx0 / sl;
         this.spinY[s] = sy0 / sl;
         this.spinZ[s] = sz0 / sl;
         this.spinRate[s] = (0.12F + r.nextFloat() * 0.35F) * (float)(r.nextBoolean() ? 1 : -1);
      }
   }

   private float cornerU(int i, int j, int c) {
      int i2 = (i + 1) % this.radials;

      return switch (c) {
         case 0 -> this.ringU(i, j - 1);
         case 1 -> this.ringU(i2, j - 1);
         case 2 -> this.ringU(i2, j);
         default -> this.ringU(i, j);
      };
   }

   private float cornerV(int i, int j, int c) {
      int i2 = (i + 1) % this.radials;

      return switch (c) {
         case 0 -> this.ringV(i, j - 1);
         case 1 -> this.ringV(i2, j - 1);
         case 2 -> this.ringV(i2, j);
         default -> this.ringV(i, j);
      };
   }

   private float ringU(int i, int j) {
      return j == 0 ? 0.0F : this.ru[i * 7 + j];
   }

   private float ringV(int i, int j) {
      return j == 0 ? 0.0F : this.rv[i * 7 + j];
   }

   private void breakUp() {
      this.shattered = this.age;
      Vec3 p = this.push != null ? this.push : Vec3.ZERO;
      RandomSource r = RandomSource.create(this.seed ^ 2654435769L);
      if (!this.billboard || this.u.lengthSquared() < 1.0E-6F) {
         this.plane(null);
      }

      this.frozen = true;

      for (int i = 0; i < this.radials; i++) {
         for (int j = 1; j <= this.rings; j++) {
            int s = i * 4 + j - 1;
            float mu = 0.0F;
            float mv = 0.0F;

            for (int c = 0; c < 4; c++) {
               mu += this.cornerU(i, j, c) * 0.25F;
               mv += this.cornerV(i, j, c) * 0.25F;
            }

            float out = (0.04F + r.nextFloat() * 0.1F) * this.burst;
            float k = 0.6F + r.nextFloat() * 0.7F;
            this.vx[s] = (float)p.x * k + (this.u.x * mu + this.v.x * mv) * out * this.radius + this.n.x * (r.nextFloat() - 0.3F) * 0.08F;
            this.vy[s] = (float)p.y * k + (this.u.y * mu + this.v.y * mv) * out * this.radius + this.n.y * (r.nextFloat() - 0.3F) * 0.08F + 0.04F;
            this.vz[s] = (float)p.z * k + (this.u.z * mu + this.v.z * mv) * out * this.radius + this.n.z * (r.nextFloat() - 0.3F) * 0.08F;
         }
      }

      int glints = VfxSettings.count(Math.min(40.0F, 8.0F + this.radius * 10.0F));

      for (int g = 0; g < glints; g++) {
         float a = r.nextFloat() * (float) (Math.PI * 2);
         float d = r.nextFloat() * this.radius;
         double px = this.cx + (double)((this.u.x * Mth.cos(a) + this.v.x * Mth.sin(a)) * d);
         double py = this.cy + (double)((this.u.y * Mth.cos(a) + this.v.y * Mth.sin(a)) * d);
         double pz = this.cz + (double)((this.u.z * Mth.cos(a) + this.v.z * Mth.sin(a)) * d);
         VfxParticles.emit(
               g % 3 == 0 ? VfxPresets.GLASS_DUST : VfxPresets.GLASS_GLINT,
               px,
               py,
               pz,
               p.x * 0.7 + (double)(r.nextFloat() - 0.5F) * 0.2,
               p.y * 0.7 + (double)r.nextFloat() * 0.15,
               p.z * 0.7 + (double)(r.nextFloat() - 0.5F) * 0.2
            )
            .tint(this.rgb)
            .size(0.6F + this.radius * 0.15F);
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (!this.built) {
         this.build();
      }

      if (this.followId >= 0 && this.shattered < 0) {
         Entity e = level.getEntity(this.followId);
         if (e != null) {
            this.cx = e.getX() + this.fox;
            this.cy = e.getY() + this.foy;
            this.cz = e.getZ() + this.foz;
         }
      }

      if (this.shattered < 0) {
         if (this.push != null && this.age >= this.growTicks + this.holdTicks) {
            this.breakUp();
         } else if (this.push == null && this.age > this.growTicks + this.holdTicks + this.fadeTicks) {
            this.finish();
         }
      } else if (this.age - this.shattered > 22) {
         this.finish();
      }
   }

   private void plane(@Nullable VfxFrame f) {
      if (!this.frozen) {
         if (this.billboard && f != null) {
            this.u.set(f.left).negate();
            this.v.set(f.up);
            this.n.set(f.look).negate();
         } else {
            if (Math.abs(this.n.y) < 0.95F) {
               this.u.set(this.n.z, 0.0F, -this.n.x).normalize();
            } else {
               this.u.set(1.0F, 0.0F, 0.0F);
            }

            this.n.cross(this.u, this.v).normalize();
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      if (this.built && f.allowMesh()) {
         float t = (float)this.age + f.partial;
         this.plane(f);
         float a = this.alpha;
         if (this.shattered < 0 && this.push == null) {
            a *= 1.0F - Mth.clamp((t - (float)this.growTicks - (float)this.holdTicks) / (float)this.fadeTicks, 0.0F, 1.0F);
         }

         if (!(a <= 0.01F)) {
            if (this.shattered < 0) {
               float reach = Math.min(1.0F, t / (float)this.growTicks);
               reach = 1.0F - (1.0F - reach) * (1.0F - reach) * (1.0F - reach);
               this.renderShards(f, a * Math.min(1.0F, (reach - 0.3F) / 0.7F), -1.0F);
               this.renderLines(f, a, reach * 1.02F);
            } else {
               float s = t - (float)this.shattered;
               this.renderShards(f, a * Math.max(0.0F, 1.0F - s / 20.0F), s);
            }
         }
      }
   }

   private void renderLines(VfxFrame f, float a, float reach) {
      BufferBuilder b = f.mesh(MeshTex.LANCE);
      float rx = f.rx(this.cx);
      float ry = f.ry(this.cy);
      float rz = f.rz(this.cz);
      float w = Math.max(0.012F, this.lineWidth * this.radius);
      float flick = 0.9F + 0.1F * Mth.sin(((float)this.age + f.partial) * 1.7F);

      for (int i = 0; i < this.radials; i++) {
         int o = i * 7;
         int k = 0;

         for (int q = 0; q < this.rn[i]; q++) {
            float d = this.rd[o + q];
            if (d > reach) {
               if (q > 0) {
                  float d0 = this.rd[o + q - 1];
                  float e = (reach - d0) / Math.max(1.0E-4F, d - d0);
                  this.point(k++, rx, ry, rz, Mth.lerp(e, this.ru[o + q - 1], this.ru[o + q]), Mth.lerp(e, this.rv[o + q - 1], this.rv[o + q]));
               }
               break;
            }

            this.point(k++, rx, ry, rz, this.ru[o + q], this.rv[o + q]);
         }

         for (int q = 0; q < k; q++) {
            float along = (float)q / (float)Math.max(1, this.rn[i] - 1);
            this.sw[q] = w * (1.6F - 1.0F * along);
            this.sh[q] = -1.0F;
            this.sa[q] = a * flick * VfxWorld.nearFade(this.sx[q], this.sy[q], this.sz[q]);
         }

         this.strip(b, k, w * 3.5F, 0.22F, 0.8F);
         this.strip(b, k, 1.0F, 1.0F, 2.6F);
         if (this.bd[i * 2] < reach) {
            float e = Math.min(1.0F, (reach - this.bd[i * 2]) / Math.max(0.01F, this.bd[i * 2 + 1]));
            int m = 0;
            this.point(m++, rx, ry, rz, this.bu[i * 4], this.bv[i * 4]);
            this.point(
               m++,
               rx,
               ry,
               rz,
               Mth.lerp(Math.min(1.0F, e * 2.0F), this.bu[i * 4], this.bu[i * 4 + 1]),
               Mth.lerp(Math.min(1.0F, e * 2.0F), this.bv[i * 4], this.bv[i * 4 + 1])
            );
            if (e > 0.5F) {
               this.point(
                  m++,
                  rx,
                  ry,
                  rz,
                  Mth.lerp((e - 0.5F) * 2.0F, this.bu[i * 4 + 1], this.bu[i * 4 + 2]),
                  Mth.lerp((e - 0.5F) * 2.0F, this.bv[i * 4 + 1], this.bv[i * 4 + 2])
               );
            }

            for (int q = 0; q < m; q++) {
               this.sw[q] = w * 0.8F * (1.0F - 0.6F * (float)q / 2.0F);
               this.sh[q] = -1.0F;
               this.sa[q] = a * 0.85F * VfxWorld.nearFade(this.sx[q], this.sy[q], this.sz[q]);
            }

            this.strip(b, m, 1.0F, 1.0F, 2.2F);
         }

         int i2 = (i + 1) % this.radials;

         for (int j = 1; j <= this.rings; j++) {
            if (!this.ringGap[i * 4 + j - 1] && !(this.ringR[j] > reach)) {
               float e = Mth.clamp((reach - this.ringR[j]) / 0.18F, 0.0F, 1.0F);
               float u0 = this.ringU(i, j);
               float v0 = this.ringV(i, j);
               float u1 = this.ringU(i2, j);
               float v1 = this.ringV(i2, j);
               float bow = 1.0F - this.ringBow[i * 4 + j - 1];
               float mu = (u0 + u1) * 0.5F * bow;
               float mv = (v0 + v1) * 0.5F * bow;
               int m = 0;
               this.point(m++, rx, ry, rz, u0, v0);
               if (e < 0.5F) {
                  this.point(m++, rx, ry, rz, Mth.lerp(e * 2.0F, u0, mu), Mth.lerp(e * 2.0F, v0, mv));
               } else {
                  this.point(m++, rx, ry, rz, mu, mv);
                  this.point(m++, rx, ry, rz, Mth.lerp((e - 0.5F) * 2.0F, mu, u1), Mth.lerp((e - 0.5F) * 2.0F, mv, v1));
               }

               for (int q = 0; q < m; q++) {
                  this.sw[q] = w * (0.9F - 0.15F * (float)j);
                  this.sh[q] = -1.0F;
                  this.sa[q] = a * 0.9F * VfxWorld.nearFade(this.sx[q], this.sy[q], this.sz[q]);
               }

               this.strip(b, m, 2.6F, 0.2F, 0.7F);
               this.strip(b, m, 1.0F, 1.0F, 2.2F);
            }
         }
      }
   }

   private void point(int k, float rx, float ry, float rz, float pu, float pv) {
      this.sx[k] = rx + (this.u.x * pu + this.v.x * pv) * this.radius;
      this.sy[k] = ry + (this.u.y * pu + this.v.y * pv) * this.radius;
      this.sz[k] = rz + (this.u.z * pu + this.v.z * pv) * this.radius;
   }

   private void strip(BufferBuilder b, int k, float widthScale, float alphaScale, float emissive) {
      if (k >= 2) {
         if (widthScale == 1.0F && alphaScale == 1.0F) {
            MeshWriter.strip(b, k, this.sx, this.sy, this.sz, this.sw, this.sh, this.sa, 0.0F, 0.5F, this.rgb, 0, emissive);
         } else {
            for (int q = 0; q < k; q++) {
               this.tw[q] = this.sw[q] * widthScale;
               this.ta[q] = this.sa[q] * alphaScale;
            }

            MeshWriter.strip(b, k, this.sx, this.sy, this.sz, this.tw, this.sh, this.ta, 0.0F, 0.5F, this.rgb, 0, emissive);
         }
      }
   }

   private void renderShards(VfxFrame f, float a, float flight) {
      if (!(a <= 0.01F)) {
         BufferBuilder sheenBuf = this.sheen > 0.0F ? f.mesh(MeshTex.ATLAS) : null;
         BufferBuilder lines = flight >= 0.0F ? f.mesh(MeshTex.LANCE) : null;
         BufferBuilder d = f.distortion();
         float gu = (VfxSprites.GLOW.u0() + VfxSprites.GLOW.u1()) * 0.5F;
         float gv = (VfxSprites.GLOW.v0() + VfxSprites.GLOW.v1()) * 0.5F;
         float rx = f.rx(this.cx);
         float ry = f.ry(this.cy);
         float rz = f.rz(this.cz);
         float lx = -f.left.x;
         float ly = -f.left.y;
         float lz = -f.left.z;

         for (int i = 0; i < this.radials; i++) {
            for (int j = 1; j <= this.rings; j++) {
               int s = i * 4 + j - 1;
               float mu = 0.0F;
               float mv = 0.0F;

               for (int c = 0; c < 4; c++) {
                  mu += this.cornerU(i, j, c) * 0.25F;
                  mv += this.cornerV(i, j, c) * 0.25F;
               }

               float mx = rx + (this.u.x * mu + this.v.x * mv) * this.radius;
               float my = ry + (this.u.y * mu + this.v.y * mv) * this.radius;
               float mz = rz + (this.u.z * mu + this.v.z * mv) * this.radius;
               float ang = flight >= 0.0F ? this.spinRate[s] * flight : 0.0F;
               float ca = Mth.cos(ang);
               float sa0 = Mth.sin(ang);
               float tx = 0.0F;
               float ty = 0.0F;
               float tz = 0.0F;
               if (flight >= 0.0F) {
                  tx = this.vx[s] * flight;
                  ty = this.vy[s] * flight - 0.012F * flight * flight;
                  tz = this.vz[s] * flight;
               }

               for (int c = 0; c < 4; c++) {
                  float pu = this.cornerU(i, j, c);
                  float pv = this.cornerV(i, j, c);
                  pu = mu + (pu - mu) * 0.97F;
                  pv = mv + (pv - mv) * 0.97F;
                  float ox = (this.u.x * (pu - mu) + this.v.x * (pv - mv)) * this.radius;
                  float oy = (this.u.y * (pu - mu) + this.v.y * (pv - mv)) * this.radius;
                  float oz = (this.u.z * (pu - mu) + this.v.z * (pv - mv)) * this.radius;
                  if (flight >= 0.0F) {
                     float kx = this.spinX[s];
                     float ky = this.spinY[s];
                     float kz = this.spinZ[s];
                     float dot = kx * ox + ky * oy + kz * oz;
                     float cxp = ky * oz - kz * oy;
                     float cyp = kz * ox - kx * oz;
                     float czp = kx * oy - ky * ox;
                     float nx = ox * ca + cxp * sa0 + kx * dot * (1.0F - ca);
                     float ny = oy * ca + cyp * sa0 + ky * dot * (1.0F - ca);
                     float nz = oz * ca + czp * sa0 + kz * dot * (1.0F - ca);
                     ox = nx;
                     oy = ny;
                     oz = nz;
                  }

                  this.qx[c] = mx + tx + ox;
                  this.qy[c] = my + ty + oy;
                  this.qz[c] = mz + tz + oz;
               }

               float near = VfxWorld.nearFade(mx + tx, my + ty, mz + tz);
               if (sheenBuf != null) {
                  for (int c = 0; c < 4; c++) {
                     float sh = this.shine[s] * this.sheen * a * near * (c == 0 ? 1.8F : (c == 2 ? 0.4F : 1.0F));
                     MeshWriter.vertex(
                        sheenBuf,
                        this.qx[c],
                        this.qy[c],
                        this.qz[c],
                        MeshWriter.argb(this.rgb, sh),
                        gu,
                        gv,
                        -1.0F,
                        0,
                        0.4F,
                        0.0F,
                        0.0F,
                        this.n.x,
                        this.n.y,
                        this.n.z
                     );
                  }
               }

               if (d != null) {
                  float str = this.refraction * a * near;

                  for (int c = 0; c < 4; c++) {
                     float ex = this.qx[c] - mx - tx;
                     float ey = this.qy[c] - my - ty;
                     float ez = this.qz[c] - mz - tz;
                     float sxp = ex * lx + ey * ly + ez * lz;
                     float syp = ex * f.up.x + ey * f.up.y + ez * f.up.z;
                     float sl = Mth.sqrt(sxp * sxp + syp * syp) + 1.0E-4F;
                     MeshWriter.distortVertex(
                        d,
                        this.qx[c],
                        this.qy[c],
                        this.qz[c],
                        Mth.clamp(this.offX[s] + 0.3F * sxp / sl, -1.0F, 1.0F),
                        Mth.clamp(this.offY[s] + 0.3F * syp / sl, -1.0F, 1.0F),
                        str,
                        0.5F,
                        2
                     );
                  }
               }

               if (lines != null) {
                  for (int c = 0; c <= 4; c++) {
                     int cc = c % 4;
                     this.sx[c] = this.qx[cc];
                     this.sy[c] = this.qy[cc];
                     this.sz[c] = this.qz[cc];
                     this.sw[c] = Math.max(0.01F, this.lineWidth * this.radius * 0.7F);
                     this.sh[c] = -1.0F;
                     this.sa[c] = a * 0.8F * near;
                  }

                  MeshWriter.strip(lines, 5, this.sx, this.sy, this.sz, this.sw, this.sh, this.sa, 0.0F, 0.5F, this.rgb, 0, 2.0F);
               }
            }
         }
      }
   }

   @Override
   public double x() {
      return this.cx;
   }

   @Override
   public double y() {
      return this.cy;
   }

   @Override
   public double z() {
      return this.cz;
   }

   @Override
   public double cullDistance() {
      return (double)(96.0F + this.radius);
   }
}
