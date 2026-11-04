package com.devilfruits.core.haki;

import java.util.Locale;

public enum HakiTier {
   NOVICE,
   ADEPT,
   EXPERT,
   MASTER;

   public static final int MAX_LEVEL = 10;

   public static HakiTier of(int level) {
      if (level >= 10) {
         return MASTER;
      } else if (level >= 7) {
         return EXPERT;
      } else {
         return level >= 4 ? ADEPT : NOVICE;
      }
   }

   public String translationKey() {
      return "haki.devilfruits.tier." + this.name().toLowerCase(Locale.ROOT);
   }

   public boolean ryuo() {
      return this == EXPERT || this == MASTER;
   }
}
