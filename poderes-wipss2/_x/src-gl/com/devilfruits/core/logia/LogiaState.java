package com.devilfruits.core.logia;

import java.util.Locale;

public enum LogiaState {
   SOLID,
   PARTIAL,
   ELEMENTAL_BODY,
   REFORMING,
   SUPPRESSED;

   private static final LogiaState[] VALUES = values();

   public static LogiaState byId(int id) {
      return id >= 0 && id < VALUES.length ? VALUES[id] : SOLID;
   }

   public boolean dissolved() {
      return this == ELEMENTAL_BODY;
   }

   public String serializedName() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   public String translationKey() {
      return "logia_state.devilfruits." + this.serializedName();
   }
}
