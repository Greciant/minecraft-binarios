package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.caster.Caster;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

public record FlameTone(float blueFraction) {
   public static final FlameTone ORANGE = new FlameTone(0.0F);
   public static final FlameTone BLUE = new FlameTone(1.0F);
   public static final int SYNC_STEPS = 15;
   public static final int SYNC_MASK = 15;
   private static final float UI_LIFT = 0.65F;
   private static final int UI_ORANGE = uiEnd(FlameStyle.ORANGE);
   private static final int UI_BLUE = uiEnd(FlameStyle.BLUE);

   public FlameTone(float blueFraction) {
      blueFraction = Mth.clamp(blueFraction, 0.0F, 1.0F);
      this.blueFraction = blueFraction;
   }

   public static FlameTone forHeat(float heat01) {
      float f = FlameStyle.blueFraction(heat01);
      return f <= 0.0F ? ORANGE : (f >= 1.0F ? BLUE : new FlameTone(f));
   }

   public static FlameTone of(boolean blue) {
      return blue ? BLUE : ORANGE;
   }

   public static FlameTone of(ServerPlayer player) {
      return forHeat(PyroHeatState.heat(PowerService.data(player)));
   }

   public static FlameTone of(Caster caster) {
      return forHeat(PyroHeatState.heat(caster));
   }

   public FlameStyle style() {
      return this.blueFraction >= 1.0F ? FlameStyle.BLUE : FlameStyle.ORANGE;
   }

   public boolean isBlue() {
      return this.blueFraction >= 1.0F;
   }

   public float soundPitch() {
      return Mth.lerp(this.blueFraction, FlameStyle.ORANGE.soundPitch(), FlameStyle.BLUE.soundPitch());
   }

   private static int uiEnd(FlameStyle style) {
      return mixRgb(style.body(), style.mid(), 0.65F);
   }

   public int uiColor() {
      return uiColor(this.blueFraction);
   }

   public static int uiColor(float blueFraction) {
      return mixRgb(UI_ORANGE, UI_BLUE, blueFraction);
   }

   public static int mixRgb(int a, int b, float t) {
      float f = Mth.clamp(t, 0.0F, 1.0F);
      int r = Math.round(Mth.lerp(f, (float)(a >> 16 & 0xFF), (float)(b >> 16 & 0xFF)));
      int g = Math.round(Mth.lerp(f, (float)(a >> 8 & 0xFF), (float)(b >> 8 & 0xFF)));
      int b2 = Math.round(Mth.lerp(f, (float)(a & 0xFF), (float)(b & 0xFF)));
      return r << 16 | g << 8 | b2;
   }

   public int wireBody() {
      return this.style().body();
   }

   public int wireColor2() {
      return Math.round(this.blueFraction * 255.0F) & 0xFF;
   }

   public static float blueFractionFromWire(int color, int color2) {
      if (color2 == -1) {
         return FlameStyle.nearestBody(color) == FlameStyle.BLUE ? 1.0F : 0.0F;
      } else {
         return (float)(color2 & 0xFF) / 255.0F;
      }
   }

   public int syncByte() {
      return Math.round(this.blueFraction * 15.0F) & 15;
   }

   public static float blueFractionFromSyncByte(int code) {
      return (float)(code & 15) / 15.0F;
   }
}
