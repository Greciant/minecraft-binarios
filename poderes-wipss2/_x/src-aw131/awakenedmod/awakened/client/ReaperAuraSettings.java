package com.awakenedmod.awakened.client;

import net.minecraft.util.Mth;

public final class ReaperAuraSettings {
   private static float shownPct = 1.0F;

   private ReaperAuraSettings() {
   }

   public static float get() {
      return shownPct;
   }

   public static void set(float pct) {
      shownPct = round(Mth.clamp(pct, 0.0F, 1.0F));
   }

   public static void seedFromServer(float serverShown) {
      shownPct = round(Mth.clamp(serverShown, 0.0F, 1.0F));
   }

   public static void step(float delta) {
      set(shownPct + delta);
   }

   private static float round(float v) {
      return (float)Math.round(v * 10.0F) / 10.0F;
   }
}
