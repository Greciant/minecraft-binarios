package com.awakenedmod.awakened.client.screen.card;

import com.awakenedmod.awakened.client.screen.GuiPanels;

public final class CardStyle {
   public static final int INK = -1185569;
   public static final int SEC = -3554376;
   public static final int SOFT = -5659750;
   public static final int LOCK = -6054250;
   public static final int FACE = -13751512;
   public static final int TABFACE = -14277856;
   public static final int BORDER = -15461872;
   public static final int SHADE = -12238791;
   public static final int DEEP = -15987958;
   public static final int MID = -9541534;
   public static final int ROWFILL = -13225168;
   public static final int LOCKFACE = -14803942;
   public static final int LOCKEDGE = -12962254;
   public static final int TRACK = -14803942;
   public static final int CHIP = -12962254;
   public static final int CHIP_HOV = -12041411;
   public static final int CHIP_ON = -2567480;
   public static final int CHIP_ON_HOV = -1646120;
   public static final int CHIP_ON_INK = -13751512;
   public static final int DANGER = -1734024;
   public static final int CHIP_H = 12;
   public static final int GOOD = -11141291;
   public static final int BAD = -43691;
   public static final int CARD_W = 400;
   public static final int GUTTER = 20;
   public static final int BAR_H = 9;
   public static final int TAB_H = 18;
   public static final int TAB_PITCH = 20;
   public static final int RULE_GAP = 6;

   private CardStyle() {
   }

   public static int opaque(int rgb) {
      return 0xFF000000 | rgb & 16777215;
   }

   public static double lum(int rgb) {
      return (0.2126 * (double)(rgb >> 16 & 0xFF) + 0.7152 * (double)(rgb >> 8 & 0xFF) + 0.0722 * (double)(rgb & 0xFF)) / 255.0;
   }

   public static int band(int theme) {
      int t = opaque(theme);
      return lum(t) < 0.35 ? GuiPanels.blend(t, 16777215, 0.45F) : t;
   }

   public static int titleInk(int band) {
      return lum(band) > 0.5 ? -13952240 : -2848;
   }

   public static int ring(int band) {
      return GuiPanels.blend(band, 3025704, 0.2F);
   }
}
