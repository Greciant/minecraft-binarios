package com.awakenedmod.awakened.client.screen;

import com.awakenedmod.awakened.client.ClientConfig;
import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.client.FreeHud;
import com.awakenedmod.awakened.client.HudAnchor;
import com.awakenedmod.awakened.client.HudElements;
import com.awakenedmod.awakened.client.HudPlacement;
import com.awakenedmod.awakened.client.MovableHud;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

public final class HudPlacementScreen extends AwakenedScreen {
   private static final int SNAP = 6;
   private static final int DRAG_SLOP = 3;
   private static final int NUDGE_COARSE = 10;
   private static final int BTN_W = 74;
   private static final int BTN_H = 20;
   private static final int BTN_GAP = 6;
   private static final int BTN_PAD = 14;
   private static final int DIM = -5723984;
   private static final int WARN = -9846;
   private static final int LINE_H = 10;
   private static final int TEXT_TOP = 14;
   private static final int DIM_TOP = 1880100888;
   private static final int DIM_BOTTOM = -1777332200;
   private static final int OFF_WASH = -1727000556;
   private static final int GHOST_LINE = 119;
   private static final int GHOST_WASH = 28;
   private static final int HEADING_BACKDROP = -1274542064;
   private final List<MovableHud> elements;
   private int selected;
   private boolean dragging;
   private int dragX;
   private int dragY;
   private int pickupX;
   private int pickupY;
   private int dragButton = -1;
   private double grabDx;
   private double grabDy;
   private double pressX;
   private double pressY;
   private boolean dragMoved;
   private boolean changed;
   private int guideX = -1;
   private int guideY = -1;
   private float snapBandX = Float.NaN;
   private float snapBandY = Float.NaN;
   private final List<AbstractWidget> controls = new ArrayList<>();
   private int textBottom = 34;

   public HudPlacementScreen() {
      super(Component.translatable("awakened.screen.hud_move_title"));
      this.elements = HudElements.relevant();
   }

   public boolean isPauseScreen() {
      return false;
   }

   protected void init() {
      super.init();
      this.selected = Math.max(0, Math.min(this.selected, this.elements.size() - 1));
      this.endDrag();
      this.rebuildControls();
      HudPlacement.placing(this.elements);
   }

   private void rebuildControls() {
      this.clearWidgets();
      this.controls.clear();
      MovableHud hud = this.selection();
      if (hud != null) {
         this.controls.add(this.button(Component.translatable(hud.enabled() ? "awakened.screen.hud_move_hide" : "awakened.screen.hud_move_show"), b -> {
            hud.setEnabled(!hud.enabled());
            this.changed = true;
            this.rebuildControls();
         }));
         this.controls.add(this.button(Component.translatable("awakened.screen.hud_move_reset"), b -> this.reset()));
         if (this.hasGroupMates(hud)) {
            this.controls
               .add(
                  this.button(
                     Component.translatable(ClientConfig.HUD_MOVE_TOGETHER.get() ? "awakened.screen.hud_move_group_off" : "awakened.screen.hud_move_group_on"),
                     b -> {
                        ClientConfig.HUD_MOVE_TOGETHER.set(!(Boolean)ClientConfig.HUD_MOVE_TOGETHER.get());
                        this.changed = true;
                        this.rebuildControls();
                     }
                  )
               );
         }
      }

      if (this.elements.size() > 1) {
         this.controls.add(this.button(Component.translatable("awakened.screen.hud_move_reset_all"), b -> this.resetAll()));
      }

      this.controls.add(this.button(CommonComponents.GUI_DONE, b -> this.onClose()));
      this.controls.forEach(x$0 -> {
         AbstractWidget var10000 = (AbstractWidget)this.addRenderableWidget(x$0);
      });
      this.layoutControls();
   }

   private Button button(Component label, OnPress onPress) {
      int bw = Math.max(74, this.font.width(label) + 14);
      return Button.builder(label, onPress).bounds(0, 0, bw, 20).build();
   }

   public void removed() {
      HudPlacement.placing(null);
      if (this.changed) {
         ClientConfig.save();
      }

      super.removed();
   }

   @Nullable
   private MovableHud selection() {
      return this.selected >= 0 && this.selected < this.elements.size() ? this.elements.get(this.selected) : null;
   }

