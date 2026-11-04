package com.devilfruits.core.fruit;

import java.util.Locale;

public enum DevilFruitType {
   PARAMECIA,
   ZOAN,
   ANCIENT_ZOAN,
   MYTHICAL_ZOAN,
   LOGIA;

   public String translationKey() {
      return "devilfruit_type.devilfruits." + this.name().toLowerCase(Locale.ROOT);
   }

   public boolean isZoan() {
      return this == ZOAN || this == ANCIENT_ZOAN || this == MYTHICAL_ZOAN;
   }
}
