package com.devilfruits.client.hud;

import com.devilfruits.client.ClientFruitState;
import com.devilfruits.client.input.DFKeys;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.gear.GearDefinition;
import com.devilfruits.core.gear.GearRegistry;
import com.devilfruits.core.gear.GearState;
import com.devilfruits.core.gear.GearSwitchPayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

public class GearWheelScreen extends Screen {
   private static final int ICON = 48;
   private static final int SUB_ICON = 40;
   private static final float SUB_SECTOR = 40.0F;
   private static final int SUB_GAP = 18;
   private final DevilFruit fruit;
   private final List<GearDefinition> gears;
   private int hovered = -1;
   private int hoveredSub = -1;
   private int open = -1;
   private final boolean holdToUse;
   @Nullable
   public static int[] capturePointer;

   public GearWheelScreen(DevilFruit fruit) {
      this(fruit, true);
   }

   public GearWheelScreen(DevilFruit fruit, boolean holdToUse) {
      super(Component.translatable("screen.devilfruits.gear_wheel"));
      this.fruit = fruit;
      this.gears = GearRegistry.of(fruit);
      this.holdToUse = holdToUse;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private int radius() {
      return Math.min(78, Math.min(this.width, this.height) / 2 - 34);
   }

   private double angle(int i) {
      return (-Math.PI / 2) + (double)i * Math.PI * 2.0 / (double)this.gears.size();
   }

   private int subX(int gear, int sub) {
      return this.subCentres(gear)[sub][0];
   }

   private int subY(int gear, int sub) {
      return this.subCentres(gear)[sub][1];
   }

   private int[][] subCentres(int gear) {
      int n = this.gears.get(gear).subGears().size();
      double a = this.angle(gear);
      double gx = (double)this.width / 2.0 + Math.cos(a) * (double)this.radius();
      double gy = (double)this.height / 2.0 + Math.sin(a) * (double)this.radius();
      double out = Math.abs(Math.cos(a)) > 0.2 ? (Math.cos(a) > 0.0 ? 0.0 : Math.PI) : (Math.sin(a) > 0.0 ? Math.PI / 2 : -Math.PI / 2);
      double ring = 62.0;
      double step = 2.0 * Math.asin(Math.min(1.0, 29.0 / ring));
      double[][] at = new double[n][2];
      double minX = Double.MAX_VALUE;
      double maxX = -Double.MAX_VALUE;
      double minY = Double.MAX_VALUE;
      double maxY = -Double.MAX_VALUE;

      for (int s = 0; s < n; s++) {
         double t = out + ((double)s - (double)(n - 1) * 0.5) * step;
         at[s][0] = gx + Math.cos(t) * ring;
         at[s][1] = gy + Math.sin(t) * ring;
         minX = Math.min(minX, at[s][0]);
         maxX = Math.max(maxX, at[s][0]);
         minY = Math.min(minY, at[s][1]);
         maxY = Math.max(maxY, at[s][1]);
      }

      double edge = 22.0;
      double shiftX = minX < edge ? edge - minX : (maxX > (double)this.width - edge ? (double)this.width - edge - maxX : 0.0);
      double shiftY = minY < edge ? edge - minY : (maxY > (double)this.height - edge ? (double)this.height - edge - maxY : 0.0);
      int[][] centres = new int[n][2];

      for (int s = 0; s < n; s++) {
         centres[s][0] = (int)Math.round(at[s][0] + shiftX);
         centres[s][1] = (int)Math.round(at[s][1] + shiftY);
      }

      return centres;
   }

   private int subAt(int gear, double dx, double dy) {
      int[][] at = this.subCentres(gear);
      double half = 23.0;

      for (int s = 0; s < at.length; s++) {
         if (Math.abs((double)this.width / 2.0 + dx - (double)at[s][0]) <= half && Math.abs((double)this.height / 2.0 + dy - (double)at[s][1]) <= half) {
            return s;
         }
      }

      return -1;
   }

   public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
      gg.fillGradient(0, 0, this.width, this.height, 1342177280, 1879048192);
   }