   private HudPlacement.Box boxOf(int index) {
      MovableHud hud = this.elements.get(index);
      HudPlacement.Box live = hud.layout(this.width, this.height);
      if (this.dragging) {
         if (index == this.selected) {
            return hud.previewDrop(this.dragX, this.dragY, live.width(), live.height(), this.width, this.height);
         }

         MovableHud held = this.selection();
         if (held != null && carries(held, hud)) {
            return this.carry(hud, live, this.dragX - this.pickupX, this.dragY - this.pickupY);
         }
      }

      return live;
   }

   private static boolean belongsWith(MovableHud hud, MovableHud other) {
      return other != hud && other.hudGroup().equals(hud.hudGroup());
   }

   private static boolean carries(MovableHud hud, MovableHud other) {
      return (Boolean)ClientConfig.HUD_MOVE_TOGETHER.get() && belongsWith(hud, other);
   }

   private boolean hasGroupMates(MovableHud hud) {
      for (MovableHud other : this.elements) {
         if (belongsWith(hud, other)) {
            return true;
         }
      }

      return false;
   }

   private List<MovableHud> carriedMates(MovableHud hud) {
      List<MovableHud> mates = new ArrayList<>();

      for (MovableHud other : this.elements) {
         if (carries(hud, other)) {
            mates.add(other);
         }
      }

      return mates;
   }

   private HudPlacement.Box movedBounds(MovableHud hud, HudPlacement.Box rest) {
      int left = rest.x();
      int top = rest.y();
      int right = rest.right();
      int bottom = rest.bottom();

      for (MovableHud mate : this.carriedMates(hud)) {
         HudPlacement.Box box = mate.layout(this.width, this.height);
         left = Math.min(left, box.x());
         top = Math.min(top, box.y());
         right = Math.max(right, box.right());
         bottom = Math.max(bottom, box.bottom());
      }

      return new HudPlacement.Box(left, top, right - left, bottom - top);
   }

   private static int clampDelta(int d, int start, int size, int screen) {
      return HudPlacement.clampToScreen(start + d, size, screen) - start;
   }

   private HudPlacement.Box carry(MovableHud mate, HudPlacement.Box rest, int dx, int dy) {
      return mate.previewDrop(rest.x() + dx, rest.y() + dy, rest.width(), rest.height(), this.width, this.height);
   }

   private void carryGroup(MovableHud hud, int dx, int dy, @Nullable HudAnchor anchor) {
      if (dx != 0 || dy != 0) {
         for (MovableHud mate : this.carriedMates(hud)) {
            HudPlacement.Box rest = mate.layout(this.width, this.height);
            HudPlacement.Box landing = this.carry(mate, rest, dx, dy);
            if (landing.x() != rest.x() || landing.y() != rest.y()) {
               this.store(mate, landing, anchor);
            }
         }
      }
   }

   private void reset() {
      MovableHud hud = this.selection();
      if (hud != null) {
         this.clear(hud);
      }

      this.endDrag();
   }

   private void resetAll() {
      for (MovableHud hud : this.elements) {
         this.clear(hud);
      }

      this.endDrag();
   }

   private void store(MovableHud hud, HudPlacement.Box box, @Nullable HudAnchor anchor) {
      if (anchor != null && hud instanceof FreeHud free) {
         free.place(box.x(), box.y(), box.width(), box.height(), this.width, this.height, anchor);
      } else {
         hud.place(box.x(), box.y(), box.width(), box.height(), this.width, this.height);
      }

      this.changed = true;
   }

   private void clear(MovableHud hud) {
      if (hud.hasStoredPlacement()) {
         hud.resetPlacement();
         this.changed = true;
      }
   }

   private void endDrag() {
      this.dragging = false;
      this.dragMoved = false;
      this.dragButton = -1;
      this.clearSnap();
   }

   private void clearSnap() {
      this.guideX = -1;
      this.guideY = -1;
      this.snapBandX = Float.NaN;
      this.snapBandY = Float.NaN;
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      g.fillGradient(0, 0, this.width, this.height, 1880100888, -1777332200);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(g, mouseX, mouseY, partialTick);
      int theme = theme();
      if (this.guideX >= 0) {
         g.fill(this.guideX, 0, this.guideX + 1, this.height, GuiPanels.withAlpha(theme, 85));
      }

      if (this.guideY >= 0) {
         g.fill(0, this.guideY, this.width, this.guideY + 1, GuiPanels.withAlpha(theme, 85));
      }

      int hovered = this.boxWinsClick((double)mouseX, (double)mouseY) ? this.elementAt((double)mouseX, (double)mouseY) : -1;

      for (int i = 0; i < this.elements.size(); i++) {
         this.drawElement(g, i, this.boxOf(i), theme, i == hovered);
      }

      this.drawDragGhost(g, theme);
      this.drawHeading(g);
      this.layoutControls();

      for (Renderable renderable : this.renderables) {
         renderable.render(g, mouseX, mouseY, partialTick);
      }
   }

