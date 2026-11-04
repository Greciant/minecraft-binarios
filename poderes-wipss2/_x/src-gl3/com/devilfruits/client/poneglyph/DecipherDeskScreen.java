package com.devilfruits.client.poneglyph;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.poneglyph.Cipher;
import com.devilfruits.core.poneglyph.DecipherDeskMenu;
import com.devilfruits.core.poneglyph.PoneglyphNetwork;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class DecipherDeskScreen extends AbstractContainerScreen<DecipherDeskMenu> {
   private static final ResourceLocation TEX = DevilFruits.id("textures/gui/poneglyph/decipher_desk.png");
   private static final ResourceLocation SMUDGE = DevilFruits.id("poneglyph/ink_smudge");
   private static final int TW = 512;
   private static final int TH = 256;
   private static final int INK = -12900844;
   private static final int FADED = -8361652;
   private static final int PARCH_LIGHT = -924480;
   private static final int OPT_X = 86;
   private static final int OPT_Y = 11;
   private static final int OPT_W = 64;
   private static final int OPT_H = 18;
   private static final int OPT_STEP = 19;
   private static final int OPT_ROWS = 6;
   private static final int HINT_X = 174;
   private static final int HINT_Y = 142;
   private static final int REVEAL_Y = 160;
   private static final int BTN_W = 40;
   private static final int BTN_H = 16;
   @Nullable
   private PoneglyphNetwork.DeskStatePayload state;
   @Nullable
   private PoneglyphNetwork.DeskStatePayload lastActive;
   private List<String> oldOptions = List.of();
   private int glowIndex = -1;
   private int wrongIndex = -1;
   private long turnStart = -100L;
   private long wrongStart = -100L;
   private long sealStart = -100L;
   private long smudgeStart = -100L;
   private long glowStart = -100L;
   private int scroll;

   public DecipherDeskScreen(DecipherDeskMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageWidth = 300;
      this.imageHeight = 224;
      this.inventoryLabelX = 9;
      this.inventoryLabelY = 130;
      PoneglyphNetwork.DeskStatePayload last = ClientPoneglyphState.desk;
      if (last != null && last.containerId() == menu.containerId) {
         this.state = last;
      }
   }

   private long now() {
      return this.minecraft != null && this.minecraft.level != null ? this.minecraft.level.getGameTime() : 0L;
   }

   void onState(PoneglyphNetwork.DeskStatePayload next) {
      long t = this.now();
      List<String> before = this.state != null ? this.state.options() : List.of();
      switch (next.event()) {
         case 1:
            this.oldOptions = before;
            this.glowIndex = next.eventIndex();
            this.glowStart = t;
            this.turnStart = t + 8L;
            break;
         case 2:
         case 3:
            this.wrongIndex = next.eventIndex();
            this.wrongStart = t;
            break;
         case 4:
            this.oldOptions = before;
            this.turnStart = t;
            this.wrongIndex = next.eventIndex();
            this.wrongStart = t;
            break;
         case 5:
            this.oldOptions = before;
            this.glowIndex = next.eventIndex();
            this.glowStart = t;
            this.sealStart = t + 6L;
            break;
         case 6:
            this.smudgeStart = t;
      }

      if (next.status() == 1) {
         this.lastActive = next;
      }

      if (next.options().size() != before.size()) {
         this.scroll = 0;
      }

      this.state = next;
   }

   private int lockLeft() {
      return this.state == null ? 0 : (int)Math.max(0L, (long)this.state.lockTicks() - (this.now() - ClientPoneglyphState.deskTime));
   }

   private boolean active() {
      return this.state != null && this.state.status() == 1;
   }

   protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
      gg.blit(TEX, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 512, 256);
   }

   public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
      super.render(gg, mouseX, mouseY, partialTick);
      this.renderTooltip(gg, mouseX, mouseY);
      this.tooltips(gg, mouseX - this.leftPos, mouseY - this.topPos, mouseX, mouseY);
   }

   protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
      gg.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
      float t = (float)this.now() + (this.minecraft != null ? this.minecraft.getTimer().getGameTimeDeltaPartialTick(false) : 0.0F);
      this.parchment(gg, t);
      this.options(gg, mouseX - this.leftPos, mouseY - this.topPos, t);
      this.literacy(gg);
      this.alphabet(gg, t);
      this.buttons(gg, mouseX - this.leftPos, mouseY - this.topPos);
      this.seal(gg, t);
   }

   private void parchment(GuiGraphics gg, float t) {
      boolean solved = !this.active() && this.state != null && this.state.event() == 5 && t - (float)this.sealStart <= 60.0F && this.lastActive != null;
      if (!this.active() && !solved) {
         this.centered(gg, Component.translatable("poneglyph.devilfruits.desk.title"), 44, 13, -8361652);
      } else {
         PoneglyphNetwork.DeskStatePayload s = solved ? this.lastActive : this.state;
         int rounds = Math.max(1, s.rounds());
         int done = solved ? rounds : s.round();
         this.centered(gg, Component.translatable("poneglyph.devilfruits.desk.round", new Object[]{Math.min(done + 1, rounds), rounds}), 44, 13, -8361652);
         gg.fill(14, 24, 74, 25, 1429939732);
         Component clue = Component.literal("«" + s.clue() + "»");
         float scale = Math.min(1.5F, 64.0F / (float)Math.max(1, this.font.width(clue)));
         gg.pose().pushPose();
         gg.pose().translate(44.0F, 36.0F, 0.0F);
         gg.pose().scale(scale, scale, 1.0F);
         gg.drawString(this.font, clue, -this.font.width(clue) / 2, 0, -12900844, false);
         gg.pose().popPose();
         if (!solved && !s.hint().isEmpty()) {
            this.centered(gg, Component.translatable("poneglyph.devilfruits.desk.starts_with"), 44, 54, -8361652);
            gg.pose().pushPose();
            gg.pose().translate(44.0F, 64.0F, 0.0F);
            gg.pose().scale(2.0F, 2.0F, 1.0F);
            MutableComponent glyph = Component.literal(s.hint()).withStyle(Style.EMPTY.withFont(Cipher.FONT));
            gg.drawString(this.font, glyph, -this.font.width(glyph) / 2, 0, -14717064, false);
            gg.pose().popPose();
         }

         int max = Math.max(1, s.maxMistakes());
         int bx = 44 - (max * 12 - 2) / 2;

         for (int i = 0; i < max; i++) {
            gg.blit(TEX, bx + i * 12, 84, !solved && i < s.mistakes() ? 372.0F : 382.0F, 20.0F, 10, 10, 512, 256);
         }

         int sw = rounds <= 3 ? 20 : 12;
         int step = sw + 2;
         int sx = 44 - (rounds * step - 2) / 2;

         for (int i = 0; i < rounds; i++) {
            int u = i < done ? 392 : (i == done ? 412 : 372);
            gg.blit(TEX, sx + i * step, 102, sw, sw, (float)u, 0.0F, 20, 20, 512, 256);
         }
      }
   }

   private void options(GuiGraphics gg, int mx, int my, float t) {
      if (this.active() && this.state != null) {
         float turn = (t - (float)this.turnStart) / 8.0F;
         boolean turning = turn >= 0.0F && turn < 1.0F;
         boolean glowing = t - (float)this.glowStart < 8.0F && this.glowIndex >= 0;
         List<String> shown = (!turning || !(turn < 0.5F)) && (!glowing || turning || !(t < (float)this.turnStart)) ? this.state.options() : this.oldOptions;
         float squash = turning ? Math.abs(Mth.cos(turn * (float) Math.PI)) : 1.0F;
         float shake = t - (float)this.wrongStart < 10.0F ? Mth.sin((t - (float)this.wrongStart) * 2.6F) * (10.0F - (t - (float)this.wrongStart)) * 0.3F : 0.0F;
         gg.pose().pushPose();
         gg.pose().translate(150.0F + shake, 0.0F, 0.0F);
         gg.pose().scale(Math.max(0.02F, squash), 1.0F, 1.0F);
         gg.pose().translate(-150.0F, 0.0F, 0.0F);
         boolean locked = this.lockLeft() > 0;

         for (int i = 0; i < shown.size(); i++) {
            int row = i / 2 - this.scroll;
            if (row >= 0 && row < 6) {
               int x = 86 + i % 2 * 64;
               int y = 11 + row * 19;
               int v = 0;
               if (glowing && shown == this.oldOptions && i == this.glowIndex) {
                  v = 36;
               } else if (t - (float)this.wrongStart < 12.0F && i == this.wrongIndex && shown == this.state.options()) {
                  v = 54;
               } else if (this.state.reveal() == i && shown == this.state.options()) {
                  v = 72;
               } else if (locked) {
                  v = 90;
               } else if (!turning && mx >= x && mx < x + 64 && my >= y && my < y + 18) {
                  v = 18;
               }

               gg.blit(TEX, x, y, 304.0F, (float)v, 64, 18, 512, 256);
               MutableComponent word = Component.literal(shown.get(i)).withStyle(Style.EMPTY.withFont(Cipher.FONT));
               int color = v == 36 ? -8762864 : (v == 54 ? -7726064 : (v == 90 ? -9544891 : -12900844));
               gg.drawString(this.font, word, x + 32 - this.font.width(word) / 2, y + 5, color, false);
            }
         }

         gg.pose().popPose();
         if (t - (float)this.wrongStart < 10.0F) {
            int a = (int)(90.0F * (1.0F - (t - (float)this.wrongStart) / 10.0F));
            gg.fill(84, 8, 216, 128, a << 24 | 11542540);
         }

         if (locked) {
            float frac = (float)this.lockLeft() / (float)Math.max(1, this.state.lockTicks());
            gg.fill(86, 124, 86 + (int)(128.0F * Math.min(1.0F, frac)), 126, -7726064);
            this.centered(gg, Component.translatable("poneglyph.devilfruits.desk.locked", new Object[]{(this.lockLeft() + 19) / 20}), 150, 118, -7726064);
         }

         if (this.state.options().size() > 12) {
            gg.drawString(this.font, "↕", 208, 118, -8361652, false);
         }

         this.smudge(gg, t);
      } else {
         if (this.state != null && this.state.event() == 5 && t - (float)this.sealStart < 60.0F) {
            this.centered(gg, Component.translatable("poneglyph.devilfruits.desk.deciphered"), 150, 60, -8762864);
         } else {
            Component msg = Component.translatable("poneglyph.devilfruits.desk.status." + (this.state == null ? 0 : this.state.status()));
            int y = 56;

            for (FormattedCharSequence line : this.font.split(msg, 120)) {
               gg.drawString(this.font, line, 150 - this.font.width(line) / 2, y, -8361652, false);
               y += 10;
            }
         }

         this.smudge(gg, t);
      }
   }

   private void smudge(GuiGraphics gg, float t) {
      if (t - (float)this.smudgeStart < 40.0F) {
         gg.setColor(1.0F, 1.0F, 1.0F, 1.0F - (t - (float)this.smudgeStart) / 40.0F);
         gg.blitSprite(SMUDGE, 110, 20, 80, 80);
         gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void literacy(GuiGraphics gg) {
      if (this.state != null) {
         gg.drawString(this.font, Component.translatable("poneglyph.devilfruits.desk.literacy", new Object[]{this.state.literacy()}), 222, 63, -12566464, false);
         int span = Math.max(1, this.state.xpTo() - this.state.xpFrom());
         float frac = this.state.xpTo() <= this.state.xpFrom() ? 1.0F : Mth.clamp((float)(this.state.xp() - this.state.xpFrom()) / (float)span, 0.0F, 1.0F);
         gg.fill(222, 73, 292, 76, -12961222);
         gg.fill(222, 73, 222 + (int)(70.0F * frac), 76, -12601406);
      }
   }

   private void alphabet(GuiGraphics gg, float t) {
      if (!((DecipherDeskMenu)this.menu).hasPrimer()) {
         gg.fill(222, 82, 292, 214, 1073741824);
         gg.blit(TEX, 245, 118, 432.0F, 32.0F, 24, 32, 512, 256);
         int y = 160;

         for (FormattedCharSequence line : this.font.split(Component.translatable("poneglyph.devilfruits.desk.need_primer"), 66)) {
            gg.drawString(this.font, line, 257 - this.font.width(line) / 2, y, -8361652, false);
            y += 10;
         }
      } else {
         int[] perm = ClientPoneglyphState.permutation();

         for (int i = 0; i < 26; i++) {
            int col = i / 13;
            int row = i % 13;
            int x = 226 + col * 36;
            int y = 85 + row * 10;
            char latin = (char)(97 + i);
            MutableComponent glyph = Component.literal(String.valueOf((char)(97 + perm[i]))).withStyle(Style.EMPTY.withFont(Cipher.FONT));
            gg.drawString(this.font, glyph, x, y, -14717064, false);
            gg.drawString(this.font, "= " + latin, x + 10, y, -12900844, false);
         }
      }
   }

   private void buttons(GuiGraphics gg, int mx, int my) {
      if (this.active() && this.state != null) {
         if ((this.state.perks() & 4) != 0) {
            this.button(gg, 142, (this.state.perks() & 1) != 0, mx, my, "poneglyph.devilfruits.desk.hint");
         }

         if ((this.state.perks() & 8) != 0) {
            this.button(gg, 160, (this.state.perks() & 2) != 0, mx, my, "poneglyph.devilfruits.desk.reveal");
         }
      }
   }

   private void button(GuiGraphics gg, int y, boolean ready, int mx, int my, String key) {
      boolean hover = ready && mx >= 174 && mx < 214 && my >= y && my < y + 16;
      gg.blit(TEX, 174, y, 372.0F, ready ? (float)(hover ? 48 : 32) : 64.0F, 40, 16, 512, 256);
      this.centered(gg, Component.translatable(key), 194, y + 4, ready ? -12900844 : -8361652);
   }

   private void seal(GuiGraphics gg, float t) {
      float s = (t - (float)this.sealStart) / 8.0F;
      if (!(s < 0.0F) && !(s > 6.0F)) {
         float scale = s < 1.0F ? 3.0F - 2.0F * s : 1.0F;
         gg.pose().pushPose();
         gg.pose().translate(256.0F, 51.0F, 300.0F);
         gg.pose().scale(scale, scale, 1.0F);
         gg.setColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, 6.0F - s));
         gg.blit(TEX, -16, -16, 432.0F, 0.0F, 32, 32, 512, 256);
         gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         gg.pose().popPose();
      }
   }

   private void tooltips(GuiGraphics gg, int mx, int my, int sx, int sy) {
      if (this.state != null) {
         if (mx >= 220 && mx < 294 && my >= 60 && my < 78) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("poneglyph.devilfruits.desk.literacy", new Object[]{this.state.literacy()}));
            lines.add(Component.translatable("poneglyph.devilfruits.desk.xp", new Object[]{this.state.xp(), this.state.xpTo()}));

            for (int l = 1; l <= 5; l++) {
               lines.add(
                  Component.translatable("poneglyph.devilfruits.literacy.perk." + l)
                     .withStyle(l <= this.state.literacy() ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY)
               );
            }

            gg.renderComponentTooltip(this.font, lines, sx, sy);
         } else if (this.active() && mx >= 174 && mx < 214 && my >= 142 && my < 176) {
            gg.renderTooltip(
               this.font, Component.translatable(my < 160 ? "poneglyph.devilfruits.desk.hint.desc" : "poneglyph.devilfruits.desk.reveal.desc"), sx, sy
            );
         }
      }
   }

   private void centered(GuiGraphics gg, Component text, int cx, int y, int color) {
      gg.drawString(this.font, text, cx - this.font.width(text) / 2, y, color, false);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.active() && this.state != null && this.minecraft != null && this.minecraft.gameMode != null) {
         int mx = (int)mouseX - this.leftPos;
         int my = (int)mouseY - this.topPos;
         if (this.lockLeft() <= 0 && this.now() - this.turnStart >= 8L) {
            for (int i = 0; i < this.state.options().size(); i++) {
               int row = i / 2 - this.scroll;
               int x = 86 + i % 2 * 64;
               int y = 11 + row * 19;
               if (row >= 0 && row < 6 && mx >= x && mx < x + 64 && my >= y && my < y + 18) {
                  this.click(i);
                  return true;
               }
            }
         }

         if (mx >= 174 && mx < 214) {
            if (my >= 142 && my < 158 && (this.state.perks() & 1) != 0) {
               this.click(100);
               return true;
            }

            if (my >= 160 && my < 176 && (this.state.perks() & 2) != 0) {
               this.click(101);
               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   private void click(int id) {
      this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.5F));
      this.minecraft.gameMode.handleInventoryButtonClick(((DecipherDeskMenu)this.menu).containerId, id);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.state != null) {
         int rows = (this.state.options().size() + 1) / 2;
         this.scroll = Mth.clamp(this.scroll - (int)Math.signum(scrollY), 0, Math.max(0, rows - 6));
      }

      return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }
}
