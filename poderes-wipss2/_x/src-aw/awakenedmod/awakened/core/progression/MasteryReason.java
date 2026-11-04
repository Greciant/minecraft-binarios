package com.awakenedmod.awakened.core.progression;

public enum MasteryReason {
   HIT(4.0),
   KILL(20.0),
   SURVIVAL(12.0),
   USE(1.5);

   private final double basePoints;

   private MasteryReason(double basePoints) {
      this.basePoints = basePoints;
   }

   public double basePoints() {
      return this.basePoints;
   }
}
