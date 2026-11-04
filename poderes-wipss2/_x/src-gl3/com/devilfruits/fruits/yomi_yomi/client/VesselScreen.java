package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.fruits.yomi_yomi.Vessel;
import com.devilfruits.fruits.yomi_yomi.YomiNetwork;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.soul.YomiSoul;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class VesselScreen extends Screen {
   private static final int GREEN_HOT = -3604630;
   private static final int GREEN = -8585414;
   private static final int GREEN_DEEP = -14779886;
   private static final int RED = -34198;
   private final int corpse;
   private Vessel picked;
   private final float[] lift = new float[3];
   private float opened;
   private boolean confirmed;

   public VesselScreen(int corpse) {
      super(Component.translatable("screen.devilfruits.yomi_yomi.vessel.title"));
      this.corpse = corpse;
   }

   protected void init() {
      YomiHud.dim(0.65F, 0);
      this.opened = 0.0F;
   }

   private int cardW() {
      return Math.min(150, (this.width - 64) / 3);
   }

   private int cardH() {
      return Math.min(232, this.height - 96);
   }

   private int cardX(int i) {
      int w = this.cardW();
      int gap = 14;
      return this.width / 2 - (3 * w + 2 * gap) / 2 + i * (w + gap);
   }

   private int cardY() {
      return this.height / 2 - this.cardH() / 2 + 6;
   }

   private int hovered(double mx, double my) {
      for (int i = 0; i < 3; i++) {
         if (mx >= (double)this.cardX(i)
            && mx < (double)(this.cardX(i) + this.cardW())
            && my >= (double)(this.cardY() - 8)
            && my < (double)(this.cardY() + this.cardH())) {
            return i;
         }
      }

      return -1;
   }

   private boolean overConfirm(double mx, double my) {
      int bw = 170;
      int bx = this.width / 2 - bw / 2;
      int by = this.cardY() + this.cardH() + 10;
      return this.picked != null && mx >= (double)bx && mx < (double)(bx + bw) && my >= (double)by && my < (double)(by + 20);
   }

   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 0) {
         int h = this.hovered(mx, my);
         if (h >= 0) {
            this.picked = Vessel.values()[h];
            this.click(1.2F);
            return true;
         }

         if (this.overConfirm(mx, my)) {
            this.confirm();
            return true;
         }
      }

      return super.mouseClicked(mx, my, button);
   }

   private void click(float pitch) {
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, pitch));
   }

   private void confirm() {
      if (this.picked != null) {
         this.click(0.7F);
         PacketDistributor.sendToServer(new YomiNetwork.ChooseVessel(this.corpse, this.picked.ordinal()), new CustomPacketPayload[0]);
         this.confirmed = true;
         YomiHud.dim(0.65F, 60);
         this.onClose();
      }
   }

   public void autotestPick(Vessel v) {
      this.picked = v;
   }

   public void autotestConfirm() {
      this.confirm();
   }

   public void render(GuiGraphics g, int mx, int my, float partial) {
      super.render(g, mx, my, partial);
      Minecraft mc = Minecraft.getInstance();
      float time = (float)(mc.level == null ? 0L : mc.level.getGameTime()) + partial;
      this.opened = Math.min(1.0F, this.opened + 0.06F);
      float in = 1.0F - (1.0F - this.opened) * (1.0F - this.opened);
      this.motes(g, time);
      int titleY = this.cardY() - 34;
      g.drawCenteredString(this.font, this.title.copy().withStyle(ChatFormatting.BOLD), this.width / 2, titleY, -3604630);
      g.drawCenteredString(this.font, Component.translatable("screen.devilfruits.yomi_yomi.vessel.subtitle"), this.width / 2, titleY + 12, -2555968);
      LocalPlayer player = mc.player;
      if (player != null) {
         int s = Math.max(0, YomiSoul.of(player).ticksLeft() / 20);
         g.drawString(
            this.font,
            Component.translatable("hud.devilfruits.yomi_yomi.soul_time", new Object[]{String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60)}),
            8,
            8,
            s <= 20 ? -34198 : -8585414
         );
      }

      int h = this.hovered((double)mx, (double)my);

      for (int i = 0; i < 3; i++) {
         Vessel v = Vessel.values()[i];
         float target = (h == i ? 1.0F : 0.0F) + (this.picked == v ? 0.6F : 0.0F);
         this.lift[i] = this.lift[i] + (target - this.lift[i]) * 0.25F;
         this.card(g, i, v, time, in, h == i, this.picked == v, player, mx, my);
      }

      if (this.picked != null) {
         int bw = 170;
         int bx = this.width / 2 - bw / 2;
         int by = this.cardY() + this.cardH() + 10;
         boolean over = this.overConfirm((double)mx, (double)my);
         float pulse = 0.5F + 0.5F * Mth.sin(time * 0.25F);
         g.fill(bx, by, bx + bw, by + 20, over ? -266319336 : -535680496);
         frame(g, bx, by, bw, 20, mix(-14779886, -3604630, over ? 1.0F : pulse));
         g.drawCenteredString(
            this.font,
            Component.translatable("screen.devilfruits.yomi_yomi.vessel.confirm", new Object[]{this.picked.displayName()}),
            this.width / 2,
            by + 6,
            over ? -1 : -3604630
         );
         g.drawCenteredString(
            this.font,
            Component.translatable("screen.devilfruits.yomi_yomi.vessel.forever").withStyle(ChatFormatting.ITALIC),
            this.width / 2,
            by + 24,
            -6635632
         );
      }
   }

   public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
      g.fillGradient(0, 0, this.width, this.height, -1342043644, -805306368);
      int v = Math.max(this.width, this.height) / 3;

      for (int i = 0; i < 12; i++) {
         int a = (int)(6.0F + (float)i * 4.5F);
         g.fill(0, 0, this.width, Math.max(1, v * (12 - i) / 48), a << 24);
         g.fill(0, this.height - Math.max(1, v * (12 - i) / 48), this.width, this.height, a << 24);
      }
   }

   private void card(GuiGraphics g, int i, Vessel v, float time, float in, boolean hover, boolean chosen, LocalPlayer player, int mx, int my) {
      int w = this.cardW();
      int h = this.cardH();
      float l = this.lift[i];
      int x = this.cardX(i);
      int y = this.cardY() - Math.round(l * 6.0F) + Math.round((1.0F - in) * (float)(20 + i * 10));
      g.pose().pushPose();
      float sc = 1.0F + 0.03F * l;
      g.pose().translate((float)x + (float)w / 2.0F, (float)y + (float)h / 2.0F, 0.0F);
      g.pose().scale(sc, sc, 1.0F);
      g.pose().translate(-((float)x + (float)w / 2.0F), -((float)y + (float)h / 2.0F), 0.0F);
      g.fill(x, y, x + w, y + h, chosen ? -368172016 : -536342008);
      g.fillGradient(x + 1, y + 1, x + w - 1, y + 40, 1076928544, 0);
      flames(g, x, y, w, h, time + (float)i * 13.0F, 0.55F + 0.45F * Math.min(1.0F, l));
      frame(g, x, y, w, h, chosen ? -3604630 : (hover ? -8585414 : -14779886));
      g.drawCenteredString(this.font, v.displayName().copy().withStyle(ChatFormatting.BOLD), x + w / 2, y + 8, chosen ? -1 : -3604630);
      int py = y + 20;
      int ph = Math.min(88, h / 2 - 26);
      if (player != null) {
         preview(g, player, v, (float)x + (float)w / 2.0F, (float)(py + ph), (float)ph * 0.5F, time * 2.2F + (float)i * 40.0F);
      }

      int ty = py + ph + 10;
      FruitForm form = YomiYomi.fruit().form(v.formName());
      List<FormAbility> abilities = form == null ? List.of() : form.abilities();
      g.drawString(this.font, Component.translatable("screen.devilfruits.yomi_yomi.vessel.techniques"), x + 6, ty, -6627206);
      ty += 11;
      int ix = x + 6;

      for (FormAbility a : abilities) {
         if (ix + 14 > x + w - 4) {
            ix = x + 6;
            ty += 15;
         }

         g.blitSprite(a.ability().iconSprite(), ix, ty, 13, 13);
         if (mx >= ix && mx < ix + 13 && my >= ty && my < ty + 13) {
            g.renderTooltip(this.font, a.ability().displayName(), mx, my);
         }

         ix += 15;
      }

      ty += 16;

      for (int k = 1; k <= 3; k++) {
         String key = "screen.devilfruits.yomi_yomi.vessel." + v.formName() + ".perk" + k;
         if (I18n.exists(key)) {
            String text = I18n.get(key, new Object[0]);
            boolean bad = text.startsWith("-");

            for (FormattedCharSequence line : this.font
               .split(Component.literal((bad ? "✗ " : "✓ ") + text.substring(!text.startsWith("-") && !text.startsWith("+") ? 0 : 1).trim()), w - 12)) {
               if (ty + 9 > y + h - 4) {
                  break;
               }

               g.drawString(this.font, line, x + 6, ty, bad ? -34198 : -2555968);
               ty += 9;
            }

            ty++;
         }
      }

      g.pose().popPose();
   }

   private static void preview(GuiGraphics g, LocalPlayer player, Vessel v, float cx, float bottom, float scale, float yaw) {
      float bodyRot = player.yBodyRot;
      float yRot = player.getYRot();
      float xRot = player.getXRot();
      float head = player.yHeadRot;
      float headO = player.yHeadRotO;
      player.yBodyRot = yaw;
      player.setYRot(yaw);
      player.setXRot(0.0F);
      player.yHeadRot = yaw;
      player.yHeadRotO = yaw;
      VesselSkins.preview = v;
      SoulRenderer.suppressed = true;

      try {
         Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
         InventoryScreen.renderEntityInInventory(g, cx, bottom, scale, new Vector3f(0.0F, 0.0F, 0.0F), pose, null, player);
      } finally {
         VesselSkins.preview = null;
         SoulRenderer.suppressed = false;
         player.yBodyRot = bodyRot;
         player.setYRot(yRot);
         player.setXRot(xRot);
         player.yHeadRot = head;
         player.yHeadRotO = headO;
      }
   }

   private static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
      g.fill(x + 2, y, x + w - 2, y + 1, color);
      g.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
      g.fill(x, y + 2, x + 1, y + h - 2, color);
      g.fill(x + w - 1, y + 2, x + w, y + h - 2, color);
      g.fill(x + 1, y + 1, x + 2, y + 2, color);
      g.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
      g.fill(x + 1, y + h - 2, x + 2, y + h - 1, color);
      g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
   }

   private static void flames(GuiGraphics g, int x, int y, int w, int h, float t, float power) {
      int step = 2;

      for (int side = 0; side < 2; side++) {
         int sx = side == 0 ? x - 1 : x + w + 1;

         for (int py = y + h - 2; py > y + 4; py -= step) {
            float u = (float)(y + h - py) / (float)h;
            float n = tongue((float)py * 0.37F + (float)side * 11.0F, t);
            int len = Math.round((3.0F + 7.0F * n) * power * (0.5F + 0.5F * (1.0F - u)));

            for (int k = 0; k < len; k++) {
               int col = k >= len - 1 ? -14779886 : (k < len / 3 ? -3604630 : -8585414);
               int px = side == 0 ? sx - k : sx + k;
               g.fill(px, py - step, px + 1, py, col & 16777215 | (int)(220.0F * (1.0F - (float)k / (float)(len + 1))) << 24);
            }
         }
      }

      for (int px = x + 2; px < x + w - 2; px += step) {
         float n = tongue((float)px * 0.41F + 5.0F, t);
         int len = Math.round((2.0F + 6.0F * n) * power);

         for (int k = 0; k < len; k++) {
            int col = k >= len - 1 ? -14779886 : (k < len / 3 ? -3604630 : -8585414);
            g.fill(px, y - 1 - k, px + step, y - k, col & 16777215 | (int)(220.0F * (1.0F - (float)k / (float)(len + 1))) << 24);
         }
      }
   }

   private static float tongue(float p, float t) {
      float a = Mth.sin(p * 0.9F + t * 0.21F) * 0.5F + 0.5F;
      float b = Mth.sin(p * 2.3F - t * 0.37F) * 0.5F + 0.5F;
      float c = Mth.sin(p * 5.1F + t * 0.9F) * 0.5F + 0.5F;
      return Mth.clamp(a * 0.55F + b * 0.3F + c * 0.15F, 0.0F, 1.0F);
   }

   private void motes(GuiGraphics g, float t) {
      for (int i = 0; i < 40; i++) {
         float sx = (float)i * 97.13F % 1.0F;
         float speed = 0.15F + (float)(i * 31 % 7) * 0.05F;
         int px = Math.round(sx * (float)this.width + Mth.sin(t * 0.05F + (float)i) * 12.0F);
         float fy = ((float)i * 0.618F % 1.0F - t * speed / (float)this.height) % 1.0F;
         int py = Math.round((fy < 0.0F ? fy + 1.0F : fy) * (float)this.height);
         int a = (int)(90.0F + 80.0F * Mth.sin(t * 0.3F + (float)i));
         g.fill(px, py, px + (i % 3 == 0 ? 2 : 1), py + (i % 3 == 0 ? 2 : 1), Math.max(0, a) << 24 | (i % 2 == 0 ? 13172586 : 8191802));
      }
   }

   private static int mix(int a, int b, float t) {
      int r = Math.round(Mth.lerp(t, (float)(a >> 16 & 0xFF), (float)(b >> 16 & 0xFF)));
      int gg = Math.round(Mth.lerp(t, (float)(a >> 8 & 0xFF), (float)(b >> 8 & 0xFF)));
      int bb = Math.round(Mth.lerp(t, (float)(a & 0xFF), (float)(b & 0xFF)));
      return 0xFF000000 | r << 16 | gg << 8 | bb;
   }

   public void removed() {
      if (!this.confirmed) {
         YomiHud.dim(0.0F, 0);
      }

      super.removed();
   }

   public boolean isPauseScreen() {
      return false;
   }
}
