package com.awakenedmod.awakened.client.screen;

import com.awakenedmod.awakened.client.AbilityIcons;
import com.awakenedmod.awakened.core.net.FocusedRerollPayload;
import com.awakenedmod.awakened.core.net.OpenCatalystSelectPayload;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CatalystSelectScreen extends AwakenedScreen {
   private static final int BASE_CELL_W = 74;
   private static final int BASE_CELL_H = 54;
   private static final int BASE_ICON = 24;
   private static final int CELL_GAP = 4;
   private static final float MIN_FIT = 0.62F;
   private static final int PANEL_PAD = 12;
   private static final int HEADER_H = 40;
   private static final int FOOTER_H = 58;
   private static final int PAGE_ROW_H = 16;
   private static final int COLOR_PANEL = -267710701;
   private static final int COLOR_PANEL_EDGE = -4667692;
   private static final int COLOR_BORDER = -14011072;
   private static final int COLOR_CELL_BG = -16447477;
   private static final int COLOR_TITLE = -1511692;
   private static final int COLOR_DIM = -8483943;
   private static final int COLOR_SELECTED = -9864;
   private final int picks;
   private final int xpCost;
   private final boolean offHand;
   private final List<Power> powers;
   private final Set<String> locked;
   private final Set<Power> selected = new LinkedHashSet<>();
   private Button clear;
   private int cols;
   private int rows;
   private int cellW;
   private int cellH;
   private int icon;
   private float fit;
   private int gridX;
   private int gridY;
   private int pages = 1;
   private int page;
   private int countY;
   private int pageRowY;
   private Button confirm;

   public static void open(OpenCatalystSelectPayload payload) {
      Minecraft.getInstance()
         .setScreen(new CatalystSelectScreen(payload.picks(), payload.xpCost(), payload.offHand(), payload.locked(), payload.unavailable()));
   }

   private CatalystSelectScreen(int picks, int xpCost, boolean offHand, List<String> locked, List<String> unavailable) {
      super(Component.translatable("awakened.select.title"));
      this.picks = picks;
      this.xpCost = xpCost;
      this.offHand = offHand;
      Set<String> off = Set.copyOf(unavailable);
      this.powers = PowerRegistry.all().stream().filter(p -> p.rollable() && !off.contains(p.id())).toList();
      this.locked = Set.copyOf(locked);
   }

   protected void init() {
      int availW = this.width - 36;
      int availH = this.height - 40 - 58 - 12;
      int maxCols = Math.max(2, Math.min(this.powers.size(), (availW + 4) / (Math.round(45.88F) + 4)));
      float best = 0.0F;

      for (int c = 2; c <= maxCols; c++) {
         best = Math.max(best, this.fitFor(c, availW, availH, this.rowsFor(c)));
      }

      this.cols = 2;
      float want = (float)availW / (float)availH;
      float bestShape = Float.MAX_VALUE;

      for (int c = 2; c <= maxCols; c++) {
         if (!(this.fitFor(c, availW, availH, this.rowsFor(c)) < best - 0.005F)) {
            float shape = Math.abs((float)(c * 74) / (float)(this.rowsFor(c) * 54) - want);
            if (shape < bestShape) {
               bestShape = shape;
               this.cols = c;
            }
         }
      }

      this.fit = this.fitFor(this.cols, availW, availH, this.rowsFor(this.cols));
      boolean paging = this.fit < 0.62F;
      if (paging) {
         this.cols = maxCols;
      }

      while (true) {
         if (paging) {
            this.fit = Math.max(0.62F, this.fitFor(this.cols, availW, availH, 1));
         }

         this.applyCellSize();
         if (this.cols <= 2 || this.gridW() <= availW) {
            if (paging || !(this.fit < 0.62F)) {
               this.rows = Math.max(1, paging ? (availH - 16 + 4) / (this.cellH + 4) : this.rowsFor(this.cols));
               this.pages = Math.max(1, (this.powers.size() + this.perPage() - 1) / this.perPage());
               this.page = Mth.clamp(this.page, 0, this.pages - 1);
               int gridW = this.gridW();
               int gridH = this.gridH();
               this.gridX = this.width / 2 - gridW / 2;
               this.gridY = Math.max(44, this.height / 2 - (gridH + 40 + this.footerH()) / 2 + 40);
               int buttonsY = Math.min(this.gridY + gridH + (this.pages > 1 ? 30 : 14), this.height - 26);
               this.pageRowY = buttonsY - 16;
               this.countY = (this.pages > 1 ? this.pageRowY : buttonsY) - 11;
               Component igniteLabel = this.xpCost > 0
                  ? Component.translatable("awakened.select.confirm", new Object[]{this.xpCost})
                  : Component.translatable("awakened.select.confirm.free");
               this.confirm = Button.builder(igniteLabel, b -> this.ignite()).bounds(this.width / 2 - 132, buttonsY, 130, 20).build();
               this.addRenderableWidget(this.confirm);
               this.clear = Button.builder(Component.translatable("awakened.select.clear"), b -> this.clearSelection())
                  .bounds(this.width / 2 + 4, buttonsY, 54, 20)
                  .build();
               this.addRenderableWidget(this.clear);
               this.addRenderableWidget(
                  Button.builder(Component.translatable("gui.cancel"), b -> this.onClose()).bounds(this.width / 2 + 62, buttonsY, 70, 20).build()
               );
               if (this.pages > 1) {
                  this.addRenderableWidget(
                     Button.builder(Component.literal("<"), b -> this.turnPage(-1)).bounds(this.width / 2 - 70, this.pageRowY, 16, 13).build()
                  );
                  this.addRenderableWidget(
                     Button.builder(Component.literal(">"), b -> this.turnPage(1)).bounds(this.width / 2 + 54, this.pageRowY, 16, 13).build()
                  );
               }

               this.updateConfirm();
               return;
            }

            paging = true;
         } else {
            this.cols--;
            if (!paging) {
               this.fit = this.fitFor(this.cols, availW, availH, this.rowsFor(this.cols));
            }
         }
      }
   }

   private void applyCellSize() {
      this.cellW = Math.round(74.0F * this.fit);
      this.cellH = Math.round(54.0F * this.fit);
      this.icon = Math.round(24.0F * this.fit);
   }

   private int rowsFor(int c) {
      return (this.powers.size() + c - 1) / c;
   }

   private float fitFor(int c, int availW, int availH, int r) {
      float w = (float)(availW - (c - 1) * 4) / (float)(c * 74);
      float h = (float)(availH - (r - 1) * 4) / (float)(r * 54);
      return Math.max(0.0F, Math.min(1.0F, Math.min(w, h)));
   }

   private int perPage() {
      return this.cols * this.rows;
   }

   private int footerH() {
      return this.pages > 1 ? 74 : 58;
   }

   private void turnPage(int delta) {
      this.setFocused(null);
      if (this.pages > 1) {
         this.page = Math.floorMod(this.page + delta, this.pages);
         this.click(1.0F);
      }
   }

   private int gridW() {
      return this.cols * this.cellW + (this.cols - 1) * 4;
   }

   private int gridH() {
      return this.rows * this.cellH + (this.rows - 1) * 4;
   }

   private void updateConfirm() {
      this.confirm.active = this.selected.size() == this.picks;
      if (this.clear != null) {
         this.clear.active = !this.selected.isEmpty();
      }
   }

   private void clearSelection() {
      this.selected.clear();
      this.setFocused(null);
      this.updateConfirm();
   }

   private void ignite() {
      if (this.selected.size() == this.picks) {
         List<String> ids = new ArrayList<>(this.picks);

         for (Power power : this.selected) {
            ids.add(power.id());
         }

         PacketDistributor.sendToServer(new FocusedRerollPayload(this.offHand, ids), new CustomPacketPayload[0]);
         this.onClose();
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      int gridW = this.gridW();
      int gridH = this.gridH();
      int panelX0 = Math.min(this.gridX - 12, this.width / 2 - 134);
      int panelY0 = this.gridY - 40;
      int panelX1 = Math.max(this.gridX + gridW + 12, this.width / 2 + 134);
      int panelY1 = Math.max(this.gridY + gridH + this.footerH(), this.confirm.getY() + this.confirm.getHeight() + 6);
      g.fill(panelX0 - 1, panelY0 - 1, panelX1 + 1, panelY1 + 1, -4667692);
      g.fill(panelX0, panelY0, panelX1, panelY1, -267710701);
      int cx = this.width / 2;
      g.drawCenteredString(this.font, this.title, cx, panelY0 + 9, -1511692);
      Component hint = this.picks == 1
         ? Component.translatable(this.locked.isEmpty() ? "awakened.select.hint.one" : "awakened.select.hint.one.locked")
         : Component.translatable(this.locked.isEmpty() ? "awakened.select.hint" : "awakened.select.hint.locked", new Object[]{this.picks});
      g.drawCenteredString(this.font, hint, cx, panelY0 + 23, -8483943);
      int first = this.page * this.perPage();
      int last = Math.min(this.powers.size(), first + this.perPage());

      for (int i = first; i < last; i++) {
         this.drawCell(g, this.powers.get(i), i - first, mouseX, mouseY);
      }

      Component count = Component.translatable("awakened.select.count", new Object[]{this.selected.size(), this.picks});
      g.drawCenteredString(this.font, count, cx, this.countY, this.selected.size() == this.picks ? -9864 : -8483943);
      if (this.pages > 1) {
         g.drawCenteredString(
            this.font, Component.translatable("awakened.select.page", new Object[]{this.page + 1, this.pages}), cx, this.pageRowY + 3, -8483943
         );
      }

      super.render(g, mouseX, mouseY, partialTick);
   }

   private void drawCell(GuiGraphics g, Power power, int index, int mouseX, int mouseY) {
      int x0 = this.gridX + index % this.cols * (this.cellW + 4);
      int y0 = this.gridY + index / this.cols * (this.cellH + 4);
      int rgb = power.themeColor() & 16777215;
      boolean isLocked = this.locked.contains(power.id());
      boolean isSelected = this.selected.contains(power);
      boolean hovered = !isLocked && mouseX >= x0 && mouseX < x0 + this.cellW && mouseY >= y0 && mouseY < y0 + this.cellH;
      g.fill(x0 - 1, y0 - 1, x0 + this.cellW + 1, y0 + this.cellH + 1, isSelected ? -9864 : -14011072);
      g.fill(x0, y0, x0 + this.cellW, y0 + this.cellH, -16447477);
      g.fill(x0, y0, x0 + this.cellW, y0 + this.cellH, (!hovered && !isSelected ? 671088640 : 1342177280) | rgb);
      g.fill(x0, y0, x0 + this.cellW, y0 + 2, -872415232 | rgb);
      int ix = x0 + this.cellW / 2 - this.icon / 2;
      int iy = y0 + Math.max(3, Math.round(7.0F * this.fit));
      AbilityIcons.draw(g, power, ix, iy, this.icon);
      if (isLocked) {
         g.fill(x0, y0, x0 + this.cellW, y0 + this.cellH, -2012870130);
      }

      Component name = power.displayName();
      float s = Math.min(this.fit, (float)(this.cellW - 6) / (float)this.font.width(name));
      int nameCx = x0 + this.cellW / 2;
      int nameY = y0 + this.cellH - Math.round(9.0F * s) - 3;
      int color = isLocked ? -8483943 : (isSelected ? -9864 : -1511692);
      g.pose().pushPose();
      g.pose().translate((float)nameCx, (float)nameY, 0.0F);
      g.pose().scale(s, s, 1.0F);
      g.drawCenteredString(this.font, name, 0, 0, color);
      g.pose().popPose();
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return super.mouseClicked(mouseX, mouseY, button) ? true : this.toggleAt(mouseX, mouseY);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return this.handleKey(keyCode) ? true : super.keyPressed(keyCode, scanCode, modifiers);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.pages > 1 && scrollY != 0.0) {
         this.turnPage(scrollY < 0.0 ? 1 : -1);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   private boolean handleKey(int key) {
      if (key != 263 && key != 262) {
         if (key != 257 && key != 335) {
            return false;
         } else if (this.getFocused() != null) {
            return false;
         } else if (this.selected.size() != this.picks) {
            this.click(0.5F);
            return true;
         } else {
            this.ignite();
            return true;
         }
      } else if (this.pages <= 1) {
         return false;
      } else {
         this.turnPage(key == 263 ? -1 : 1);
         return true;
      }
   }

   private boolean toggleAt(double mouseX, double mouseY) {
      int col = (int)((mouseX - (double)this.gridX) / (double)(this.cellW + 4));
      int row = (int)((mouseY - (double)this.gridY) / (double)(this.cellH + 4));
      if (!(mouseX < (double)this.gridX) && !(mouseY < (double)this.gridY) && col >= 0 && col < this.cols && row >= 0 && row < this.rows) {
         if (!(mouseX - (double)this.gridX - (double)(col * (this.cellW + 4)) >= (double)this.cellW)
            && !(mouseY - (double)this.gridY - (double)(row * (this.cellH + 4)) >= (double)this.cellH)) {
            int index = this.page * this.perPage() + row * this.cols + col;
            if (index >= this.powers.size()) {
               return false;
            } else {
               Power power = this.powers.get(index);
               if (this.locked.contains(power.id())) {
                  this.click(0.5F);
                  return true;
               } else {
                  if (this.selected.remove(power)) {
                     this.click(0.8F);
                  } else if (this.selected.size() < this.picks) {
                     this.selected.add(power);
                     this.click(1.2F);
                  } else {
                     this.click(0.5F);
                  }

                  this.updateConfirm();
                  return true;
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void click(float pitch) {
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.5F));
   }

   public boolean isPauseScreen() {
      return false;
   }
}
