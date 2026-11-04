package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.summoning.PossessMotion;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.world.entity.Mob;

public final class PossessJumpBarHud implements Layer, FreeHud {
   private static final int WIDTH = 182;
   private static final int HEIGHT = 5;
   private static final int FRAME = 1711276032;
   private static final int TRACK = -1072688104;
   private static final int FILL = -11218848;
   public static final PossessJumpBarHud BAR = new PossessJumpBarHud();

   private PossessJumpBarHud() {
   }

   @Override
   public String hudId() {
      return "possess_jump";
   }

   @Override
   public HudSettings hudSettings() {
      return ClientConfig.HUD_POSSESS_JUMP;
   }

   @Override
   public int hudWidth() {
      return 182;
   }

   @Override
   public int hudHeight() {
      return 5;
   }

   @Override
   public HudPlacement.Box defaultBox(int screenW, int screenH, int w, int h) {
      return new HudPlacement.Box((screenW - w) / 2, screenH - 32 - h, w, h);
   }

   @Override
   public boolean hudRelevant() {
      return "summoning".equals(ClientPowerState.get().powerId());
   }

   public void render(GuiGraphics g, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !mc.options.hideGui && PossessCamera.isActive() && this.hudVisible()) {
         Mob var5 = PossessCamera.piloted();
         if (var5 instanceof Mob && PossessMotion.isChargeMount(var5)) {
            draw(g, this.layout(g.guiWidth(), g.guiHeight()), PossessMotion.jumpCharge(var5));
         }
      }
   }

   @Override
   public void drawPreview(GuiGraphics g, HudPlacement.Box box) {
      draw(g, box, 0.0);
   }

   private static void draw(GuiGraphics g, HudPlacement.Box box, double charge) {
      int x = box.x();
      int y = box.y();
      g.fill(x - 1, y - 1, box.right() + 1, box.bottom() + 1, 1711276032);
      g.fill(x, y, box.right(), box.bottom(), -1072688104);
      int fill = (int)((double)box.width() * Math.max(0.0, Math.min(1.0, charge)));
      if (fill > 0) {
         g.fill(x, y, x + fill, box.bottom(), -11218848);
      }
   }
}
