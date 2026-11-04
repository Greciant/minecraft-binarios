package com.awakenedmod.awakened.content.spider;

public enum SpiderTrick {
   FRONT_FLIP("web_trick_front_flip"),
   BACKFLIP("web_trick_backflip"),
   TWIST_LEFT("web_trick_twist_left"),
   TWIST_RIGHT("web_trick_twist_right"),
   SPIN("web_trick_spin");

   public static final int DURATION = 9;
   public static final int MAX_COMBO = 3;
   public static final double BOOST_PER_POINT = 0.1;
   private static final SpiderTrick[] VALUES = values();
   private final String animation;

   private SpiderTrick(String animation) {
      this.animation = animation;
   }

   public String animation() {
      return this.animation;
   }

   public static SpiderTrick fromInput(boolean forward, boolean back, boolean left, boolean right) {
      if (forward != back) {
         return forward ? FRONT_FLIP : BACKFLIP;
      } else if (left != right) {
         return left ? TWIST_LEFT : TWIST_RIGHT;
      } else {
         return SPIN;
      }
   }

   public static SpiderTrick byId(int id) {
      return id >= 0 && id < VALUES.length ? VALUES[id] : null;
   }

   public static double boost(int points) {
      return 1.0 + 0.1 * (double)Math.min(points, 3);
   }
}
