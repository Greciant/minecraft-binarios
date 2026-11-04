package com.awakenedmod.awakened.client;

public enum HudAnchor {
   TOP_LEFT(0.0F, 0.0F),
   TOP_CENTER(0.5F, 0.0F),
   TOP_RIGHT(1.0F, 0.0F),
   MIDDLE_LEFT(0.0F, 0.5F),
   MIDDLE_CENTER(0.5F, 0.5F),
   MIDDLE_RIGHT(1.0F, 0.5F),
   BOTTOM_LEFT(0.0F, 1.0F),
   BOTTOM_CENTER(0.5F, 1.0F),
   BOTTOM_RIGHT(1.0F, 1.0F);

   private final float boxX;
   private final float boxY;
   public static final float NEAR = 0.0F;
   public static final float MIDDLE = 0.5F;
   public static final float FAR = 1.0F;

   private HudAnchor(float boxX, float boxY) {
      this.boxX = boxX;
      this.boxY = boxY;
   }

   public float bandX() {
      return this.boxX;
   }

   public float bandY() {
      return this.boxY;
   }

   public static HudAnchor of(float bandX, float bandY) {
      for (HudAnchor anchor : values()) {
         if (anchor.boxX == bandX && anchor.boxY == bandY) {
            return anchor;
         }
      }

      return MIDDLE_CENTER;
   }

   private static int inset(int size, float fraction) {
      return Math.round((float)size * fraction);
   }

   public int originX(int anchorX, int width) {
      return anchorX - inset(width, this.boxX);
   }

   public int originY(int anchorY, int height) {
      return anchorY - inset(height, this.boxY);
   }

   public int anchorX(int x, int width) {
      return x + inset(width, this.boxX);
   }

   public int anchorY(int y, int height) {
      return y + inset(height, this.boxY);
   }

   public static HudAnchor nearest(int x, int y, int screenW, int screenH) {
      float fx = screenW <= 0 ? 0.0F : (float)x / (float)screenW;
      float fy = screenH <= 0 ? 0.0F : (float)y / (float)screenH;
      HudAnchor best = TOP_LEFT;
      float bestDist = Float.MAX_VALUE;

      for (HudAnchor anchor : values()) {
         float dx = fx - anchor.boxX;
         float dy = fy - anchor.boxY;
         float dist = dx * dx + dy * dy;
         if (dist < bestDist) {
            bestDist = dist;
            best = anchor;
         }
      }

      return best;
   }
}
