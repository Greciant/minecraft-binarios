package com.awakenedmod.awakened.content.limitless.rct;

import java.util.ArrayDeque;

final class RecentHarm {
   private final ArrayDeque<RecentHarm.Hit> hits = new ArrayDeque<>();
   private final int window;

   RecentHarm(int window) {
      this.window = window;
   }

   void add(long now, float amount) {
      this.prune(now);
      if (amount > 0.0F) {
         this.hits.addLast(new RecentHarm.Hit(now, amount));
      }
   }

   float consume(long now, float healed) {
      this.prune(now);
      float taken = 0.0F;

      while (healed - taken > 0.0F && !this.hits.isEmpty()) {
         RecentHarm.Hit first = this.hits.pollFirst();
         float use = Math.min(first.left(), healed - taken);
         taken += use;
         if (first.left() > use) {
            this.hits.addFirst(new RecentHarm.Hit(first.at(), first.left() - use));
         }
      }

      return taken;
   }

   boolean isEmpty(long now) {
      this.prune(now);
      return this.hits.isEmpty();
   }

   private void prune(long now) {
      while (!this.hits.isEmpty() && now - this.hits.peekFirst().at() > (long)this.window) {
         this.hits.pollFirst();
      }
   }

   private static record Hit(long at, float left) {
   }
}
