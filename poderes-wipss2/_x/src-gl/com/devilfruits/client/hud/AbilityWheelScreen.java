package com.devilfruits.client.hud;

import com.devilfruits.client.ClientFruitState;
import com.devilfruits.client.input.DFKeys;
import com.devilfruits.core.form.FormAbility;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class AbilityWheelScreen extends Screen {
   private int hovered = -1;
   private final boolean holdToUse;

   public AbilityWheelScreen() {
      this(true);
   }

   public AbilityWheelScreen(boolean holdToUse) {
      super(Component.translatable("screen.devilfruits.wheel"));
      this.holdToUse = holdToUse;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static int count() {
      return Math.max(1, ClientFruitState.abilities().size());
   }

   private int radius() {
      return Math.min(78, Math.min(this.width, this.height) / 2 - 30);
   }

   private int entryX(int i) {
      double a = (-Math.PI / 2) + (double)i * Math.PI * 2.0 / (double)count();
      return this.width / 2 + (int)Math.round(Math.cos(a) * (double)this.radius());
   }

   private int entryY(int i) {
      double a = (-Math.PI / 2) + (double)i * Math.PI * 2.0 / (double)count();
      return this.height / 2 + (int)Math.round(Math.sin(a) * (double)this.radius());
   }

   public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
      gg.fillGradient(0, 0, this.width, this.height, 1342177280, 1879048192);
   }

   public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
      super.render(gg, mouseX, mouseY, partialTick);
      List<FormAbility> abilities = ClientFruitState.abilities();
      if (!abilities.isEmpty()) {
         int cx = this.width / 2;
         int cy = this.height / 2;
         double dx = (double)(mouseX - cx);
         double dy = (double)(mouseY - cy);
         this.hovered = -1;
         if (dx * dx + dy * dy > 676.0) {
            double angle = Math.atan2(dy, dx) + (Math.PI / 2);
            double step = (Math.PI * 2) / (double)abilities.size();
            this.hovered = Math.floorMod((int)Math.round(angle / step), abilities.size());
         }

         int selected = ClientFruitState.selected();

         for (int i = 0; i < abilities.size(); i++) {
            FormAbility entry = abilities.get(i);
            int x = this.entryX(i) - 18;
            int y = this.entryY(i) - 18;
            gg.blitSprite(DFHud.SLOT, x, y, 36, 36);
            boolean locked = ClientFruitState.isLocked(entry.ability());
            if (locked) {
               gg.setColor(0.32F, 0.32F, 0.36F, 1.0F);
            }

            gg.blitSprite(entry.ability().iconSprite(), x + 2, y + 2, 32, 32);
            gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (locked) {
               gg.blitSprite(DFHud.LOCK, x + 12, y + 8, 12, 15);
            }

            if (i == this.hovered) {
               gg.fill(x + 2, y + 2, x + 34, y + 34, 1090519039);
            }

            if (i == selected) {
               gg.blitSprite(DFHud.theme().selection(), x - 2, y - 2, 40, 40);
            }

            String number = String.valueOf(i + 1);
            gg.drawString(this.font, number, x + 3, y + 3, 10526880, true);
         }

         int show = this.hovered >= 0 ? this.hovered : selected;
         FormAbility entryx = ClientFruitState.ability(show);
         if (entryx != null) {
            Component name = (Component)(ClientFruitState.isLocked(entryx.ability()) ? Component.literal("???") : entryx.ability().displayName());
            gg.drawCenteredString(this.font, name.copy().withStyle(ChatFormatting.BOLD), cx, cy - 22, 16777215);
            Component description = ClientFruitState.isLocked(entryx.ability())
               ? Component.literal("???")
               : Component.translatable(entryx.ability().descriptionKey());
            List<FormattedCharSequence> lines = this.font.split(description, Math.max(80, this.radius() * 2 - 60));
            int yx = cy - 10;

            for (int l = 0; l < Math.min(3, lines.size()); l++) {
               gg.drawCenteredString(this.font, lines.get(l), cx, yx, 11579568);
               yx += 10;
            }

            Component stats = ClientFruitState.isLocked(entryx.ability())
               ? Component.translatable("screen.devilfruits.requires_mastery", new Object[]{ClientFruitState.requiredMastery(entryx.ability())})
                  .withStyle(ChatFormatting.RED)
               : Component.translatable(
                     "screen.devilfruits.cost_line",
                     new Object[]{
                        ClientFruitState.energyCost(entryx), String.format(Locale.ROOT, "%.1f", (float)ClientFruitState.cooldownTicks(entryx) / 20.0F)
                     }
                  )
                  .withStyle(ChatFormatting.AQUA);
            gg.drawCenteredString(this.font, stats, cx, yx + 2, 16777215);
         }
      }
   }

   public void tick() {
      super.tick();
      Key key = DFKeys.WHEEL.getKey();
      if (this.holdToUse && key.getType() == Type.KEYSYM && !InputConstants.isKeyDown(this.minecraft.getWindow().getWindow(), key.getValue())) {
         this.confirm();
      }
   }

   public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
      if (this.holdToUse && DFKeys.WHEEL.matches(keyCode, scanCode)) {
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
      if (this.hovered >= 0) {
         ClientFruitState.select(this.hovered);
      }

      this.onClose();
   }
}
