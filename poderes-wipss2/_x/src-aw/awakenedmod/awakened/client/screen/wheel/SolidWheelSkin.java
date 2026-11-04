package com.awakenedmod.awakened.client.screen.wheel;

import com.awakenedmod.awakened.client.screen.RadialWheelScreen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public final class SolidWheelSkin implements WheelSkin {
   private static final int MAX_OUTER = 92;
   private static final double GAP = 2.0;
   private static final float ICON_SCALE = 1.5F;
   private static final int SPRITE_SIZE = 32;
   private static final int MAX_LABEL_LINES = 2;
   private static final int BACKDROP = 805306368;
   private static final int WEDGE = -1340859870;
   private static final int WEDGE_SELECTED = -1071368888;
   private static final int WEDGE_UNAVAILABLE = -1339419624;
   private static final int WEDGE_UNAVAILABLE_SELECTED = -1068883928;
   private static final int HUB = -804515818;
   private static final int TEXT = -1;
   private static final int TEXT_DIM = -6642769;
   private static final int COST_OK = -3610976;
   private static final int COST_LOW = -1949638;
   private int width;
   private int height;
   private int cx;
   private int cy;
   private int outer;
   private int inner;
   private int count;
   private final List<List<int[]>> wedgeRuns = new ArrayList<>();
   private List<int[]> hubRuns = List.of();

   @Override
   public void layout(int width, int height, int count) {
      this.width = width;
      this.height = height;
      this.cx = width / 2;
      this.cy = height / 2;
      this.count = count;
      this.outer = Math.max(40, Math.min(92, Math.min(width, height) / 2 - 8));
      this.inner = this.outer * 2 / 5;
      this.wedgeRuns.clear();

      for (int i = 0; i < count; i++) {
         int wedge = i;
         this.wedgeRuns.add(this.rasterise(this.outer, (dx, dy) -> this.insideWedge(dx, dy, wedge)));
      }

      this.hubRuns = this.rasterise(this.inner, (dx, dy) -> dx * dx + dy * dy <= ((double)this.inner - 2.0) * ((double)this.inner - 2.0));
   }

   @Override
   public int hover(double dx, double dy) {
      return this.count > 0 && !(Math.sqrt(dx * dx + dy * dy) < (double)this.inner)
         ? Math.floorMod((int)Math.round(clockwiseAngle(dx, dy) / this.step()), this.count)
         : -1;
   }

   @Override
   public void draw(GuiGraphics g, Font font, List<RadialWheelScreen.RadialOption> options, int hovered) {
      g.fill(0, 0, this.width, this.height, 805306368);

      for (int i = 0; i < options.size() && i < this.wedgeRuns.size(); i++) {
         RadialWheelScreen.RadialOption opt = options.get(i);
         fillRuns(g, this.wedgeRuns.get(i), wedgeColor(opt, i == hovered));
      }

      fillRuns(g, this.hubRuns, -804515818);
      double iconRadius = (double)(this.inner + this.outer) / 2.0;

      for (int i = 0; i < options.size() && i < this.wedgeRuns.size(); i++) {
         this.drawIcon(g, font, options.get(i), this.centreAngle(i), iconRadius);
      }

      this.drawHubText(g, font, options, hovered);
   }

   private void drawHubText(GuiGraphics g, Font font, List<RadialWheelScreen.RadialOption> options, int hovered) {
      int shown = hovered >= 0 ? hovered : selectedIndex(options);
      if (shown >= 0) {
         RadialWheelScreen.RadialOption opt = options.get(shown);
         boolean live = hovered >= 0;
         Component detail = opt.locked() ? opt.lockHint() : opt.cost();
         List<FormattedCharSequence> lines = this.labelLines(font, opt.label());
         int lineStep = 9 + 1;
         int blockHeight = lines.size() * lineStep + (detail == null ? 0 : lineStep + 1) - 1;
         int y = this.cy - blockHeight / 2;
         int labelColor = live && !opt.locked() ? -1 : -6642769;

         for (FormattedCharSequence line : lines) {
            g.drawCenteredString(font, line, this.cx, y, labelColor);
            y += lineStep;
         }

         if (detail != null) {
            int color = opt.locked() ? -6642769 : (opt.affordable() ? -3610976 : -1949638);
            g.drawCenteredString(font, detail, this.cx, y + 1, color);
         }
      }
   }

   private List<FormattedCharSequence> labelLines(Font font, Component label) {
      int maxWidth = Math.max(20, (int)(2.0 * ((double)this.inner - 2.0)) - 8);
      List<FormattedCharSequence> lines = font.split(label, maxWidth);
      return lines.size() <= 2 ? lines : lines.subList(0, 2);
   }

   private static int selectedIndex(List<RadialWheelScreen.RadialOption> options) {
      for (int i = 0; i < options.size(); i++) {
         if (options.get(i).selected()) {
            return i;
         }
      }

      return -1;
   }

   private static int wedgeColor(RadialWheelScreen.RadialOption opt, boolean hovered) {
      if (unavailable(opt)) {
         if (hovered) {
            return -1069539288;
         } else {
            return opt.selected() ? -1068883928 : -1339419624;
         }
      } else if (hovered) {
         return -536870912 | opt.color() & 16777215;
      } else {
         return opt.selected() ? -1071368888 : -1340859870;
      }
   }

   private static boolean unavailable(RadialWheelScreen.RadialOption opt) {
      return opt.locked() || !opt.affordable();
   }

   private void drawIcon(GuiGraphics g, Font font, RadialWheelScreen.RadialOption opt, double angle, double radius) {
      int x = (int)Math.round((double)this.cx + Math.sin(angle) * radius);
      int y = (int)Math.round((double)this.cy - Math.cos(angle) * radius);
      if (!opt.stack().isEmpty()) {
         drawScaledItem(g, opt.stack(), x, y);
         int half = 12;
         if (opt.stack().getCount() > 1) {
            g.renderItemDecorations(font, opt.stack(), x + half - 16, y + half - 16, String.valueOf(opt.stack().getCount()));
         }
      } else if (opt.icon() != null) {
         int half = 16;
         g.blit(opt.icon(), x - half, y - half, 32, 32, 0.0F, 0.0F, 16, 16, 16, 16);
      } else {
         int sw = 14;
         g.fill(x - sw / 2, y - sw / 2, x + sw / 2, y + sw / 2, opt.color());
      }
   }

   private static void drawScaledItem(GuiGraphics g, ItemStack stack, int x, int y) {
      g.pose().pushPose();
      g.pose().translate((float)x, (float)y, 0.0F);
      g.pose().scale(1.5F, 1.5F, 1.0F);
      g.renderItem(stack, -8, -8);
      g.pose().popPose();
   }

   private double step() {
      return (Math.PI * 2) / (double)this.count;
   }

   private double centreAngle(int wedge) {
      return (double)wedge * this.step();
   }

   private boolean insideWedge(double dx, double dy, int wedge) {
      if (this.count != 1) {
         return insideSector(dx, dy, (double)this.inner, (double)this.outer, this.centreAngle(wedge), this.step() / 2.0);
      } else {
         double r = Math.sqrt(dx * dx + dy * dy);
         return r >= (double)this.inner && r <= (double)this.outer;
      }
   }

   private static boolean insideSector(double dx, double dy, double rInner, double rOuter, double centre, double half) {
      double r = Math.sqrt(dx * dx + dy * dy);
      if (!(r < rInner) && !(r > rOuter)) {
         double offset = Math.abs(wrap(clockwiseAngle(dx, dy) - centre));
         return offset <= half && r * Math.sin(half - offset) >= 1.0;
      } else {
         return false;
      }
   }

   private static double clockwiseAngle(double dx, double dy) {
      return Math.atan2(dx, -dy);
   }

   private static double wrap(double angle) {
      double wrapped = angle % (Math.PI * 2);
      if (wrapped > Math.PI) {
         return wrapped - (Math.PI * 2);
      } else {
         return wrapped < -Math.PI ? wrapped + (Math.PI * 2) : wrapped;
      }
   }

   private List<int[]> rasterise(int bound, SolidWheelSkin.Shape shape) {
      List<int[]> runs = new ArrayList<>();

      for (int dy = -bound; dy <= bound; dy++) {
         int start = Integer.MIN_VALUE;

         for (int dx = -bound; dx <= bound + 1; dx++) {
            boolean in = dx <= bound && shape.contains((double)dx + 0.5, (double)dy + 0.5);
            if (in && start == Integer.MIN_VALUE) {
               start = dx;
            } else if (!in && start != Integer.MIN_VALUE) {
               runs.add(new int[]{this.cx + start, this.cy + dy, this.cx + dx, this.cy + dy + 1});
               start = Integer.MIN_VALUE;
            }
         }
      }

      return runs;
   }

   private static void fillRuns(GuiGraphics g, List<int[]> runs, int color) {
      for (int[] run : runs) {
         g.fill(run[0], run[1], run[2], run[3], color);
      }
   }

   private interface Shape {
      boolean contains(double var1, double var3);
   }
}
