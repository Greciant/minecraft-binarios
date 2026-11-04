package com.awakenedmod.awakened.client;

public final class ScalerClientState {
   private static boolean rayShifted;
   private static boolean seized;
   private static boolean seizing;

   private ScalerClientState() {
   }

   public static void set(boolean rayShiftedNow, boolean seizedNow, boolean seizingNow) {
      rayShifted = rayShiftedNow;
      seized = seizedNow;
      seizing = seizingNow;
   }

   public static boolean restrained() {
      return rayShifted || seized;
   }

   public static boolean seizing() {
      return seizing;
   }

   public static void reset() {
      rayShifted = false;
      seized = false;
      seizing = false;
   }
}
