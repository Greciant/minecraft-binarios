package com.awakenedmod.awakened.client;

public interface FreeHud extends MovableHud {
   HudSettings hudSettings();

   HudPlacement.Box defaultBox(int var1, int var2, int var3, int var4);

   @Override
   default boolean enabled() {
      return this.hudSettings().enabled();
   }

   @Override
   default void setEnabled(boolean on) {
      this.hudSettings().setEnabled(on);
   }

   @Override
   default HudPlacement.Box layout(int screenW, int screenH) {
      int w = this.hudWidth();
      int h = this.hudHeight();
      HudSettings settings = this.hudSettings();
      if (!settings.placed()) {
         HudPlacement.Box shipped = this.defaultBox(screenW, screenH, w, h);
         return new HudPlacement.Box(HudPlacement.clampToScreen(shipped.x(), w, screenW), HudPlacement.clampToScreen(shipped.y(), h, screenH), w, h);
      } else {
         return HudPlacement.resolve(settings.anchor(), settings.x(), settings.y(), w, h, screenW, screenH);
      }
   }

   @Override
   default void place(int x, int y, int w, int h, int screenW, int screenH) {
      this.place(x, y, w, h, screenW, screenH, HudAnchor.nearest(x + w / 2, y + h / 2, screenW, screenH));
   }

   default void place(int x, int y, int w, int h, int screenW, int screenH, HudAnchor anchor) {
      double[] f = HudPlacement.fractionsFor(anchor, screenW, screenH, x, y, w, h);
      this.hudSettings().store(anchor, f[0], f[1]);
   }

   @Override
   default void resetPlacement() {
      this.hudSettings().clearPlacement();
   }

   @Override
   default boolean hasStoredPlacement() {
      return this.hudSettings().placed();
   }
}
