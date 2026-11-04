package com.awakenedmod.awakened.client.screen;

import com.awakenedmod.awakened.client.AwakenedKeybinds;
import com.awakenedmod.awakened.client.ReaperAuraSettings;
import com.awakenedmod.awakened.client.ReaperAuraState;
import com.awakenedmod.awakened.core.net.ReaperSetAuraPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ReaperAuraSliderScreen extends AwakenedScreen {
   private static final int BACKDROP = -1341848056;
   private static final int PANEL = -234090476;
   private static final int PANEL_EDGE = -10499872;
   private static final int PANEL_INNER = 861915360;
   private static final int TITLE = -1377281;
   private static final int DIM = -8485232;
   private static final int GAUGE_TRACK = -16447732;
   private static final int FILL_TOP = -7346442;
   private static final int FILL_BOT = -13863545;
   private static final int MARKER = -1377281;
   private static final int TICK = 1090519039;
   private static final int SEEN_HI = 13628671;
   private static final int HIDDEN_LO = 9079442;
   private static final int BTN_W = 96;
   private static final int BTN_H = 20;
   private static final int BTN_GAP = 6;
   private static final int ROW_GAP = 6;
   private static final int PANEL_PAD = 16;
   private static final int HEADER_H = 80;

   public ReaperAuraSliderScreen() {
      super(Component.translatable("awakened.reaper.aura_settings"));
   }

   public boolean isPauseScreen() {
      return false;
   }

   protected void init() {
      super.init();
      float server = ReaperAuraState.localShown();
      if (server >= 0.0F) {
         ReaperAuraSettings.seedFromServer(server);
      }

      int cx = this.width / 2;
      int rowW = 198;
      int leftX = cx - rowW / 2;
      int rightX = leftX + 96 + 6;
      int firstY = this.height / 2 - 40 + 6;
      int row2Y = firstY + 20 + 6;
      int doneY = row2Y + 20 + 6;
      this.addRenderableWidget(Button.builder(Component.literal("− 10%"), b -> this.stepAndSend(-0.1F)).bounds(leftX, firstY, 96, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("+ 10%"), b -> this.stepAndSend(0.1F)).bounds(rightX, firstY, 96, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("Hide · 0%"), b -> this.setAndSend(0.0F)).bounds(leftX, row2Y, 96, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("Show · 100%"), b -> this.setAndSend(1.0F)).bounds(rightX, row2Y, 96, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose()).bounds(leftX, doneY, rowW, 20).build());
   }

   private void stepAndSend(float delta) {
      ReaperAuraSettings.step(delta);
      send(new ReaperSetAuraPayload(ReaperAuraSettings.get()));
   }

   private void setAndSend(float pct) {
      ReaperAuraSettings.set(pct);
      send(new ReaperSetAuraPayload(ReaperAuraSettings.get()));
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Font font = this.minecraft.font;
      int cx = this.width / 2;
      int rowW = 198;
      int contentL = cx - rowW / 2;
      int contentR = cx + rowW / 2;
      int firstY = this.height / 2 - 40 + 6;
      int doneBottom = firstY + 78 - 6;
      int panelL = contentL - 16;
      int panelR = contentR + 16;
      int panelT = firstY - 80;
      int hintY = doneBottom + 8;
      int hint2Y = hintY + 9 + 1;
      int panelB = hint2Y + 9 + 8;
      float shown = ReaperAuraSettings.get();
      g.fill(0, 0, this.width, this.height, -1341848056);
      g.fill(panelL, panelT, panelR, panelB, -234090476);
      drawFrame(g, panelL, panelT, panelR, panelB, -10499872);
      drawFrame(g, panelL + 2, panelT + 2, panelR - 2, panelB - 2, 861915360);
      g.drawCenteredString(font, Component.translatable("awakened.reaper.aura_settings"), cx, panelT + 9, -1377281);
      String pct = Math.round(shown * 100.0F) + "%";
      int readoutColor = 0xFF000000 | lerpRgb(9079442, 13628671, shown);
      drawScaled(g, font, pct, cx, panelT + 30, 1.8F, readoutColor);
      g.drawCenteredString(font, Component.translatable("awakened.reaper.aura_shown_label"), cx, panelT + 44, -8485232);
      int gy0 = panelT + 56;
      int gh = 9;
      int gy1 = gy0 + gh;
      int gx0 = contentL;
      int span = contentR - contentL;
      g.fill(contentL, gy0, contentR, gy1, -16447732);
      drawFrame(g, contentL - 1, gy0 - 1, contentR + 1, gy1 + 1, -10499872);
      int fillW = Math.round((float)span * shown);
      if (fillW > 0) {
         g.fillGradient(contentL, gy0, contentL + fillW, gy1, -7346442, -13863545);
      }

      for (int i = 1; i < 4; i++) {
         int tx = gx0 + span * i / 4;
         g.fill(tx, gy0, tx + 1, gy1, 1090519039);
      }

      if (shown > 0.001F && shown < 0.999F) {
         int mx = gx0 + fillW;
         g.fill(mx - 1, gy0 - 2, mx + 1, gy1 + 2, -1377281);
      }

      g.drawString(font, Component.translatable("awakened.reaper.aura_hidden"), gx0, gy1 + 4, -8485232, false);
      Component seen = Component.translatable("awakened.reaper.aura_seen");
      g.drawString(font, seen, contentR - font.width(seen), gy1 + 4, -8485232, false);
      g.drawCenteredString(font, Component.translatable("awakened.reaper.aura_menu_hint"), cx, hintY, -8485232);
      g.drawCenteredString(font, Component.translatable("awakened.reaper.aura_menu_hint2"), cx, hint2Y, -8485232);
      super.render(g, mouseX, mouseY, partialTick);
   }

   private static void drawFrame(GuiGraphics g, int l, int t, int r, int b, int color) {
      g.fill(l, t, r, t + 1, color);
      g.fill(l, b - 1, r, b, color);
      g.fill(l, t, l + 1, b, color);
      g.fill(r - 1, t, r, b, color);
   }

   private static void drawScaled(GuiGraphics g, Font font, String text, int cx, int cy, float s, int color) {
      g.pose().pushPose();
      g.pose().translate((float)cx, (float)cy, 0.0F);
      g.pose().scale(s, s, 1.0F);
      g.drawCenteredString(font, text, 0, -(9 / 2), color);
      g.pose().popPose();
   }

   private static int lerpRgb(int a, int b, float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      int r = Math.round((float)(a >> 16 & 0xFF) + (float)((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
      int gg = Math.round((float)(a >> 8 & 0xFF) + (float)((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
      int bl = Math.round((float)(a & 0xFF) + (float)((b & 0xFF) - (a & 0xFF)) * t);
      return r << 16 | gg << 8 | bl;
   }

   private static void send(CustomPacketPayload payload) {
      PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }

   public void onClose() {
      while (AwakenedKeybinds.SECONDARY_ABILITY.consumeClick()) {
      }

      super.onClose();
   }
}