   private void drawElement(GuiGraphics g, int index, HudPlacement.Box box, int theme, boolean hovered) {
      MovableHud hud = this.elements.get(index);
      hud.drawPreview(g, box);
      if (!hud.enabled()) {
         g.fill(box.x(), box.y(), box.right(), box.bottom(), -1727000556);
      }

      boolean isSelected = index == this.selected;
      int alpha = isSelected ? (!this.dragging && !hovered ? 170 : 238) : (hovered ? 136 : 68);
      GuiPanels.outline(g, box.x() - 2, box.y() - 2, box.width() + 4, box.height() + 4, GuiPanels.withAlpha(hud.enabled() ? theme : -5723984, alpha));
      if (isSelected) {
         int tick = GuiPanels.withAlpha(GuiPanels.lighten(theme, 0.4F), 255);
         int x0 = box.x() - 2;
         int y0 = box.y() - 2;
         int x1 = box.right() + 2;
         int y1 = box.bottom() + 2;
         g.fill(x0, y0, x0 + 4, y0 + 1, tick);
         g.fill(x0, y0, x0 + 1, y0 + 4, tick);
         g.fill(x1 - 4, y0, x1, y0 + 1, tick);
         g.fill(x1 - 1, y0, x1, y0 + 4, tick);
         g.fill(x0, y1 - 1, x0 + 4, y1, tick);
         g.fill(x0, y1 - 4, x0 + 1, y1, tick);
         g.fill(x1 - 4, y1 - 1, x1, y1, tick);
         g.fill(x1 - 1, y1 - 4, x1, y1, tick);
      }
   }

   private void drawDragGhost(GuiGraphics g, int theme) {
      if (this.dragging) {
         MovableHud hud = this.selection();
         if (hud != null && hud.snapsItself()) {
            HudPlacement.Box landing = this.boxOf(this.selected);
            if (landing.x() != this.dragX || landing.y() != this.dragY) {
               g.fill(this.dragX, this.dragY, this.dragX + landing.width(), this.dragY + landing.height(), GuiPanels.withAlpha(theme, 28));
               GuiPanels.outline(g, this.dragX - 2, this.dragY - 2, landing.width() + 4, landing.height() + 4, GuiPanels.withAlpha(theme, 119));
            }
         }
      }
   }

   private void drawHeading(GuiGraphics g) {
      int wrapW = Math.max(80, this.width - 24);
      MovableHud hud = this.selection();
      if (hud == null) {
         this.textBottom = this.drawWrapped(g, Component.translatable("awakened.screen.hud_move_none"), 14, -5723984, wrapW);
      } else {
         int cursor = this.drawWrapped(g, this.nameLine(hud), 14, theme(), wrapW);
         this.textBottom = this.drawWrapped(g, Component.translatable(this.hintKey(hud)), cursor, -5723984, wrapW);
      }
   }

   private Component nameLine(MovableHud hud) {
      MutableComponent line = Component.translatable("awakened.screen.hud_move_selected", new Object[]{hud.hudName()});
      if (!hud.enabled()) {
         line = line.append(CommonComponents.SPACE)
            .append(Component.translatable("awakened.screen.hud_move_off").withStyle(style -> style.withColor(16767370)));
      }

      if ((Boolean)ClientConfig.HUD_MOVE_TOGETHER.get() && this.hasGroupMates(hud)) {
         line = line.append(CommonComponents.SPACE)
            .append(Component.translatable("awakened.screen.hud_move_grouped").withStyle(style -> style.withColor(11053232)));
      }

      return line;
   }

   private String hintKey(MovableHud hud) {
      boolean many = this.elements.size() > 1;
      if (hud.snapsItself()) {
         return many ? "awakened.screen.hud_move_hint_snap_many" : "awakened.screen.hud_move_hint_snap";
      } else {
         return many ? "awakened.screen.hud_move_hint_many" : "awakened.screen.hud_move_hint";
      }
   }

