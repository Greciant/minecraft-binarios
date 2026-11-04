package com.awakenedmod.awakened.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public interface MovableHud {
   String hudId();

   default String hudGroup() {
      return this.hudId();
   }

   int hudWidth();

   int hudHeight();

   HudPlacement.Box layout(int var1, int var2);

   void place(int var1, int var2, int var3, int var4, int var5, int var6);

   void resetPlacement();

   default boolean hasStoredPlacement() {
      return true;
   }

   void drawPreview(GuiGraphics var1, HudPlacement.Box var2);

   boolean enabled();

   void setEnabled(boolean var1);

   default Component hudName() {
      return Component.translatable("awakened.hud." + this.hudId());
   }

   default boolean hudRelevant() {
      return true;
   }

   default boolean hudVisible() {
      return this.enabled() && !HudPlacement.isPlacing(this);
   }

   default HudPlacement.Box previewDrop(int x, int y, int w, int h, int screenW, int screenH) {
      return new HudPlacement.Box(x, y, w, h);
   }

   default int nudgeStep(int step, int axisLength) {
      return this.snapsItself() ? Math.max(step, axisLength / 2) : step;
   }

   default boolean snapsItself() {
      return false;
   }
}
