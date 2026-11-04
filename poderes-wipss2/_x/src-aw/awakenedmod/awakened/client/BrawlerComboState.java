package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.brawler.BrawlerTuning;
import com.awakenedmod.awakened.core.net.BrawlerComboPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class BrawlerComboState {
   private static final long NEVER = -4611686018427387904L;
   private static int count;
   private static int tier;
   private static int best;
   private static boolean paused;
   private static int remainingAtReceipt;
   private static long receivedMs;
   private static int sessionBest;
   private static int bestBeforeCombo;
   private static long hitMs = -4611686018427387904L;
   private static float hitStartScale = 1.0F;
   private static float hitPeakScale = 1.6F;
   private static boolean hitTenth;
   private static long tierUpMs = -4611686018427387904L;
   private static long spentMs = -4611686018427387904L;
   private static int spentCount;
   private static long dropMs = -4611686018427387904L;
   private static int endedCombo;
   private static boolean endedWasBest;
   private static long fovKickMs = -4611686018427387904L;
   private static long freeMs = -4611686018427387904L;
   private static int hurryStage;

   private BrawlerComboState() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(BrawlerComboState::onClientTick);
   }

   public static void apply(BrawlerComboPayload payload) {
      long now = Util.getMillis();
      int previous = count;
      int event = payload.event();
      if (event == 3) {
         endedCombo = payload.count();
         endedWasBest = endedCombo > 0 && endedCombo > bestBeforeCombo;
         dropMs = now;
         count = 0;
         tier = 0;
         paused = false;
         remainingAtReceipt = 0;
         hurryStage = 0;
         BrawlerComboSounds.drop(endedCombo);
      } else {
         if (previous <= 0 && payload.count() > 0) {
            bestBeforeCombo = sessionBest;
         }

         int oldTier = tier;
         count = payload.count();
         sessionBest = Math.max(sessionBest, count);
         tier = BrawlerTuning.tier(count);
         paused = payload.paused();
         remainingAtReceipt = payload.remainingTicks();
         switch (event) {
            case 1:
            case 2:
            case 5:
               if (count > previous) {
                  onHit(now, payload.flavor(), event == 2, oldTier);
               }
            case 3:
            default:
               break;
            case 4:
               spentCount = previous;
               spentMs = now;
               hurryStage = 0;
               hitMs = now + 500L;
               hitStartScale = 1.35F;
               hitPeakScale = 1.6F;
               hitTenth = false;
               BrawlerComboSounds.cashIn();
               break;
            case 6:
               freeMs = now;
         }
      }

      best = payload.best();
      receivedMs = now;
   }

   private static void onHit(long now, int flavor, boolean tierUp, int oldTier) {
      float current = BrawlerComboAnimator.popScale(now);
      hitStartScale = Math.max(current, 1.35F);
      hitTenth = count % 10 == 0;
      hitPeakScale = hitTenth ? 1.9F : 1.6F;
      hitMs = Math.max(now, spentMs + 500L);
      hurryStage = 0;
      BrawlerComboSounds.hit(count, flavor);
      if (flavor == 2) {
         fovKickMs = now;
      }

      if (flavor != 0 && freeAge(now) > 0L) {
         freeMs = -4611686018427387904L;
      }

      if (tierUp && tier > oldTier) {
         tierUpMs = now;
         BrawlerComboSounds.tierUp(tier);
         ScreenShakeFx.local(BrawlerTuning.shakeMagnitude(1.0F), 0, 6);
      } else {
         hitShake(tier);
      }
   }

   private static void hitShake(int tier) {
      float deg;
      int ticks;
      if (tier >= 4) {
         deg = 0.5F;
         ticks = 4;
      } else if (tier >= 3) {
         deg = 0.4F;
         ticks = 3;
      } else {
         if (tier < 2) {
            return;
         }

         deg = 0.25F;
         ticks = 3;
      }

      ScreenShakeFx.local(BrawlerTuning.shakeMagnitude(deg), 0, ticks);
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         reset();
      } else if (!mc.isPaused()) {
         BrawlerComboSounds.tick();
         if (count >= 5 && !paused) {
            float left = remainingTicks();
            if (hurryStage < 1 && left <= 10.0F) {
               hurryStage = 1;
               BrawlerComboSounds.hurry();
            }

            if (hurryStage < 2 && left <= 5.0F) {
               hurryStage = 2;
               BrawlerComboSounds.hurry();
            }
         }
      }
   }

   public static void reset() {
      count = 0;
      tier = 0;
      paused = false;
      remainingAtReceipt = 0;
      endedCombo = 0;
      endedWasBest = false;
      sessionBest = 0;
      bestBeforeCombo = 0;
      hitMs = -4611686018427387904L;
      tierUpMs = -4611686018427387904L;
      spentMs = -4611686018427387904L;
      dropMs = -4611686018427387904L;
      fovKickMs = -4611686018427387904L;
      freeMs = -4611686018427387904L;
      hurryStage = 0;
      BrawlerComboSounds.clear();
   }

   public static int count() {
      return count;
   }

   public static int tier() {
      return tier;
   }

   public static int best() {
      return best;
   }

   public static boolean paused() {
      return paused;
   }

   public static long hitAge(long now) {
      return now - hitMs;
   }

   public static float hitStartScale() {
      return hitStartScale;
   }

   public static float hitPeakScale() {
      return hitPeakScale;
   }

   public static boolean hitTenth() {
      return hitTenth;
   }

   public static long tierUpAge(long now) {
      return now - tierUpMs;
   }

   public static long spentAge(long now) {
      return now - spentMs;
   }

   public static int spentCount() {
      return spentCount;
   }

   public static long dropAge(long now) {
      return now - dropMs;
   }

   public static int endedCombo() {
      return endedCombo;
   }

   public static boolean endedWasBest() {
      return endedWasBest;
   }

   public static long fovKickAge(long now) {
      return now - fovKickMs;
   }

   public static long freeAge(long now) {
      return now - freeMs;
   }

   public static boolean freeShowing(long now) {
      long age = freeAge(now);
      return age >= 0L && age < 1500L;
   }

   public static float remainingTicks() {
      if (count <= 0) {
         return 0.0F;
      } else if (paused) {
         return (float)remainingAtReceipt;
      } else {
         float elapsed = (float)(Util.getMillis() - receivedMs) / 50.0F;
         return Math.max(0.0F, (float)remainingAtReceipt - elapsed);
      }
   }

   public static float windowFraction() {
      return Math.min(1.0F, remainingTicks() / 60.0F);
   }
}
