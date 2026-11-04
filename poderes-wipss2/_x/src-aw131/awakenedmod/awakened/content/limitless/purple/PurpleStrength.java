package com.awakenedmod.awakened.content.limitless.purple;

import com.awakenedmod.awakened.content.limitless.LimitlessTimings;
import net.minecraft.world.phys.Vec3;

public record PurpleStrength(int tier, double radius, double speed, float damage, double ballScale) {
   public static final PurpleStrength NORMAL = new PurpleStrength(0, 3.0, 0.9, 30.0F, 1.0);
   public static final PurpleStrength OVERCHARGED = new PurpleStrength(1, 5.0, 0.6, 60.0F, 2.0);

   public static PurpleStrength of(int tier) {
      return tier == OVERCHARGED.tier ? OVERCHARGED : NORMAL;
   }

   public boolean overcharged() {
      return this == OVERCHARGED;
   }

   public int gone() {
      return LimitlessTimings.Purple.gone(this.speed);
   }

   public double launchRadius() {
      return 0.42 * this.ballScale * 0.6;
   }

   public int afterLife() {
      return 10 + (int)Math.ceil(this.radius / this.speed) + 72;
   }

   public Vec3 centre(Vec3 merge, Vec3 fwd, double age, int gone) {
      return LimitlessTimings.Purple.centre(merge, fwd, age, gone, this.speed);
   }

   public double radius(double age) {
      return this.radius(age, this.launchRadius());
   }

   public double radius(double age, double startR) {
      return LimitlessTimings.Purple.radius(age, startR, this.radius);
   }
}
