package com.awakenedmod.awakened.client;

public final class ExoClientState {
   private static boolean defaultJet;

   private ExoClientState() {
   }

   public static boolean defaultJet() {
      return defaultJet;
   }

   public static void setDefaultJet(boolean jet) {
      defaultJet = jet;
   }
}
