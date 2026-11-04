package com.awakenedmod.awakened.client.chrono.stasis;

public final class StasisStyles {
   private static final StasisStyle LOOK = new WindUpKeyStyle();

   private StasisStyles() {
   }

   public static StasisStyle look() {
      return LOOK;
   }
}
