package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.caster.Caster;
import net.minecraft.server.level.ServerPlayer;

public enum FlameStyle {
   ORANGE(12073477, 15227410, 15240232, 9.0F, 1.0F),
   BLUE(1844624, 2909160, 5417184, 16.0F, 0.85F);

   public static final float BLUE_THRESHOLD = 0.7F;
   public static final float BLEND_START = 0.55F;
   private final int body;
   private final int mid;
   private final int tip;
   private final float hueSpread;
   private final float pitch;

   private FlameStyle(int body, int mid, int tip, float hueSpread, float pitch) {
      this.body = body;
      this.mid = mid;
      this.tip = tip;
      this.hueSpread = hueSpread;
      this.pitch = pitch;
   }

   public int body() {
      return this.body;
   }

   public int mid() {
      return this.mid;
   }

   public int tip() {
      return this.tip;
   }

   public float hueSpread() {
      return this.hueSpread;
   }

   public float soundPitch() {
      return this.pitch;
   }

   public static FlameStyle forHeat(float heat01) {
      return heat01 >= 0.7F ? BLUE : ORANGE;
   }

   public static float blueFraction(float heat01) {
      float t = (heat01 - 0.55F) / 0.14999998F;
      if (t <= 0.0F) {
         return 0.0F;
      } else {
         return t >= 1.0F ? 1.0F : t * t * (3.0F - 2.0F * t);
      }
   }

   public static FlameStyle of(boolean blue) {
      return blue ? BLUE : ORANGE;
   }

   public static FlameStyle nearestBody(int rgb) {
      FlameStyle best = ORANGE;
      int bestDist = Integer.MAX_VALUE;

      for (FlameStyle style : values()) {
         int d = distance(style.body(), rgb);
         if (d < bestDist) {
            bestDist = d;
            best = style;
         }
      }

      return best;
   }

   private static int distance(int a, int b) {
      int dr = (a >> 16 & 0xFF) - (b >> 16 & 0xFF);
      int dg = (a >> 8 & 0xFF) - (b >> 8 & 0xFF);
      int db = (a & 0xFF) - (b & 0xFF);
      return dr * dr + dg * dg + db * db;
   }

   public static FlameStyle of(ServerPlayer player) {
      return forHeat(PyroHeatState.heat(PowerService.data(player)));
   }

   public static FlameStyle of(Caster caster) {
      return forHeat(PyroHeatState.heat(caster));
   }
}