   public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
      super.render(gg, mouseX, mouseY, partialTick);
      if (!this.gears.isEmpty()) {
         int cx = this.width / 2;
         int cy = this.height / 2;
         if (capturePointer != null && capturePointer[0] < this.gears.size()) {
            int g = capturePointer[0];
            boolean sub = capturePointer[1] >= 0 && capturePointer[1] < this.gears.get(g).subGears().size();
            if (sub) {
               this.pick(Math.cos(this.angle(g)) * (double)this.radius() * 0.8, Math.sin(this.angle(g)) * (double)this.radius() * 0.8);
            }

            mouseX = sub ? this.subX(g, capturePointer[1]) : cx + (int)Math.round(Math.cos(this.angle(g)) * (double)this.radius());
            mouseY = sub ? this.subY(g, capturePointer[1]) : cy + (int)Math.round(Math.sin(this.angle(g)) * (double)this.radius());
         }

         this.pick((double)(mouseX - cx), (double)(mouseY - cy));
         GearState state = GearState.of(this.fruit, ClientFruitState.form());

         for (int i = 0; i < this.gears.size(); i++) {
            GearDefinition gear = this.gears.get(i);
            int x = cx + (int)Math.round(Math.cos(this.angle(i)) * (double)this.radius()) - 24;
            int y = cy + (int)Math.round(Math.sin(this.angle(i)) * (double)this.radius()) - 24;
            boolean current = state != null && state.gear() == gear;
            this.drawGear(gg, gear, gear, x, y, 48, i == this.hovered && this.hoveredSub < 0, current);
         }

         if (this.open >= 0) {
            GearDefinition owner = this.gears.get(this.open);

            for (int s = 0; s < owner.subGears().size(); s++) {
               GearDefinition sub = owner.subGears().get(s);
               boolean current = state != null && state.subGear() == sub;
               this.drawGear(gg, owner, sub, this.subX(this.open, s) - 20, this.subY(this.open, s) - 20, 40, s == this.hoveredSub, current);
            }
         }

         GearDefinition shown = this.hovered < 0
            ? (state == null ? null : state.current())
            : (this.hoveredSub >= 0 ? this.gears.get(this.hovered).subGears().get(this.hoveredSub) : this.gears.get(this.hovered));
         GearDefinition owner = this.hovered >= 0 ? this.gears.get(this.hovered) : (state == null ? null : state.gear());
         if (shown != null && owner != null) {
            this.info(gg, owner, shown, cx, cy);
            if (this.hovered >= 0 && (!owner.available() || !shown.available())) {
               gg.renderTooltip(this.font, Component.translatable("gear.devilfruits.coming_soon").withStyle(ChatFormatting.GRAY), mouseX, mouseY);
            }
         }
      }
   }

   private void drawGear(GuiGraphics gg, GearDefinition owner, GearDefinition gear, int x, int y, int size, boolean hover, boolean current) {
      boolean usable = selectable(owner, gear);
      gg.fill(x - 2, y - 2, x + size + 2, y + size + 2, hover ? -1339021256 : -1877470176);
      int edge = current ? -868018 : (hover ? -1 : 1627389951);
      gg.renderOutline(x - 3, y - 3, size + 6, size + 6, edge);
      if (!usable) {
         gg.setColor(0.36F, 0.36F, 0.4F, 1.0F);
      }

      gg.blitSprite(gear.iconFor(this.fruit.id()), x, y, size, size);
      gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (!usable) {
         gg.blitSprite(DFHud.LOCK, x + size / 2 - 6, y + size / 2 - 7, 12, 15);
      }
   }

   private void info(GuiGraphics gg, GearDefinition owner, GearDefinition gear, int cx, int cy) {
      Component name = gear.displayName(this.fruit.id());
      gg.drawCenteredString(this.font, name.copy().withStyle(ChatFormatting.BOLD), cx, cy - 18, 16777215);
      String descKey = gear.translationKey(this.fruit.id()) + ".desc";
      List<FormattedCharSequence> lines = this.font.split(Component.translatable(descKey), Math.max(80, this.radius() * 2 - 64));
      int y = cy - 6;

      for (int l = 0; l < Math.min(2, lines.size()); l++) {
         gg.drawCenteredString(this.font, lines.get(l), cx, y, 11579568);
         y += 10;
      }

      Component status;
      if (owner.available() && gear.available()) {
         if (ClientFruitState.data() != null && ClientFruitState.data().mastery() < needed(owner, gear)) {
            status = Component.translatable("screen.devilfruits.requires_mastery", new Object[]{needed(owner, gear)}).withStyle(ChatFormatting.RED);
         } else {
            GearState state = GearState.of(this.fruit, ClientFruitState.form());
            status = state != null && state.current() == gear ? Component.translatable("gear.devilfruits.active").withStyle(ChatFormatting.GOLD) : null;
         }
      } else {
         status = Component.translatable("gear.devilfruits.coming_soon").withStyle(ChatFormatting.GRAY);
      }

      if (status != null) {
         gg.drawCenteredString(this.font, status, cx, y + 2, 16777215);
      }
   }

   private void pick(double dx, double dy) {
      this.hovered = -1;
      this.hoveredSub = -1;
      double dist = Math.sqrt(dx * dx + dy * dy);
      if (dist < 26.0) {
         this.open = -1;
      } else {
         if (this.open >= 0) {
            int on = this.subAt(this.open, dx, dy);
            if (on >= 0) {
               this.hovered = this.open;
               this.hoveredSub = on;
               return;
            }
         }

         double angle = Math.atan2(dy, dx);
         double step = (Math.PI * 2) / (double)this.gears.size();
         int nearest = Math.floorMod((int)Math.round((angle + (Math.PI / 2)) / step), this.gears.size());
         if (this.open >= 0 && Math.abs(Mth.wrapDegrees(Math.toDegrees(angle - this.angle(this.open)))) > 40.0) {
            this.open = -1;
         }

         if (this.open < 0 && !this.gears.get(nearest).leaf()) {
            this.open = nearest;
         }

         if (this.open >= 0 && dist > (double)(this.radius() + 24 + 2)) {
            this.hovered = this.open;
            int[][] at = this.subCentres(this.open);
            double best = Double.MAX_VALUE;

            for (int s = 0; s < at.length; s++) {
               double ex = (double)this.width / 2.0 + dx - (double)at[s][0];
               double ey = (double)this.height / 2.0 + dy - (double)at[s][1];
               double d = ex * ex + ey * ey;
               if (d < best) {
                  best = d;
                  this.hoveredSub = s;
               }
            }
         } else {
            this.hovered = nearest;
         }
      }
   }

   private static int needed(GearDefinition owner, GearDefinition gear) {
      return Math.max(owner.unlockMastery(), gear.unlockMastery());
   }

   private static boolean selectable(GearDefinition owner, GearDefinition gear) {
      if (owner.available() && gear.available()) {
         if (gear == owner && !owner.leaf()) {
            gear = owner.subGears().get(0);
            if (!gear.available()) {
               return false;
            }
         }

         return ClientFruitState.data() == null || ClientFruitState.data().mastery() >= needed(owner, gear);
      } else {
         return false;
      }
   }

   public void tick() {
      super.tick();
      Key key = DFKeys.TRANSFORM.getKey();
      if (this.holdToUse && key.getType() == Type.KEYSYM && !InputConstants.isKeyDown(this.minecraft.getWindow().getWindow(), key.getValue())) {
         this.confirm();
      }
   }

   public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
      if (this.holdToUse && DFKeys.TRANSFORM.matches(keyCode, scanCode)) {
         this.confirm();
         return true;
      } else {
         return super.keyReleased(keyCode, scanCode, modifiers);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.confirm();
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private void confirm() {
      GearDefinition pick = this.pickDefinition();
      if (pick != null && selectable(this.gears.get(this.hovered), pick)) {
         GearDefinition owner = this.gears.get(this.hovered);
         PacketDistributor.sendToServer(new GearSwitchPayload(owner.name(), pick == owner ? "" : pick.name()), new CustomPacketPayload[0]);
      }

      this.onClose();
   }

   @Nullable
   private GearDefinition pickDefinition() {
      if (this.hovered < 0) {
         return null;
      } else {
         GearDefinition owner = this.gears.get(this.hovered);
         return this.hoveredSub >= 0 ? owner.subGears().get(this.hoveredSub) : owner;
      }
   }
}
