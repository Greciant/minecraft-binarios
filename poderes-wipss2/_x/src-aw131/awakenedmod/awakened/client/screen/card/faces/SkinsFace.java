package com.awakenedmod.awakened.client.screen.card.faces;

import com.awakenedmod.awakened.client.ExoSkinClient;
import com.awakenedmod.awakened.client.screen.card.CardContext;
import com.awakenedmod.awakened.client.screen.card.CardFace;
import com.awakenedmod.awakened.client.screen.card.CardGfx;
import com.awakenedmod.awakened.content.exosavant.ExoSkin;
import com.awakenedmod.awakened.content.exosavant.ExoSkins;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class SkinsFace implements CardFace {
   private static final int BOX = 64;
   private static final int MIN_BOX_H = 100;
   private static final int NAME_H = 13;
   private static final int LINE = 9;
   private static final int GAP = 6;

   @Override
   public int height(CardContext ctx, int w, boolean compact) {
      int h = listHeight(ctx, w - 64 - 12);
      return Math.max(h, 100) + (ExoSkinClient.suited() ? 0 : 14);
   }

   private static int listHeight(CardContext ctx, int lw) {
      int h = 0;

      for (ExoSkin skin : ExoSkin.values()) {
         h += rowHeight(ctx, skin, lw);
      }

      return h - 6;
   }

   private static int rowHeight(CardContext ctx, ExoSkin skin, int lw) {
      boolean locked = ctx.mc.player != null && !ExoSkins.isUnlocked(ctx.mc.player, skin);
      return 13 + (locked ? ctx.lines(skin.unlockHint(), lw) * 9 : 0) + 6;
   }

   @Override
   public void draw(CardGfx gfx, CardContext ctx, int x, int y, int w, boolean compact) {
      LocalPlayer player = ctx.mc.player;
      if (player != null) {
         int lx = x + 64 + 12;
         int lw = w - 64 - 12;
         int boxH = Math.max(listHeight(ctx, lw), 100);
         ExoSkin hovered = null;
         int ry = y;

         for (ExoSkin skin : ExoSkin.values()) {
            if (row(gfx, ctx, player, skin, lx, ry, lw)) {
               hovered = skin;
            }

            ry += rowHeight(ctx, skin, lw);
         }

         ExoSkinClient.preview(hovered);
         gfx.fill(x, y, 64, boxH, -12238791);
         gfx.entity(player, x + 2, y + 2, x + 64 - 2, y + boxH - 2, 30);
         gfx.frame(x, y, 64, boxH, -15461872);
         if (!ExoSkinClient.suited()) {
            gfx.text(Component.translatable("awakened.card.skins.suit_off"), x, y + boxH + 4, -6054250);
         }
      }
   }

   private static boolean row(CardGfx gfx, CardContext ctx, LocalPlayer player, ExoSkin skin, int x, int y, int w) {
      String key = "skin/" + skin.id();
      boolean worn = ExoSkins.worn(player) == skin;
      boolean unlocked = ExoSkins.isUnlocked(player, skin);
      int h = rowHeight(ctx, skin, w) - 6;
      boolean hov = gfx.hover(key);
      if (hov) {
         gfx.fill(x - 4, y - 2, w + 8, h + 3, -13225168);
      }

      Runnable wear = () -> ExoSkinClient.wear(skin);
      boolean clickable = unlocked && !worn;
      if (clickable) {
         gfx.hit(x - 4, y - 2, w + 8, h + 3, key, wear);
      } else {
         gfx.hit(x - 4, y - 2, w + 8, h + 3, key);
      }

      String label = Component.translatable(worn ? "awakened.card.skins.wearing" : (unlocked ? "awakened.card.skins.wear" : "awakened.card.skins.locked"))
         .getString();
      int chipW = gfx.chip(x + w, y - 1, label, 0, true, worn, false, !unlocked, clickable ? key + "/chip" : null, wear);
      String name = gfx.clip(skin.displayName().getString(), w - chipW - 6);
      if (worn) {
         gfx.textBold(name, x, y + 1, -1185569);
      } else {
         gfx.text(name, x, y + 1, unlocked ? -1185569 : -5659750);
      }

      if (!unlocked) {
         gfx.paragraph(skin.unlockHint(), x, y + 13, w, -6054250, Integer.MAX_VALUE);
      }

      return hov;
   }
}
