package com.awakenedmod.awakened.client;

import java.util.List;
import javax.annotation.Nullable;

public final class HudPlacement {
   public static final int MARGIN = 2;
   private static List<MovableHud> placing = List.of();

   private HudPlacement() {
   }

   public static void placing(@Nullable List<MovableHud> huds) {
      placing = huds == null ? List.of() : List.copyOf(huds);
   }

   public static boolean isPlacing(MovableHud hud) {
      for (MovableHud held : placing) {
         if (held == hud) {
            return true;
         }
      }

      return false;
   }

   public static HudPlacement.Box resolve(HudAnchor anchor, double fx, double fy, int w, int h, int screenW, int screenH) {
      int ax = (int)Math.round(fx * (double)screenW);
      int ay = (int)Math.round(fy * (double)screenH);
      return new HudPlacement.Box(clampToScreen(anchor.originX(ax, w), w, screenW), clampToScreen(anchor.originY(ay, h), h, screenH), w, h);
   }

   public static double[] fractionsFor(HudAnchor anchor, int screenW, int screenH, int x, int y, int w, int h) {
      double fx = screenW <= 0 ? 0.0 : (double)anchor.anchorX(x, w) / (double)screenW;
      double fy = screenH <= 0 ? 0.0 : (double)anchor.anchorY(y, h) / (double)screenH;
      return new double[]{clamp01(fx), clamp01(fy)};
   }

   public static int clampToScreen(int v, int size, int screen) {
      return Math.max(2, Math.min(v, screen - size - 2));
   }

   private static double clamp01(double v) {
      return Math.max(0.0, Math.min(1.0, v));
   }

   public static record Box(int x, int y, int width, int height) {
      public int right() {
         return this.x + this.width;
      }

      public int bottom() {
         return this.y + this.height;
      }

      public boolean contains(double mx, double my) {
         return mx >= (double)this.x && mx < (double)(this.x + this.width) && my >= (double)this.y && my < (double)(this.y + this.height);
      }

      public boolean overlaps(int ox, int oy, int ow, int oh) {
         return this.x < ox + ow && ox < this.right() && this.y < oy + oh && oy < this.bottom();
      }
   }
}