   private int drawWrapped(GuiGraphics g, Component text, int top, int colour, int wrapW) {
      List<FormattedCharSequence> lines = this.font.split(text, wrapW);
      int lineY = top;

      for (FormattedCharSequence line : lines) {
         int lineW = this.font.width(line);
         int x = this.width / 2 - lineW / 2;
         if (lineW > 0) {
            g.fill(x - 3, lineY - 1, x + lineW + 3, lineY + 10 - 1, -1274542064);
         }

         g.drawString(this.font, line, x, lineY, colour);
         lineY += 10;
      }

      return lineY;
   }

   private void layoutControls() {
      if (!this.controls.isEmpty()) {
         int rowW = -6;

         for (AbstractWidget control : this.controls) {
            rowW += control.getWidth() + 6;
         }

         int left = (this.width - rowW) / 2;
         int rowY = this.height - 20 - 8;
         if (this.selected < this.elements.size()) {
            HudPlacement.Box box = this.boxOf(this.selected);
            if (box.overlaps(left, rowY, rowW, 20)) {
               rowY = Math.max(this.textBottom + 4, box.y() - 20 - 6);
            }
         }

         int cursor = left;

         for (AbstractWidget control : this.controls) {
            control.setPosition(cursor, rowY);
            cursor += control.getWidth() + 6;
         }
      }
   }

   private static int theme() {
      Power power = PowerRegistry.get(ClientPowerState.get().powerId());
      return power != null ? power.themeColor() : -13056257;
   }

   private int elementAt(double mx, double my) {
      if (this.selected < this.elements.size() && this.boxOf(this.selected).contains(mx, my)) {
         return this.selected;
      } else {
         int best = -1;
         long bestArea = Long.MAX_VALUE;

         for (int i = this.elements.size() - 1; i >= 0; i--) {
            HudPlacement.Box box = this.boxOf(i);
            if (box.contains(mx, my)) {
               long area = (long)box.width() * (long)box.height();
               if (area < bestArea) {
                  bestArea = area;
                  best = i;
               }
            }
         }

         return best;
      }
   }

   private boolean boxWinsClick(double mx, double my) {
      if (this.selected < this.elements.size() && this.boxOf(this.selected).contains(mx, my)) {
         return true;
      } else {
         for (AbstractWidget control : this.controls) {
            if (control.isMouseOver(mx, my)) {
               return false;
            }
         }

         return true;
      }
   }

   private void select(int index) {
      if (index != this.selected && index >= 0 && index < this.elements.size()) {
         this.selected = index;
         this.endDrag();
         this.rebuildControls();
      }
   }

   private boolean grab(double mx, double my, int button) {
      int index = this.elementAt(mx, my);
      if (index < 0) {
         return false;
      } else {
         this.select(index);
         HudPlacement.Box box = this.boxOf(index);
         this.dragging = true;
         this.dragMoved = false;
         this.dragButton = button;
         this.dragX = box.x();
         this.dragY = box.y();
         this.pickupX = box.x();
         this.pickupY = box.y();
         this.pressX = mx;
         this.pressY = my;
         this.grabDx = mx - (double)box.x();
         this.grabDy = my - (double)box.y();
         return true;
      }
   }

   private void dragTo(double mx, double my) {
      if (!this.dragMoved) {
         if (Math.abs(mx - this.pressX) < 3.0 && Math.abs(my - this.pressY) < 3.0) {
            return;
         }

         this.dragMoved = true;
      }

      this.moveTo((int)Math.round(mx - this.grabDx), (int)Math.round(my - this.grabDy));
   }

   private void moveTo(int nx, int ny) {
      this.clearSnap();
      MovableHud hud = this.elements.get(this.selected);
      HudPlacement.Box bounds = this.heldBounds();
      int offX = this.pickupX - bounds.x();
      int offY = this.pickupY - bounds.y();
      if (!hud.snapsItself()) {
         int nearX = 2 + offX;
         int farX = this.width - bounds.width() - 2 + offX;
         int midX = (this.width - bounds.width()) / 2 + offX;
         if (Math.abs(nx - nearX) <= 6) {
            nx = nearX;
            this.guideX = 2;
            this.snapBandX = 0.0F;
         } else if (Math.abs(nx - farX) <= 6) {
            nx = farX;
            this.guideX = this.width - 2 - 1;
            this.snapBandX = 1.0F;
         } else if (Math.abs(nx - midX) <= 6) {
            nx = midX;
            this.guideX = this.width / 2;
            this.snapBandX = 0.5F;
         }

         int nearY = 2 + offY;
         int farY = this.height - bounds.height() - 2 + offY;
         int midY = (this.height - bounds.height()) / 2 + offY;
         if (Math.abs(ny - nearY) <= 6) {
            ny = nearY;
            this.guideY = 2;
            this.snapBandY = 0.0F;
         } else if (Math.abs(ny - farY) <= 6) {
            ny = farY;
            this.guideY = this.height - 2 - 1;
            this.snapBandY = 1.0F;
         } else if (Math.abs(ny - midY) <= 6) {
            ny = midY;
            this.guideY = this.height / 2;
            this.snapBandY = 0.5F;
         }
      }

      this.holdAt(bounds, nx, ny);
   }

