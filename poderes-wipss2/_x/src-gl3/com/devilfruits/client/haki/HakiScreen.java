package com.devilfruits.client.haki;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.haki.HakiManager;
import com.devilfruits.core.haki.HakiParts;
import com.devilfruits.core.haki.HakiTier;
import com.devilfruits.core.haki.PlayerHakiData;
import com.devilfruits.core.network.HakiInputPayload;
import com.devilfruits.registry.DFSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;

public class HakiScreen extends Screen {
   private static final int W = 276;
   private static final int H = 172;
   private static final int PAPER_W = 248;
   private static final int PAPER_H = 160;
   private static final int OPEN_TICKS = 9;
   private static final int INK = -13949662;
   private static final int INK_FADED = -7570594;
   private static final int INK_RED = -7726060;
   private static final int PAPER_TEXT = -989492;
   private static final int RIM = -855310;
   private static final ResourceLocation PAPER = sprite("paper");
   private static final ResourceLocation ROLLER = sprite("roller");
   private static final ResourceLocation ROLLER_RIGHT = sprite("roller_right");
   private static final ResourceLocation BODY = sprite("body");
   private static final ResourceLocation LOCKED = sprite("locked");
   private static final ResourceLocation SHEEN = sprite("sheen");
   private static final ResourceLocation STAMP = sprite("stamp");
   private static final ResourceLocation BAR_BG = sprite("bar_bg");
   private static final ResourceLocation BAR_XP = sprite("bar_xp");
   private static final ResourceLocation BAR_SPIRIT = sprite("bar_spirit");
   private static final ResourceLocation DOT_ON = sprite("dot_on");
   private static final ResourceLocation DOT_OFF = sprite("dot_off");
   private static final ResourceLocation[] BOLTS = new ResourceLocation[]{sprite("bolt_0"), sprite("bolt_1"), sprite("bolt_2")};
   private static final ResourceLocation[] COATS = new ResourceLocation[9];
   private static final int[][] PARTS = new int[][]{
      {14, 51, 12, 9},
      {50, 51, 12, 9},
      {14, 24, 12, 27},
      {50, 24, 12, 27},
      {26, 60, 12, 36},
      {38, 60, 12, 36},
      {26, 24, 24, 36},
      {26, 0, 24, 24},
      {0, 0, 10, 96}
   };
   private static final int[][] WEAPON_PIECES = new int[][]{{3, 2, 4, 64}, {0, 66, 10, 3}, {3, 69, 4, 21}, {2, 90, 6, 3}};
   private static final int WEAPON_BIT = 8;
   private final RandomSource random = RandomSource.create();
   private final List<HakiScreen.Bolt> bolts = new ArrayList<>();
   private int x0;
   private int y0;
   private int ticks;
   private boolean announced;
   private int selected;
   private final int initial;

   private static ResourceLocation sprite(String name) {
      return DevilFruits.id("haki/screen_" + name);
   }

   public HakiScreen() {
      super(Component.translatable("screen.devilfruits.haki"));
      PlayerHakiData data = HakiManager.data(Minecraft.getInstance().player);
      this.selected = HakiManager.wanted(data);
      this.initial = this.selected;
   }

   protected void init() {
      this.x0 = (this.width - 276) / 2;
      this.y0 = (this.height - 172) / 2;
      if (!this.announced) {
         this.announced = true;
         Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.8F, 0.9F));
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   private int paperX() {
      return this.x0 + 14;
   }

   private int paperY() {
      return this.y0 + 6;
   }

   private int figX() {
      return this.paperX() + 142;
   }

   private int figY() {
      return this.paperY() + 34;
   }

   private int[] rect(int bit) {
      int[] p = PARTS[bit];
      return new int[]{this.figX() + p[0], this.figY() + p[1], p[2], p[3]};
   }

   private int[][] pieces(int bit) {
      if (bit != 8) {
         return new int[][]{this.rect(bit)};
      } else {
         int[][] out = new int[WEAPON_PIECES.length][];

         for (int i = 0; i < out.length; i++) {
            int[] p = WEAPON_PIECES[i];
            out[i] = new int[]{this.figX() + p[0], this.figY() + p[1], p[2], p[3]};
         }

         return out;
      }
   }

