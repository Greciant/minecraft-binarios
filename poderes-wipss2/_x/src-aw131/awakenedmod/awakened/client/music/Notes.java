package com.awakenedmod.awakened.client.music;

public final class Notes {
   private Notes() {
   }

   public static float pitch(int note) {
      return (float)Math.pow(2.0, (double)(note - 12) / 12.0);
   }
}
