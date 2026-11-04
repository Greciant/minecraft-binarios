package com.awakenedmod.awakened.client.render;

public final class WaterWave {
   private WaterWave() {
   }

   public static record Look(int rgb, float alpha, int deepRgb, int crestRgb) {
      public Look(int rgb, float alpha) {
         this(rgb, alpha, rgb, rgb);
      }

      public WaterWave.Look withAlpha(float a) {
         return new WaterWave.Look(this.rgb, a, this.deepRgb, this.crestRgb);
      }
   }
}
