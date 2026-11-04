package com.awakenedmod.awakened.content.velocity;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;

public record VelocityPalette(int body, int hot, int ember, int dark, int fog, int vapour) {
   public static final int SCARLET = 15217212;
   public static final int WHITE_HOT = 16774128;
   public static final int EMBER = 16742973;
   public static final int DARK = 2755088;
   public static final int PHASE_FOG = 655619;
   public static final int VAPOUR = 15919336;
   public static final int THEME_ARGB = -1560004;
   public static final DyeColor DEFAULT_DYE = DyeColor.RED;
   public static final VelocityPalette DEFAULT = new VelocityPalette(15217212, 16774128, 16742973, 2755088, 655619, 15919336);
   private static final int NEAR_WHITE = 16774128;
   private static final int WARM = 16742973;
   private static final float HOT_BODY_MIX = 0.6F;
   private static final float EMBER_LIGHTEN = 0.35F;
   private static final float EMBER_WARM_MIX = 0.25F;
   private static final float DARK_BRIGHTNESS = 0.18F;
   private static final float FOG_BRIGHTNESS = 0.043F;
   private static final float VAPOUR_BODY_MIX = 0.09F;
   private static final float SATURATION_BOOST = 1.15F;
   private static final float NEUTRAL_SATURATION = 0.12F;
   private static final float MIN_LUMINANCE = 0.22F;
   private static final VelocityPalette[] BY_DYE = new VelocityPalette[DyeColor.values().length];
   private static final Map<Integer, VelocityPalette> BY_BODY = new HashMap<>();

   public static VelocityPalette of(DyeColor dye) {
      return dye == null ? DEFAULT : BY_DYE[dye.getId()];
   }

   public static VelocityPalette of(Player player) {
      return player == null ? DEFAULT : of(VelocityVisualState.of(player).color());
   }

   public static VelocityPalette fromBody(int bodyRgb) {
      VelocityPalette known = BY_BODY.get(bodyRgb & 16777215);
      return known != null ? known : build(bodyRgb & 16777215);
   }

   public int bodyArgb() {
      return 0xFF000000 | this.body;
   }

   private static int bodyOf(DyeColor dye) {
      return dye.getTextureDiffuseColor() & 16777215;
   }

   private static VelocityPalette derive(int dyeRgb) {
      return build(readable(dyeRgb));
   }

   private static VelocityPalette build(int body) {
      return new VelocityPalette(
         body, mix(16774128, body, 0.6F), mix(lighten(body, 0.35F), 16742973, 0.25F), shade(body, 0.18F), shade(body, 0.043F), mix(16774128, body, 0.09F)
      );
   }

   private static int readable(int rgb) {
      return atLeastLuminance(saturate(rgb, 1.15F), 0.22F);
   }

   public static int saturate(int rgb, float factor) {
      float r = (float)(rgb >> 16 & 0xFF) / 255.0F;
      float g = (float)(rgb >> 8 & 0xFF) / 255.0F;
      float b = (float)(rgb & 0xFF) / 255.0F;
      float max = Math.max(r, Math.max(g, b));
      float min = Math.min(r, Math.min(g, b));
      if (max <= 0.0F) {
         return rgb;
      } else {
         float saturation = (max - min) / max;
         if (saturation < 0.12F) {
            return rgb;
         } else {
            float scaled = Math.min(1.0F, saturation * factor);
            float newMin = max * (1.0F - scaled);
            float span = max - min;
            return pack(rescale(r, min, span, newMin, max), rescale(g, min, span, newMin, max), rescale(b, min, span, newMin, max));
         }
      }
   }

   private static float rescale(float c, float min, float span, float newMin, float max) {
      return span <= 0.0F ? max : newMin + (c - min) / span * (max - newMin);
   }

   public static float luminance(int rgb) {
      return (0.2126F * (float)(rgb >> 16 & 0xFF) + 0.7152F * (float)(rgb >> 8 & 0xFF) + 0.0722F * (float)(rgb & 0xFF)) / 255.0F;
   }

   public static int atLeastLuminance(int rgb, float target) {
      float lum = luminance(rgb);
      return !(lum >= target) && !(lum >= 1.0F) ? mix(rgb, 16777215, (target - lum) / (1.0F - lum)) : rgb;
   }

   private static int pack(float r, float g, float b) {
      return channel(r) << 16 | channel(g) << 8 | channel(b);
   }

   private static int channel(float v) {
      return Math.max(0, Math.min(255, Math.round(v * 255.0F)));
   }

   public static int mix(int a, int b, float t) {
      float f = Math.max(0.0F, Math.min(1.0F, t));
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      return ar + (int)((float)(br - ar) * f) << 16 | ag + (int)((float)(bg - ag) * f) << 8 | ab + (int)((float)(bb - ab) * f);
   }

   public static int lighten(int rgb, float t) {
      return mix(rgb, 16777215, t);
   }

   public static int shade(int rgb, float f) {
      int r = (int)((float)(rgb >> 16 & 0xFF) * f);
      int g = (int)((float)(rgb >> 8 & 0xFF) * f);
      int b = (int)((float)(rgb & 0xFF) * f);
      return r << 16 | g << 8 | b;
   }

   static {
      for (DyeColor dye : DyeColor.values()) {
         VelocityPalette palette = dye == DEFAULT_DYE ? DEFAULT : derive(bodyOf(dye));
         BY_DYE[dye.getId()] = palette;
         BY_BODY.put(palette.body(), palette);
      }
   }
}
