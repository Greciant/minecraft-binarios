package com.awakenedmod.awakened.client.ruin;

import com.mojang.blaze3d.vertex.VertexConsumer;

final class DecayVertexSink implements VertexConsumer {
   static final int BODY = 0;
   static final int GREY = 1;
   static final int CRACK = 2;
   private VertexConsumer out;
   private DecayVertexSink.Pass p;
   private final float[] vx = new float[4];
   private final float[] vy = new float[4];
   private final float[] vz = new float[4];
   private final float[] vu = new float[4];
   private final float[] vv = new float[4];
   private final int[] vc = new int[4];
   private float nx;
   private float ny;
   private float nz;
   private int overlay;
   private int light;
   private int count;

   void bind(VertexConsumer target, DecayVertexSink.Pass pass) {
      this.out = target;
      this.p = pass;
      this.count = 0;
   }

   public void addVertex(
      float x, float y, float z, int color, float u, float v, int packedOverlay, int packedLight, float normalX, float normalY, float normalZ
   ) {
      int i = this.count;
      this.vx[i] = x;
      this.vy[i] = y;
      this.vz[i] = z;
      this.vu[i] = u;
      this.vv[i] = v;
      this.vc[i] = color;
      if (i == 0) {
         this.nx = normalX;
         this.ny = normalY;
         this.nz = normalZ;
         this.overlay = packedOverlay;
         this.light = packedLight;
      }

      if (++this.count == 4) {
         this.count = 0;
         this.face();
      }
   }

   private void face() {
      DecayVertexSink.Pass p = this.p;
      if (!p.playerLayout || p.mode == 0 || !secondLayer((this.vu[0] + this.vu[2]) * 0.5F, (this.vv[0] + this.vv[2]) * 0.5F)) {
         boolean crumbling = !Float.isNaN(p.crumbleY);
         float rgbMul = 1.0F;
         if (p.mode == 0 && crumbling) {
            float cx = this.vx[0] + this.vx[1] + this.vx[2] + this.vx[3];
            float cy = this.vy[0] + this.vy[1] + this.vy[2] + this.vy[3];
            float cz = this.vz[0] + this.vz[1] + this.vz[2] + this.vz[3];
            if (this.nx * cx + this.ny * cy + this.nz * cz > 0.0F) {
               rgbMul = p.inside;
            }
         }

         if (crumbling) {
            int above = 0;

            for (int i = 0; i < 4; i++) {
               if (this.vy[i] > p.crumbleY) {
                  above++;
               }
            }

            if (above == 4) {
               return;
            }

            for (int ix = 0; ix < 4; ix++) {
               if (this.vy[ix] > p.crumbleY) {
                  this.vy[ix] = p.crumbleY;
               }
            }
         }

         int cell = -1;
         float faceAlpha = 1.0F;
         switch (p.mode) {
            case 1:
               faceAlpha = p.alpha;
               rgbMul *= p.darken;
               break;
            case 2:
               float stageF = crumbling ? 4.0F : Math.min(4.0F, p.depth);
               int lower = (int)stageF;
               float frac = stageF - (float)lower;
               switch (p.crackPass) {
                  case 0:
                     if (lower < 1) {
                        return;
                     }

                     cell = Math.min(lower, 4);
                     faceAlpha = p.stageAlpha(cell);
                     break;
                  case 1:
                     int upper = lower + 1;
                     if (upper > 4 || frac < 0.02F) {
                        return;
                     }

                     cell = upper;
                     faceAlpha = p.stageAlpha(upper) * RuinLook.smooth(frac);
                     break;
                  default:
                     cell = Math.max(1, Math.min(lower + (frac > 0.5F ? 1 : 0), 4)) + 4;
                     faceAlpha = p.alpha;
               }
         }

         if (!(faceAlpha <= 0.004F)) {
            float uOffset = 0.0F;
            float uScale = 1.0F;
            if (cell >= 0 && p.cells > 0) {
               uScale = 1.0F / (float)p.cells;
               uOffset = (float)(cell - 1) * uScale;
            }

            for (int ixx = 0; ixx < 4; ixx++) {
               int c = this.vc[ixx];
               int a = Math.round((float)(c >>> 24) * faceAlpha);
               int rgb = c & 16777215;
               if (rgbMul != 1.0F) {
                  int r = Math.round((float)(rgb >> 16 & 0xFF) * rgbMul);
                  int g = Math.round((float)(rgb >> 8 & 0xFF) * rgbMul);
                  int b = Math.round((float)(rgb & 0xFF) * rgbMul);
                  rgb = r << 16 | g << 8 | b;
               }

               this.out
                  .addVertex(
                     this.vx[ixx],
                     this.vy[ixx],
                     this.vz[ixx],
                     Math.min(255, a) << 24 | rgb,
                     this.vu[ixx] * uScale + uOffset,
                     this.vv[ixx],
                     this.overlay,
                     this.light,
                     this.nx,
                     this.ny,
                     this.nz
                  );
            }
         }
      }
   }

   private static boolean secondLayer(float u, float v) {
      float px = u * 64.0F;
      float py = v * 64.0F;
      if (py < 16.0F) {
         return px >= 32.0F;
      } else if (py < 32.0F) {
         return false;
      } else {
         return py < 48.0F ? px < 16.0F || px >= 16.0F && px < 40.0F || px >= 40.0F && px < 56.0F : px < 16.0F || px >= 48.0F;
      }
   }

   public VertexConsumer addVertex(float x, float y, float z) {
      return this.out.addVertex(x, y, z);
   }

   public VertexConsumer setColor(int red, int green, int blue, int alpha) {
      return this.out.setColor(red, green, blue, alpha);
   }

   public VertexConsumer setColor(int color) {
      return this.out.setColor(color);
   }

   public VertexConsumer setUv(float u, float v) {
      return this.out.setUv(u, v);
   }

   public VertexConsumer setUv1(int u, int v) {
      return this.out.setUv1(u, v);
   }

   public VertexConsumer setUv2(int u, int v) {
      return this.out.setUv2(u, v);
   }

   public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
      return this.out.setNormal(normalX, normalY, normalZ);
   }

   static final class Pass {
      static final int LOWER = 0;
      static final int UPPER = 1;
      static final int FLASH = 2;
      int mode;
      int crackPass;
      float depth;
      float alpha1;
      float alpha2;
      float alpha3;
      float alpha4;
      float alpha;
      float darken;
      float inside;
      float crumbleY = Float.NaN;
      boolean playerLayout;
      int cells;

      float stageAlpha(int stage) {
         return switch (stage) {
            case 1 -> this.alpha1;
            case 2 -> this.alpha2;
            case 3 -> this.alpha3;
            case 4 -> this.alpha4;
            default -> 0.0F;
         };
      }
   }
}
