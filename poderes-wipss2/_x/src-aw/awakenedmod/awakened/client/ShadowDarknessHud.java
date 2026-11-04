package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.shadow.ShadowDark;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class ShadowDarknessHud implements Layer, FreeHud {
   private static final int BAR_W = 8;
   private static final int BAR_H = 84;
   private static final int RIGHT_MARGIN = 12;
   private static final int VIOLET = 9064447;
   private static final int COLOR_BG = -1442840576;
   private static final int COLOR_BORDER = -15069136;
   private static final String CAPTION = "☾";
   private static final int LINE_H = 9;
   private static final int CAPTION_BLOCK = 11;
   private static final int TIER_GAP = 3;
   private static final int TIER_BLOCK = 12;
   private static final int[] TIER_UNLOCK = new int[]{0, 0, 4, 8, 12};
   private static final int MAX_TIER = TIER_UNLOCK.length - 1;
   private static final float PREVIEW_FILL = 0.5F;
   public static final ShadowDarknessHud GAUGE = new ShadowDarknessHud();
   private float displayed;

   private ShadowDarknessHud() {
   }

   @Override
   public String hudId() {
      return "shadow_darkness";
   }

   @Override
   public HudSettings hudSettings() {
      return ClientConfig.HUD_SHADOW_DARKNESS;
   }

   @Override
   public boolean hudRelevant() {
      return "shadow".equals(ClientPowerState.get().powerId());
   }

   @Override
   public int hudWidth() {
      Font font = Minecraft.getInstance().font;
      return Math.max(8, Math.max(font.width("☾"), tierWidth(font)));
   }

   @Override
   public int hudHeight() {
      return 107;
   }

   @Override
   public HudPlacement.Box defaultBox(int screenW, int screenH, int w, int h) {
      int barX = screenW - 12 - 8;
      int barY = (screenH - 84) / 2;
      return new HudPlacement.Box(barX - barInset(w), barY - 11, w, h);
   }

   public void render(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      ClientPowerState state = ClientPowerState.get();
      if (mc.player != null && mc.level != null && !mc.options.hideGui && state.awakened() && "shadow".equals(state.powerId()) && this.hudVisible()) {
         int light = ShadowDark.lightAt(mc.player);
         float target = Mth.clamp(1.0F - (float)light / 15.0F, 0.0F, 1.0F);
         this.displayed = this.displayed + (target - this.displayed) * 0.2F;
         draw(g, this.layout(g.guiWidth(), g.guiHeight()), this.displayed, tierFor(target, state.masteryLevel()));
      }
   }

   @Override
   public void drawPreview(GuiGraphics g, HudPlacement.Box box) {
      float fill = Math.max(this.displayed, 0.5F);
      draw(g, box, fill, tierFor(fill, ClientPowerState.get().masteryLevel()));
   }

   private static void draw(GuiGraphics g, HudPlacement.Box box, float fill, int tier) {
      int x = box.x() + barInset(box.width());
      int y = box.y() + 11;
      g.fill(x - 1, y - 1, x + 8 + 1, y + 84 + 1, -15069136);
      g.fill(x, y, x + 8, y + 84, -1442840576);
      int fillH = Math.round(84.0F * fill);
      if (fillH > 0) {
         g.fill(x, y + 84 - fillH, x + 8, y + 84, fillColor(fill));
      }

      Font font = Minecraft.getInstance().font;
      int midX = x + 4;
      g.drawCenteredString(font, "☾", midX, box.y(), fillColor(Math.max(0.35F, fill)));
      g.drawCenteredString(font, tierText(tier), midX, y + 84 + 3, -4611841);
   }

   private static int barInset(int w) {
      return (w - 8) / 2;
   }

   private static Component tierText(int tier) {
      return Component.literal("T" + tier);
   }

   private static int tierWidth(Font font) {
      int widest = 0;

      for (int tier = 1; tier <= MAX_TIER; tier++) {
         widest = Math.max(widest, font.width(tierText(tier)));
      }

      return widest;
   }

   private static int fillColor(float darkness) {
      int r = 138;
      int gg = 79;
      int b = 255;
      float t = 0.25F + 0.75F * darkness;
      r = (int)((float)r * t + 255.0F * (1.0F - t) * 0.15F);
      gg = (int)((float)gg * t);
      b = (int)((float)b * t + 255.0F * (1.0F - t) * 0.2F);
      return 0xFF000000 | Mth.clamp(r, 0, 255) << 16 | Mth.clamp(gg, 0, 255) << 8 | Mth.clamp(b, 0, 255);
   }

   private static int tierFor(float darkness, int masteryLevel) {
      int fromDark = 1 + Math.round(darkness * 3.0F);
      int cap = 1;

      for (int tier = 2; tier <= MAX_TIER; tier++) {
         if (masteryLevel >= TIER_UNLOCK[tier]) {
            cap = tier;
         }
      }

      return Math.min(fromDark, cap);
   }
}
