package com.awakenedmod.awakened.client.npc;

import com.zigythebird.playeranimcore.easing.EasingType;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;

final class NpcEasing {
   private static final float BACK = 1.70158F;

   private NpcEasing() {
   }

   @Nullable
   static Float2FloatFunction named(@Nullable String name) {
      if (name == null) {
         return null;
      } else {
         return switch (name) {
            case "easeInSine" -> t -> 1.0F - Mth.cos(t * (float) (Math.PI / 2));
            case "easeOutSine" -> t -> Mth.sin(t * (float) (Math.PI / 2));
            case "easeInOutSine" -> t -> -(Mth.cos((float) Math.PI * t) - 1.0F) / 2.0F;
            case "easeInQuad" -> t -> t * t;
            case "easeOutQuad" -> t -> 1.0F - (1.0F - t) * (1.0F - t);
            case "easeInOutQuad" -> t -> t < 0.5F ? 2.0F * t * t : 1.0F - (-2.0F * t + 2.0F) * (-2.0F * t + 2.0F) / 2.0F;
            case "easeInCubic" -> t -> t * t * t;
            case "easeOutCubic" -> t -> 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
            case "easeInOutCubic" -> t -> t < 0.5F ? 4.0F * t * t * t : 1.0F - cube(-2.0F * t + 2.0F) / 2.0F;
            case "easeInBack" -> t -> t * t * (2.70158F * t - 1.70158F);
            case "easeOutBack" -> t -> 1.0F + 2.70158F * cube(t - 1.0F) + 1.70158F * (t - 1.0F) * (t - 1.0F);
            default -> null;
         };
      }
   }

   static Float2FloatFunction of(@Nullable EasingType type, String fallback) {
      String name = type == EasingType.EASE_OUT_QUAD
         ? "easeOutQuad"
         : (
            type == EasingType.EASE_IN_QUAD
               ? "easeInQuad"
               : (
                  type == EasingType.EASE_IN_OUT_QUAD
                     ? "easeInOutQuad"
                     : (
                        type == EasingType.EASE_IN_OUT_SINE
                           ? "easeInOutSine"
                           : (
                              type == EasingType.EASE_OUT_SINE
                                 ? "easeOutSine"
                                 : (type == EasingType.EASE_OUT_CUBIC ? "easeOutCubic" : (type == EasingType.EASE_IN_OUT_CUBIC ? "easeInOutCubic" : fallback))
                           )
                     )
               )
         );
      Float2FloatFunction f = named(name);
      return f == null ? t -> t : f;
   }

   private static float cube(float v) {
      return v * v * v;
   }
}
