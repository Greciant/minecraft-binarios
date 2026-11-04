package com.awakenedmod.awakened.core.compat;

import net.neoforged.fml.ModList;

public final class BetterCombatCompat {
   public static final String MOD_ID = "bettercombat";
   private static Boolean loaded;

   private BetterCombatCompat() {
   }

   public static boolean ownsSwing() {
      if (loaded == null) {
         ModList mods = ModList.get();
         if (mods == null) {
            return false;
         }

         loaded = mods.isLoaded("bettercombat");
      }

      return loaded;
   }
}
