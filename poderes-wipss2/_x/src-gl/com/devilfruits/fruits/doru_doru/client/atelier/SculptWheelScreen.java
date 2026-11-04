package com.devilfruits.fruits.doru_doru.client.atelier;

import com.devilfruits.DevilFruits;
import com.devilfruits.fruits.doru_doru.atelier.CastSlots;
import com.devilfruits.fruits.doru_doru.atelier.SculptRequest;
import com.devilfruits.fruits.doru_doru.atelier.ShapeKind;
import com.devilfruits.fruits.doru_doru.wax.WaxPalette;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public class SculptWheelScreen extends Screen {
   private final List<SculptWheelScreen.Entry> entries = new ArrayList<>();
   private int hoveredEntry = -1;
   private int hoveredDye = -1;
   private int hoveredTool = -1;

   public SculptWheelScreen() {
      super(Component.translatable("screen.devilfruits.doru_doru.wheel"));
   }

   private static ResourceLocation icon(String path) {
      return DevilFruits.id("hud/doru_doru/" + path);
   }

   protected void init() {
      this.entries.clear();
      switch (SculptClient.tool) {
         case SHAPE:
            for (ShapeKind k : ShapeKind.values()) {
               this.entries
                  .add(
                     new SculptWheelScreen.Entry(
                        icon("shape/" + k.key()),
                        Component.translatable("hud.devilfruits.doru_doru.shape." + k.key()),
                        () -> SculptClient.setKind(k),
                        k == SculptClient.kind,
                        ""
                     )
                  );
            }
            break;
         case BRUSH:
            String[] modes = new String[]{"brush_add", "brush_carve", "brush_smooth", "brush_dye"};

            for (int i = 0; i < modes.length; i++) {
               this.entries
                  .add(
                     new SculptWheelScreen.Entry(
                        icon("tool/" + modes[i]),
                        Component.translatable("hud.devilfruits.doru_doru.brush." + i),
                        () -> SculptClient.setBrush(i),
                        SculptClient.brushMode == i,
                        ""
                     )
                  );
            }
            break;
         case CAST:
            CastSlots slots = this.minecraft != null && this.minecraft.player != null ? CastSlots.of(this.minecraft.player) : null;

            for (int i = 0; i < 3; i++) {
               boolean full = slots != null && slots.get(i) != null;
               this.entries
                  .add(
                     new SculptWheelScreen.Entry(
                        icon("tool/cast_place"),
                        Component.translatable(full ? "hud.devilfruits.doru_doru.cast.place" : "hud.devilfruits.doru_doru.cast.empty", new Object[]{i + 1}),
                        () -> SculptClient.setCast(i, false),
                        !SculptClient.castCapture && SculptClient.castSlot == i,
                        String.valueOf(i + 1)
                     )
                  );
               this.entries
                  .add(
                     new SculptWheelScreen.Entry(
                        icon("tool/cast_capture"),
                        Component.translatable("hud.devilfruits.doru_doru.cast.capture", new Object[]{i + 1}),
                        () -> SculptClient.setCast(i, true),
                        SculptClient.castCapture && SculptClient.castSlot == i,
                        String.valueOf(i + 1)
                     )
                  );
            }
         case DISSOLVE:
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   private int outer() {
      return Math.min(96, Math.min(this.width, this.height) / 2 - 24);
   }

   private int inner() {
      return Math.max(40, this.outer() - 36);
   }

   public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
      gg.fillGradient(0, 0, this.width, this.height, 1342177280, 1879048192);
   }

   public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
      super.render(gg, mouseX, mouseY, partialTick);
      int cx = this.width / 2;
      int cy = this.height / 2;
      double dx = (double)(mouseX - cx);
      double dy = (double)(mouseY - cy);
      double dist = Math.sqrt(dx * dx + dy * dy);
      double angle = Math.atan2(dy, dx) + (Math.PI / 2);
      this.hoveredEntry = -1;
      this.hoveredDye = -1;
      this.hoveredTool = -1;
      if (!this.entries.isEmpty() && dist > (double)(this.outer() + this.inner()) / 2.0 - 2.0) {
         this.hoveredEntry = Math.floorMod((int)Math.round(angle / ((Math.PI * 2) / (double)this.entries.size())), this.entries.size());
      } else if (dist > (double)(this.inner() - 12) && dist <= (double)(this.outer() + this.inner()) / 2.0 - 2.0) {
         this.hoveredDye = Math.floorMod((int)Math.round(angle / (Math.PI / 8)), 16);
      } else if (Math.abs(dy + 18.0) <= 9.0 && Math.abs(dx) <= 46.0) {
         this.hoveredTool = Math.max(0, Math.min(3, (int)((dx + 46.0) / 23.0)));
      }

      for (int i = 0; i < this.entries.size(); i++) {
         SculptWheelScreen.Entry e = this.entries.get(i);
         double a = (-Math.PI / 2) + (double)i * Math.PI * 2.0 / (double)this.entries.size();
         int x = cx + (int)Math.round(Math.cos(a) * (double)this.outer()) - 12;
         int y = cy + (int)Math.round(Math.sin(a) * (double)this.outer()) - 12;
         gg.fill(x - 1, y - 1, x + 25, y + 25, e.selected() ? -1058487686 : -1876944366);
         gg.blitSprite(e.icon(), x + 4, y + 4, 16, 16);
         if (!e.badge().isEmpty()) {
            gg.drawString(this.font, e.badge(), x + 17, y + 15, -8054, true);
         }

         if (i == this.hoveredEntry) {
            gg.fill(x, y, x + 24, y + 24, 1090519039);
         }
      }

      for (int i = 0; i < 16; i++) {
         double ax = (-Math.PI / 2) + (double)i * Math.PI * 2.0 / 16.0;
         int xx = cx + (int)Math.round(Math.cos(ax) * (double)this.inner()) - 5;
         int yx = cy + (int)Math.round(Math.sin(ax) * (double)this.inner()) - 5;
         int size = i == this.hoveredDye ? 12 : 10;
         int o = (size - 10) / 2;
         gg.fill(xx - 1 - o, yx - 1 - o, xx + size - o + 1, yx + size - o + 1, i == SculptClient.color ? -8054 : -14015976);
         gg.fill(xx - o, yx - o, xx + size - o, yx + size - o, 0xFF000000 | WaxPalette.tint(i, false));
      }

      SculptRequest.Tool[] tools = SculptRequest.Tool.values();

      for (int i = 0; i < tools.length; i++) {
         int xx = cx - 46 + i * 23 + 2;
         int yx = cy - 27;
         gg.fill(xx - 1, yx - 1, xx + 19, yx + 19, tools[i] == SculptClient.tool ? -1058487686 : -1876944366);
         gg.blitSprite(icon("tool/" + tools[i].name().toLowerCase(Locale.ROOT)), xx + 1, yx + 1, 16, 16);
         if (i == this.hoveredTool) {
            gg.fill(xx, yx, xx + 18, yx + 18, 1090519039);
         }
      }

      Component name;
      if (this.hoveredEntry >= 0) {
         name = this.entries.get(this.hoveredEntry).name();
      } else if (this.hoveredDye >= 0) {
         name = Component.translatable("color.minecraft." + DyeColor.byId(this.hoveredDye).getName());
      } else if (this.hoveredTool >= 0) {
         name = Component.translatable("hud.devilfruits.doru_doru.tool." + tools[this.hoveredTool].name().toLowerCase(Locale.ROOT));
      } else {
         name = Component.translatable("hud.devilfruits.doru_doru.tool." + SculptClient.tool.name().toLowerCase(Locale.ROOT));
      }

      gg.drawCenteredString(this.font, name.copy().withStyle(ChatFormatting.BOLD), cx, cy + 2, 16777215);
   }

   public void tick() {
      super.tick();
      Key key = SculptKeys.WHEEL.getKey();
      if (key.getType() == Type.KEYSYM && !InputConstants.isKeyDown(this.minecraft.getWindow().getWindow(), key.getValue())) {
         this.confirm();
      }
   }

   public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
      if (SculptKeys.WHEEL.matches(keyCode, scanCode)) {
         this.confirm();
         return true;
      } else {
         return super.keyReleased(keyCode, scanCode, modifiers);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (this.hoveredTool >= 0) {
            SculptClient.setTool(SculptRequest.Tool.values()[this.hoveredTool]);
            this.init();
            return true;
         } else {
            this.confirm();
            return true;
         }
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private void confirm() {
      if (this.hoveredEntry >= 0) {
         this.entries.get(this.hoveredEntry).pick().run();
         SculptClient.click(1.2F);
      } else if (this.hoveredDye >= 0) {
         SculptClient.color = this.hoveredDye;
         SculptClient.click(1.6F);
      } else if (this.hoveredTool >= 0) {
         SculptClient.setTool(SculptRequest.Tool.values()[this.hoveredTool]);
      }

      this.onClose();
   }

   private static record Entry(ResourceLocation icon, Component name, Runnable pick, boolean selected, String badge) {
   }
}
