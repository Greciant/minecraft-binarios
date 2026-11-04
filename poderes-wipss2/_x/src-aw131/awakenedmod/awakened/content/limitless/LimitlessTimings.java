package com.awakenedmod.awakened.content.limitless;

import net.minecraft.world.phys.Vec3;

public final class LimitlessTimings {
   private LimitlessTimings() {
   }

   public static final class Blue {
      public static final int FORM = 8;
      public static final int RELEASE = 40;
      public static final int FADE = 8;
      public static final int PULL_START = 2;
      public static final int INHALE = 5;
      public static final int IMPLODE_LEAD = 6;
      public static final int END = 12;
      public static final int PULL_TICKS = 22;
      public static final double CLUMP = 0.7;
      public static final double CLUMP_DROP = 0.5;

      private Blue() {
      }
   }

   public static final class Infinity {
      public static final int HANG = 10;
      public static final double STOP_DIST = 0.9;
      public static final double CHEST = 1.2;
      public static final double APPROACH = 2.5;
      public static final float SWING_LAND = 3.0F;
      public static final float SWING_HOLD = 8.0F;

      private Infinity() {
      }
   }

   public static final class Purple {
      public static final int RELEASE = 50;
      public static final double SPEED = 0.9;
      public static final double RANGE = 60.0;
      public static final int FADE = 10;
      public static final double RADIUS = 3.0;
      public static final double OVER_RADIUS = 5.0;
      public static final double OVER_SPEED = 0.6;
      public static final int BEATS = 4;
      public static final int BEAT = 10;
      public static final double OVER_BALL = 2.0;
      public static final int PULL = 6;
      public static final double PULL_SQUEEZE = 0.6;
      public static final double CHARGE_RADIUS = 0.42;
      public static final int GROW = 9;
      public static final int CUT_LIFE = 72;
      public static final int CANCEL_FADE = 6;
      public static final double HAND_WIDTH = 0.37;
      public static final double HAND_DROP = 0.37;
      public static final double HAND_REACH = 0.7;
      public static final double HAND_WIDTH_TP = 0.31;
      public static final double HAND_DROP_TP = 0.25;
      public static final double MERGE_AHEAD = 1.3;
      public static final double MERGE_DROP = 0.27;

      private Purple() {
      }

      public static int gone(double speed) {
         return 50 + (int)Math.round(60.0 / speed);
      }

      public static Vec3 centre(Vec3 merge, Vec3 fwd, double age, int gone, double speed) {
         return merge.add(fwd.scale(speed * Math.max(0.0, Math.min(age, (double)(gone + 10)) - 50.0)));
      }

      public static double radius(double age, double startR, double fullR) {
         double g = Math.max(0.0, Math.min(1.0, (age - 50.0) / 9.0));
         double s = 1.0 - g * g * (3.0 - 2.0 * g);
         return startR + (fullR - startR) * (1.0 - s * s);
      }
   }

   public static final class Red {
      public static final int BURST = 14;
      public static final double SPEED = 1.8;
      public static final double REACH = 16.0;
      public static final double ORB = 0.25;
      public static final int AFTER = 50;
      public static final double POINT_AHEAD = 0.75;
      public static final double POINT_HEIGHT = 1.35;
      public static final double TIP_WIDTH = 0.37;
      public static final double TIP_DROP = 0.37;
      public static final double TIP_REACH = 0.78;
      public static final double TIP_WIDTH_TP = 0.31;
      public static final double TIP_DROP_TP = 0.25;

      private Red() {
      }
   }
}
