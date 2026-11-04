package com.awakenedmod.awakened.client.render;

import net.minecraft.util.Mth;

public final class AnimEnvelopes {
   private AnimEnvelopes() {
   }

   public static float smooth(float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   public static float smoothstep(float lo, float hi, float v) {
      return smooth((v - lo) / (hi - lo));
   }

   public static float easeOutCubic(float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      float u = 1.0F - t;
      return 1.0F - u * u * u;
   }

   public static float easeInQuad(float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      return t * t;
   }

   public static float antic(AnimEnvelopes.Phases ph, float p) {
      if (p < ph.raiseEnd()) {
         return easeOutCubic(p / ph.raiseEnd());
      } else if (p < ph.snapStart()) {
         return 1.0F;
      } else {
         return p < ph.hitFrac() ? 1.0F - easeInQuad((p - ph.snapStart()) / (ph.hitFrac() - ph.snapStart())) : 0.0F;
      }
   }

   public static float strike(AnimEnvelopes.Phases ph, float p) {
      if (p < ph.snapStart()) {
         return 0.0F;
      } else if (p < ph.hitFrac()) {
         return easeInQuad((p - ph.snapStart()) / (ph.hitFrac() - ph.snapStart()));
      } else {
         return p < ph.followEnd() ? 1.0F : 1.0F - smooth((p - ph.followEnd()) / Math.max(1.0E-4F, 1.0F - ph.followEnd()));
      }
   }

   public static float jolt(AnimEnvelopes.Phases ph, float p, int joltTicks) {
      if (p < ph.hitFrac()) {
         return 0.0F;
      } else {
         float t = (p - ph.hitFrac()) * (float)ph.totalTicks() / (float)Math.max(1, joltTicks);
         if (t >= 1.0F) {
            return 0.0F;
         } else {
            float u = 1.0F - t;
            return u * u;
         }
      }
   }

   public static float channel(AnimEnvelopes.Phases ph, float p) {
      float in = smooth(p / Math.max(1.0E-4F, ph.raiseEnd()));
      float out = 1.0F - smooth((p - 0.85F) / 0.15F);
      return in * out;
   }

   public static float tremor(AnimEnvelopes.Phases ph, float p, float age, float amplitude) {
      float gateIn = smooth((p - ph.raiseEnd()) / 0.04F);
      float gateOut = 1.0F - smooth((p - ph.snapStart()) / 0.02F);
      return Mth.sin(age * 1.9F) * amplitude * Mth.clamp(gateIn * gateOut, 0.0F, 1.0F);
   }

   public static record Phases(float raiseEnd, float snapStart, float hitFrac, float followEnd, int totalTicks) {
      public static AnimEnvelopes.Phases of(int windupTicks, int recoverTicks) {
         int total = windupTicks + recoverTicks;
         int snap = Mth.clamp(windupTicks / 5, 2, 4);
         int raise = Mth.clamp(Math.round((float)windupTicks * 0.45F), 4, 12);
         if (raise + snap > windupTicks - 2) {
            raise = Math.max(2, windupTicks - snap - 2);
         }

         int follow = Mth.clamp(Math.round((float)recoverTicks * 0.4F), 3, 12);
         return new AnimEnvelopes.Phases(
            (float)raise / (float)total,
            (float)(windupTicks - snap) / (float)total,
            (float)windupTicks / (float)total,
            (float)(windupTicks + follow) / (float)total,
            total
         );
      }
   }
}
