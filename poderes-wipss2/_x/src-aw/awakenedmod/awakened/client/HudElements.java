package com.awakenedmod.awakened.client;

import java.util.ArrayList;
import java.util.List;

public final class HudElements {
   private static final List<MovableHud> ALL = new ArrayList<>();

   private HudElements() {
   }

   public static <T extends MovableHud> T register(T hud) {
      ALL.add(hud);
      return hud;
   }

   public static List<MovableHud> relevant() {
      List<MovableHud> out = new ArrayList<>();

      for (MovableHud hud : ALL) {
         if (hud.hudRelevant()) {
            out.add(hud);
         }
      }

      return out;
   }
}