   private HudPlacement.Box heldBounds() {
      MovableHud hud = this.elements.get(this.selected);
      HudPlacement.Box live = hud.layout(this.width, this.height);
      return this.movedBounds(hud, new HudPlacement.Box(this.pickupX, this.pickupY, live.width(), live.height()));
   }

   private void holdAt(HudPlacement.Box bounds, int nx, int ny) {
      this.dragX = this.pickupX + clampDelta(nx - this.pickupX, bounds.x(), bounds.width(), this.width);
      this.dragY = this.pickupY + clampDelta(ny - this.pickupY, bounds.y(), bounds.height(), this.height);
   }

   private void drop() {
      if (this.dragging) {
         MovableHud hud = this.selection();
         if (hud != null) {
            HudPlacement.Box box = this.boxOf(this.selected);
            if (box.x() != this.pickupX || box.y() != this.pickupY) {
               HudAnchor anchor = hud.snapsItself() ? null : this.droppedAnchor(box);
               this.store(hud, box, anchor);
               this.carryGroup(hud, this.dragX - this.pickupX, this.dragY - this.pickupY, anchor);
            }
         }

         this.endDrag();
      }
   }

   private HudAnchor droppedAnchor(HudPlacement.Box box) {
      HudAnchor inferred = HudAnchor.nearest(box.x() + box.width() / 2, box.y() + box.height() / 2, this.width, this.height);
      return HudAnchor.of(Float.isNaN(this.snapBandX) ? inferred.bandX() : this.snapBandX, Float.isNaN(this.snapBandY) ? inferred.bandY() : this.snapBandY);
   }

   public void onClose() {
      this.drop();
      if (this.minecraft != null) {
         this.minecraft.setScreen(new CharacterScreen(6));
      } else {
         super.onClose();
      }
   }

   private boolean handleKey(int key, boolean shift) {
      if (this.elements.isEmpty()) {
         return false;
      } else if (key == 258) {
         int step = shift ? this.elements.size() - 1 : 1;
         this.select((this.selected + step) % this.elements.size());
         return true;
      } else {
         int dirX = key == 263 ? -1 : (key == 262 ? 1 : 0);
         int dirY = key == 265 ? -1 : (key == 264 ? 1 : 0);
         if (dirX == 0 && dirY == 0) {
            return false;
         } else {
            MovableHud hud = this.selection();
            if (hud == null) {
               return false;
            } else {
               int step = shift ? 10 : 1;
               HudPlacement.Box box = this.boxOf(this.selected);
               int w = box.width();
               int h = box.height();
               if (this.dragging) {
                  int wasX = this.dragX;
                  int wasY = this.dragY;
                  this.holdAt(this.heldBounds(), this.dragX + dirX * step, this.dragY + dirY * step);
                  this.grabDx = this.grabDx - (double)(this.dragX - wasX);
                  this.grabDy = this.grabDy - (double)(this.dragY - wasY);
                  this.clearSnap();
                  return true;
               } else {
                  HudPlacement.Box bounds = this.movedBounds(hud, box);
                  int dx = clampDelta(dirX * hud.nudgeStep(step, this.width), bounds.x(), bounds.width(), this.width);
                  int dy = clampDelta(dirY * hud.nudgeStep(step, this.height), bounds.y(), bounds.height(), this.height);
                  HudPlacement.Box landed = hud.previewDrop(box.x() + dx, box.y() + dy, w, h, this.width, this.height);
                  if (landed.x() != box.x() || landed.y() != box.y()) {
                     this.store(hud, landed, null);
                     this.carryGroup(hud, dx, dy, null);
                  }

                  return true;
               }
            }
         }
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return this.handleKey(keyCode, hasShiftDown()) ? true : super.keyPressed(keyCode, scanCode, modifiers);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return button == 0 && this.boxWinsClick(mouseX, mouseY) && this.grab(mouseX, mouseY, button) ? true : super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.dragging && button == this.dragButton) {
         this.dragTo(mouseX, mouseY);
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == this.dragButton) {
         this.drop();
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }
}
