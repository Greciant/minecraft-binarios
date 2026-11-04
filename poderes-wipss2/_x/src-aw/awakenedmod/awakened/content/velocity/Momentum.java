package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.Stride;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

public final class Momentum {
   private static final String KEY = "velocity_momentum";
   private static final String GEAR_KEY = "velocity_gear";
   private static final String STILL_KEY = "velocity_still";
   private static final String IDLE_KEY = "velocity_idle";
   private static final String SPRINT_GAP_KEY = "velocity_sprint_gap";
   private static final String CARRIED_X_KEY = "velocity_carried_x";
   private static final String CARRIED_Z_KEY = "velocity_carried_z";
   private static final String EARNED_KEY = "velocity_earned";
   public static final float MAX = 100.0F;
   public static final float[] GEAR_THRESHOLDS = new float[]{20.0F, 40.0F, 60.0F, 80.0F, 95.0F};
   public static final int[] GEAR_UNLOCK_LEVEL = new int[]{1, 2, 4, 6, 8};
   private static final float GEAR_HYSTERESIS = 3.0F;
   private static final double MAX_BONUS = 0.6;
   private static final float GAIN = 0.6F;
   private static final float IDLE_DECAY = 0.22F;
   private static final int BLEED_GRACE_TICKS = 50;
   private static final int BLEED_RAMP_TICKS = 30;
   private static final float WALK_BLEED_FACTOR = 0.5F;
   private static final int SPRINT_GAP_TICKS = 4;
   private static final float MOVING_EPS = 0.01F;
   private static final float SYNC_EPS = 0.05F;
   private static final Map<UUID, Set<String>> BLEED_HOLDS = new ConcurrentHashMap<>();

   private Momentum() {
   }

   public static void holdBleed(ServerPlayer player, String abilityId, boolean hold) {
      if (player != null && abilityId != null) {
         if (hold) {
            BLEED_HOLDS.computeIfAbsent(player.getUUID(), id -> ConcurrentHashMap.newKeySet()).add(abilityId);
         } else {
            Set<String> mine = BLEED_HOLDS.get(player.getUUID());
            if (mine != null && mine.remove(abilityId) && mine.isEmpty()) {
               BLEED_HOLDS.remove(player.getUUID());
            }
         }
      }
   }

   public static boolean bleedHeld(ServerPlayer player) {
      Set<String> mine = player == null ? null : BLEED_HOLDS.get(player.getUUID());
      return mine != null && !mine.isEmpty();
   }

   public static float value(PowerData data) {
      return Math.max(0.0F, Math.min(100.0F, data.var("velocity_momentum", 0.0F)));
   }

   public static float fraction(PowerData data) {
      return value(data) / 100.0F;
   }

   public static int unlockedGear(PowerData data) {
      int level = data.masteryLevel();
      int cap = 0;

      while (cap < 5 && level >= GEAR_UNLOCK_LEVEL[cap]) {
         cap++;
      }

      return cap;
   }

   public static int unlockLevel(int gear) {
      return gear <= 0 ? 0 : GEAR_UNLOCK_LEVEL[Math.min(5, gear) - 1];
   }

   public static int earnedGear(PowerData data) {
      return Math.min(unlockedGear(data), storedEarnedGear(data));
   }

   private static int storedEarnedGear(PowerData data) {
      return Math.max(0, Math.min(5, Math.round(data.var("velocity_earned", 0.0F))));
   }

   private static boolean reconcileEarned(PowerData data) {
      int current = earnedGear(data);
      int cap = unlockedGear(data);
      float v = value(data);
      int next = current;

      while (next < cap && v >= GEAR_THRESHOLDS[next]) {
         next++;
      }

      while (next > 0 && (next > cap || v < GEAR_THRESHOLDS[next - 1] - 3.0F)) {
         next--;
      }

      if (next == current && next == storedEarnedGear(data)) {
         return false;
      } else {
         data.setVar("velocity_earned", (float)next);
         return true;
      }
   }

   static boolean reconcileLock(PowerData data) {
      int cap = unlockedGear(data);
      boolean moved = false;
      if (storedEarnedGear(data) > cap) {
         data.setVar("velocity_earned", (float)cap);
         moved = true;
      }

      if (storedChosenGear(data) > cap) {
         data.setVar("velocity_gear", (float)cap);
         moved = true;
      }

      return moved;
   }

   public static float thresholdFraction(int gear) {
      int g = Math.max(0, Math.min(5, gear));
      return g == 0 ? 0.0F : GEAR_THRESHOLDS[g - 1] / 100.0F;
   }

   public static int chosenGear(PowerData data) {
      return Math.min(unlockedGear(data), storedChosenGear(data));
   }

   private static int storedChosenGear(PowerData data) {
      return Math.max(0, Math.min(5, Math.round(data.var("velocity_gear", 0.0F))));
   }

   public static float bonusFraction(PowerData data) {
      return (float)earnedGear(data) / 5.0F;
   }

   public static void set(PowerData data, float value) {
      data.setVar("velocity_momentum", Math.max(0.0F, Math.min(100.0F, value)));
      reconcileEarned(data);
   }

   public static void reset(PowerData data) {
      data.setVar("velocity_momentum", 0.0F);
      data.setVar("velocity_earned", 0.0F);
      data.setVar("velocity_still", 0.0F);
      data.setVar("velocity_idle", 0.0F);
      data.setVar("velocity_sprint_gap", 4.0F);
   }

