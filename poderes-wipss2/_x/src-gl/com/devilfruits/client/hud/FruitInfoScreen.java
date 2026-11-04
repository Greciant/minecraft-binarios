package com.devilfruits.client.hud;

import com.devilfruits.client.ClientFruitState;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.fruit.FruitTheme;
import com.devilfruits.core.network.AbilityTablePayload;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class FruitInfoScreen extends Screen {
   private static final ResourceLocation XP_BG = ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
   private static final ResourceLocation XP_FILL = ResourceLocation.withDefaultNamespace("hud/experience_bar_progress");
   private static final int PANEL_W = 276;
   private static final int ROW_H = 54;
   private static final int FORM_H = 16;
   private static final int FRUIT_ROW_H = 24;
   private static final int XP_BAR = 182;
   @Nullable
   private final DevilFruit focus;
   private int panelX;
   private int panelY;
   private int panelH;
   private double scroll;

   public FruitInfoScreen() {
      this(null);
   }

   public FruitInfoScreen(@Nullable DevilFruit focus) {
      super(Component.translatable("screen.devilfruits.info"));
      this.focus = focus;
   }

   protected void init() {
      this.panelH = Math.min(this.height - 20, 300);
      this.panelX = (this.width - 276) / 2;
      this.panelY = (this.height - this.panelH) / 2;
   }

   public boolean isPauseScreen() {
      return false;
   }

   @Nullable
   private DevilFruit shown() {
      return this.focus != null ? this.focus : ClientFruitState.fruit();
   }

   private boolean owned() {
      DevilFruit fruit = this.shown();
      return fruit != null && fruit == ClientFruitState.fruit();
   }

   private int headerHeight() {
      return this.owned() ? 84 : 40;
   }

   private int contentHeight() {
      DevilFruit fruit = this.shown();
      if (fruit == null) {
         return FruitRegistry.playable().size() * 24 + 20;
      } else if (!this.owned()) {
         return 120;
      } else {
         int h = 0;

         for (FruitForm form : fruit.forms()) {
            h += (fruit.hasForms() ? 16 : 0) + form.abilities().size() * 54;
         }

         return h;
      }
   }

   private int maxScroll() {
      return Math.max(0, this.contentHeight() - (this.panelH - this.headerHeight() - 24));
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      this.scroll = Mth.clamp(this.scroll - scrollY * 16.0, 0.0, (double)this.maxScroll());
      return true;
   }

   public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
      super.render(gg, mouseX, mouseY, partialTick);
      gg.blitSprite(DFHud.PANEL, this.panelX, this.panelY, 276, this.panelH);
      int x = this.panelX + 12;
      int y = this.panelY + 10;
      DevilFruit fruit = this.shown();
      if (fruit == null) {
         this.renderFruitList(gg, x, y);
      } else {
         fruit.item().ifPresent(item -> gg.renderItem(new ItemStack(item), x, y));
         gg.drawString(this.font, fruit.displayName().withStyle(ChatFormatting.BOLD), x + 22, y, 4144959, false);
         gg.drawString(this.font, Component.translatable(fruit.key("type")), x + 22, y + 10, 2781062, false);
         if (!this.owned()) {
            this.renderLore(gg, fruit, x, y + 30);
         } else {
            PlayerFruitData data = ClientFruitState.data();
            FruitTheme theme = fruit.theme();
            int my = y + 26;
            int barW = 252;
            gg.drawString(this.font, Component.translatable("screen.devilfruits.mastery", new Object[]{data.mastery(), 10}), x, my, 4144959, false);
            int need = ClientFruitState.xpForNextLevel();
            float xp = need <= 0 ? 1.0F : Mth.clamp(data.masteryXp() / (float)need, 0.0F, 1.0F);
            String xpText = need <= 0 ? Component.translatable("screen.devilfruits.max_mastery").getString() : Mth.floor(data.masteryXp()) + " / " + need;
            gg.drawString(this.font, xpText, x + barW - this.font.width(xpText), my, 5921370, false);
            gg.blitSprite(XP_BG, x, my + 11, 182, 5);
            int filled = Mth.floor(xp * 182.0F);
            if (filled > 0) {
               gg.blitSprite(XP_FILL, 182, 5, 0, 0, x, my + 11, filled, 5);
            }

            int energyMax = ClientFruitState.maxEnergy();
            gg.drawString(
               this.font,
               Component.translatable(
                  "screen.devilfruits.energy", new Object[]{Component.translatable(fruit.key("energy")), Mth.floor(data.energy()), energyMax}
               ),
               x,
               my + 22,
               4144959,
               false
            );
            int ebar = Mth.floor(Mth.clamp(data.energy() / (float)Math.max(1, energyMax), 0.0F, 1.0F) * 142.0F);
            gg.blitSprite(theme.energyBackground(), x, my + 33, 142, 5);
            if (ebar > 0) {
               gg.blitSprite(theme.energyProgress(), 142, 5, 0, 0, x, my + 33, ebar, 5);
            }

            int listTop = this.panelY + this.headerHeight() + 6;
            int listBottom = this.panelY + this.panelH - 22;
            gg.enableScissor(this.panelX + 4, listTop, this.panelX + 276 - 4, listBottom);
            int ry = listTop - (int)this.scroll;
            FruitForm current = ClientFruitState.form();
            int selected = ClientFruitState.selected();

            for (FruitForm form : fruit.forms()) {
               if (fruit.hasForms()) {
                  this.renderFormHeader(gg, form, form == current, x, ry);
                  ry += 16;
               }

               List<FormAbility> abilities = form.abilities();

               for (int i = 0; i < abilities.size(); i++) {
                  this.renderAbility(gg, abilities.get(i), i, x, ry, form == current && i == selected, data);
                  ry += 54;
               }
            }

            gg.disableScissor();
            if (this.maxScroll() > 0) {
               int trackH = listBottom - listTop;
               int knobH = Math.max(12, trackH * trackH / (trackH + this.maxScroll()));
               int knobY = listTop + (int)((double)(trackH - knobH) * (this.scroll / (double)this.maxScroll()));
               gg.fill(this.panelX + 276 - 8, knobY, this.panelX + 276 - 5, knobY + knobH, -7631989);
            }

            gg.drawCenteredString(this.font, Component.translatable("screen.devilfruits.controls"), this.width / 2, this.panelY + this.panelH - 16, 14737632);
         }
      }
   }

   private void renderFormHeader(GuiGraphics gg, FruitForm form, boolean current, int x, int y) {
      gg.fill(x, y + 2, x + 276 - 24, y + 13, current ? 1076912256 : 536870912);
      gg.blitSprite(form.iconSprite(), x + 1, y + 2, 11, 11);
      Component name = form.displayName().copy().withStyle(current ? ChatFormatting.DARK_BLUE : ChatFormatting.BLACK);
      gg.drawString(this.font, name, x + 15, y + 4, 4144959, false);
      AbilityTablePayload.FormEntry entry = ClientFruitState.formEntry(form);
      if (entry != null && !form.isBase()) {
         boolean locked = ClientFruitState.data().mastery() < entry.requiredMastery();
         Component info = locked
            ? Component.translatable("screen.devilfruits.requires_mastery", new Object[]{entry.requiredMastery()}).withStyle(ChatFormatting.DARK_RED)
            : Component.translatable(
                  "screen.devilfruits.form_line", new Object[]{entry.enterEnergyCost(), String.format(Locale.ROOT, "%.1f", entry.drainPerSecond())}
               )
               .withStyle(ChatFormatting.DARK_AQUA);
         gg.drawString(this.font, info, x + 276 - 26 - this.font.width(info), y + 4, 16777215, false);
      }
   }

   private void renderAbility(GuiGraphics gg, FormAbility entry, int index, int x, int y, boolean selected, PlayerFruitData data) {
      boolean locked = ClientFruitState.isLocked(entry.ability());
      gg.blitSprite(DFHud.SLOT, x, y + 6, 36, 36);
      if (locked) {
         gg.setColor(0.32F, 0.32F, 0.36F, 1.0F);
      }

      gg.blitSprite(entry.ability().iconSprite(), x + 2, y + 8, 32, 32);
      gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (locked) {
         gg.blitSprite(DFHud.LOCK, x + 12, y + 14, 12, 15);
      }

      int tx = x + 42;
      Component name = Component.literal(index + 1 + ". ").append(entry.ability().displayName());
      gg.drawString(this.font, name.copy().withStyle(selected ? ChatFormatting.DARK_BLUE : ChatFormatting.BLACK), tx, y + 2, 4144959, false);
      List<FormattedCharSequence> lines = this.font.split(Component.translatable(entry.ability().descriptionKey()), 206);

      for (int l = 0; l < Math.min(3, lines.size()); l++) {
         gg.drawString(this.font, lines.get(l), tx, y + 12 + l * 9, 5921370, false);
      }

      Component stats = locked
         ? Component.translatable("screen.devilfruits.requires_mastery", new Object[]{ClientFruitState.requiredMastery(entry.ability())})
            .withStyle(ChatFormatting.DARK_RED)
         : Component.translatable(
               "screen.devilfruits.cost_line",
               new Object[]{ClientFruitState.energyCost(entry), String.format(Locale.ROOT, "%.1f", (float)ClientFruitState.cooldownTicks(entry) / 20.0F)}
            )
            .withStyle(ChatFormatting.DARK_AQUA);
      gg.drawString(this.font, stats, tx, y + 41, 16777215, false);
      if (data.isToggleActive(entry.ability().id())) {
         gg.drawString(
            this.font, Component.translatable("screen.devilfruits.active").withStyle(ChatFormatting.DARK_GREEN), x + 276 - 70, y + 2, 16777215, false
         );
      }
   }

   private void renderLore(GuiGraphics gg, DevilFruit fruit, int x, int y) {
      int ly = y;

      for (String key : new String[]{fruit.key("info1"), fruit.key("info2"), fruit.key("info3")}) {
         for (FormattedCharSequence line : this.font.split(Component.translatable(key), 252)) {
            gg.drawString(this.font, line, x, ly, 4144959, false);
            ly += 10;
         }

         ly += 6;
      }

      gg.drawString(this.font, Component.translatable("screen.devilfruits.not_eaten").withStyle(ChatFormatting.DARK_RED), x, ly + 4, 16777215, false);
   }

   private void renderFruitList(GuiGraphics gg, int x, int y) {
      gg.drawString(this.font, Component.translatable("screen.devilfruits.info").withStyle(ChatFormatting.BOLD), x, y + 4, 4144959, false);
      int ry = y + 24 - (int)this.scroll;

      for (DevilFruit fruit : FruitRegistry.playable()) {
         int rowY = ry;
         fruit.item().ifPresent(item -> gg.renderItem(new ItemStack(item), x, rowY));
         gg.drawString(this.font, fruit.displayName(), x + 22, ry, 4144959, false);
         gg.drawString(this.font, Component.translatable(fruit.key("type")), x + 22, ry + 10, 2781062, false);
         ry += 24;
      }

      gg.drawString(this.font, Component.translatable("screen.devilfruits.no_fruit").withStyle(ChatFormatting.DARK_RED), x, ry + 6, 16777215, false);
   }
}
