package com.devilfruits.client.vfx.particle;

import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.render.VfxFormat;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class VfxParticles {
   private static final double CULL = 9216.0;
   private static final VfxParticles.Handle HANDLE = new VfxParticles.Handle();
   private static final MutableBlockPos POS = new MutableBlockPos();
   private static int cap;
   private static int count;
   private static ParticleStyle[] style = new ParticleStyle[0];
   private static double[] x;
   private static double[] y;
   private static double[] z;
   private static double[] ox;
   private static double[] oy;
   private static double[] oz;
   private static float[] vx;
   private static float[] vy;
   private static float[] vz;
   private static float[] age;
   private static float[] life;
   private static float[] size;
   private static float[] rot;
   private static float[] spin;
   private static float[] heat;
   private static float[] alpha;
   private static float[] orbitX;
   private static float[] orbitZ;
   private static float[] orbitW;
   private static int[] tint;
   private static boolean[] flip;
   private static int[] order;
   private static float[] keys;
   private static float nearFade = 2.0F;
   private static float nearWanted = 2.0F;

   private static void ensure() {
      int want = VfxSettings.maxParticles();
      if (want != cap) {
         cap = want;
         count = 0;
         style = new ParticleStyle[cap];
         x = new double[cap];
         y = new double[cap];
         z = new double[cap];
         ox = new double[cap];
         oy = new double[cap];
         oz = new double[cap];
         vx = new float[cap];
         vy = new float[cap];
         vz = new float[cap];
         age = new float[cap];
         life = new float[cap];
         size = new float[cap];
         rot = new float[cap];
         spin = new float[cap];
         heat = new float[cap];
         alpha = new float[cap];
         orbitX = new float[cap];
         orbitZ = new float[cap];
         orbitW = new float[cap];
         tint = new int[cap];
         flip = new boolean[cap];
         order = new int[cap];
         keys = new float[cap];
      }
   }

   public static VfxParticles.Handle emit(ParticleStyle s, double px, double py, double pz, double velX, double velY, double velZ) {
      ensure();
      if (count < cap && (VfxSettings.soft() || !s.occluding)) {
         if (s.sprite.atlas() == 1) {
            VfxSprites.wantPack();
         }

         RandomSource r = VfxSettings.random();
         int i = count++;
         style[i] = s;
         x[i] = ox[i] = px;
         y[i] = oy[i] = py;
         z[i] = oz[i] = pz;
         vx[i] = (float)velX;
         vy[i] = (float)velY;
         vz[i] = (float)velZ;
         age[i] = 0.0F;
         life[i] = (float)(s.lifeMin + r.nextInt(s.lifeMax - s.lifeMin + 1));
         size[i] = 1.0F;
         rot[i] = r.nextFloat() * (float) (Math.PI * 2);
         spin[i] = (r.nextFloat() * 2.0F - 1.0F) * s.spin;
         heat[i] = 1.0F;
         alpha[i] = 1.0F;
         orbitW[i] = 0.0F;
         tint[i] = 16777215;
         flip[i] = r.nextBoolean();
         HANDLE.i = i;
         return HANDLE;
      } else {
         HANDLE.i = -1;
         return HANDLE;
      }
   }

   public static int count() {
      return count;
   }

   public static void clear() {
      count = 0;
   }

   public static void nearFade(float blocks) {
      nearWanted = Math.max(nearWanted, blocks);
   }

   public static void tick(ClientLevel level) {
      nearFade = nearWanted;
      nearWanted = 2.0F;

      for (int i = 0; i < count; i++) {
         ParticleStyle s = style[i];
         ox[i] = x[i];
         oy[i] = y[i];
         oz[i] = z[i];
         if (++age[i] >= life[i]) {
            remove(i--);
         } else if (s.mode == ParticleStyle.Mode.FLAT && s.gravity == 0.0F) {
            rot[i] = rot[i] + spin[i];
         } else {
            float keep = 1.0F - s.drag;
            vx[i] = vx[i] * keep;
            vy[i] = vy[i] * keep - s.gravity;
            vz[i] = vz[i] * keep;
            if (orbitW[i] != 0.0F) {
               float c = Mth.cos(orbitW[i]);
               float sn = Mth.sin(orbitW[i]);
               double ax = x[i] + (double)orbitX[i];
               double az = z[i] + (double)orbitZ[i];
               float dx = -orbitX[i];
               float dz = -orbitZ[i];
               float nx = dx * c - dz * sn;
               float nz = dx * sn + dz * c;
               x[i] = ax + (double)nx;
               z[i] = az + (double)nz;
               orbitX[i] = -nx;
               orbitZ[i] = -nz;
            }

            double nyPos = y[i] + (double)vy[i];
            if (s.collide && vy[i] < 0.0F) {
               POS.set(Mth.floor(x[i] + (double)vx[i]), Mth.floor(nyPos), Mth.floor(z[i] + (double)vz[i]));
               if (level.getBlockState(POS).isSolidRender(level, POS)) {
                  nyPos = (double)POS.getY() + 1.02;
                  vy[i] = -vy[i] * s.bounce;
                  vx[i] = vx[i] * 0.7F;
                  vz[i] = vz[i] * 0.7F;
                  if (vy[i] < 0.03F) {
                     vy[i] = 0.0F;
                  }
               }
            }

            x[i] = x[i] + (double)vx[i];
            y[i] = nyPos;
            z[i] = z[i] + (double)vz[i];
            rot[i] = rot[i] + spin[i];
         }
      }
   }

   private static void remove(int i) {
      int last = --count;
      if (i != last) {
         style[i] = style[last];
         x[i] = x[last];
         y[i] = y[last];
         z[i] = z[last];
         ox[i] = ox[last];
         oy[i] = oy[last];
         oz[i] = oz[last];
         vx[i] = vx[last];
         vy[i] = vy[last];
         vz[i] = vz[last];
         age[i] = age[last];
         life[i] = life[last];
         size[i] = size[last];
         rot[i] = rot[last];
         spin[i] = spin[last];
         heat[i] = heat[last];
         alpha[i] = alpha[last];
         orbitX[i] = orbitX[last];
         orbitZ[i] = orbitZ[last];
         orbitW[i] = orbitW[last];
         tint[i] = tint[last];
         flip[i] = flip[last];
      }

      style[last] = null;
   }

   public static void write(BufferBuilder b, VfxFrame f) {
      if (count != 0) {
         float p = f.partial;
         int n = 0;

         for (int i = 0; i < count; i++) {
            double px = Mth.lerp((double)p, ox[i], x[i]) - f.camX;
            double py = Mth.lerp((double)p, oy[i], y[i]) - f.camY;
            double pz = Mth.lerp((double)p, oz[i], z[i]) - f.camZ;
            double d2 = px * px + py * py + pz * pz;
            if (!(d2 > 9216.0)) {
               float reach = style[i].size1 * size[i] + 1.0F;
               if (!(px * (double)f.look.x + py * (double)f.look.y + pz * (double)f.look.z < (double)(-reach))) {
                  order[n] = i;
                  keys[n] = (float)d2;
                  n++;
               }
            }
         }

         sort(0, n - 1);

         for (int k = 0; k < n; k++) {
            quad(b, f, order[k], p);
         }
      }
   }

   private static void quad(BufferBuilder b, VfxFrame f, int i, float p) {
      ParticleStyle s = style[i];
      float t = Mth.clamp((age[i] + p) / life[i], 0.0F, 1.0F);
      float sz = (s.size0 + (s.size1 - s.size0) * (float)Math.pow((double)t, (double)s.sizePow)) * size[i] * 0.5F;
      float a = alpha[i];
      if (s.fadeIn > 0.0F && t < s.fadeIn) {
         a *= t / s.fadeIn;
      }

      if (s.fadeOut > 0.0F && t > 1.0F - s.fadeOut) {
         a *= (1.0F - t) / s.fadeOut;
      }

      if (!(a <= 0.004F) && !(sz <= 0.001F)) {
         float h = s.palette >= 0 ? (s.heat0 + (s.heat1 - s.heat0) * t) * heat[i] : -1.0F;
         VfxSprites.Sprite sp = s.sprite;
         int fa;
         int fb;
         float blend;
         if (sp.frames() <= 1) {
            fb = 0;
            fa = 0;
            blend = 0.0F;
         } else if (s.fps > 0.0F) {
            float fr = (age[i] + p) * s.fps / 20.0F;
            int whole = Mth.floor(fr);
            fa = Math.floorMod(whole, sp.frames());
            fb = (fa + 1) % sp.frames();
            blend = fr - (float)whole;
         } else {
            float fr = t * (float)(sp.frames() - 1);
            fa = Math.min(sp.frames() - 1, (int)fr);
            fb = Math.min(sp.frames() - 1, fa + 1);
            blend = fr - (float)fa;
         }

         if (s.stepped) {
            blend = 0.0F;
         }

         float ua0 = sp.frameU(fa);
         float va0 = sp.frameV(fa);
         float ub0 = sp.frameU(fb);
         float vb0 = sp.frameV(fb);
         float cu = sp.cellU();
         float cv = sp.cellV();
         float ua1 = ua0 + cu;
         float va1 = va0 + cv;
         float ub1 = ub0 + cu;
         float vb1 = vb0 + cv;
         if (flip[i] && s.mode != ParticleStyle.Mode.STRETCH) {
            float tmp = ua0;
            ua0 = ua1;
            ua1 = tmp;
            tmp = ub0;
            ub0 = ub1;
            ub1 = tmp;
         }

         float cx = (float)(Mth.lerp((double)p, ox[i], x[i]) - f.camX);
         float cy = (float)(Mth.lerp((double)p, oy[i], y[i]) - f.camY);
         float cz = (float)(Mth.lerp((double)p, oz[i], z[i]) - f.camZ);
         if (s.mode != ParticleStyle.Mode.FLAT) {
            float d = Mth.sqrt(cx * cx + cy * cy + cz * cz);
            a *= Mth.clamp((d - sz * 0.5F - nearFade * 0.25F) / nearFade, 0.0F, 1.0F);
            if (a <= 0.004F) {
               return;
            }
         }

         int argb = Mth.clamp((int)(a * 255.0F), 0, 255) << 24 | tint[i];
         int heatBits = VfxFormat.heat(h);
         int extra = VfxFormat.extra(Math.max(0, s.palette), s.emissive, sp.atlas());
         float soft = Math.min(1.0F, s.soft / 4.0F);
         float rx;
         float ry;
         float rz;
         float ux;
         float uy;
         float uz;
         if (s.mode == ParticleStyle.Mode.FLAT) {
            float c = Mth.cos(rot[i]) * sz;
            float sn = Mth.sin(rot[i]) * sz;
            rx = c;
            ry = 0.0F;
            rz = sn;
            ux = -sn;
            uy = 0.0F;
            uz = c;
         } else if (s.mode == ParticleStyle.Mode.STRETCH && (vx[i] != 0.0F || vy[i] != 0.0F || vz[i] != 0.0F)) {
            float lx = f.look.x;
            float ly = f.look.y;
            float lz = f.look.z;
            float d = vx[i] * lx + vy[i] * ly + vz[i] * lz;
            float ax = vx[i] - lx * d;
            float ay = vy[i] - ly * d;
            float az = vz[i] - lz * d;
            float speed = Mth.sqrt(ax * ax + ay * ay + az * az);
            if (speed < 1.0E-4F) {
               ax = -f.left.x;
               ay = -f.left.y;
               az = -f.left.z;
               speed = 1.0F;
            }

            float len = sz * (1.0F + s.stretch * Mth.sqrt(vx[i] * vx[i] + vy[i] * vy[i] + vz[i] * vz[i]) * 8.0F);
            rx = ax / speed * len;
            ry = ay / speed * len;
            rz = az / speed * len;
            float sx = ay * cz - az * cy;
            float sy = az * cx - ax * cz;
            float sz2 = ax * cy - ay * cx;
            float sl = Mth.sqrt(sx * sx + sy * sy + sz2 * sz2) + 1.0E-5F;
            float w = sz * 0.35F / sl;
            ux = sx * w;
            uy = sy * w;
            uz = sz2 * w;
            cx -= rx * 0.6F;
            cy -= ry * 0.6F;
            cz -= rz * 0.6F;
         } else {
            float c = Mth.cos(rot[i]);
            float sn = Mth.sin(rot[i]);
            float lx = -f.left.x;
            float ly = -f.left.y;
            float lz = -f.left.z;
            rx = (lx * c + f.up.x * sn) * sz;
            ry = (ly * c + f.up.y * sn) * sz;
            rz = (lz * c + f.up.z * sn) * sz;
            ux = (f.up.x * c - lx * sn) * sz;
            uy = (f.up.y * c - ly * sn) * sz;
            uz = (f.up.z * c - lz * sn) * sz;
         }

         v(b, cx - rx - ux, cy - ry - uy, cz - rz - uz, argb, ua0, va1, ub0, vb1, heatBits, extra, blend, soft, s.additive);
         v(b, cx + rx - ux, cy + ry - uy, cz + rz - uz, argb, ua1, va1, ub1, vb1, heatBits, extra, blend, soft, s.additive);
         v(b, cx + rx + ux, cy + ry + uy, cz + rz + uz, argb, ua1, va0, ub1, vb0, heatBits, extra, blend, soft, s.additive);
         v(b, cx - rx + ux, cy - ry + uy, cz - rz + uz, argb, ua0, va0, ub0, vb0, heatBits, extra, blend, soft, s.additive);
      }
   }

   private static void v(
      BufferBuilder b,
      float x,
      float y,
      float z,
      int argb,
      float ua,
      float va,
      float ub,
      float vb,
      int heatBits,
      int extra,
      float blend,
      float soft,
      float additive
   ) {
      b.addVertex(x, y, z).setColor(argb).setUv(ua, va).setUv1(VfxFormat.unit(ub), VfxFormat.unit(vb)).setUv2(heatBits, extra).setNormal(blend, soft, additive);
   }

   private static void sort(int lo, int hi) {
      while (lo < hi) {
         float pivot = keys[lo + hi >>> 1];
         int i = lo;
         int j = hi;

         while (i <= j) {
            while (keys[i] > pivot) {
               i++;
            }

            while (keys[j] < pivot) {
               j--;
            }

            if (i <= j) {
               float tk = keys[i];
               keys[i] = keys[j];
               keys[j] = tk;
               int to = order[i];
               order[i] = order[j];
               order[j] = to;
               i++;
               j--;
            }
         }

         if (j - lo < hi - i) {
            sort(lo, j);
            lo = i;
         } else {
            sort(i, hi);
            hi = j;
         }
      }
   }

   private VfxParticles() {
   }

   public static final class Handle {
      int i = -1;

      public boolean alive() {
         return this.i >= 0;
      }

      public VfxParticles.Handle size(float multiplier) {
         if (this.i >= 0) {
            VfxParticles.size[this.i] = VfxParticles.size[this.i] * multiplier;
         }

         return this;
      }

      public VfxParticles.Handle life(float multiplier) {
         if (this.i >= 0) {
            VfxParticles.life[this.i] = Math.max(2.0F, VfxParticles.life[this.i] * multiplier);
         }

         return this;
      }

      public VfxParticles.Handle heat(float multiplier) {
         if (this.i >= 0) {
            VfxParticles.heat[this.i] = VfxParticles.heat[this.i] * multiplier;
         }

         return this;
      }

      public VfxParticles.Handle alpha(float multiplier) {
         if (this.i >= 0) {
            VfxParticles.alpha[this.i] = VfxParticles.alpha[this.i] * multiplier;
         }

         return this;
      }

      public VfxParticles.Handle tint(int rgb) {
         if (this.i >= 0) {
            VfxParticles.tint[this.i] = rgb;
         }

         return this;
      }

      public VfxParticles.Handle rotation(float radians) {
         if (this.i >= 0) {
            VfxParticles.rot[this.i] = radians;
            VfxParticles.spin[this.i] = 0.0F;
         }

         return this;
      }

      public VfxParticles.Handle unflipped() {
         if (this.i >= 0) {
            VfxParticles.flip[this.i] = false;
         }

         return this;
      }

      public VfxParticles.Handle orbit(double cx, double cz, float radPerTick) {
         if (this.i >= 0) {
            VfxParticles.orbitX[this.i] = (float)(cx - VfxParticles.x[this.i]);
            VfxParticles.orbitZ[this.i] = (float)(cz - VfxParticles.z[this.i]);
            VfxParticles.orbitW[this.i] = radPerTick;
         }

         return this;
      }
   }
}
