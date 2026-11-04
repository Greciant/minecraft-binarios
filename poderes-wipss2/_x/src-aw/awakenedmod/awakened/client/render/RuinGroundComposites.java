package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public final class RuinGroundComposites {
   private static final int VEIN_SEGS = 7;
   private static final int BRANCH_SEGS = 3;
   private static final int BRANCHES_PER_VEIN = 2;
   private static final float VEIN_LIFT = 0.03F;
   private static final float VEIN_WIDTH = 0.11F;
   private static final int CURTAIN_PIECES = 72;
   private static final float CURTAIN_GAPS = 0.3F;
   private static final float CURTAIN_BACK = 0.6F;
   private static final float CURTAIN_LEAN = 0.55F;
   private static final int HAZE_SHEETS = 4;
   private static final float HAZE_TOP = 0.18F;
   private static final float HAZE_STEP = 0.2F;
   private static final float HAZE_SINK = 0.3F;

   private RuinGroundComposites() {
   }

   public static void register() {
      VfxComposites.register(VfxEntity.Kind.RUIN_FRACTURE, RuinGroundComposites::fracture);
      VfxComposites.register(VfxEntity.Kind.RUIN_COLLAPSE_FRONT, RuinGroundComposites::collapseFront);
      VfxComposites.register(VfxEntity.Kind.RUIN_PIT_HAZE, RuinGroundComposites::pitHaze);
      VfxComposites.register(VfxEntity.Kind.RUIN_CRACK_FLASH, RuinGroundComposites::crackFlash);
   }

   private static float ease(float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      return 1.0F - (1.0F - t) * (1.0F - t);
   }

   private static float noise(int seed, int a, int b) {
      return VfxGeometry.noise01(seed, a, b);
   }

   private static int mix(int a, int b, float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      int r = (int)Mth.lerp(t, (float)(a >> 16 & 0xFF), (float)(b >> 16 & 0xFF));
      int g = (int)Mth.lerp(t, (float)(a >> 8 & 0xFF), (float)(b >> 8 & 0xFF));
      int bl = (int)Mth.lerp(t, (float)(a & 0xFF), (float)(b & 0xFF));
      return r << 16 | g << 8 | bl;
   }

   private static RuinGroundComposites.FractureShape fractureShape(ClientLevel level, int entityId, int veins, float radius) {
      return level != null && level.getEntity(entityId) instanceof VfxEntity shell
         ? shell.drawerMemo(RuinGroundComposites.FractureShape.class, () -> new RuinGroundComposites.FractureShape(entityId, veins, radius))
         : new RuinGroundComposites.FractureShape(entityId, veins, radius);
   }

   private static void fracture(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      ClientLevel level = Minecraft.getInstance().level;
      float radius = Math.max(1.0F, state.scale);
      float growTicks = Math.max(1.0F, (float)state.secondary.x);
      int veins = Math.max(3, (int)state.secondary.y);
      float age = state.ageInTicks - (float)state.delay;
      float in = state.intensity;
      RuinGroundComposites.FractureShape shape = fractureShape(level, state.seed, veins, radius);
      shape.seat(level, state.worldPos.x, state.worldPos.y, state.worldPos.z);
      float reach = ease(age / growTicks);
      float fade = life < 0.72F ? 1.0F : (1.0F - life) / 0.28F;
      int crackA = VfxDraw.ia(215.0F * fade, in);
      if (crackA > 1) {
         int crack = VfxColor.argb(crackA, state.color);
         int lines = shape.xs.length;
         float[] progress = new float[lines];

         for (int l = 0; l < lines; l++) {
            progress[l] = l < shape.mains ? reach : Mth.clamp((reach - shape.startAt[l]) * 1.7F, 0.0F, 1.0F);
         }

         Vector3f a = new Vector3f();
         Vector3f b = new Vector3f();
         VfxDraw.translucent(ps, sink, (pose, vc) -> {
            for (int l = 0; l < lines; l++) {
               float p = progress[l];
               if (!(p <= 0.001F)) {
                  float[] x = shape.xs[l];
                  float[] y = shape.ys[l];
                  float[] z = shape.zs[l];
                  float target = shape.length[l] * p;
                  float run = 0.0F;
                  float w0 = shape.width[l];

                  for (int k = 0; k + 1 < x.length; k++) {
                     float dx = x[k + 1] - x[k];
                     float dz = z[k + 1] - z[k];
                     float seg = Mth.sqrt(dx * dx + dz * dz);
                     if (!(seg < 1.0E-4F)) {
                        float t0 = run / shape.length[l];
                        float end = Math.min(1.0F, (target - run) / seg);
                        if (end <= 0.0F) {
                           break;
                        }

                        float t1 = (run + seg * end) / shape.length[l];
                        a.set(x[k], y[k], z[k]);
                        b.set(x[k] + dx * end, Mth.lerp(end, y[k], y[k + 1]), z[k] + dz * end);
                        VfxGeometry.crackSeg(pose, vc, a, b, w0 * (1.0F - 0.85F * t0), w0 * (1.0F - 0.85F * t1), crack);
                        run += seg;
                     }
                  }
               }
            }
         });
      }
   }

   private static void collapseFront(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float radius = Math.max(1.0F, state.scale);
      float spread = Math.max(1.0F, (float)state.secondary.x);
      float depth = Math.max(2.0F, (float)state.secondary.y);
      float age = state.ageInTicks - (float)state.delay;
      float in = state.intensity;
      float front = Math.min(1.0F, age / spread);
      float edge = radius * front;
      if (!(edge < 1.5F)) {
         float fade = life < 0.7F ? 1.0F : (1.0F - life) / 0.3F;
         float e = Mth.clamp(edge / radius, 0.0F, 0.97F);
         float drop = Math.max(1.2F, depth * Mth.sqrt(1.0F - e * e));
         int upperA = VfxDraw.ia(96.0F * fade, in);
         int lowerA = VfxDraw.ia(50.0F * fade, in);
         int streakA = VfxDraw.ia(120.0F * fade, in);
         if (upperA > 1) {
            int seed = state.seed;
            int dark = state.color2 == -1 ? VfxColor.shade(state.color, 0.6F) : mix(state.color, state.color2, 0.35F);
            int upper = VfxColor.argb(upperA, state.color);
            int lower = VfxColor.argb(lowerA, dark);
            float top = edge - 0.6F;
            VfxDraw.translucent(ps, sink, (pose, vc) -> {
               double arc = 0.08726646259971647;

               for (int j = 0; j < 72; j++) {
                  if (!(noise(seed, j, 1) < 0.3F)) {
                     double a0 = (double)j * arc;
                     double a1 = a0 + arc * (double)(0.8F + 0.35F * noise(seed, j, 2));
                     float len = drop * (0.55F + 0.45F * noise(seed, j, 3));
                     float mid = -len * 0.5F;
                     float rMid = top - len * 0.5F * 0.55F;
                     float rEnd = top - len * 0.55F;
                     VfxGeometry.wallPiece(pose, vc, a0, a1, top, 0.35F, rMid, mid, upper);
                     VfxGeometry.wallPiece(pose, vc, a0, a1, rMid, mid, rEnd, -len, lower);
                     if (j % 3 == 0) {
                        float slide = (age * 0.045F + noise(seed, j, 4)) % 1.0F;
                        float y0 = -len * slide;
                        float y1 = y0 - len * 0.3F;
                        double s0 = a0 + (a1 - a0) * 0.35;
                        double s1 = a0 + (a1 - a0) * 0.6;
                        int streak = VfxColor.argb((int)((float)streakA * (1.0F - slide)), dark);
                        VfxGeometry.wallPiece(pose, vc, s0, s1, top + y0 * 0.55F - 0.05F, y0, top + y1 * 0.55F - 0.05F, y1, streak);
                     }
                  }
               }
            });
         }
      }
   }

   private static void pitHaze(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float radius = Math.max(1.0F, state.scale);
      float depth = Math.max(2.0F, (float)state.secondary.y);
      float age = state.ageInTicks - (float)state.delay;
      float in = state.intensity;
      float rise = life < 0.12F ? life / 0.12F : 1.0F;
      float out = life > 0.5F ? (1.0F - life) / 0.5F : 1.0F;
      int baseA = VfxDraw.ia(46.0F * rise * out, in);
      if (baseA > 1) {
         int seed = state.seed;
         int dark = state.color2 == -1 ? VfxColor.shade(state.color, 0.6F) : mix(state.color, state.color2, 0.3F);
         float sinkBy = 0.3F * ease(life);
         VfxDraw.translucent(ps, sink, (pose, vc) -> {
            for (int k = 0; k < 4; k++) {
               float f = Math.min(0.95F, 0.18F + (float)k * 0.2F + sinkBy);
               float y = -depth * f + 0.25F * Mth.sin(age * 0.03F + (float)k * 1.7F);
               float r = radius * Mth.sqrt(1.0F - f * f) * 0.95F;
               int rgb = mix(state.color, dark, (float)k / (float)Math.max(1, 3));
               int a = (int)((float)baseA * (1.0F - 0.15F * (float)k));
               int drift = seed * 7 + k * 31;
               VfxGeometry.ring(pose, vc, y, 0.0F, r, 48, 0.12F, drift, VfxColor.argb(a, rgb));
            }
         });
      }
   }

   private static void crackFlash(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float s = Math.max(0.3F, state.scale);
      float age = state.ageInTicks - (float)state.delay;
      float in = state.intensity;
      int seed = state.seed;
      float pop = Math.min(1.0F, age / 2.0F);
      float gone = 1.0F - life;
      int crackA = VfxDraw.ia(225.0F * (life < 0.45F ? 1.0F : (1.0F - life) / 0.55F), in);
      int puffA = VfxDraw.ia(90.0F * gone, in);
      int ringA = VfxDraw.ia(110.0F * gone, in);
      if (crackA > 1 || puffA > 1) {
         int dust = state.color2 == -1 ? VfxColor.lighten(state.color, 0.5F) : state.color2;
         int crack = VfxColor.argb(crackA, state.color);
         int puff = VfxColor.argb(puffA, VfxColor.shade(dust, 0.7F));
         int ring = VfxColor.argb(ringA, VfxColor.shade(dust, 0.8F));
         float ringR = s * (0.25F + 1.15F * ease(life));
         VfxDraw.translucent(ps, sink, (pose, vc) -> {
            if (crackA > 1) {
               VfxGeometry.cracks(pose, vc, 0.012F, s * 0.1F, s * 1.1F * pop, 8, seed, s * 0.08F, crack);
            }

            if (puffA > 1) {
               VfxGeometry.dome(pose, vc, 0.04F + life * 0.3F, s * 0.5F * (0.6F + 0.6F * life), 5, 12, 0.3F, seed + (int)(age * 0.5F), puff);
            }

            if (ringA > 1) {
               VfxGeometry.ring(pose, vc, 0.03F, Math.max(0.0F, ringR - s * 0.3F), ringR, 40, 0.25F, seed + 9, ring);
            }
         });
      }
   }

   private static final class FractureShape {
      final float[][] xs;
      final float[][] zs;
      final float[][] ys;
      final float[] length;
      final float[] width;
      final float[] startAt;
      final int mains;
      final KineticComposites.FloorSeats seats;
      boolean seated;

      FractureShape(int seed, int veins, float radius) {
         this.mains = veins;
         int lines = veins + veins * 2;
         this.xs = new float[lines][];
         this.zs = new float[lines][];
         this.ys = new float[lines][];
         this.length = new float[lines];
         this.width = new float[lines];
         this.startAt = new float[lines];
         float widthScale = Mth.clamp(radius / 12.0F, 0.7F, 1.7F);
         int slots = 0;
         int line = veins;

         for (int i = 0; i < veins; i++) {
            float reach = radius * (0.74F + 0.32F * RuinGroundComposites.noise(seed, i, 1));
            double angle = (double)i * ((Math.PI * 2) / (double)veins) + (double)(RuinGroundComposites.noise(seed, i, 2) - 0.5F) * 0.6;
            float[] vx = new float[8];
            float[] vz = new float[8];
            float total = 0.0F;

            for (int k = 1; k <= 7; k++) {
               angle += (double)(RuinGroundComposites.noise(seed, i, 10 + k) - 0.5F) * 0.55;
               float step = reach / 7.0F * (0.7F + 0.6F * RuinGroundComposites.noise(seed, i, 30 + k));
               vx[k] = vx[k - 1] + (float)Math.cos(angle) * step;
               vz[k] = vz[k - 1] + (float)Math.sin(angle) * step;
               total += step;
            }

            this.xs[i] = vx;
            this.zs[i] = vz;
            this.ys[i] = new float[8];
            this.length[i] = total;
            this.width[i] = 0.11F * widthScale * (0.85F + 0.4F * RuinGroundComposites.noise(seed, i, 3));
            this.startAt[i] = 0.0F;
            slots += 8;

            for (int b = 0; b < 2; b++) {
               float at = 0.28F + 0.42F * ((float)b + RuinGroundComposites.noise(seed, i, 50 + b)) / 2.0F;
               int root = Math.min(6, (int)(at * 7.0F));
               double dir = Math.atan2((double)(vz[root + 1] - vz[root]), (double)(vx[root + 1] - vx[root]));
               double side = (double)(b % 2 == 0 ? 1 : -1) * (0.55 + 0.5 * (double)RuinGroundComposites.noise(seed, i, 60 + b));
               double ba = dir + side;
               float[] bx = new float[4];
               float[] bz = new float[4];
               bx[0] = vx[root];
               bz[0] = vz[root];
               float blen = reach * (0.22F + 0.2F * RuinGroundComposites.noise(seed, i, 70 + b));
               float btotal = 0.0F;

               for (int k = 1; k <= 3; k++) {
                  ba += (double)(RuinGroundComposites.noise(seed, i, 80 + b * 8 + k) - 0.5F) * 0.6;
                  float step = blen / 3.0F * (0.75F + 0.5F * RuinGroundComposites.noise(seed, i, 100 + b * 8 + k));
                  bx[k] = bx[k - 1] + (float)Math.cos(ba) * step;
                  bz[k] = bz[k - 1] + (float)Math.sin(ba) * step;
                  btotal += step;
               }

               this.xs[line] = bx;
               this.zs[line] = bz;
               this.ys[line] = new float[4];
               this.length[line] = btotal;
               this.width[line] = this.width[i] * 0.55F;
               this.startAt[line] = (float)root / 7.0F;
               slots += 4;
               line++;
            }
         }

         this.seats = new KineticComposites.FloorSeats(slots);
      }

      void seat(ClientLevel level, double wx, double wy, double wz) {
         if (!this.seated && level != null) {
            int slot = 0;

            for (int l = 0; l < this.xs.length; l++) {
               for (int k = 0; k < this.xs[l].length; k++) {
                  this.ys[l][k] = this.seats.seat(level, slot++, wx + (double)this.xs[l][k], wy, wz + (double)this.zs[l][k]) - (float)wy + 0.03F;
               }
            }

            this.seated = true;
         }
      }
   }
}
