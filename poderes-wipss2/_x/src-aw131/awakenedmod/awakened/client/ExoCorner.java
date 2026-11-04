package com.awakenedmod.awakened.client;

public enum ExoCorner {
   TOP_LEFT(false, false),
   TOP_RIGHT(true, false),
   BOTTOM_LEFT(false, true),
   BOTTOM_RIGHT(true, true);

   private final boolean right;
   private final boolean bottom;

   private ExoCorner(boolean right, boolean bottom) {
      this.right = right;
      this.bottom = bottom;
   }

   public boolean right() {
      return this.right;
   }

   public boolean bottom() {
      return this.bottom;
   }

   public int originX(int screenW, int inset, int w) {
      return this.right ? screenW - inset - w : inset;
   }

   public int originY(int screenH, int inset, int h) {
      return this.bottom ? screenH - inset - h : inset;
   }
}
