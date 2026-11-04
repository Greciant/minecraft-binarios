package com.devilfruits.fruits.kilo_kilo.weight;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class WeightCurves {
   public static final double MIN_KG = 1.0;
   public static final double NORMAL_KG = 70.0;
   public static final double MAX_KG = 10000.0;
   private static final double LN_NORMAL = Math.log(70.0);
   private static final double LIGHT_SPAN = Math.log(70.0);
   private static final double HEAVY_SPAN = Math.log(142.85714285714286);
   public static final double FULL_SPAN = Math.log(10000.0);
   public static final double GRAVITY = 0.08;
   public static final double JUMP = 0.42;
   public static final double DRAG = 0.98;
   public static final double STEP = 0.6;
   public static final double SAFE_FALL = 3.0;
   public static final double VANILLA_JUMP_HEIGHT = apex(0.42, 0.08);
   public static final double VANILLA_TERMINAL = terminal(0.08);
   public static final double DEAD_ZONE = 0.003;

   public static double clampKg(double kg) {
      return Double.isNaN(kg) ? 70.0 : Mth.clamp(kg, 1.0, 10000.0);
   }

   public static double scale(double kg) {
      double l = Math.log(clampKg(kg)) - LN_NORMAL;
      return l < 0.0 ? l / LIGHT_SPAN : l / HEAVY_SPAN;
   }

   public static double kgAt(double scale) {
      double s = Mth.clamp(scale, -1.0, 1.0);
      return Math.exp(LN_NORMAL + (s < 0.0 ? s * LIGHT_SPAN : s * HEAVY_SPAN));
   }

   public static double lightness(double kg) {
      return Math.max(0.0, -scale(kg));
   }

   public static double heaviness(double kg) {
      return Math.max(0.0, scale(kg));
   }

   public static double rate(double seconds) {
      return FULL_SPAN / Math.max(1.0, seconds * 20.0);
   }

   public static double rateBetween(double from, double to, double seconds) {
      return Math.max(1.0E-6, Math.abs(Math.log(clampKg(to)) - Math.log(clampKg(from)))) / Math.max(1.0, seconds * 20.0);
   }

   public static double step(double kg, double target, double rate) {
      double a = Math.log(clampKg(kg));
      double b = Math.log(clampKg(target));
      return Math.abs(b - a) <= rate ? clampKg(target) : Math.exp(a + Math.copySign(rate, b - a));
   }

   public static int rampTicks(double from, double to, double rate) {
      return (int)Math.ceil(Math.abs(Math.log(clampKg(to)) - Math.log(clampKg(from))) / rate - 1.0E-9);
   }

   public static double gravity(double kg, WeightCurves.Tuning t) {
      double s = scale(kg);
      if (s < 0.0) {
         double light = t.lightTerminal() * 0.020000000000000018 / 0.98;
         return 0.08 * Math.pow(light / 0.08, -s);
      } else {
         return 0.08 * Math.pow(t.heavyGravity(), s);
      }
   }

   public static double terminal(double gravity) {
      return gravity * 0.98 / 0.020000000000000018;
   }

   public static double fallCap(double kg, WeightCurves.Tuning t) {
      double h = heaviness(kg);
      double x = Math.min(1.0, h * 4.0);
      double smooth = x * x * (3.0 - 2.0 * x);
      return Mth.lerp(smooth, Math.max(VANILLA_TERMINAL, t.maxFallSpeed()), t.maxFallSpeed());
   }

   public static double jumpHeight(double kg, WeightCurves.Tuning t) {
      double s = scale(kg);
      return s < 0.0 ? VANILLA_JUMP_HEIGHT * Math.pow(t.lightJumpBlocks() / VANILLA_JUMP_HEIGHT, -s) : VANILLA_JUMP_HEIGHT * (1.0 - Math.pow(s, 1.5));
   }

   public static double keepFalling(double vy) {
      return Math.abs(vy) < 0.003 ? -0.00315 : vy;
   }

   public static double jumpStrength(double kg, WeightCurves.Tuning t) {
      double h = jumpHeight(kg, t);
      return h <= 1.0E-4 ? 0.0 : takeoffSpeed(h, gravity(kg, t));
   }

   public static double apex(double v, double gravity) {
      double y = 0.0;

      for (int i = 0; i < 4000 && v > 0.0; i++) {
         y += v;
         v = (v - gravity) * 0.98;
      }

      return y;
   }

   public static double takeoffSpeed(double height, double gravity) {
      double lo = 0.0;
      double hi = 8.0;

      for (int i = 0; i < 48; i++) {
         double mid = (lo + hi) * 0.5;
         if (apex(mid, gravity) < height) {
            lo = mid;
         } else {
            hi = mid;
         }
      }

      return (lo + hi) * 0.5;
   }

   public static double speedMultiplier(double kg, WeightCurves.Tuning t) {
      double s = scale(kg);
      return s < 0.0 ? 1.0 + t.lightSpeed() * -s : 1.0 - (1.0 - t.heavySpeed()) * Math.pow(s, 1.2);
   }

   public static double stepHeight(double kg, WeightCurves.Tuning t) {
      double s = scale(kg);
      return s < 0.0 ? 0.6 + (t.lightStep() - 0.6) * -s : 0.6 * (1.0 - s * s);
   }

   public static double knockbackResistance(double kg) {
      double h = heaviness(kg);
      return h <= 0.0 ? 0.0 : Math.pow(h, 0.75);
   }

   public static double knockbackMultiplier(double kg, WeightCurves.Tuning t) {
      return Math.min(t.maxKnockback(), Math.max(1.0, 70.0 / clampKg(kg)));
   }

   public static double safeFallDistance(double kg) {
      return 3.0 + 200.0 * lightness(kg);
   }

   public static double drainPerSecond(double kg, WeightCurves.Tuning t) {
      double s = scale(kg);
      double d = Math.abs(s);
      if (d <= t.drainDeadZone()) {
         return 0.0;
      } else {
         double x = (d - t.drainDeadZone()) / (1.0 - t.drainDeadZone());
         return (s < 0.0 ? t.lightDrain() : t.heavyDrain()) * Math.pow(x, 1.3);
      }
   }

   public static double kineticEnergy(double kg, double blocksPerTick) {
      double v = blocksPerTick * 20.0;
      return 0.5 * kg * v * v;
   }

   public static double speedFor(double kg, double joules) {
      return joules <= 0.0 ? 0.0 : Math.sqrt(2.0 * joules / clampKg(kg)) / 20.0;
   }

   public static double fallSpeed(double distance, double gravity, double cap) {
      double v = 0.0;
      double d = 0.0;

      for (int i = 0; i < 4000 && d < distance; i++) {
         v = Math.min(cap, (v + gravity) * 0.98);
         d += v;
      }

      return v;
   }

   public static double fallDistanceFor(double speed, double gravity, double cap) {
      double v = 0.0;
      double d = 0.0;

      for (int i = 0; i < 4000 && v < speed && v < cap - 1.0E-9; i++) {
         v = Math.min(cap, (v + gravity) * 0.98);
         d += v;
      }

      return d;
   }

   public static double blockCost(double hardness, double blastResistance, double base) {
      return base * (Math.max(0.0, hardness) + 0.1 * Math.min(Math.max(0.0, blastResistance), 20.0) + 0.5);
   }

   public static int smash(double energy, double[] costs, double[] left) {
      int n;
      for (n = 0; n < costs.length && energy >= costs[n]; n++) {
         energy -= costs[n];
      }

      if (left != null && left.length > 0) {
         left[0] = energy;
      }

      return n;
   }

   public static double smashDepth(Vec3 start, Vec3 dir, Vec3 at) {
      return at.subtract(start).dot(dir) + 1.0;
   }

   public static int creakTicks(double seconds, double hardness, double kg, double minKg) {
      double t = seconds * 20.0 * (1.0 + Math.sqrt(Math.max(0.0, hardness)) * 0.5) * Math.sqrt(minKg / Math.max(kg, minKg));
      return Math.max(1, (int)Math.round(t));
   }

   public static double impactPower(double joules) {
      return Math.log10(1.0 + Math.max(0.0, joules) / 50000.0);
   }

   public static double impactRadius(double joules) {
      return 1.5 + 2.2 * impactPower(joules);
   }

   public static double impactDamage(double joules) {
      double p = impactPower(joules);
      return 4.0 * p * p;
   }

   public static double crushDamage(double joules, double cap) {
      double p = impactPower(joules);
      return Math.min(cap, 12.0 * p * p);
   }

   private WeightCurves() {
   }

   public static record Tuning(
      double lightJumpBlocks,
      double lightTerminal,
      double heavyGravity,
      double maxFallSpeed,
      double lightSpeed,
      double heavySpeed,
      double lightStep,
      double maxKnockback,
      double lightDrain,
      double heavyDrain,
      double drainDeadZone
   ) {
      public static final WeightCurves.Tuning DEFAULT = new WeightCurves.Tuning(50.0, 0.1, 6.0, 3.0, 0.2, 0.45, 1.0, 8.0, 3.0, 4.0, 0.08);
   }
}