   private static boolean inside(int[] r, double mx, double my) {
      return mx >= (double)r[0] && mx < (double)(r[0] + r[2]) && my >= (double)r[1] && my < (double)(r[1] + r[3]);
   }

   private float opened(float partialTick) {
      float t = Mth.clamp(((float)this.ticks + partialTick) / 9.0F, 0.0F, 1.0F);
      return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
   }

   public void tick() {
      super.tick();
      this.ticks++;
      this.bolts.removeIf(b -> this.ticks - b.born > 3);
      int coated = this.coated();
      if (coated != 0 && this.bolts.size() < 3 && this.random.nextFloat() < 0.1F) {
         List<Integer> parts = new ArrayList<>();

         for (int bit = 0; bit < 9; bit++) {
            if ((coated & 1 << bit) != 0) {
               parts.add(bit);
            }
         }

         int[] p = PARTS[parts.get(this.random.nextInt(parts.size()))];
         boolean vertical = this.random.nextBoolean();
         int bx = vertical ? p[0] + (this.random.nextBoolean() ? -8 : p[2] - 8) : p[0] + this.random.nextInt(Math.max(1, p[2])) - 8;
         int by = vertical ? p[1] + this.random.nextInt(Math.max(1, p[3])) - 8 : p[1] + (this.random.nextBoolean() ? -8 : p[3] - 8);
         this.bolts.add(new HakiScreen.Bolt(bx, by, this.random.nextInt(BOLTS.length), this.ticks));
      }
   }

   private int coated() {
      PlayerHakiData data = HakiManager.data(Minecraft.getInstance().player);
      return data.armament() ? HakiParts.normalize(this.selected) & HakiParts.allowed(data.tier()) : 0;
   }

   public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
      super.render(gg, mouseX, mouseY, partialTick);
      PlayerHakiData data = HakiManager.data(Minecraft.getInstance().player);
      float open = this.opened(partialTick);
      int centre = this.paperX() + 124;
      int half = Math.max(0, Math.round(124.0F * open));
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      gg.enableScissor(centre - half, this.paperY() - 8, centre + half, this.paperY() + 160 + 8);
      gg.blitSprite(PAPER, this.paperX(), this.paperY(), 248, 160);
      this.calligraphy(gg);
      if (data.armament()) {
         this.info(gg, data, mouseX, mouseY);
         this.figure(gg, data, mouseX, mouseY, partialTick, open >= 0.98F);
      } else {
         gg.drawString(
            this.font,
            Component.translatable("screen.devilfruits.haki").withStyle(ChatFormatting.BOLD),
            this.paperX() + 10,
            this.paperY() + 12,
            -7726060,
            false
         );
         gg.drawWordWrap(this.font, Component.translatable("screen.devilfruits.haki.locked"), this.paperX() + 10, this.paperY() + 28, 116, -13949662);
         gg.blitSprite(BODY, this.figX(), this.figY(), 64, 96);

         for (int bit = 0; bit < 9; bit++) {
            for (int[] r : this.pieces(bit)) {
               gg.blitSprite(LOCKED, r[0], r[1], r[2], r[3]);
            }
         }
      }

