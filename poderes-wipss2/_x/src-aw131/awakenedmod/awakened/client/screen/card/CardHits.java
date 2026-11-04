package com.awakenedmod.awakened.client.screen.card;

import java.util.ArrayList;
import java.util.List;

public final class CardHits {
   private List<CardHits.Hit> current = new ArrayList<>();
   private List<CardHits.Hit> previous = new ArrayList<>();
   private String hovered;

   public void begin(double mouseX, double mouseY) {
      this.previous = this.current;
      this.current = new ArrayList<>();
      this.hovered = null;

      for (int i = this.previous.size() - 1; i >= 0; i--) {
         CardHits.Hit h = this.previous.get(i);
         if (h.contains(mouseX, mouseY)) {
            this.hovered = h.key;
            break;
         }
      }
   }

   public void add(int x, int y, int w, int h, String key, Runnable action) {
      this.current.add(new CardHits.Hit(x, y, w, h, key, action));
   }

   public void add(int x, int y, int w, int h, String key) {
      this.add(x, y, w, h, key, null);
   }

   public boolean hover(String key) {
      return this.hovered != null && (this.hovered.equals(key) || this.hovered.startsWith(key + "/"));
   }

   public String hovered() {
      return this.hovered;
   }

   public boolean click(double mouseX, double mouseY) {
      for (int i = this.current.size() - 1; i >= 0; i--) {
         CardHits.Hit h = this.current.get(i);
         if (h.contains(mouseX, mouseY) && h.action != null) {
            h.action.run();
            return true;
         }
      }

      return false;
   }

   private static record Hit(int x, int y, int w, int h, String key, Runnable action) {
      boolean contains(double mx, double my) {
         return mx >= (double)this.x && mx < (double)(this.x + this.w) && my >= (double)this.y && my < (double)(this.y + this.h);
      }
   }
}
