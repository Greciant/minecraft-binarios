package com.awakenedmod.awakened.client;

public final class DoubleTap {
   public static final int WINDOW_TICKS = 7;
   private boolean held;
   private int window;

   public boolean update(boolean down) {
      return this.update(down, false);
   }

   public boolean update(boolean down, boolean toggle) {
      boolean press = toggle ? down != this.held : down && !this.held;
      boolean fired = false;
      if (press) {
         if (this.window > 0) {
            fired = true;
            this.window = 0;
         } else {
            this.window = 7;
         }
      }

      this.held = down;
      if (this.window > 0) {
         this.window--;
      }

      return fired;
   }

   public void reset(boolean down) {
      this.held = down;
      this.window = 0;
   }
}
