package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import java.util.Locale;
import javax.annotation.Nullable;

public final class CommandCost {
   private static final int[] DURATION_LADDER_LEVEL = new int[]{0, 2, 4, 6, 8};
   private static final int[] DURATION_LADDER_CAP = new int[]{10, 15, 20, 30, 60};
   private static final int[] LEVEL_LADDER_LEVEL = new int[]{0, 3, 5, 7, 10};
   private static final int[] LEVEL_LADDER_CAP = new int[]{2, 3, 5, 7, 10};
   public static final int INFINITE_MASTERY = 11;

   private CommandCost() {
   }

   public static int points(Rule rule, int masteryLevel) {
      CommandCost.Breakdown breakdown = breakdown(rule, masteryLevel);
      return breakdown == null ? 0 : breakdown.points();
   }

   public static boolean paysOnSwitchOn(TargetKind kind) {
      return kind == null || !kind.lands();
   }

   @Nullable
   public static CommandCost.Breakdown breakdown(Rule rule, int masteryLevel) {
      Target target = rule.target();
      Effect effect = rule.effect();
      Trigger trigger = rule.trigger();
      if (target != null && effect != null && trigger != null && !rule.isEmpty()) {
         double effectBase = CommandConfig.weight(effect) * effect.costFactor(target.kind());
         double targetWeight = CommandConfig.weight(target);
         double size = sizeFactor(effect, rule.level());
         boolean radial = target.usesRadius();
         double radius = radial ? radiusFactor(rule.radius()) : 1.0;
         double duration = durationFactor(rule);
         double mastery = masteryFactor(masteryLevel);
         double cost = effectBase * targetWeight * size * radius * duration * mastery;
         long rounded = Math.round(cost);
         int points = (int)Math.min(2147483647L, Math.max(1L, rounded));
         return new CommandCost.Breakdown(effectBase, targetWeight, size, radial, radius, duration, mastery, points);
      } else {
         return null;
      }
   }

   private static double radiusFactor(int blocks) {
      double per = CommandConfig.value(CommandConfig.RADIUS_BLOCKS_PER_STEP);
      return 1.0 + (double)Math.max(0, blocks) / per;
   }

   private static double sizeFactor(Effect effect, int level) {
      if (!effect.hasLevel()) {
         return 1.0;
      } else {
         double step = CommandConfig.value(CommandConfig.LEVEL_STEP);
         return 1.0 + step * (double)(Math.max(1, Math.abs(level)) - 1);
      }
   }

   private static double durationFactor(Rule rule) {
      if (rule.instant()) {
         return 1.0;
      } else if (rule.infinite()) {
         return CommandConfig.value(CommandConfig.INFINITE_FACTOR);
      } else {
         double per = CommandConfig.value(CommandConfig.DURATION_SECONDS_PER_STEP);
         return 1.0 + (double)Math.max(0, rule.seconds()) / per;
      }
   }

   private static double masteryFactor(int masteryLevel) {
      double perLevel = CommandConfig.value(CommandConfig.MASTERY_DISCOUNT_PER_LEVEL);
      double most = CommandConfig.value(CommandConfig.MASTERY_DISCOUNT_MAX);
      return 1.0 - Math.min(most, perLevel * (double)Math.max(0, masteryLevel));
   }

   public static int durationCap(int masteryLevel) {
      return ladder(DURATION_LADDER_LEVEL, DURATION_LADDER_CAP, masteryLevel);
   }

   public static int levelCap(int masteryLevel) {
      return ladder(LEVEL_LADDER_LEVEL, LEVEL_LADDER_CAP, masteryLevel);
   }

   public static int strengthCap(int masteryLevel, float abilityPower) {
      int base = levelCap(masteryLevel);
      double raised = Math.floor((double)base * (1.0 + (double)Math.max(0.0F, abilityPower)) + 1.0E-4);
      return (int)Math.max((double)base, Math.min(2.147483647E9, raised));
   }

   public static int radiusCap(int masteryLevel) {
      return levelCap(masteryLevel);
   }

   public static boolean allowsInfinite(int masteryLevel) {
      return masteryLevel >= 11;
   }

   public static int masteryForDuration(int seconds) {
      return seconds == -1 ? 11 : lowestFor(DURATION_LADDER_LEVEL, DURATION_LADDER_CAP, seconds);
   }

   public static int masteryForLevel(int level) {
      return lowestFor(LEVEL_LADDER_LEVEL, LEVEL_LADDER_CAP, level);
   }

   private static int lowestFor(int[] levels, int[] caps, int wanted) {
      for (int i = 0; i < levels.length; i++) {
         if (caps[i] >= wanted) {
            return levels[i];
         }
      }

      return levels[levels.length - 1];
   }

   private static int ladder(int[] levels, int[] caps, int masteryLevel) {
      int cap = caps[0];

      for (int i = 0; i < levels.length; i++) {
         if (masteryLevel >= levels[i]) {
            cap = caps[i];
         }
      }

      return cap;
   }

   public static record Breakdown(
      double effectBase, double targetWeight, double sizeFactor, boolean radial, double radiusFactor, double durationFactor, double masteryFactor, int points
   ) {
      public String formula() {
         return whole(this.effectBase)
            + " x "
            + factor(this.targetWeight)
            + " x "
            + factor(this.sizeFactor)
            + (this.radial ? " x " + factor(this.radiusFactor) : "")
            + " x "
            + factor(this.durationFactor)
            + " x "
            + factor(this.masteryFactor)
            + " = "
            + this.points;
      }

      private static String whole(double value) {
         return value == Math.rint(value) ? String.valueOf((long)value) : factor(value);
      }

      private static String factor(double value) {
         String text = String.format(Locale.ROOT, "%.2f", value);
         return text.endsWith("0") ? text.substring(0, text.length() - 1) : text;
      }
   }
}
