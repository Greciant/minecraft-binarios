package com.devilfruits.core.limb;

import javax.annotation.Nullable;

public final class LimbUv {
   public static final int BAND = 4;
   public static final int LENGTH = 12;

   public static boolean legacy(int texWidth, int texHeight) {
      return texWidth == texHeight * 2;
   }

   @Nullable
   public static LimbUv.Layout resolve(LimbUv.Model model, LimbUv.Side side, LimbUv.Limb limb, int texWidth, int texHeight, LimbUv.Layer layer) {
      boolean legacy = legacy(texWidth, texHeight);
      if (legacy && layer == LimbUv.Layer.OVERLAY) {
         return null;
      } else {
         boolean right = side == LimbUv.Side.RIGHT;
         boolean overlay = layer == LimbUv.Layer.OVERLAY;
         boolean slim = model == LimbUv.Model.SLIM;
         float scale = (float)texWidth / 64.0F;
         boolean mirror = false;
         int u;
         int v;
         if (limb == LimbUv.Limb.ARM) {
            if (!right && !legacy) {
               u = overlay ? 48 : 32;
               v = 48;
            } else {
               u = 40;
               v = overlay ? 32 : 16;
               mirror = !right;
            }
         } else if (!right && !legacy) {
            u = overlay ? 0 : 16;
            v = 48;
         } else {
            u = 0;
            v = overlay ? 32 : 16;
            mirror = !right;
         }

         int width = limb == LimbUv.Limb.ARM && slim ? 3 : 4;
         float minX;
         float minY;
         float pivotX;
         float pivotY;
         if (limb == LimbUv.Limb.ARM) {
            minX = right ? (slim ? -2.0F : -3.0F) : -1.0F;
            minY = -2.0F;
            pivotX = right ? -5.0F : 5.0F;
            pivotY = slim ? 2.5F : 2.0F;
         } else {
            minX = -2.0F;
            minY = 0.0F;
            pivotX = right ? -1.9F : 1.9F;
            pivotY = 12.0F;
         }

         return new LimbUv.Layout(texWidth, texHeight, scale, u, v, width, 4, minX, minY, -2.0F, pivotX, pivotY, 0.0F, overlay ? 0.25F : 0.0F, mirror);
      }
   }

   private LimbUv() {
   }

   public static enum Face {
      WEST,
      NORTH,
      EAST,
      SOUTH;
   }

   public static enum Layer {
      SKIN,
      OVERLAY;
   }

   public static record Layout(
      int texWidth,
      int texHeight,
      float scale,
      int u,
      int v,
      int width,
      int depth,
      float minX,
      float minY,
      float minZ,
      float pivotX,
      float pivotY,
      float pivotZ,
      float grow,
      boolean mirror
   ) {
      public float u(float texel) {
         return texel * this.scale / (float)this.texWidth;
      }

      public float v(float texel) {
         return texel * this.scale / (float)this.texHeight;
      }

      public float px(float texel) {
         return texel * this.scale;
      }

      public float bandRows() {
         return 4.0F * this.scale;
      }

      public float sideTop() {
         return (float)(this.v + this.depth);
      }

      public float centreX() {
         return this.minX + (float)this.width * 0.5F;
      }

      public float centreZ() {
         return this.minZ + (float)this.depth * 0.5F;
      }

      public float sideU(int face, int corner) {
         int u = this.u;
         int w = this.width;
         int d = this.depth;

         return switch (face) {
            case 0 -> corner == 0 ? (float)(u + d) : (float)u;
            case 1 -> corner == 0 ? (float)(u + d + w) : (float)(u + d);
            case 2 -> corner == 0 ? (float)(u + 2 * d + w) : (float)(u + d + w);
            default -> corner == 0 ? (float)(u + 2 * d + 2 * w) : (float)(u + 2 * d + w);
         };
      }

      public static int cornerX(int face, int corner) {
         return switch (face) {
            case 0 -> -1;
            case 1 -> corner == 0 ? 1 : -1;
            case 2 -> 1;
            default -> corner == 0 ? -1 : 1;
         };
      }

      public static int cornerZ(int face, int corner) {
         return switch (face) {
            case 0 -> corner == 0 ? -1 : 1;
            case 1 -> -1;
            case 2 -> corner == 0 ? 1 : -1;
            default -> 1;
         };
      }

      public void cap(boolean top, float[] out) {
         float a = (float)(this.u + this.depth);
         float b = (float)(this.u + this.depth + this.width);
         float c = (float)(this.u + this.depth + 2 * this.width);
         if (top) {
            put(out, 0, 1.0F, 1.0F, b, (float)this.v);
            put(out, 1, -1.0F, 1.0F, a, (float)this.v);
            put(out, 2, -1.0F, -1.0F, a, (float)(this.v + this.depth));
            put(out, 3, 1.0F, -1.0F, b, (float)(this.v + this.depth));
         } else {
            put(out, 0, 1.0F, -1.0F, c, (float)(this.v + this.depth));
            put(out, 1, -1.0F, -1.0F, b, (float)(this.v + this.depth));
            put(out, 2, -1.0F, 1.0F, b, (float)this.v);
            put(out, 3, 1.0F, 1.0F, c, (float)this.v);
         }
      }

      private static void put(float[] out, int i, float x, float z, float u, float v) {
         out[i * 4] = x;
         out[i * 4 + 1] = z;
         out[i * 4 + 2] = u;
         out[i * 4 + 3] = v;
      }
   }

   public static enum Limb {
      ARM,
      LEG;
   }

   public static enum Model {
      CLASSIC,
      SLIM;
   }

   public static enum Side {
      RIGHT,
      LEFT;
   }
}
