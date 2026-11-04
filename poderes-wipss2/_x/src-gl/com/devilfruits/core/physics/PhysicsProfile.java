package com.devilfruits.core.physics;

import com.devilfruits.core.ability.AbilityStats;

public record PhysicsProfile(PhysicsProfile.Impact impact, double forceScale, PhysicsProfile.Reaction reaction, double blockDamage, boolean auto) {
   public static final PhysicsProfile NONE = new PhysicsProfile(PhysicsProfile.Impact.NONE, 0.0, PhysicsProfile.Reaction.NONE, 0.0, false);

   public double strength(double damage, double knockback) {
      return 40.0 * (Math.max(0.0, damage) + 6.0 * Math.max(0.0, knockback)) * this.forceScale;
   }

   public static PhysicsProfile derive(AbilityStats stats) {
      boolean strikes = stats.damage() > 0.0 || stats.knockback() > 0.0;
      boolean breaks = stats.blockDamage() != null && stats.blockDamage();
      return new PhysicsProfile(
         strikes ? PhysicsProfile.Impact.DIRECTIONAL : PhysicsProfile.Impact.NONE, 1.0, PhysicsProfile.Reaction.NONE, breaks ? 1.0 : 0.0, strikes || breaks
      );
   }

   public static PhysicsProfile.Builder builder(AbilityStats stats) {
      return new PhysicsProfile.Builder(derive(stats));
   }

   public static final class Builder {
      private PhysicsProfile.Impact impact;
      private double forceScale;
      private PhysicsProfile.Reaction reaction;
      private double blockDamage;
      private boolean auto;

      private Builder(PhysicsProfile base) {
         this.impact = base.impact;
         this.forceScale = base.forceScale;
         this.reaction = base.reaction;
         this.blockDamage = base.blockDamage;
         this.auto = base.auto;
      }

      public PhysicsProfile.Builder impact(PhysicsProfile.Impact impact) {
         this.impact = impact;
         return this;
      }

      public PhysicsProfile.Builder force(double scale) {
         this.forceScale = scale;
         return this;
      }

      public PhysicsProfile.Builder reaction(PhysicsProfile.Reaction reaction) {
         this.reaction = reaction;
         return this;
      }

      public PhysicsProfile.Builder blockDamage(double factor) {
         this.blockDamage = factor;
         return this;
      }

      public PhysicsProfile.Builder manual() {
         this.auto = false;
         return this;
      }

      public PhysicsProfile build() {
         return new PhysicsProfile(this.impact, this.forceScale, this.reaction, this.blockDamage, this.auto);
      }
   }

   public static enum Impact {
      NONE,
      DIRECTIONAL,
      RADIAL,
      TANGENTIAL,
      SUSTAINED;
   }

   public static enum Reaction {
      NONE,
      JUMP,
      DASH,
      LANDING;
   }
}
