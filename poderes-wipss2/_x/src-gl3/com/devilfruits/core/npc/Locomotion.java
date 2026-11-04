package com.devilfruits.core.npc;

import net.minecraft.util.Mth;

public final class Locomotion {
   public static final float MIN_RATE = 0.6F;
   public static final float MAX_RATE = 1.6F;

   public static Locomotion.State state(float speed, Locomotion.Gait previous, Locomotion.Speeds s) {
      float walkIn = s.walk() * 0.18F;
      float walkOut = s.walk() * 0.1F;
      float mid = (float)Math.sqrt((double)(s.walk() * s.run()));

      Locomotion.Gait gait = switch (previous) {
         case IDLE -> speed > walkIn ? (speed > mid * 1.08F ? Locomotion.Gait.RUN : Locomotion.Gait.WALK) : Locomotion.Gait.IDLE;
         case WALK -> speed < walkOut ? Locomotion.Gait.IDLE : (speed > mid * 1.08F ? Locomotion.Gait.RUN : Locomotion.Gait.WALK);
         case RUN -> speed < walkOut ? Locomotion.Gait.IDLE : (speed < mid * 0.92F ? Locomotion.Gait.WALK : Locomotion.Gait.RUN);
      };
      return new Locomotion.State(gait, rate(speed, gait, s));
   }

   public static float rate(float speed, Locomotion.Gait gait, Locomotion.Speeds s) {
      return switch (gait) {
         case IDLE -> 1.0F;
         case WALK -> Mth.clamp(speed / s.walk(), 0.6F, 1.6F);
         case RUN -> Mth.clamp(speed / s.run(), 0.6F, 1.6F);
      };
   }

   private Locomotion() {
   }

   public static enum Gait {
      IDLE,
      WALK,
      RUN;
   }

   public static record Speeds(float walk, float run) {
   }

   public static record State(Locomotion.Gait gait, float rate) {
   }
}
