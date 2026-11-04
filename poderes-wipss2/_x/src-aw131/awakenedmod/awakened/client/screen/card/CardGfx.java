package com.awakenedmod.awakened.client.screen.card;

import com.awakenedmod.awakened.client.screen.GuiPanels;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class CardGfx {
   public final GuiGraphics g;
   public final Font font;
   public final CardHits hits;
   public final int band;
   public final int mouseX;
   public final int mouseY;
   public final float scale;

   public CardGfx(GuiGraphics g, Font font, CardHits hits, int band, int mouseX, int mouseY, float scale) {
      this.g = g;
      this.font = font;
      this.hits = hits;
      this.band = band;
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      this.scale = scale;
   }

   public void fill(int x, int y, int w, int h, int argb) {
      if (w > 0 && h > 0) {
         this.g.fill(x, y, x + w, y + h, argb);
      }
   }

   public void frame(int x, int y, int w, int h, int argb) {
      this.fill(x, y, w, 2, argb);
      this.fill(x, y + h - 2, w, 2, argb);
      this.fill(x, y, 2, h, argb);
      this.fill(x + w - 2, y, 2, h, argb);
   }

   public void rule(int x, int y, int w) {
      this.fill(x, y, w, 1, -12238791);
   }

   public void bar(int x, int y, int w, float frac, int fillRgb) {
      this.fill(x, y, w, 9, -15461872);
      int fw = Math.round((float)(w - 4) * GuiPanels.clamp01(frac));
      int c = CardStyle.opaque(fillRgb);
      this.fill(x + 2, y + 2, fw, 5, c);
      this.fill(x + 2, y + 2, fw, 1, GuiPanels.lighten(c, 0.3F));
   }

   public void thinBar(int x, int y, int w, float frac, int fillRgb) {
      this.fill(x, y, w, 2, -14803942);
      this.fill(x, y, Math.round((float)w * GuiPanels.clamp01(frac)), 2, CardStyle.opaque(fillRgb));
   }

   public void slot(int x, int y, int s, int ring) {
      this.fill(x, y, s, s, ring != 0 ? CardStyle.opaque(ring) : -15461872);
      this.fill(x + 1, y + 1, s - 2, s - 2, -12238791);
      this.fill(x + 1, y + 1, s - 2, 1, -9541534);
      this.fill(x + 1, y + 1, 1, s - 2, -9541534);
   }

   public void slotHover(int x, int y, int s) {
      this.fill(x + 1, y + 1, s - 2, s - 2, -2130706433);
   }

   public int width(Component c) {
      return this.font.width(c);
   }

   public int width(String s) {
      return this.font.width(s);
   }

   public int widthBold(String s) {
      return this.font.width(bold(s));
   }

   public static Component bold(String s) {
      return Component.literal(s).withStyle(ChatFormatting.BOLD);
   }

   public static Component bold(Component c) {
      return c.copy().withStyle(ChatFormatting.BOLD);
   }

   public int text(String s, int x, int y, int argb) {
      this.g.drawString(this.font, s, x, y, argb, false);
      return this.font.width(s);
   }

   public int text(Component c, int x, int y, int argb) {
      this.g.drawString(this.font, c, x, y, argb, false);
      return this.font.width(c);
   }

   public int textBold(String s, int x, int y, int argb) {
      return this.text(bold(s), x, y, argb);
   }

   public int textBold(Component c, int x, int y, int argb) {
      return this.text(bold(c), x, y, argb);
   }

   public int textRight(String s, int right, int y, int argb) {
      int w = this.font.width(s);
      this.text(s, right - w, y, argb);
      return right - w;
   }

   public int textRight(Component c, int right, int y, int argb) {
      int w = this.font.width(c);
      this.text(c, right - w, y, argb);
      return right - w;
   }

   public int scaled(Component c, int x, int y, int argb, int scale) {
      this.g.pose().pushPose();
      this.g.pose().translate((float)x, (float)y, 0.0F);
      this.g.pose().scale((float)scale, (float)scale, 1.0F);
      this.g.drawString(this.font, c, 0, 0, argb, false);
      this.g.pose().popPose();
      return this.font.width(c) * scale;
   }

   public List<String> wrap(Component c, int w) {
      List<String> out = new ArrayList<>();

      for (FormattedText t : this.font.getSplitter().splitLines(c, Math.max(8, w), Style.EMPTY)) {
         out.add(t.getString());
      }

      return out;
   }

   public List<String> wrap(String s, int w) {
      return this.wrap(Component.literal(s), w);
   }

   public List<String> wrapNoOrphan(Component c, int w) {
      List<String> lines = this.wrap(c, w);

      for (int k = 0; k < 60 && lines.size() > 1 && lines.get(lines.size() - 1).trim().split("\\s+").length < 2; k++) {
         w -= 3;
         lines = this.wrap(c, w);
      }

      return lines;
   }

   public int paragraph(Component c, int x, int y, int w, int argb, int maxLines) {
      List<String> lines = this.wrap(c, w);
      int n = Math.min(lines.size(), maxLines);

      for (int i = 0; i < n; i++) {
         String s = lines.get(i);
         if (i == n - 1 && n < lines.size()) {
            s = this.clip(s + "...", w);
         }

         this.text(s, x, y + i * 9, argb);
      }

      return n;
   }

   public String clip(String s, int w) {
      if (this.font.width(s) <= w) {
         return s;
      } else {
         String t = s;

         while (t.length() > 1 && this.font.width(t + "...") > w) {
            t = t.substring(0, t.length() - 1);
         }

         return t.stripTrailing() + "...";
      }
   }

   public int chip(int x, int y, String label, int fixedW, boolean right, boolean on, boolean danger, boolean disabled, String key, Runnable action) {
      int tw = this.font.width(label);
      int w = fixedW > 0 ? fixedW : tw + 8;
      int lx = right ? x - w : x;
      boolean hov = !disabled && key != null && this.hits.hover(key);
      this.fill(lx, y, w, 12, -15461872);
      this.fill(lx + 1, y + 1, w - 2, 10, on ? (hov ? -1646120 : -2567480) : (hov ? -12041411 : -12962254));
      int ink = on ? -13751512 : (disabled ? -6054250 : (danger ? -1734024 : -1185569));
      this.text(label, lx + (w - tw + 1) / 2, y + 2, ink);
      if (key != null && !disabled) {
         this.hits.add(lx, y, w, 12, key, action);
      }

      return w;
   }

   public int chip(int x, int y, String label, boolean right, String key, Runnable action) {
      return this.chip(x, y, label, 0, right, false, false, false, key, action);
   }

   public void hit(int x, int y, int w, int h, String key, Runnable action) {
      this.hits.add(x, y, w, h, key, action);
   }

   public void hit(int x, int y, int w, int h, String key) {
      this.hits.add(x, y, w, h, key);
   }

   public boolean hover(String key) {
      return this.hits.hover(key);
   }

   public void texture(ResourceLocation tex, int x, int y, int size) {
      this.g.blit(tex, x, y, 0.0F, 0.0F, size, size, size, size);
   }

   public void sprite(String path, int x, int y, int w, int h) {
      ResourceLocation id = ResourceLocation.withDefaultNamespace(path);
      this.g.blitSprite(id, x, y, w, h);
   }

   public void entity(LivingEntity e, int x1, int y1, int x2, int y2, int size) {
      InventoryScreen.renderEntityInInventoryFollowsMouse(this.g, x1, y1, x2, y2, size, 0.0625F, (float)this.mouseX, (float)this.mouseY, e);
   }

   public void item(ItemStack stack, int x, int y) {
      this.g.renderItem(stack, x, y);
   }

   public void itemWithCount(ItemStack stack, int x, int y) {
      this.g.renderItem(stack, x, y);
      this.g.renderItemDecorations(this.font, stack, x, y);
   }

   public void tooltip(ItemStack stack) {
      this.g.renderTooltip(this.font, stack, this.mouseX, this.mouseY);
   }
}
