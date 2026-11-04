package com.awakenedmod.awakened.content.limitless;

public final class LimitlessTuning {
   private LimitlessTuning() {
   }

   public static final class Blue {
      public static final String ID = "limitless_blue";
      public static final float TAP_COST = 10.0F;
      public static final float HOLD_COST = 4.0F;
      public static final float HOLD_DRAIN_PER_SECOND = 12.0F;
      public static final int HOLD_TICKS = 8;
      public static final double RANGE = 20.0;
      public static final double AIM_CONE = 4.0;
      public static final double AIR_RANGE = 8.0;
      public static final double OVER_BODY = 0.4;
      public static final double PULL_RADIUS = 8.0;
      public static final double PULL_RADIUS_MAX = 13.0;
      public static final double CRUSH_RADIUS = 1.5;
      public static final float CRUSH_DAMAGE = 1.0F;
      public static final int CRUSH_EVERY = 10;
      public static final double STASH_RADIUS = 1.5;
      public static final double STEER = 0.5;
      public static final double STEER_MAX = 2.6;
      public static final double LEAN = 0.07;
      public static final int SLAM_STOP = 2;
      public static final double KEEP = 0.2;
      public static final double YANK_LIFT = 0.3;
      public static final double YANK_LIFT_MAX = 2.0;
      public static final double SLAM_SPEED = 0.45;
      public static final float SLAM_DAMAGE = 3.0F;
      public static final int PLAYER_PULSE = 3;
      public static final double PLAYER_TUG = 0.55;
      public static final double LOOSE_SPEED = 0.6;
      public static final double CLUMPED = 1.6;
      public static final float IMPLODE_DAMAGE = 3.0F;
      public static final float IMPLODE_PER_BODY = 1.0F;
      public static final float IMPLODE_BONUS_MAX = 3.0F;
      public static final double IMPLODE_REACH = 3.0;
      public static final int IMPLODE_STOP = 3;
      public static final double IMPLODE_SHAKE_RADIUS = 16.0;
      public static final float IMPLODE_SHAKE = 0.12F;
      public static final int IMPLODE_SHAKE_HOLD = 1;
      public static final int IMPLODE_SHAKE_FADE = 8;
      public static final double POP_REACH = 2.5;
      public static final double POP_BODY_REACH = 1.5;
      public static final double ORB_TARGET = 1.2;
      public static final double ORB_CLEARANCE = 3.0;
      public static final double ORB_OFF_FACE = 0.8;
      public static final double PULL_SPEED = 1.6;
      public static final int PULL_RAMP = 8;
      public static final double WALL_OFF = 1.0;
      public static final double ARRIVE = 0.6;
      public static final double ARRIVE_SLACK = 0.6;
      public static final double BRAKE = 0.25;
      public static final double CARRY = 0.25;
      public static final double HOLD_GRAVITY = 0.03;
      public static final double SANITY_SPEED = 6.0;
      public static final int CONFIRM_TICKS = 10;
      public static final double CARRY_MASTERY = 8.0;
      public static final int GUARD_GRACE = 10;
      public static final int GUARD_MAX = 400;

      private Blue() {
      }
   }

   public static final class Infinity {
      public static final float COST_PER_DAMAGE = 1.0F;
      public static final float MIN_COST = 1.0F;
      public static final double ARROW_BASE_DAMAGE = 2.0;
      public static final float OTHER_SHOT_DAMAGE = 2.0F;
      public static final double MELEE_REACH = 3.5;
      public static final double PLAYER_REACH_SLACK = 0.5;
      public static final double SCAN = 3.0;
      public static final double COURSE_MARGIN = 0.2;
      public static final double MIN_SPEED = 0.05;
      public static final int HELD_MEMORY = 80;
      public static final int BROKE_CUE_TICKS = 40;
      public static final double DROP_BACK = 0.01;
      public static final double DROP_DOWN = 0.05;

      private Infinity() {
      }
   }

   public static final class Purple {
      public static final String ID = "limitless_purple";
      public static final float COST = 45.0F;
      public static final float CANCEL_REFUND = 0.5F;
      public static final int UNLOCK = 5;
      public static final int HOLD_MAX = 100;
      public static final float DAMAGE = 30.0F;
      public static final int OVER_UNLOCK = 8;
      public static final float OVER_COST = 45.0F;
      public static final float OVER_DAMAGE = 60.0F;
      public static final double OVER_SLOW = -0.6;
      public static final int OVER_SYNC_TICKS = 5;
      public static final double BODY_SLACK = 0.2;
      public static final double ERASE_SLACK = 0.4;
      public static final int LOOT_GRACE = 20;
      public static final int HOLD = 200;
      public static final int RISE = 6;
      public static final int CARVE_EVERY = 2;
      public static final int CARVE_BUDGET = 256;
      public static final int CRUMBS_PER_CARVE = 14;
      public static final int WALL_DUST_POINTS = 8;
      public static final double RIPPLE_REACH = 5.0;
      public static final int LAUNCH_AIR = 14;
      public static final int LAUNCH_GROUND = 10;

      private Purple() {
      }
   }

   public static final class Rct {
      public static final String ID = "limitless_rct";
      public static final int UNLOCK = 7;
      public static final float DRAIN_PER_SECOND = 8.0F;
      public static final float HEAL_PER_SECOND = 2.0F;
      public static final int HEAL_EVERY = 5;
      public static final int HARM_WINDOW = 200;
      public static final int SYNC_TICKS = 5;

      private Rct() {
      }
   }

   public static final class Red {
      public static final String ID = "limitless_red";
      public static final float COST = 15.0F;
      public static final int UNLOCK = 2;
      public static final float DAMAGE = 9.0F;
      public static final double THROW_OUT = 2.0;
      public static final double THROW_UP = 0.7;
      public static final int HIT_STOP = 3;
      public static final float COMBO_MULTIPLIER = 2.0F;
      public static final double COMBO_THROW_OUT = 1.1;
      public static final double COMBO_THROW_UP = 0.35;
      public static final double RADIUS = 3.0;
      public static final double KNOCK_OUT = 0.85;
      public static final double KNOCK_UP = 0.5;
      public static final double PUSH_CAP = 2.2;
      public static final int SLAM_WINDOW = 14;
      public static final float SLAM_DAMAGE = 6.0F;
      public static final double SLAM_REACH = 0.35;
      public static final double FLOOR_SCAN = 6.0;
      public static final double LAUNCH_POINT = 1.5;
      public static final double SELF_PUSH = 1.35;
      public static final double SHIFT_PUSH = 1.8;
      public static final int SHIFT_GATHER = 5;
      public static final double SELF_FULL = 1.5;
      public static final double SELF_LIFT = 0.45;
      public static final double SHAKE_RADIUS = 20.0;
      public static final float SHAKE = 0.22F;
      public static final int SHAKE_HOLD = 2;
      public static final int SHAKE_FADE = 10;
      public static final int GUARD_GRACE = 10;
      public static final int GUARD_MAX = 400;

      private Red() {
      }
   }
}
