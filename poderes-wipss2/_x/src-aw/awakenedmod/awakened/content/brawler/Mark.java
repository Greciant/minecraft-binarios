package com.awakenedmod.awakened.content.brawler;

public enum Mark {
   AIRBORNE,
   GROUNDED,
   PINNED;

   public static Mark byId(int id) {
      Mark[] all = values();
      return id >= 0 && id < all.length ? all[id] : null;
   }

   public boolean airborne() {
      return this == AIRBORNE;
   }

   public static enum Flavour {
      NONE,
      WALL,
      HAULED;

      public static Mark.Flavour byId(int id) {
         Mark.Flavour[] all = values();
         return id >= 0 && id < all.length ? all[id] : NONE;
      }
   }
}
