package com.devilfruits.fruits.gomu_gomu.strike;

public enum StrikeKind {
   PISTOL(0, 2, 5, 2.5, 4.0, 2, 5, 0.0F, 0.42),
   BAZOOKA(1, 2, 7, 2.2, 4.0, 3, 6, 0.0F, 0.55),
   RIFLE(2, 2, 7, 2.5, 4.0, 2, 6, 3.5F, 0.32),
   ROCKET(3, 1, 5, 1.6, 3.5, 3, 6, 0.0F, 0.45);

   public final int code;
   public final int feint;
   public final int coil;
   public final double back;
   public final double speed;
   public final int hold;
   public final int retract;
   public final float twist;
   public final double radius;

   private StrikeKind(int code, int feint, int coil, double back, double speed, int hold, int retract, float twist, double radius) {
      this.code = code;
      this.feint = feint;
      this.coil = coil;
      this.back = back;
      this.speed = speed;
      this.hold = hold;
      this.retract = retract;
      this.twist = twist;
      this.radius = radius;
   }

   public int release() {
      return this.feint + this.coil;
   }

   public boolean twoArms() {
      return this == BAZOOKA || this == ROCKET;
   }

   public static StrikeKind of(int code) {
      for (StrikeKind k : values()) {
         if (k.code == code) {
            return k;
         }
      }

      return PISTOL;
   }
}
