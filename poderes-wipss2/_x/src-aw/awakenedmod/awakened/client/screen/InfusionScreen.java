package com.awakenedmod.awakened.client.screen;

import com.awakenedmod.awakened.core.net.BeginInfusionPayload;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

public final class InfusionScreen extends AwakenedScreen {
   private static final int PANEL_W = 252;
   private static final int PANEL_H = 176;
   private static final int PANEL_MARGIN = 14;
   private static final int BAR_W = 200;
   private static final int BAR_H = 14;
   private static final int CAPACITY = 4;
   private static final int MAX_TIER = 5;
   private static final int COLOR_PANEL = -267710701;
   private static final int COLOR_PANEL_EDGE = -4667692;
   private static final int COLOR_BORDER = -14011072;
   private static final int COLOR_CELL_BG = -16447477;
   private static final int COLOR_ADAMANTIUM = -3417882;
   private static final int COLOR_ADAMANTIUM_DIM = -11114634;
   private static final int COLOR_LABEL = -4667692;
   private static final int COLOR_DIM = -8483943;
   private static final int COLOR_READY = -7610208;
   private static final int COLOR_WAIT = -1727925;
   private final BlockPos pos;
   private final int charge;
   private final int tier;
   private final boolean ready;
   private List<FormattedCharSequence> benefitLines;
   private int benefitsY;
   private int hintY;

   public InfusionScreen(BlockPos pos, float chargeFraction, int tier, boolean ready) {
      super(Component.translatable("awakened.infusion.title"));
      this.pos = pos;
      this.charge = Math.round(Math.max(0.0F, Math.min(1.0F, chargeFraction)) * 4.0F);
      this.tier = tier;
      this.ready = ready;
   }

   protected void init() {
      int cx = this.width / 2;
      int top = (this.height - 176) / 2;
      this.benefitsY = top + 86;
      this.benefitLines = this.font.split(Component.translatable("awakened.infusion.benefits"), 224);
      this.hintY = this.benefitsY + this.benefitLines.size() * 9 + 4;
      int beginY = this.hintY + 14;
      int cancelY = beginY + 26;
      Button begin = Button.builder(Component.translatable("awakened.infusion.begin"), b -> {
         PacketDistributor.sendToServer(new BeginInfusionPayload(this.pos), new CustomPacketPayload[0]);
         this.onClose();
      }).bounds(cx - 100, beginY, 200, 20).build();
      begin.active = this.ready;
      this.addRenderableWidget(begin);
      this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose()).bounds(cx - 50, cancelY, 100, 20).build());
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      int cx = this.width / 2;
      int top = (this.height - 176) / 2;
      int left = cx - 126;
      g.fill(left - 1, top - 1, cx + 126 + 1, top + 176 + 1, -4667692);
      g.fill(left, top, cx + 126, top + 176, -267710701);
      g.drawCenteredString(this.font, this.title, cx, top + 12, -3417882);
      int barX = cx - 100;
      int barY = top + 42;
      g.drawString(this.font, Component.translatable("awakened.infusion.charge"), barX, top + 30, -4667692);
      g.drawString(this.font, this.charge + " / 4", barX + 200 - this.font.width(this.charge + " / 4"), top + 30, -3417882);
      int gap = 3;
      int cellW = (200 - gap * 3) / 4;

      for (int i = 0; i < 4; i++) {
         int x0 = barX + i * (cellW + gap);
         g.fill(x0 - 1, barY - 1, x0 + cellW + 1, barY + 14 + 1, -14011072);
         g.fill(x0, barY, x0 + cellW, barY + 14, i < this.charge ? -3417882 : -16447477);
      }

      int pipY = top + 70;
      Component coating = Component.translatable("awakened.infusion.tier", new Object[]{this.tier, 5});
      g.drawString(this.font, coating, barX, pipY + 1, -4667692);
      int pipR = 4;
      int pipsX = barX + 200 - 5 * (pipR * 2 + 3);

      for (int i = 0; i < 5; i++) {
         int x0 = pipsX + i * (pipR * 2 + 3);
         g.fill(x0, pipY, x0 + pipR * 2, pipY + pipR * 2, i < this.tier ? -3417882 : -11114634);
      }

      for (int i = 0; i < this.benefitLines.size(); i++) {
         FormattedCharSequence line = this.benefitLines.get(i);
         g.drawString(this.font, line, cx - this.font.width(line) / 2, this.benefitsY + i * 9, -8483943);
      }

      Component hint = Component.translatable(this.ready ? "awakened.infusion.ready" : "awakened.infusion.not_ready");
      g.drawCenteredString(this.font, hint, cx, this.hintY, this.ready ? -1727925 : -8483943);
      super.render(g, mouseX, mouseY, partialTick);
   }

   public boolean isPauseScreen() {
      return false;
   }
}