      gg.disableScissor();
      gg.blitSprite(ROLLER, centre - half - 14, this.y0, 14, 172);
      gg.blitSprite(ROLLER_RIGHT, centre + half, this.y0, 14, 172);
      RenderSystem.disableBlend();
   }

   private void calligraphy(GuiGraphics gg) {
      String text = "武装色";

      for (int i = 0; i < text.length(); i++) {
         String glyph = text.substring(i, i + 1);
         gg.pose().pushPose();
         gg.pose().translate((float)(this.paperX() + 236 - this.font.width(glyph)), (float)(this.paperY() + 14 + i * 20), 0.0F);
         gg.pose().scale(2.0F, 2.0F, 1.0F);
         gg.drawString(this.font, glyph, 0, 0, -13949662, false);
         gg.pose().popPose();
      }
   }

   private void info(GuiGraphics gg, PlayerHakiData data, int mouseX, int mouseY) {
      int x = this.paperX() + 10;
      int y = this.paperY();
      gg.drawString(this.font, Component.translatable("screen.devilfruits.haki").withStyle(ChatFormatting.BOLD), x, y + 12, -7726060, false);
      HakiTier tier = data.tier();
      Component tierName = Component.translatable(tier.translationKey());
      int sw = this.font.width(tierName) + 10;
      gg.blitSprite(STAMP, x, y + 25, sw, 12);
      gg.drawString(this.font, tierName, x + 5, y + 27, -989492, false);
      gg.drawString(this.font, Component.translatable("screen.devilfruits.haki.lv", new Object[]{data.level()}), x + sw + 5, y + 27, -13949662, false);
      boolean max = data.level() >= 10;
      float need = HakiManager.xpToNext(data.level());
      float xp = max ? 1.0F : Mth.clamp(data.xp() / need, 0.0F, 1.0F);
      gg.drawString(this.font, Component.translatable("screen.devilfruits.haki.training"), x, y + 43, -7570594, false);
      String xpText = max ? Component.translatable("screen.devilfruits.haki.max").getString() : Mth.floor(data.xp()) + " / " + Mth.floor(need);
      gg.drawString(this.font, xpText, x + 104 - this.font.width(xpText), y + 43, -7570594, false);
      stroke(gg, BAR_XP, x, y + 53, xp);
      float maxSpirit = HakiManager.maxSpirit(data.level());
      float spirit = Math.max(0.0F, data.spirit());
      gg.drawString(this.font, Component.translatable("screen.devilfruits.haki.spirit_label"), x, y + 64, -7570594, false);
      String spiritText = Mth.floor(spirit) + " / " + Mth.floor(maxSpirit);
      gg.drawString(this.font, spiritText, x + 104 - this.font.width(spiritText), y + 64, -7570594, false);
      stroke(gg, BAR_SPIRIT, x, y + 74, maxSpirit <= 0.0F ? 0.0F : Mth.clamp(spirit / maxSpirit, 0.0F, 1.0F));
      this.line(gg, x, y + 88, "coat", true);
      this.line(gg, x, y + 99, "parts", tier != HakiTier.NOVICE);
      this.line(gg, x, y + 110, "full_body", tier.ryuo());
      this.line(gg, x, y + 121, "ryuo", tier.ryuo());
      gg.drawString(
         this.font,
         Component.translatable("screen.devilfruits.haki.logia", new Object[]{Mth.floor(HakiManager.logiaFraction(data.level()) * 100.0F)}),
         x,
         y + 136,
         -13949662,
         false
      );
   }

   private static void stroke(GuiGraphics gg, ResourceLocation fill, int x, int y, float fraction) {
      gg.blitSprite(BAR_BG, x, y, 104, 7);
      int w = Mth.floor(fraction * 104.0F);
      if (w > 0) {
         gg.blitSprite(fill, 104, 7, 0, 0, x, y, w, 7);
      }
   }

   private void line(GuiGraphics gg, int x, int y, String key, boolean open) {
      gg.blitSprite(open ? DOT_ON : DOT_OFF, x, y, 7, 7);
      gg.drawString(this.font, Component.translatable("screen.devilfruits.haki.can." + key), x + 11, y, open ? -13949662 : -7570594, false);
   }

   private void figure(GuiGraphics gg, PlayerHakiData data, int mouseX, int mouseY, float partialTick, boolean interactive) {
      int allowed = HakiParts.allowed(data.tier());
      int coated = this.coated();
      gg.blitSprite(BODY, this.figX(), this.figY(), 64, 96);

      for (int bit = 0; bit < 9; bit++) {
         if ((coated & 1 << bit) != 0) {
            for (int[] r : this.pieces(bit)) {
               gg.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, -855310);
            }
         }
      }

      for (int bitx = 0; bitx < 9; bitx++) {
         int[] r = this.rect(bitx);
         if ((coated & 1 << bitx) != 0) {
            gg.blitSprite(COATS[bitx], r[0], r[1], r[2], r[3]);
         } else if ((allowed & 1 << bitx) == 0) {
            for (int[] piece : this.pieces(bitx)) {
               gg.blitSprite(LOCKED, piece[0], piece[1], piece[2], piece[3]);
            }
         }
      }

      float sweep = ((float)this.ticks + partialTick) % 70.0F / 22.0F;
      if (sweep < 1.0F && coated != 0) {
         int sx = this.figX() - 20 + Math.round(sweep * 104.0F);

         for (int bitxx = 0; bitxx < 9; bitxx++) {
            if ((coated & 1 << bitxx) != 0) {
               for (int[] r : this.pieces(bitxx)) {
                  gg.enableScissor(r[0], r[1], r[0] + r[2], r[1] + r[3]);
                  gg.blitSprite(SHEEN, sx, this.figY() - 16, 20, 128);
                  gg.disableScissor();
               }
            }
         }
      }

      for (HakiScreen.Bolt b : this.bolts) {
         gg.blitSprite(BOLTS[b.frame()], this.figX() + b.x(), this.figY() + b.y(), 16, 16);
      }

      Component caption = Component.translatable("screen.devilfruits.haki.pick");
      int captionColour = -7570594;
      if (interactive) {
         for (int bitxxx = 0; bitxxx < 9; bitxxx++) {
            int[] r = this.rect(bitxxx);
            if (inside(r, (double)mouseX, (double)mouseY)) {
               boolean lock = (allowed & 1 << bitxxx) == 0;
               float pulse = 0.55F + 0.45F * Mth.sin(((float)this.ticks + partialTick) * 0.35F);
               int colour = (int)(pulse * 200.0F) << 24 | (lock ? 7233875 : 11540524);
               frame(gg, r, colour);
               Component part = Component.translatable("haki.devilfruits.part." + HakiParts.NAMES[bitxxx]);
               caption = Component.translatable(
                  lock
                     ? "screen.devilfruits.haki.part_closed"
                     : ((coated & 1 << bitxxx) != 0 ? "screen.devilfruits.haki.part_on" : "screen.devilfruits.haki.part_off"),
                  new Object[]{part}
               );
               captionColour = lock ? -7570594 : -13949662;
               break;
            }
         }
      }

      int cx = this.figX() + 38;
      gg.drawString(this.font, caption, cx - this.font.width(caption) / 2, this.paperY() + 139, captionColour, false);
   }

   private static void frame(GuiGraphics gg, int[] r, int colour) {
      int x1 = r[0] - 2;
      int y1 = r[1] - 2;
      int x2 = r[0] + r[2] + 2;
      int y2 = r[1] + r[3] + 2;
      gg.fill(x1, y1, x2, y1 + 1, colour);
      gg.fill(x1, y2 - 1, x2, y2, colour);
      gg.fill(x1, y1 + 1, x1 + 1, y2 - 1, colour);
      gg.fill(x2 - 1, y1 + 1, x2, y2 - 1, colour);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      PlayerHakiData data = HakiManager.data(Minecraft.getInstance().player);
      if (data.armament() && !(this.opened(0.0F) < 0.98F)) {
         int allowed = HakiParts.allowed(data.tier());

         for (int bit = 0; bit < 9; bit++) {
            int b = 1 << bit;
            if (inside(this.rect(bit), mouseX, mouseY)) {
               if ((allowed & b) == 0) {
                  Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.5F));
                  return true;
               }

               int current = HakiParts.normalize(this.selected);
               boolean adding = (current & b) == 0;
               if (adding) {
                  current |= b;
               } else {
                  current &= ~b;
                  if (b == 1) {
                     current &= -5;
                  }

                  if (b == 2) {
                     current &= -9;
                  }
               }

               this.selected = current & allowed;
               Minecraft.getInstance()
                  .getSoundManager()
                  .play(SimpleSoundInstance.forUI(adding ? DFSounds.HAKI_COAT.get() : DFSounds.HAKI_UNCOAT.get(), adding ? 1.15F : 1.0F, 0.45F));
               if (adding) {
                  int[] p = PARTS[bit];
                  this.bolts.add(new HakiScreen.Bolt(p[0] + p[2] / 2 - 8, p[1] + p[3] / 2 - 8, this.random.nextInt(BOLTS.length), this.ticks));
               }

               return true;
            }
         }

         return super.mouseClicked(mouseX, mouseY, button);
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public void removed() {
      if (this.selected != this.initial && Minecraft.getInstance().player != null) {
         PacketDistributor.sendToServer(new HakiInputPayload(HakiInputPayload.Action.SELECT, this.selected), new CustomPacketPayload[0]);
      }

      super.removed();
   }

   static {
      for (int bit = 0; bit < 9; bit++) {
         COATS[bit] = sprite("coat_" + HakiParts.NAMES[bit]);
      }
   }

   private static record Bolt(int x, int y, int frame, int born) {
   }
}
