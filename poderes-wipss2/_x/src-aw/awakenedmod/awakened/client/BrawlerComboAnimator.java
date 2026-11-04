package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.brawler.BrawlerTuning;
import net.minecraft.Util;
import net.minecraft.util.Mth;

public record BrawlerComboAnimator(
   BrawlerComboAnimator.Mode mode,
   float numberScale,
   float flash,
   float blockX,
   float blockY,
   float numberDx,
   float numberDy,
   float rankScale,
   float rankFlash,
   int outline,
   boolean barWhite,
   float alpha,
   float blowScale,
   float blowAlpha,
   boolean spentLabel,
   boolean newBest
) {
   public static final int OUTLINE = -15724528;
   private static final float HUE_CYCLE_MS = 2000.0F;
   public static final BrawlerComboAnimator REST = new BrawlerComboAnimator(
      BrawlerComboAnimator.Mode.LIVE, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, -15724528, false, 1.0F, 1.0F, 0.0F, false, false
   );

   public static BrawlerComboAnimator frame() {
      long now = Util.getMillis();
      int count = BrawlerComboState.count();
      long spentAge = BrawlerComboState.spentAge(now);
      if (spentAge >= 0L && spentAge < 500L) {
         return spentFrame(spentAge);
      } else if (count >= 2) {
         return liveFrame(now, count);
      } else {
         long dropAge = BrawlerComboState.dropAge(now);
         int ended = BrawlerComboState.endedCombo();
         if (dropAge >= 0L && ended >= 2) {
            if (ended < 8) {
               if (dropAge < 400L) {
                  return fade(BrawlerComboAnimator.Mode.DROP_FADE, 1.0F - (float)dropAge / 400.0F, false);
               }
            } else if (dropAge < 2000L) {
               float alpha = dropAge < 1500L ? 1.0F : 1.0F - (float)(dropAge - 1500L) / 500.0F;
               boolean newBest = BrawlerComboState.endedWasBest() && dropAge < 1500L;
               return fade(BrawlerComboAnimator.Mode.SUMMARY, alpha, newBest);
            }
         }

         return hidden();
      }
   }

   private static BrawlerComboAnimator hidden() {
      return new BrawlerComboAnimator(
         BrawlerComboAnimator.Mode.HIDDEN, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, -15724528, false, 0.0F, 1.0F, 0.0F, false, false
      );
   }

   private static BrawlerComboAnimator fade(BrawlerComboAnimator.Mode mode, float alpha, boolean newBest) {
      return new BrawlerComboAnimator(
         mode, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, -15724528, false, Mth.clamp(alpha, 0.0F, 1.0F), 1.0F, 0.0F, false, newBest
      );
   }

   private static BrawlerComboAnimator spentFrame(long age) {
      float t = Math.min(1.0F, (float)age / 400.0F);
      float ease = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
      float blowScale = 1.0F + 1.5999999F * ease;
      float blowAlpha = 1.0F - t;
      return new BrawlerComboAnimator(
         BrawlerComboAnimator.Mode.SPENT, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, -15724528, false, 1.0F, blowScale, blowAlpha, true, false
      );
   }

   private static BrawlerComboAnimator liveFrame(long now, int count) {
      int tier = BrawlerTuning.tier(count);
      long hitAge = BrawlerComboState.hitAge(now);
      long tierUpAge = BrawlerComboState.tierUpAge(now);
      float scale = popScale(now);
      float flash = 0.0F;
      if (hitAge >= 0L && hitAge < 100L) {
         flash = 1.0F - (float)hitAge / 100.0F;
      }

      float rankScale = 1.0F;
      float rankFlash = 0.0F;
      float blockX = 0.0F;
      float blockY = 0.0F;
      if (tierUpAge >= 0L) {
         if (tierUpAge < 50L) {
            flash = 1.0F;
            rankFlash = 1.0F;
         }

         if (tierUpAge < 250L) {
            float decay = 1.0F - (float)tierUpAge / 250.0F;
            blockX += 2.0F * decay * Mth.sin((float)tierUpAge * 0.09F);
            blockY += 1.0F * decay * Mth.cos((float)tierUpAge * 0.11F);
         }

         if (tierUpAge < 400L) {
            float t = 1.0F - (float)tierUpAge / 400.0F;
            float from = 1.6923078F;
            rankScale = 1.0F + (from - 1.0F) * t * t * t;
         }
      }

      float numberDx = 0.0F;
      float numberDy = 0.0F;
      if (tier >= 4 && hitAge >= 0L && hitAge < 100L) {
         long step = hitAge / 16L;
         numberDx = ((step & 1L) == 0L ? 1.0F : -1.0F) * 1.0F;
         numberDy = ((step / 2L & 1L) == 0L ? 1.0F : -1.0F) * 1.0F;
      }

      int outline = -15724528;
      if (tier >= 5) {
         blockY += 0.5F * Mth.sin((float)now * 0.006F);
         float hue = (float)(now % 2000L) / 2000.0F;
         outline = 0xFF000000 | Mth.hsvToRgb(hue, 0.9F, 1.0F);
      }

      boolean barWhite = false;
      if (!BrawlerComboState.paused() && BrawlerComboState.remainingTicks() <= 10.0F) {
         barWhite = now / 100L % 2L == 0L;
         blockY += 1.0F * Mth.sin((float)now * 0.02F);
      }

      return new BrawlerComboAnimator(
         BrawlerComboAnimator.Mode.LIVE,
         scale,
         flash,
         blockX,
         blockY,
         numberDx,
         numberDy,
         rankScale,
         rankFlash,
         outline,
         barWhite,
         1.0F,
         1.0F,
         0.0F,
         false,
         false
      );
   }

   public static float popScale(long now) {
      long age = BrawlerComboState.hitAge(now);
      if (age < 0L) {
         return 1.0F;
      } else {
         float start = BrawlerComboState.hitStartScale();
         float peak = BrawlerComboState.hitPeakScale();
         if (age < 50L) {
            return start + (peak - start) * ((float)age / 50.0F);
         } else {
            long hold = BrawlerComboState.hitTenth() ? 100L : 0L;
            long decayAge = age - 50L - hold;
            if (decayAge < 0L) {
               return peak;
            } else if (decayAge >= 300L) {
               return 1.0F;
            } else {
               float t = 1.0F - (float)decayAge / 300.0F;
               return 1.0F + (peak - 1.0F) * t * t * t;
            }
         }
      }
   }

   public static enum Mode {
      HIDDEN,
      LIVE,
      DROP_FADE,
      SUMMARY,
      SPENT;
   }
}
