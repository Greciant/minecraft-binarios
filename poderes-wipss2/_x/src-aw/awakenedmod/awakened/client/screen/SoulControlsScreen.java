package com.awakenedmod.awakened.client.screen;

import com.awakenedmod.awakened.client.ClientPowerState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class SoulControlsScreen extends AwakenedScreen {
   private static final int PANEL_W = 300;
   private static final int PAD = 12;
   private static final int TITLE_SCALE_PCT = 150;
   private static final int HEADER_H = 32;
   private static final int LINE = 10;
   private static final int SCREEN_MARGIN = 16;
   private static final int BG = -435023338;
   private static final int TEXT = -1;
   private static final int DIM = -5723984;
   private static final int CELL_BORDER = -14671832;
   private static final int BTN_BG = -870704606;
   private int backX0;
   private int backY0;
   private int backX1;
   private int backY1;

   public SoulControlsScreen() {
      super(Component.translatable("awakened.souls.legend_title"));
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(new SoulMenuScreen());
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      int theme = ClientPowerState.get().awakened() ? -13049400 : -13056257;
      int textW = 276;
      int keyW = 96;
      int ctrlW = textW - keyW;
      int lines = 5;
      int bodyH = 5 + lines * 10;
      int footerH = 18;
      int panelH = 32 + bodyH + footerH + 12;
      int x = (this.width - 300) / 2;
      int y = Math.max(16, (this.height - panelH) / 2);
      g.fill(x - 1, y - 1, x + 300 + 1, y + panelH + 1, theme);
      g.fill(x, y, x + 300, y + panelH, -435023338);
      g.fill(x, y, x + 300, y + 32 - 4, theme & 16777215 | 637534208);
      g.fill(x, y, x + 300, y + 1, 1073741824);
      g.fill(x, y, x + 1, y + panelH, 1073741824);
      g.fill(x, y + panelH - 1, x + 300, y + panelH, 587202559);
      g.fill(x + 300 - 1, y, x + 300, y + panelH, 587202559);
      int cx = x + 12;
      int cy = y + 12;
      this.drawScaledTitle(g, cx, cy, theme);
      cy = y + 32;
      g.fill(cx, cy, x + 300 - 12, cy + 1, 872415231);
      cy += 4;
      cy = this.drawRow(
         g, Component.translatable("awakened.souls.k_left"), -1, keyW, Component.translatable("awakened.souls.d_active"), -5723984, cx, cy, ctrlW
      );
      cy = this.drawRow(
         g, Component.translatable("awakened.souls.k_scroll"), -1, keyW, Component.translatable("awakened.souls.d_count"), -5723984, cx, cy, ctrlW
      );
      cy = this.drawRow(g, Component.translatable("awakened.souls.k_right"), -1, keyW, Component.translatable("awakened.souls.d_all"), -5723984, cx, cy, ctrlW);
      cy = this.drawRow(
         g, Component.translatable("awakened.souls.k_discard"), -1, keyW, Component.translatable("awakened.souls.d_discard"), -5723984, cx, cy, ctrlW
      );
      cy = this.drawRow(g, Component.translatable("awakened.souls.k_ban"), -1, keyW, Component.translatable("awakened.souls.d_ban"), -5723984, cx, cy, ctrlW);
      cy += 8;
      String backText = Component.translatable("awakened.souls.blacklist_back").getString();
      this.backX0 = cx - 2;
      this.backY0 = cy - 2;
      this.backX1 = x + 300 - 12;
      this.backY1 = cy + 10;
      boolean hoverBack = mouseX >= this.backX0 && mouseX <= this.backX1 && mouseY >= this.backY0 && mouseY <= this.backY1;
      g.fill(this.backX0 - 1, this.backY0 - 1, this.backX1 + 1, this.backY1 + 1, hoverBack ? theme : -14671832);
      g.fill(this.backX0, this.backY0, this.backX1, this.backY1, hoverBack ? -869651912 : -870704606);
      g.drawString(this.font, backText, cx, cy, hoverBack ? -1 : -5723984);
   }

   private void drawScaledTitle(GuiGraphics g, int x, int y, int color) {
      float scale = 1.5F;
      Component title = Component.translatable("awakened.souls.legend_title");
      g.pose().pushPose();
      g.pose().translate((float)x, (float)y, 0.0F);
      g.pose().scale(scale, scale, 1.0F);
      g.drawString(this.font, title, 0, 0, color);
      g.pose().popPose();
   }

   private int drawRow(GuiGraphics g, Component left, int leftColor, int leftW, Component right, int rightColor, int x, int y, int rightMaxW) {
      g.drawString(this.font, left, x, y, leftColor);
      int n = 0;

      for (FormattedCharSequence line : this.font.split(right, rightMaxW)) {
         g.drawString(this.font, line, x + leftW, y + n * 10, rightColor);
         n++;
      }

      return y + Math.max(1, n) * 10;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return this.handleClick((int)mouseX, (int)mouseY) ? true : super.mouseClicked(mouseX, mouseY, button);
   }

   private boolean handleClick(int mx, int my) {
      if (mx >= this.backX0 && mx <= this.backX1 && my >= this.backY0 && my <= this.backY1) {
         this.onClose();
         return true;
      } else {
         return false;
      }
   }
}
