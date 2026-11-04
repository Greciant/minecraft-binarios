package com.awakenedmod.awakened.content.limitless.blue;

public enum BlueEnd {
   FADE,
   IMPLODE,
   POP;

   static BlueEnd of(int ordinal) {
      BlueEnd[] all = values();
      return ordinal >= 0 && ordinal < all.length ? all[ordinal] : FADE;
   }
}
