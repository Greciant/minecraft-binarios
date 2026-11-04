package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ChainLinks {
   public static final int HEAD_NONE = 0;
   public static final int HEAD_WEIGHT = 1;
   public static final int HEAD_HOOK = 2;
   public static final int HEAD_SPIKE = 3;
   public static final float LINK = 0.4F;
   private static final float MIN_PITCH = 0.02F;
   private static final int MAX_LINKS = 1024;
   private static final float GLOW_INFLATE = 1.45F;
   private static final float PULSE_INFLATE = 1.9F;
   private static final float WIRE = 0.095F;
   private static final float LINK_HALF_LEN = 0.7F;
   private static final float LINK_HALF_WIDE = 0.35F;
   private static final int END_CHORDS = 4;
   private static final float[] LOOP = buildLoop();
   private static final float TWIST = 0.1F;
   private static final float WOBBLE = 0.06F;
   private static final float BODY_SHARE = 0.16F;
   private static final float GLOW_SHARE = 0.3F;
   private static final float CHARGE_TINT = 0.18F;
   private static final float PULSE_TINT = 0.4F;
   private static final float LIGHT_FLOOR = 0.55F;
   private static final float CHARGE_EDGE = 0.14F;
   private static final float SHARD_FLY = 1.6F;
   private static final float SHARD_DROP = 2.0F;
   private static final float KEY_X = 0.52F;
   private static final float KEY_Y = 0.72F;
   private static final float KEY_Z = -0.46F;
   private static final float FACE_AMBIENT = 0.62F;
   private static final float FACE_DIFFUSE = 0.48F;
   private static final float FACE_SHADOW = 0.12F;
   private static final float FACE_COOL = 0.35F;
   public static final int IRON = 5659494;
   public static final int IRON_HIGHLIGHT = 10266814;
   public static final int IRON_CHARGED = 8279476;
   public static final int VIOLET_DIM = 6171526;
   public static final int VIOLET = 11544319;
   public static final int VIOLET_PALE = 14257151;
   public static final int VIOLET_CAP = 15244287;

   private ChainLinks() {
   }

   public static void draw(PoseStack ps, BufferSource buffers, Vec3 cam, List<ChainLinks.Chain> chains, float time) {
      if (!chains.isEmpty()) {
         ps.pushPose();
         ps.translate(-cam.x, -cam.y, -cam.z);
         Pose pose = ps.last();
         VertexConsumer solid = FxLayers.begin(buffers, FxLayer.SOLID);

         for (ChainLinks.Chain c : chains) {
            emit(pose, solid, c, time, ChainLinks.Pass.SOLID);
         }

         FxLayers.end(buffers, FxLayer.SOLID);
         VertexConsumer body = FxLayers.begin(buffers, FxLayer.PAINT);

         for (ChainLinks.Chain c : chains) {
            emit(pose, body, c, time, ChainLinks.Pass.BODY);
         }

         FxLayers.end(buffers, FxLayer.PAINT);
         VertexConsumer glow = FxLayers.begin(buffers, FxLayer.LIGHT);

         for (ChainLinks.Chain c : chains) {
            emit(pose, glow, c, time, ChainLinks.Pass.GLOW);
         }

         FxLayers.end(buffers, FxLayer.LIGHT);
         ps.popPose();
      }
   }

   private static float pulseAt(ChainLinks.Chain c, float t) {
      if (c.pulse < 0.0F) {
         return 0.0F;
      } else {
         float d = Math.abs(t - c.pulse) / Math.max(0.01F, c.pulseWidth);
         if (d >= 1.0F) {
            return 0.0F;
         } else {
            float w = 1.0F - d;
            return w * w;
         }
      }
   }

   private static float frontAt(ChainLinks.Chain c, float t) {
      return c.chargeFront >= 1.5F ? 1.0F : Mth.clamp((c.chargeFront - t) / 0.14F, 0.0F, 1.0F);
   }

   private static void emit(Pose pose, VertexConsumer vc, ChainLinks.Chain c, float time, ChainLinks.Pass pass) {
      float ax = (float)c.ax;
      float ay = (float)c.ay;
      float az = (float)c.az;
      float dx = (float)(c.bx - c.ax);
      float dy = (float)(c.by - c.ay);
      float dz = (float)(c.bz - c.az);
      float dist = Mth.sqrt(dx * dx + dy * dy + dz * dz);
      if (!(dist < 0.001F) && !(c.growth <= 0.0F)) {
         float swayPhase = time * 0.085F + (float)c.seed * 0.71F;
         float s = c.sag * (1.0F + c.settle) * (c.sway > 0.0F ? 1.0F + 0.12F * Mth.sin(swayPhase * 1.37F) : 1.0F);
         float arcLen = dist + 8.0F * s * s / (3.0F * dist);
         float pitchNominal = Math.max(0.02F, 0.4F * c.thickness);
         int n = Mth.clamp(Math.round(arcLen / pitchNominal), 1, 1024);
         float pitch = arcLen / (float)n;
         int shown = c.growth >= 1.0F ? n : Math.min(n, (int)Math.ceil((double)((float)n * c.growth)));
         int first = c.growFromB ? n - shown : 0;
         int last = c.growFromB ? n : shown;
         float shrink = 1.0F - c.shatter;
         float fly = 1.6F * AnimEnvelopes.easeOutCubic(c.shatter);
         float drop = 2.0F * c.shatter * c.shatter;
         float swayAmt = c.sway * Mth.sin(swayPhase);
         float hx = -dz;
         float hl = Mth.sqrt(hx * hx + dx * dx);
         float hz;
         if (hl < 0.001F) {
            hx = 1.0F;
            hz = 0.0F;
         } else {
            hx /= hl;
            hz = dx / hl;
         }

         float brightness = 0.55F + 0.45F * Mth.clamp(c.brightness, 0.0F, 1.0F);
         float seedRoll = noise(c.seed, 0, 19) * (float) (Math.PI * 2);
         float waverPhase = noise(c.seed, 0, 23) * (float) (Math.PI * 2);

         for (int i = first; i < last; i++) {
            float t = ((float)i + 0.5F) / (float)n;
            float bell = 4.0F * t * (1.0F - t);
            float px = ax + dx * t + hx * swayAmt * bell;
            float py = ay + dy * t - s * bell;
            float pz = az + dz * t + hz * swayAmt * bell;
            float ty = dy - 4.0F * s * (1.0F - 2.0F * t);
            float tl = Mth.sqrt(dx * dx + ty * ty + dz * dz);
            float tx;
            float tz;
            if (tl < 1.0E-4F) {
               tx = dx / dist;
               ty = dy / dist;
               tz = dz / dist;
            } else {
               tx = dx / tl;
               ty /= tl;
               tz = dz / tl;
            }

            float ux = -tz;
            float ul = Mth.sqrt(ux * ux + tx * tx);
            float uy = 0.0F;
            float uz;
            if (ul < 0.001F) {
               ux = 1.0F;
               uz = 0.0F;
            } else {
               ux /= ul;
               uz = tx / ul;
            }

            float vx = ty * uz - tz * uy;
            float vy = tz * ux - tx * uz;
            float vz = tx * uy - ty * ux;
            float roll = seedRoll + (float)i * 0.1F + (noise(c.seed, i, 17) - 0.5F) * 2.0F * 0.06F;
            float cr = Mth.cos(roll);
            float sr = Mth.sin(roll);
            float rux = ux * cr + vx * sr;
            float ruy = uy * cr + vy * sr;
            float ruz = uz * cr + vz * sr;
            float rvx = vx * cr - ux * sr;
            float rvy = vy * cr - uy * sr;
            float rvz = vz * cr - uz * sr;
            if (c.waver > 0.0F) {
               float w1 = c.waver * bell * Mth.sin((float)i * 0.9F + waverPhase);
               float w2 = c.waver * bell * Mth.sin((float)i * 0.7F + waverPhase * 1.7F + 1.3F);
               px += rux * w1 + rvx * w2;
               py += ruy * w1 + rvy * w2;
               pz += ruz * w1 + rvz * w2;
            }

            if (c.tremble > 0.0F) {
               float ph = time * 1.9F + (float)i * 1.31F + (float)c.seed * 0.37F;
               float a1 = c.tremble * Mth.sin(ph);
               float a2 = c.tremble * Mth.cos(ph * 1.27F + (float)i * 0.7F);
               px += rux * a1 + rvx * a2;
               py += ruy * a1 + rvy * a2;
               pz += ruz * a1 + rvz * a2;
            }

            if (c.shatter > 0.0F) {
               float sx = noise(c.seed, i, 311) - 0.5F;
               float sy = noise(c.seed, i, 312) - 0.2F;
               float sz = noise(c.seed, i, 313) - 0.5F;
               float sl = Math.max(0.05F, Mth.sqrt(sx * sx + sy * sy + sz * sz));
               px += sx / sl * fly;
               py += sy / sl * fly - drop;
               pz += sz / sl * fly;
            }

            boolean even = (i & 1) == 0;
            float wx = even ? rux : rvx;
            float wy = even ? ruy : rvy;
            float wz = even ? ruz : rvz;
            float qx = even ? rvx : rux;
            float qy = even ? rvy : ruy;
            float qz = even ? rvz : ruz;
            float pw = pulseAt(c, t);
            float charge = c.charge * frontAt(c, t);
            float light = Mth.clamp(charge * 0.65F + pw, 0.0F, 1.0F);
            switch (pass) {
               case SOLID:
                  int rgb = scaleRgb(lerpRgb(5659494, 8279476, Mth.clamp(charge * 0.18F + pw * 0.4F, 0.0F, 1.0F)), brightness);
                  link(pose, vc, px, py, pz, tx, ty, tz, wx, wy, wz, qx, qy, qz, pitch * shrink, 1.0F, 255, rgb, true);
                  break;
               case BODY:
                  int axx = Math.round(255.0F * light * 0.16F);
                  if (axx >= 4) {
                     int rgbx = lerpRgb(6171526, 11544319, light);
                     link(pose, vc, px, py, pz, tx, ty, tz, wx, wy, wz, qx, qy, qz, pitch * shrink, 1.45F, axx, rgbx, false);
                  }
                  break;
               case GLOW:
                  int a = Math.round(255.0F * light * 0.3F);
                  if (a >= 4) {
                     int rgbx = lerpRgb(6171526, 11544319, light);
                     link(pose, vc, px, py, pz, tx, ty, tz, wx, wy, wz, qx, qy, qz, pitch * shrink, 1.45F, a, rgbx, false);
                  }

                  if (pw > 0.05F) {
                     link(pose, vc, px, py, pz, tx, ty, tz, wx, wy, wz, qx, qy, qz, pitch * shrink, 1.9F, Math.round(150.0F * pw), 14257151, false);
                  }
            }
         }

         if (c.head != 0 && shrink > 0.02F && (!c.growFromB || shown > 0)) {
            head(pose, vc, c, ax + dx, ay + dy, az + dz, dx, dy + 4.0F * s, dz, hx, hz, seedRoll, shrink, pass, brightness);
         }

         if (c.flash > 0.01F && pass != ChainLinks.Pass.SOLID) {
            float axx = c.flash * (pass == ChainLinks.Pass.BODY ? 0.45F : 0.95F);
            star(pose, vc, ax + dx, ay + dy, az + dz, 0.22F + 0.5F * c.flash, Math.round(255.0F * axx), 15244287);
         }
      }
   }

   private static float[] buildLoop() {
      float r = 0.255F;
      float s = 0.35F;
      int n = 10;
      float[] pts = new float[(n + 1) * 2];
      int p = 0;
      pts[p++] = -s;
      pts[p++] = r;

      for (int i = 0; i <= 4; i++) {
         double a = (Math.PI / 2) - Math.PI * (double)i / 4.0;
         pts[p++] = s + (float)(Math.cos(a) * (double)r);
         pts[p++] = (float)(Math.sin(a) * (double)r);
      }

      for (int i = 0; i <= 4; i++) {
         double a = (-Math.PI / 2) - Math.PI * (double)i / 4.0;
         pts[p++] = -s + (float)(Math.cos(a) * (double)r);
         pts[p++] = (float)(Math.sin(a) * (double)r);
      }

      return pts;
   }

   private static void link(
      Pose pose,
      VertexConsumer vc,
      float px,
      float py,
      float pz,
      float tx,
      float ty,
      float tz,
      float wx,
      float wy,
      float wz,
      float qx,
      float qy,
      float qz,
      float k,
      float section,
      int alpha,
      int rgb,
      boolean shaded
   ) {
      float h = 0.095F * k * section;
      int segs = LOOP.length / 2 - 1;

      for (int i = 0; i < segs; i++) {
         float r0 = LOOP[i * 2] * k;
         float w0 = LOOP[i * 2 + 1] * k;
         float r1 = LOOP[i * 2 + 2] * k;
         float w1 = LOOP[i * 2 + 3] * k;
         float dr = r1 - r0;
         float dw = w1 - w0;
         float len = Mth.sqrt(dr * dr + dw * dw);
         if (!(len < 1.0E-5F)) {
            float nr = dr / len;
            float nw = dw / len;
            float half = len * 0.5F + h * 0.85F;
            float mr = (r0 + r1) * 0.5F;
            float mw = (w0 + w1) * 0.5F;
            float cx = px + tx * mr + wx * mw;
            float cy = py + ty * mr + wy * mw;
            float cz = pz + tz * mr + wz * mw;
            float axx = (tx * nr + wx * nw) * half;
            float axy = (ty * nr + wy * nw) * half;
            float axz = (tz * nr + wz * nw) * half;
            float bxx = (-tx * nw + wx * nr) * h;
            float bxy = (-ty * nw + wy * nr) * h;
            float bxz = (-tz * nw + wz * nr) * h;
            box(pose, vc, cx, cy, cz, axx, axy, axz, bxx, bxy, bxz, qx * h, qy * h, qz * h, alpha, rgb, shaded);
         }
      }
   }

   private static void head(
      Pose pose,
      VertexConsumer vc,
      ChainLinks.Chain c,
      float bx,
      float by,
      float bz,
      float tx,
      float ty,
      float tz,
      float hx,
      float hz,
      float roll,
      float shrink,
      ChainLinks.Pass pass,
      float brightness
   ) {
      float tl = Mth.sqrt(tx * tx + ty * ty + tz * tz);
      if (!(tl < 1.0E-4F)) {
         tx /= tl;
         ty /= tl;
         tz /= tl;
         float buy = 0.0F;
         float bvx = ty * hz - tz * buy;
         float bvy = tz * hx - tx * hz;
         float bvz = tx * buy - ty * hx;
         float cr = Mth.cos(roll);
         float sr = Mth.sin(roll);
         float ux = hx * cr + bvx * sr;
         float uy = buy * cr + bvy * sr;
         float uz = hz * cr + bvz * sr;
         float vx = bvx * cr - hx * sr;
         float vy = bvy * cr - buy * sr;
         float vz = bvz * cr - hz * sr;
         float k = c.thickness * shrink;
         boolean solid = pass == ChainLinks.Pass.SOLID;
         float section = solid ? 1.0F : 1.45F;
         float e = k * section;
         float charge = c.charge * frontAt(c, 1.0F);
         float light = Mth.clamp(charge * 0.65F + Math.max(0.0F, c.flash), 0.0F, 1.0F);
         int alpha;
         int rgb;
         switch (pass) {
            case SOLID:
               alpha = 255;
               rgb = scaleRgb(lerpRgb(5659494, 8279476, charge * 0.18F), brightness * 0.92F);
               break;
            case BODY:
               alpha = Math.round(255.0F * light * 0.16F);
               rgb = lerpRgb(6171526, 11544319, light);
               break;
            default:
               alpha = Math.round(255.0F * light * 0.3F);
               rgb = lerpRgb(6171526, 11544319, light);
         }

         if (!solid && c.ember > 0.01F && c.head != 3) {
            float tip = c.head == 1 ? 0.52F : 0.3F;
            star(
               pose,
               vc,
               bx + tx * tip * k,
               by + ty * tip * k,
               bz + tz * tip * k,
               0.1F * k * (0.7F + 0.3F * c.ember),
               Math.round(255.0F * c.ember * (pass == ChainLinks.Pass.BODY ? 0.35F : 0.85F)),
               14257151
            );
         }

         if (alpha >= 4) {
            int collar = scaleRgb(rgb, 0.72F);
            switch (c.head) {
               case 1:
                  if (solid) {
                     box(
                        pose,
                        vc,
                        bx - tx * 0.05F * k,
                        by - ty * 0.05F * k,
                        bz - tz * 0.05F * k,
                        tx * 0.05F * e,
                        ty * 0.05F * e,
                        tz * 0.05F * e,
                        ux * 0.21F * e,
                        uy * 0.21F * e,
                        uz * 0.21F * e,
                        vx * 0.21F * e,
                        vy * 0.21F * e,
                        vz * 0.21F * e,
                        alpha,
                        collar,
                        true
                     );
                  }

                  taper(pose, vc, bx, by, bz, tx, ty, tz, ux, uy, uz, vx, vy, vz, 0.06F * k, 0.11F * e, 0.17F * e, alpha, rgb, solid);
                  box(
                     pose,
                     vc,
                     bx + tx * 0.24F * k,
                     by + ty * 0.24F * k,
                     bz + tz * 0.24F * k,
                     tx * 0.18F * e,
                     ty * 0.18F * e,
                     tz * 0.18F * e,
                     ux * 0.17F * e,
                     uy * 0.17F * e,
                     uz * 0.17F * e,
                     vx * 0.17F * e,
                     vy * 0.17F * e,
                     vz * 0.17F * e,
                     alpha,
                     rgb,
                     solid
                  );
                  taper(
                     pose,
                     vc,
                     bx + tx * 0.42F * k,
                     by + ty * 0.42F * k,
                     bz + tz * 0.42F * k,
                     tx,
                     ty,
                     tz,
                     ux,
                     uy,
                     uz,
                     vx,
                     vy,
                     vz,
                     0.09F * k,
                     0.17F * e,
                     0.11F * e,
                     alpha,
                     rgb,
                     solid
                  );
                  break;
               case 2:
                  if (solid) {
                     box(
                        pose,
                        vc,
                        bx - tx * 0.04F * k,
                        by - ty * 0.04F * k,
                        bz - tz * 0.04F * k,
                        tx * 0.04F * e,
                        ty * 0.04F * e,
                        tz * 0.04F * e,
                        ux * 0.12F * e,
                        uy * 0.12F * e,
                        uz * 0.12F * e,
                        vx * 0.12F * e,
                        vy * 0.12F * e,
                        vz * 0.12F * e,
                        alpha,
                        collar,
                        true
                     );
                  }

                  box(
                     pose,
                     vc,
                     bx + tx * 0.11F * k,
                     by + ty * 0.11F * k,
                     bz + tz * 0.11F * k,
                     tx * 0.13F * e,
                     ty * 0.13F * e,
                     tz * 0.13F * e,
                     ux * 0.055F * e,
                     uy * 0.055F * e,
                     uz * 0.055F * e,
                     vx * 0.055F * e,
                     vy * 0.055F * e,
                     vz * 0.055F * e,
                     alpha,
                     rgb,
                     solid
                  );

                  for (int side = -1; side <= 1; side += 2) {
                     float p0x = bx + tx * 0.22F * k + ux * 0.05F * k * (float)side;
                     float p0y = by + ty * 0.22F * k + uy * 0.05F * k * (float)side;
                     float p0z = bz + tz * 0.22F * k + uz * 0.05F * k * (float)side;
                     float p1x = bx + tx * 0.33F * k + ux * 0.19F * k * (float)side;
                     float p1y = by + ty * 0.33F * k + uy * 0.19F * k * (float)side;
                     float p1z = bz + tz * 0.33F * k + uz * 0.19F * k * (float)side;
                     float p2x = bx + tx * 0.28F * k + ux * 0.33F * k * (float)side;
                     float p2y = by + ty * 0.28F * k + uy * 0.33F * k * (float)side;
                     float p2z = bz + tz * 0.28F * k + uz * 0.33F * k * (float)side;
                     float p3x = bx + tx * 0.1F * k + ux * 0.37F * k * (float)side;
                     float p3y = by + ty * 0.1F * k + uy * 0.37F * k * (float)side;
                     float p3z = bz + tz * 0.1F * k + uz * 0.37F * k * (float)side;
                     bar(pose, vc, p0x, p0y, p0z, p1x, p1y, p1z, vx, vy, vz, 0.055F * e, alpha, rgb, solid);
                     bar(pose, vc, p1x, p1y, p1z, p2x, p2y, p2z, vx, vy, vz, 0.045F * e, alpha, rgb, solid);
                     bar(pose, vc, p2x, p2y, p2z, p3x, p3y, p3z, vx, vy, vz, 0.032F * e, alpha, rgb, solid);
                  }
                  break;
               case 3:
                  float dx = (ux + vx) * 0.70710677F;
                  float dy = (uy + vy) * 0.70710677F;
                  float dz = (uz + vz) * 0.70710677F;
                  float fx = (vx - ux) * 0.70710677F;
                  float fy = (vy - uy) * 0.70710677F;
                  float fz = (vz - uz) * 0.70710677F;
                  if (solid) {
                     box(
                        pose,
                        vc,
                        bx - tx * 0.03F * k,
                        by - ty * 0.03F * k,
                        bz - tz * 0.03F * k,
                        tx * 0.07F * e,
                        ty * 0.07F * e,
                        tz * 0.07F * e,
                        ux * 0.23F * e,
                        uy * 0.23F * e,
                        uz * 0.23F * e,
                        vx * 0.23F * e,
                        vy * 0.23F * e,
                        vz * 0.23F * e,
                        alpha,
                        collar,
                        true
                     );
                     box(
                        pose,
                        vc,
                        bx + tx * 0.09F * k,
                        by + ty * 0.09F * k,
                        bz + tz * 0.09F * k,
                        tx * 0.05F * e,
                        ty * 0.05F * e,
                        tz * 0.05F * e,
                        ux * 0.19F * e,
                        uy * 0.19F * e,
                        uz * 0.19F * e,
                        vx * 0.19F * e,
                        vy * 0.19F * e,
                        vz * 0.19F * e,
                        alpha,
                        scaleRgb(rgb, 0.84F),
                        true
                     );
                  }

                  float sx = bx + tx * 0.14F * k;
                  float sy = by + ty * 0.14F * k;
                  float sz = bz + tz * 0.14F * k;
                  taper(pose, vc, sx, sy, sz, tx, ty, tz, dx, dy, dz, fx, fy, fz, 0.22F * k, 0.17F * e, 0.12F * e, alpha, rgb, solid);
                  float px = sx + tx * 0.22F * k;
                  float py = sy + ty * 0.22F * k;
                  float pz = sz + tz * 0.22F * k;
                  taper(pose, vc, px, py, pz, tx, ty, tz, dx, dy, dz, fx, fy, fz, 0.73F * k, 0.12F * e, 0.012F * e, alpha, rgb, solid);
                  if (!solid && light > 0.08F) {
                     float ex = px + tx * 0.7F * k;
                     float ey = py + ty * 0.7F * k;
                     float ez = pz + tz * 0.7F * k;
                     star(
                        pose,
                        vc,
                        ex,
                        ey,
                        ez,
                        0.11F * k * (0.7F + 0.3F * light),
                        Math.round(255.0F * light * (pass == ChainLinks.Pass.BODY ? 0.35F : 0.85F)),
                        14257151
                     );
                  }
            }
         }
      }
   }

   private static void taper(
      Pose pose,
      VertexConsumer vc,
      float bx,
      float by,
      float bz,
      float tx,
      float ty,
      float tz,
      float ux,
      float uy,
      float uz,
      float vx,
      float vy,
      float vz,
      float h,
      float r0,
      float r1,
      int alpha,
      int rgb,
      boolean shaded
   ) {
      float cx = bx + tx * h;
      float cy = by + ty * h;
      float cz = bz + tz * h;
      float[] bc = new float[]{
         bx + (ux + vx) * r0,
         by + (uy + vy) * r0,
         bz + (uz + vz) * r0,
         bx + (-ux + vx) * r0,
         by + (-uy + vy) * r0,
         bz + (-uz + vz) * r0,
         bx + (-ux - vx) * r0,
         by + (-uy - vy) * r0,
         bz + (-uz - vz) * r0,
         bx + (ux - vx) * r0,
         by + (uy - vy) * r0,
         bz + (uz - vz) * r0
      };
      float[] tc = new float[]{
         cx + (ux + vx) * r1,
         cy + (uy + vy) * r1,
         cz + (uz + vz) * r1,
         cx + (-ux + vx) * r1,
         cy + (-uy + vy) * r1,
         cz + (-uz + vz) * r1,
         cx + (-ux - vx) * r1,
         cy + (-uy - vy) * r1,
         cz + (-uz - vz) * r1,
         cx + (ux - vx) * r1,
         cy + (uy - vy) * r1,
         cz + (uz - vz) * r1
      };
      float[] sd = new float[]{vx, vy, vz, -ux, -uy, -uz, -vx, -vy, -vz, ux, uy, uz};
      float slope = r0 - r1;

      for (int i = 0; i < 4; i++) {
         int a = i * 3;
         int b = (i + 1 & 3) * 3;
         float nx = sd[a] * h + tx * slope;
         float ny = sd[a + 1] * h + ty * slope;
         float nz = sd[a + 2] * h + tz * slope;
         int col = shaded ? shadeFace(rgb, nx, ny, nz) : rgb;
         quad(
            pose,
            vc,
            bc[a],
            bc[a + 1],
            bc[a + 2],
            bc[b],
            bc[b + 1],
            bc[b + 2],
            tc[b],
            tc[b + 1],
            tc[b + 2],
            tc[a],
            tc[a + 1],
            tc[a + 2],
            alpha,
            alpha,
            alpha,
            alpha,
            col
         );
      }

      int cap = shaded ? shadeFace(rgb, -tx, -ty, -tz) : rgb;
      quad(pose, vc, bc[0], bc[1], bc[2], bc[3], bc[4], bc[5], bc[6], bc[7], bc[8], bc[9], bc[10], bc[11], alpha, alpha, alpha, alpha, cap);
   }

   private static void bar(
      Pose pose,
      VertexConsumer vc,
      float x0,
      float y0,
      float z0,
      float x1,
      float y1,
      float z1,
      float nx,
      float ny,
      float nz,
      float r,
      int alpha,
      int rgb,
      boolean shaded
   ) {
      float dx = x1 - x0;
      float dy = y1 - y0;
      float dz = z1 - z0;
      float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
      if (!(len < 1.0E-5F)) {
         float ax = dx / len;
         float ay = dy / len;
         float az = dz / len;
         float half = len * 0.5F + r * 0.8F;
         float px = ny * az - nz * ay;
         float py = nz * ax - nx * az;
         float pz = nx * ay - ny * ax;
         box(
            pose,
            vc,
            (x0 + x1) * 0.5F,
            (y0 + y1) * 0.5F,
            (z0 + z1) * 0.5F,
            ax * half,
            ay * half,
            az * half,
            px * r,
            py * r,
            pz * r,
            nx * r,
            ny * r,
            nz * r,
            alpha,
            rgb,
            shaded
         );
      }
   }

   private static void star(Pose pose, VertexConsumer vc, float x, float y, float z, float size, int alpha, int rgb) {
      float thin = size * 0.08F;
      box(pose, vc, x, y, z, size, 0.0F, 0.0F, 0.0F, thin, 0.0F, 0.0F, 0.0F, thin, alpha, rgb, false);
      box(pose, vc, x, y, z, 0.0F, size, 0.0F, thin, 0.0F, 0.0F, 0.0F, 0.0F, thin, alpha, rgb, false);
      box(pose, vc, x, y, z, 0.0F, 0.0F, size, thin, 0.0F, 0.0F, 0.0F, thin, 0.0F, alpha, rgb, false);
      float d = size * 0.6F;
      box(pose, vc, x, y, z, d, d, 0.0F, -d * 0.5F, d * 0.5F, 0.0F, 0.0F, 0.0F, thin, alpha, rgb, false);
      box(pose, vc, x, y, z, 0.0F, d, d, 0.0F, -d * 0.5F, d * 0.5F, thin, 0.0F, 0.0F, alpha, rgb, false);
   }

   public static void ribbon(Pose pose, VertexConsumer vc, double[] pts, int count, float halfHeight, int alpha, int rgb) {
      if (count >= 2) {
         for (int i = 0; i + 1 < count; i++) {
            float f0 = (float)i / (float)(count - 1);
            float f1 = (float)(i + 1) / (float)(count - 1);
            int a0 = Math.round((float)alpha * f0 * f0);
            int a1 = Math.round((float)alpha * f1 * f1);
            float x0 = (float)pts[i * 3];
            float y0 = (float)pts[i * 3 + 1];
            float z0 = (float)pts[i * 3 + 2];
            float x1 = (float)pts[i * 3 + 3];
            float y1 = (float)pts[i * 3 + 4];
            float z1 = (float)pts[i * 3 + 5];
            quad(pose, vc, x0, y0 + halfHeight, z0, x1, y1 + halfHeight, z1, x1, y1 - halfHeight, z1, x0, y0 - halfHeight, z0, a0, a1, a1, a0, rgb);
         }
      }
   }

   private static void box(
      Pose pose,
      VertexConsumer vc,
      float cx,
      float cy,
      float cz,
      float ax,
      float ay,
      float az,
      float bx,
      float by,
      float bz,
      float nx,
      float ny,
      float nz,
      int alpha,
      int rgb,
      boolean shaded
   ) {
      face(pose, vc, cx, cy, cz, ax, ay, az, bx, by, bz, nx, ny, nz, alpha, rgb, shaded);
      face(pose, vc, cx, cy, cz, -ax, -ay, -az, bx, by, bz, nx, ny, nz, alpha, rgb, shaded);
      face(pose, vc, cx, cy, cz, bx, by, bz, ax, ay, az, nx, ny, nz, alpha, rgb, shaded);
      face(pose, vc, cx, cy, cz, -bx, -by, -bz, ax, ay, az, nx, ny, nz, alpha, rgb, shaded);
      face(pose, vc, cx, cy, cz, nx, ny, nz, ax, ay, az, bx, by, bz, alpha, rgb, shaded);
      face(pose, vc, cx, cy, cz, -nx, -ny, -nz, ax, ay, az, bx, by, bz, alpha, rgb, shaded);
   }

   static int shadeFace(int rgb, float nx, float ny, float nz) {
      float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
      if (len < 1.0E-6F) {
         return rgb;
      } else {
         float d = (nx * 0.52F + ny * 0.72F + nz * -0.46F) / len;
         float lit = Math.max(0.0F, d);
         float f = 0.62F + 0.48F * lit - 0.12F * Math.max(0.0F, -d);
         int shaded = scaleRgb(rgb, f);
         return lit > 0.0F ? lerpRgb(shaded, 10266814, 0.35F * lit * lit) : shaded;
      }
   }

   private static void face(
      Pose pose,
      VertexConsumer vc,
      float cx,
      float cy,
      float cz,
      float ox,
      float oy,
      float oz,
      float ux,
      float uy,
      float uz,
      float vx,
      float vy,
      float vz,
      int alpha,
      int rgb,
      boolean shade
   ) {
      int shaded = shade ? shadeFace(rgb, ox, oy, oz) : rgb;
      float fx = cx + ox;
      float fy = cy + oy;
      float fz = cz + oz;
      quad(
         pose,
         vc,
         fx + ux + vx,
         fy + uy + vy,
         fz + uz + vz,
         fx - ux + vx,
         fy - uy + vy,
         fz - uz + vz,
         fx - ux - vx,
         fy - uy - vy,
         fz - uz - vz,
         fx + ux - vx,
         fy + uy - vy,
         fz + uz - vz,
         alpha,
         alpha,
         alpha,
         alpha,
         shaded
      );
   }

   private static void quad(
      Pose pose,
      VertexConsumer vc,
      float ax,
      float ay,
      float az,
      float bx,
      float by,
      float bz,
      float cx,
      float cy,
      float cz,
      float dx,
      float dy,
      float dz,
      int aA,
      int aB,
      int aC,
      int aD,
      int rgb
   ) {
      int ca = Mth.clamp(aA, 0, 255) << 24 | rgb;
      int cb = Mth.clamp(aB, 0, 255) << 24 | rgb;
      int cc = Mth.clamp(aC, 0, 255) << 24 | rgb;
      int cd = Mth.clamp(aD, 0, 255) << 24 | rgb;
      vc.addVertex(pose, ax, ay, az).setColor(ca);
      vc.addVertex(pose, bx, by, bz).setColor(cb);
      vc.addVertex(pose, cx, cy, cz).setColor(cc);
      vc.addVertex(pose, ax, ay, az).setColor(ca);
      vc.addVertex(pose, cx, cy, cz).setColor(cc);
      vc.addVertex(pose, dx, dy, dz).setColor(cd);
   }

   public static float span(float t, float a, float b) {
      return b <= a ? (t >= b ? 1.0F : 0.0F) : Mth.clamp((t - a) / (b - a), 0.0F, 1.0F);
   }

   public static float settle(float since) {
      return since <= 0.0F ? 0.0F : 0.45F * (float)Math.exp((double)(-since * 0.22F)) * Mth.sin(since * 1.15F);
   }

   public static float noise(int seed, int a, int b) {
      return VoxelFlameDraw.noise01(seed, a, b);
   }

   public static int scaleRgb(int rgb, float f) {
      int r = Mth.clamp(Math.round((float)(rgb >> 16 & 0xFF) * f), 0, 255);
      int g = Mth.clamp(Math.round((float)(rgb >> 8 & 0xFF) * f), 0, 255);
      int b = Mth.clamp(Math.round((float)(rgb & 0xFF) * f), 0, 255);
      return r << 16 | g << 8 | b;
   }

   public static int lerpRgb(int a, int b, float t) {
      float x = Mth.clamp(t, 0.0F, 1.0F);
      int r = Math.round(Mth.lerp(x, (float)(a >> 16 & 0xFF), (float)(b >> 16 & 0xFF)));
      int g = Math.round(Mth.lerp(x, (float)(a >> 8 & 0xFF), (float)(b >> 8 & 0xFF)));
      int bl = Math.round(Mth.lerp(x, (float)(a & 0xFF), (float)(b & 0xFF)));
      return r << 16 | g << 8 | bl;
   }

   public static final class Chain {
      public double ax;
      public double ay;
      public double az;
      public double bx;
      public double by;
      public double bz;
      public float sag;
      public float growth = 1.0F;
      public boolean growFromB;
      public float settle;
      public float shatter;
      public float charge;
      public float chargeFront = 2.0F;
      public float pulse = -1.0F;
      public float pulseWidth = 0.12F;
      public float tremble;
      public float sway;
      public float waver;
      public float thickness = 1.0F;
      public int head = 0;
      public float flash;
      public float ember;
      public float brightness = 1.0F;
      public int seed;

      public ChainLinks.Chain from(Vec3 a) {
         this.ax = a.x;
         this.ay = a.y;
         this.az = a.z;
         return this;
      }

      public ChainLinks.Chain to(Vec3 b) {
         this.bx = b.x;
         this.by = b.y;
         this.bz = b.z;
         return this;
      }

      public ChainLinks.Chain reset() {
         this.sag = 0.0F;
         this.growth = 1.0F;
         this.growFromB = false;
         this.settle = 0.0F;
         this.shatter = 0.0F;
         this.charge = 0.0F;
         this.chargeFront = 2.0F;
         this.pulse = -1.0F;
         this.pulseWidth = 0.12F;
         this.tremble = 0.0F;
         this.sway = 0.0F;
         this.waver = 0.0F;
         this.thickness = 1.0F;
         this.head = 0;
         this.flash = 0.0F;
         this.ember = 0.0F;
         this.brightness = 1.0F;
         this.seed = 0;
         return this;
      }
   }

   private static enum Pass {
      SOLID,
      BODY,
      GLOW;
   }

   public static class Trail {
      public final double[] pts;
      public int count;
      public long lastTick = Long.MIN_VALUE;

      public Trail(int length) {
         this.pts = new double[length * 3];
      }

      public void sample(double x, double y, double z, long now) {
         if (this.lastTick != now) {
            this.lastTick = now;
            int cap = this.pts.length / 3;
            if (this.count == cap) {
               System.arraycopy(this.pts, 3, this.pts, 0, (cap - 1) * 3);
               this.count--;
            }

            this.pts[this.count * 3] = x;
            this.pts[this.count * 3 + 1] = y;
            this.pts[this.count * 3 + 2] = z;
            this.count++;
         }
      }

      public float speed() {
         if (this.count < 2) {
            return 0.0F;
         } else {
            int last = (this.count - 1) * 3;
            int prev = (this.count - 2) * 3;
            return (float)Math.hypot(this.pts[last] - this.pts[prev], this.pts[last + 2] - this.pts[prev + 2]);
         }
      }

      public void clear() {
         this.count = 0;
         this.lastTick = Long.MIN_VALUE;
      }

      public void ribbon(Pose pose, VertexConsumer vc, float halfHeight, int alpha, int rgb) {
         ChainLinks.ribbon(pose, vc, this.pts, this.count, halfHeight, alpha, rgb);
      }
   }
}
