package com.devilfruits.fruits.kage_kage;

import net.minecraft.util.Mth;

public final class KageTiming {
   public static final int SWARM_GATHER = 10;
   public static final float SWARM_STAGGER = 1.2F;
   public static final float BAT_SPEED = 1.05F;
   public static final int SPEAR_POOL = 6;
   public static final int SPEAR_PIERCE_MARKER = 3;
   public static final int SPEAR_DISSOLVE = 12;
   public static final int TRAP_RISE = 4;
   public static final int TRAP_OPEN = 24;
   public static final int TRAP_PULL = 31;
   public static final int TRAP_PULL_TICKS = 6;
   public static final int TRAP_SHUT = 40;
   public static final int TRAP_SPIT_AFTER = 7;
   public static final int TRAP_SINK_AFTER = 6;
   public static final int HAND_RISE = 3;
   public static final int HAND_DRAW = 27;
   public static final int HAND_SLAP = 36;
   public static final int HAND_IMPACT = 40;
   public static final float HAND_REACH = 0.66071427F;

   public static int batRun(double distance) {
      return Mth.clamp(Mth.ceil(distance / 1.0), 6, 30);
   }

   public static int trapPull(int run) {
      return run + 31;
   }

   public static int trapShut(int run) {
      return run + 40;
   }

   public static int trapSpit(int run, int hold) {
      return trapShut(run) + hold + 7;
   }

   public static int handImpact(int run) {
      return run + 40;
   }

   public static int batArrival(int i, double distance) {
      return 10 + Math.round((float)i * 1.2F) + batFlight(distance);
   }

   public static int batFlight(double distance) {
      return Mth.clamp(Mth.ceil(distance * 1.3 / 1.05F), 6, 40);
   }

   public static int spearRun(double distance) {
      return Mth.clamp(Mth.ceil(distance / 0.9), 6, 34);
   }

   public static int spearPierce(int run) {
      return run + 6 + 3;
   }

   private KageTiming() {
   }
}
