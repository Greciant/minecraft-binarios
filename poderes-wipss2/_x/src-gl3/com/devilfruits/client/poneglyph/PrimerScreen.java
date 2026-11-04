package com.devilfruits.client.poneglyph;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.poneglyph.Cipher;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public class PrimerScreen extends Screen {
   private static final ResourceLocation TEX = DevilFruits.id("textures/gui/poneglyph/primer.png");
   private static final int W = 280;
   private static final int H = 180;
   private static final int PAGE_W = 112;
   private static final String[] EXAMPLES = new String[]{"sea", "stone", "ancient", "forge", "blade"};
   private static final int INK = -12900844;
   private static final int GLYPH = -14721176;
   private static final int FADED = -8361652;
   private int spread;
   private PageButton next;
   private PageButton back;

   public PrimerScreen() {
      super(Component.translatable("item.devilfruits.ancient_script_primer"));
   }

   private int left() {
      return (this.width - 280) / 2;
   }

   private int top() {
      return (this.height - 180) / 2;
   }

   protected void init() {
      this.next = (PageButton)this.addRenderableWidget(new PageButton(this.left() + 280 - 44, this.top() + 180 - 20, true, b -> this.turn(1), true));
      this.back = (PageButton)this.addRenderableWidget(new PageButton(this.left() + 21, this.top() + 180 - 20, false, b -> this.turn(-1), true));
      this.update();
   }

   void turn(int d) {
      this.spread = Math.max(0, Math.min(1, this.spread + d));
      this.update();
   }

   private void update() {
      this.next.visible = this.spread < 1;
      this.back.visible = this.spread > 0;
   }

   public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(gg, mouseX, mouseY, partialTick);
      gg.blit(TEX, this.left(), this.top(), 0.0F, 0.0F, 280, 180, 512, 256);
   }

   public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
      super.render(gg, mouseX, mouseY, partialTick);
      int lx = this.left() + 18;
      int rx = this.left() + 140 + 10;
      int y = this.top() + 14;
      if (this.spread == 0) {
         this.centered(gg, Component.translatable("poneglyph.devilfruits.primer.title"), lx + 56, y, -12900844);
         int ty = y + 16;

         for (FormattedCharSequence line : this.font.split(Component.translatable("poneglyph.devilfruits.primer.intro"), 112)) {
            gg.drawString(this.font, line, lx, ty, -12900844, false);
            ty += 10;
         }

         if (ClientPoneglyphState.worldbound()) {
            for (FormattedCharSequence line : this.font.split(Component.translatable("poneglyph.devilfruits.primer.worldbound"), 112)) {
               gg.drawString(this.font, line, lx, ty + 4, -7726064, false);
               ty += 10;
            }
         }

         this.table(gg, rx, y, 0);
         this.footer(gg, 1, 2);
      } else {
         this.table(gg, lx, y, 13);
         this.centered(gg, Component.translatable("poneglyph.devilfruits.primer.examples"), rx + 56, y, -12900844);
         int ey = y + 18;

         for (String word : EXAMPLES) {
            gg.pose().pushPose();
            gg.pose().translate((float)rx + 56.0F, (float)ey, 0.0F);
            gg.pose().scale(1.5F, 1.5F, 1.0F);
            MutableComponent glyphs = glyphs(word);
            gg.drawString(this.font, glyphs, -this.font.width(glyphs) / 2, 0, -14721176, false);
            gg.pose().popPose();
            this.centered(gg, Component.literal(word), rx + 56, ey + 14, -8361652);
            ey += 27;
         }

         this.footer(gg, 3, 4);
      }
   }

   private void table(GuiGraphics gg, int x, int y, int from) {
      int[] perm = ClientPoneglyphState.permutation();

      for (int i = 0; i < 13; i++) {
         int letter = from + i;
         int col = i % 2;
         int row = i / 2;
         int cx = x + 8 + col * 56;
         int cy = y + 4 + row * 21;
         gg.pose().pushPose();
         gg.pose().translate((float)cx, (float)cy, 0.0F);
         gg.pose().scale(2.0F, 2.0F, 1.0F);
         gg.drawString(
            this.font, Component.literal(String.valueOf((char)(97 + perm[letter]))).withStyle(Style.EMPTY.withFont(Cipher.FONT)), 0, 0, -14721176, false
         );
         gg.pose().popPose();
         gg.drawString(this.font, "= " + (char)(65 + letter), cx + 18, cy + 4, -12900844, false);
      }
   }

   private void footer(GuiGraphics gg, int a, int b) {
      this.centered(gg, Component.literal(String.valueOf(a)), this.left() + 18 + 56, this.top() + 180 - 16, -8361652);
      this.centered(gg, Component.literal(String.valueOf(b)), this.left() + 140 + 10 + 56, this.top() + 180 - 16, -8361652);
   }

   private static MutableComponent glyphs(String word) {
      return Component.literal(Cipher.encode(word, ClientPoneglyphState.permutation())).withStyle(Style.EMPTY.withFont(Cipher.FONT));
   }

   private void centered(GuiGraphics gg, Component text, int cx, int y, int color) {
      gg.drawString(this.font, text, cx - this.font.width(text) / 2, y, color, false);
   }

   public boolean isPauseScreen() {
      return false;
   }
}