   public static int stillTicks(PowerData data) {
      return Math.max(0, Math.round(data.var("velocity_still", 0.0F)));
   }

   public static int walkTicks(PowerData data) {
      return Math.max(0, Math.round(data.var("velocity_idle", 0.0F)));
   }

   static void setChosenGear(PowerData data, int gear) {
      data.setVar("velocity_gear", (float)Math.max(0, Math.min(unlockedGear(data), gear)));
   }

   public static double amplify(ServerPlayer player, double base) {
      return base * (1.0 + (double)bonusFraction(PowerService.data(player)) * 0.6);
   }

   public static float amplify(ServerPlayer player, float base) {
      return (float)amplify(player, (double)base);
   }

   public static float empowered(ServerPlayer player, float base) {
      return amplify(player, MasteryScaling.scaled(base, player));
   }

   public static double empowered(ServerPlayer player, double base) {
      return amplify(player, MasteryScaling.scaled(base, player));
   }

   public static float empoweredDamage(ServerPlayer player, float base, String abilityKey) {
      float bonus = bonusFraction(PowerService.data(player)) * 0.6F;
      return MasteryScaling.scaledDamage(base, player, abilityKey, bonus);
   }

   public static double empoweredDamage(ServerPlayer player, double base, String abilityKey) {
      double bonus = (double)bonusFraction(PowerService.data(player)) * 0.6;
      return MasteryScaling.scaledDamage(base, player, abilityKey, bonus);
   }

   private static float rawTravel(ServerPlayer player) {
      float step = (float)player.getKnownMovement().horizontalDistance();
      return step > strideCap(player) ? -1.0F : step;
   }

   static float strideCap(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      return Stride.cap(OverdriveControl.expectedSprintStride(player, data));
   }

   static float rawStep(ServerPlayer player) {
      return (float)player.getKnownMovement().horizontalDistance();
   }

   private static boolean atRunStride(ServerPlayer player, PowerData data, float step) {
      return Stride.isRun(step, OverdriveControl.expectedSprintStride(player, data));
   }

   public static boolean runTick(ServerPlayer player, PowerData data) {
      if (!OverdriveAbility.isOn(data) || carried(player)) {
         data.setVar("velocity_sprint_gap", 4.0F);
         return false;
      } else if (player.isSprinting()) {
         data.setVar("velocity_sprint_gap", 0.0F);
         return true;
      } else {
         int gap = Math.min(4, Math.max(0, Math.round(data.var("velocity_sprint_gap", 4.0F))) + 1);
         data.setVar("velocity_sprint_gap", (float)gap);
         return gap < 4 && atRunStride(player, data, travelThisTick(player));
      }
   }

   public static float travelThisTick(ServerPlayer player) {
      return Math.max(0.0F, rawTravel(player));
   }

   public static boolean moving(ServerPlayer player) {
      return travelThisTick(player) > 0.01F;
   }

   public static boolean tick(ServerPlayer player, PowerData data, boolean gaining) {
      float step = carried(player) ? carriedTravel(player, data) : rawTravel(player);
      if (!carried(player)) {
         data.removeVar("velocity_carried_x");
         data.removeVar("velocity_carried_z");
      }

      if (step < 0.0F) {
         return false;
      } else {
         boolean held = bleedHeld(player);
         float before = value(data);
         float next;
         if (step > 0.01F) {
            data.setVar("velocity_still", 0.0F);
            if (gaining) {
               data.setVar("velocity_idle", 0.0F);
               next = before + step * 0.6F;
            } else if (atRunStride(player, data, step)) {
               data.setVar("velocity_idle", 0.0F);
               next = before;
            } else {
               int idle = bumpIdle(data);
               next = held ? before : before - idleBleed(idle) * 0.5F;
            }
         } else {
            int still = Math.min(80, Math.round(data.var("velocity_still", 0.0F)) + 1);
            data.setVar("velocity_still", (float)still);
            int idle = bumpIdle(data);
            next = held ? before : before - idleBleed(idle);
         }

         next = Math.max(0.0F, Math.min(100.0F, next));
         data.setVar("velocity_momentum", next);
         boolean gearMoved = reconcileEarned(data);
         return gearMoved || Math.abs(next - before) > 0.05F;
      }
   }

   static boolean carried(ServerPlayer player) {
      return player.isPassenger() || player.isFallFlying() || player.getAbilities().flying;
   }

   private static float carriedTravel(ServerPlayer player, PowerData data) {
      double x = player.getX();
      double z = player.getZ();
      boolean measured = data.hasVar("velocity_carried_x") && data.hasVar("velocity_carried_z");
      float lastX = data.var("velocity_carried_x", 0.0F);
      float lastZ = data.var("velocity_carried_z", 0.0F);
      data.setVar("velocity_carried_x", (float)x);
      data.setVar("velocity_carried_z", (float)z);
      return !measured ? -1.0F : (float)Math.hypot(x - (double)lastX, z - (double)lastZ);
   }

   private static int bumpIdle(PowerData data) {
      int idle = Math.min(80, Math.round(data.var("velocity_idle", 0.0F)) + 1);
      data.setVar("velocity_idle", (float)idle);
      return idle;
   }

   private static float idleBleed(int stillTicks) {
      int past = stillTicks - 50;
      return past <= 0 ? 0.0F : 0.22F * Math.min(1.0F, (float)past / 30.0F);
   }
}
