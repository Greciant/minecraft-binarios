package com.impactframe.client;

import com.impactframe.ImpactConfig;
import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class ImpactState {
   private static final int INVERT_GAP_TICKS = 4;
   private static final int NEVER = 16777216;
   private static boolean active;
   private static long startNanos;
   private static float peak;
   private static ImpactTier tier = ImpactTier.NORMAL;
   private static ImpactType currentType;
   private static String currentTextKey;
   private static int invertMask;
   private static int ticksSinceTrigger = 16777216;
   private static int ticksSinceInvert = 16777216;
   private static long triggerSerial;
   private static final long MAX_FREEZE_HOLD_NANOS = 1000000000L;
   private static boolean freezeHold;
   private static long heldNanos;
   private static long holdMark;
   private static final ArrayDeque<long[]> RECENT_FLASHES = new ArrayDeque<>();
   private static float dirX;
   private static float dirY;
   private static boolean letterboxOn;
   private static float textOffsetX;
   private static float textOffsetY;
   private static Entity focusEntity;
   private static Vec3 focusPos;
   private static float focusHalf = 0.5F;

   private ImpactState() {
   }

   public static boolean trigger(ImpactType type, ImpactTier tier, float scale, Entity entity, Vec3 pos, float half) {
      ImpactPreset preset = ImpactConfig.preset();
      boolean ridiculous = preset == ImpactPreset.RIDICULOUS;
      float scaledEvent = Mth.clamp(scale, 0.0F, 1.0F);
      float value = ridiculous
         ? scaledEvent
         : (
            preset == ImpactPreset.ADVANCED
               ? Mth.clamp((float)ImpactConfig.triggerIntensity(type) * scaledEvent, 0.0F, 1.0F)
               : Mth.clamp(type.baseStrength * scaledEvent * (float)ImpactConfig.intensity(), 0.0F, 1.0F)
         );
      if (value <= 0.001F) {
         return false;
      } else {
         float current = strength();
         boolean higher = tier.ordinal() > ImpactState.tier.ordinal();
         boolean kill = type == ImpactType.KILL;
         if (!ridiculous && !kill && ticksSinceTrigger < ImpactConfig.cooldownTicks()) {
            if (!active || !higher) {
               return false;
            }
         } else if (!ridiculous && !kill && active && value <= current && !higher) {
            return false;
         }

         int flashes = Integer.bitCount(tier.invertMask);
         boolean invertAllowed = !ImpactConfig.safeMode() && (tier == ImpactTier.SMASH || ticksSinceInvert >= 4) && flashAllowed(flashes);
         triggerSerial++;
         active = true;
         startNanos = System.nanoTime();
         heldNanos = 0L;
         holdMark = startNanos;
         freezeHold = false;
         peak = value;
         ImpactState.tier = tier;
         currentType = type;
         currentTextKey = type.randomSfxTranslationKey();
         invertMask = invertAllowed ? tier.invertMask : 0;
         ticksSinceTrigger = 0;
         if (invertAllowed) {
            ticksSinceInvert = 0;
            RECENT_FLASHES.addLast(new long[]{startNanos, (long)flashes});
         }

         dirX = 0.0F;
         dirY = 0.0F;
         letterboxOn = false;
         ThreadLocalRandom random = ThreadLocalRandom.current();
         textOffsetX = (random.nextFloat() - 0.5F) * 0.5F;
         textOffsetY = (random.nextFloat() - 0.5F) * 0.4F;
         focusEntity = entity;
         if (entity != null) {
            focusPos = entity.position().add(0.0, (double)entity.getBbHeight() * 0.5, 0.0);
            focusHalf = Math.max(entity.getBbHeight(), entity.getBbWidth()) * 0.5F;
         } else {
            focusPos = pos;
            focusHalf = half;
         }

         return true;
      }
   }

   private static boolean flashAllowed(int flashes) {
      long now = System.nanoTime();

      while (!RECENT_FLASHES.isEmpty() && now - ((long[])RECENT_FLASHES.peekFirst())[0] > 1000000000L) {
         RECENT_FLASHES.pollFirst();
      }

      int used = 0;

      for (long[] entry : RECENT_FLASHES) {
         used += (int)entry[1];
      }

      return used + flashes <= ImpactConfig.flashBudget();
   }

   public static void setDecor(float x, float y, boolean bars) {
      float length = (float)Math.sqrt((double)(x * x + y * y));
      if (length > 0.001F) {
         dirX = x / length;
         dirY = y / length;
      } else {
         dirX = 0.0F;
         dirY = 0.0F;
      }

      letterboxOn = bars;
   }

   public static long triggerSerial() {
      return triggerSerial;
   }

   public static boolean addLetterboxToTrigger(long serial) {
      if (active && serial == triggerSerial) {
         letterboxOn = true;
         return true;
      } else {
         return false;
      }
   }

   public static float dirX() {
      return dirX;
   }

   public static float dirY() {
      return dirY;
   }

   public static float textOffsetX() {
      return textOffsetX;
   }

   public static float textOffsetY() {
      return textOffsetY;
   }

   public static float letterboxAmount() {
      if (active && letterboxOn && ImpactConfig.letterbox()) {
         float in = Mth.clamp(elapsedSeconds() / 0.1F, 0.0F, 1.0F);
         float out = Mth.clamp(strength() / Math.max(peak, 0.001F) * 2.5F, 0.0F, 1.0F);
         return ImpactConfig.letterboxSize() * in * in * (3.0F - 2.0F * in) * out;
      } else {
         return 0.0F;
      }
   }

   public static float strength() {
      if (!active) {
         return 0.0F;
      } else {
         syncHold();
         float t = envelopeSeconds();
         float hold = tier.holdSeconds();
         if (t < hold) {
            return peak;
         } else if (t >= tier.totalSeconds()) {
            return 0.0F;
         } else {
            float remaining = 1.0F - (t - hold) / tier.releaseSeconds;
            return peak * remaining * remaining * remaining;
         }
      }
   }

   public static void setFreezeHold(boolean hold) {
      syncHold();
      freezeHold = hold;
   }

   private static void syncHold() {
      long now = System.nanoTime();
      if (active && freezeHold && heldNanos < 1000000000L && (float)(now - startNanos - heldNanos) / 1.0E9F >= tier.holdSeconds()) {
         heldNanos = heldNanos + (now - holdMark);
      }

      holdMark = now;
   }

   private static float envelopeSeconds() {
      return (float)(System.nanoTime() - startNanos - heldNanos) / 1.0E9F;
   }

   public static float elapsedSeconds() {
      return !active ? 0.0F : (float)(System.nanoTime() - startNanos) / 1.0E9F;
   }

   public static boolean invertNow() {
      if (active && !ImpactConfig.safeMode()) {
         int frame = (int)(elapsedSeconds() * 60.0F);
         return frame < 32 && (invertMask >>> frame & 1) != 0;
      } else {
         return false;
      }
   }

   public static ImpactTier tier() {
      return tier;
   }

   public static ImpactType type() {
      return currentType;
   }

   public static String textKey() {
      return currentTextKey;
   }

   public static Entity focusEntity() {
      return focusEntity;
   }

   public static Vec3 focusPos() {
      return focusPos;
   }

   public static float focusHalf() {
      return focusHalf;
   }

   public static void updateFocus(Vec3 pos, float half) {
      focusPos = pos;
      focusHalf = half;
   }

   public static void tick() {
      if (ticksSinceTrigger < 16777216) {
         ticksSinceTrigger++;
      }

      if (ticksSinceInvert < 16777216) {
         ticksSinceInvert++;
      }

      syncHold();
      if (active && envelopeSeconds() >= tier.totalSeconds()) {
         active = false;
         currentType = null;
         currentTextKey = null;
         focusEntity = null;
         focusPos = null;
         focusHalf = 0.5F;
         invertMask = 0;
         dirX = 0.0F;
         dirY = 0.0F;
         letterboxOn = false;
         textOffsetX = 0.0F;
         textOffsetY = 0.0F;
      }
   }

   public static void reset() {
      triggerSerial++;
      active = false;
      freezeHold = false;
      heldNanos = 0L;
      currentType = null;
      currentTextKey = null;
      focusEntity = null;
      focusPos = null;
      focusHalf = 0.5F;
      ticksSinceTrigger = 16777216;
      ticksSinceInvert = 16777216;
      RECENT_FLASHES.clear();
      dirX = 0.0F;
      dirY = 0.0F;
      letterboxOn = false;
      textOffsetX = 0.0F;
      textOffsetY = 0.0F;
   }
}
