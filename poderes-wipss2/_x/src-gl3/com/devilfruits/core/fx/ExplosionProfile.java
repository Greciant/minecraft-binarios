package com.devilfruits.core.fx;

public enum ExplosionProfile {
   SMALL(64.0),
   MEDIUM(96.0),
   LARGE(160.0),
   MASSIVE(224.0);

   public final double sendRadius;

   private ExplosionProfile(double sendRadius) {
      this.sendRadius = sendRadius;
   }

   public static ExplosionProfile of(int ordinal) {
      ExplosionProfile[] all = values();
      return all[Math.floorMod(ordinal, all.length)];
   }
}
