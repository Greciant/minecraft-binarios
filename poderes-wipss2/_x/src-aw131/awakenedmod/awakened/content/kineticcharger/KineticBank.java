package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.Stride;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.progression.BalanceConfig;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class KineticBank {
   private static final String KEY = "kinetic_charge";
   public static final String RELEASE_KEY = "kinetic_release";
   private static final int SCRATCH_AIRBORNE = 0;
   private static final int SCRATCH_FALL = 1;
   public static final float MAX = 100.0F;
   private static final int TICKS_PER_SECOND = 20;
   private static final Lab.Value SPRINT_PER_SECOND = Lab.SERVER.def("kinetic.bank.sprint", 4.0, 0.0, 15.0, 0.25);
   private static final float WALK_FACTOR = 0.5F;
   private static final Lab.Value LANDING_PER_BLOCK = Lab.SERVER.def("kinetic.bank.landing", 2.5, 0.0, 10.0, 0.1);
   private static final float LANDING_MIN_FALL = 3.0F;
   private static final float LANDING_MAX = 10.0F;
   private static final Lab.Value HIT_DEPOSIT = Lab.SERVER.def("kinetic.bank.hit", 4.0, 0.0, 20.0, 0.5);
   public static final float POUR_PER_SECOND = 20.0F;
   public static final float POUR_CAP = 60.0F;
   private static final float POUR_PER_TICK = 1.0F;
   private static final float RELEASE_BONUS_PER_LEVEL = 0.05F;
   private static final int MAX_RELEASE_LEVEL = 10;
   private static final float MOVING_EPS = 0.01F;
   private static final float SYNC_EPS = 0.05F;
   private static final int SYNC_INTERVAL = 4;
   private static final double SPRINT_STRIDE_PER_SPEED = 2.8;
   private static final Set<UUID> DIRTY = ConcurrentHashMap.newKeySet();
   private static final Map<UUID, float[]> LANDING = new ConcurrentHashMap<>();
   private static final float CAP_EPS = 1.0E-4F;

   private KineticBank() {
   }

   public static float value(PowerData data) {
      return Math.max(0.0F, data.var("kinetic_charge", 0.0F));
   }

   public static float max(Player player) {
      return maxFrom(StaminaSystem.max(player));
   }

   public static float maxFrom(float staminaMax) {
      float base = StaminaSystem.baseMax();
      return 100.0F * (base > 0.0F ? Math.max(1.0F, staminaMax / base) : 1.0F);
   }

   public static float fraction(ServerPlayer player, PowerData data) {
      float max = max(player);
      return max > 0.0F ? Math.min(1.0F, value(data) / max) : 0.0F;
   }

   public static boolean canSpend(PowerData data, float amount) {
      return value(data) >= amount;
   }

   public static int releaseLevel(PowerData data) {
      return Math.max(0, Math.min(10, Math.round(data.var("kinetic_release", 0.0F))));
   }

   public static void setReleaseLevel(PowerData data, int level) {
      data.setVar("kinetic_release", (float)Math.max(0, Math.min(10, level)));
   }

   public static float releaseBonus(PowerData data) {
      return (float)releaseLevel(data) * 0.05F;
   }

   public static void set(ServerPlayer player, PowerData data, float value) {
      data.setVar("kinetic_charge", Math.max(0.0F, Math.min(max(player), value)));
   }

   public static void bank(ServerPlayer player, PowerData data, float amount) {
      bankCapped(player, data, amount, Float.MAX_VALUE);
   }

   public static void bankCapped(ServerPlayer player, PowerData data, float amount, float cap) {
      if (player != null && !(amount <= 0.0F)) {
         set(player, data, value(data) + Math.min(cap, amount * fillRate(player)));
         spent(player);
      }
   }

   public static void deposit(ServerPlayer player, float amount) {
      if (player != null && !(amount <= 0.0F)) {
         PowerData data = PowerService.data(player);
         set(player, data, value(data) + amount);
         spent(player);
      }
   }

   private static float fillRate(Player player) {
      float base = (float)BalanceConfig.regenPerSecond();
      return base > 0.0F ? Math.max(1.0F, StaminaSystem.regenPerSecond(player) / base) : 1.0F;
   }

   public static float spend(ServerPlayer player, float amount) {
      if (player != null && !(amount <= 0.0F)) {
         PowerData data = PowerService.data(player);
         float take = Math.min(amount, value(data));
         if (take <= 0.0F) {
            return 0.0F;
         } else {
            set(player, data, value(data) - take);
            spent(player);
            return take;
         }
      } else {
         return 0.0F;
      }
   }

   public static void spent(ServerPlayer player) {
      if (player != null) {
         DIRTY.add(player.getUUID());
      }
   }

   public static void forget(ServerPlayer player) {
      if (player != null) {
         DIRTY.remove(player.getUUID());
         LANDING.remove(player.getUUID());
      }
   }

   public static float pourTick(ServerPlayer player, float poured) {
      return pourTick(player, poured, 60.0F);
   }

   public static float pourTick(ServerPlayer player, float poured, float cap) {
      float limit = Math.min(cap, 60.0F);
      float take = Math.min(1.0F, limit - poured);
      if (take <= 0.0F) {
         return poured;
      } else {
         float next = poured + spend(player, take);
         if (next >= limit - 1.0E-4F && poured < limit - 1.0E-4F) {
            KineticFx.pourCapped(player);
         }

         return next;
      }
   }

   public static float pourFraction(float poured) {
      return Math.min(1.0F, poured / 60.0F);
   }

   public static float markOf(ServerPlayer player, float amount) {
      float max = max(player);
      return max > 0.0F ? Math.max(0.0F, amount) / max : 0.0F;
   }

   public static float empowered(ServerPlayer player, float base) {
      return (float)empowered(player, (double)base);
   }

   public static double empowered(ServerPlayer player, double base) {
      return MasteryScaling.scaled(base, player) * (1.0 + (double)releaseBonus(PowerService.data(player)));
   }

   public static double boosted(ServerPlayer player, double baseline, double bonus) {
      double growth = 1.0 + (double)MasteryScaling.progressionBonus(player) + (double)releaseBonus(PowerService.data(player));
      return baseline + bonus * growth;
   }

   public static float empoweredDamage(ServerPlayer player, float base, String abilityKey) {
      return MasteryScaling.scaledDamage(base, player, abilityKey, releaseBonus(PowerService.data(player)));
   }

   public static double empoweredDamage(ServerPlayer player, double base, String abilityKey) {
      return MasteryScaling.scaledDamage(base, player, abilityKey, (double)releaseBonus(PowerService.data(player)));
   }

   public static void tick(ServerPlayer player, PowerData data) {
      float before = value(data);
      travelDeposit(player, data);
      landingDeposit(player, data);
      if (Math.abs(value(data) - before) > 0.05F) {
         DIRTY.add(player.getUUID());
      }

      if (player.tickCount % 4 == 0 && DIRTY.remove(player.getUUID())) {
         PowerService.sync(player);
      }
   }

   private static void travelDeposit(ServerPlayer player, PowerData data) {
      if (!player.isPassenger()) {
         double expected = expectedSprintStride(player);
         float step = (float)player.getKnownMovement().horizontalDistance();
         if (!(step <= 0.01F) && !(step > Stride.cap(expected))) {
            float perTick = (float)(SPRINT_PER_SECOND.get() / 20.0);
            bank(player, data, Stride.isRun(step, expected) ? perTick : perTick * 0.5F);
         }
      }
   }

   private static void landingDeposit(ServerPlayer player, PowerData data) {
      float[] scratch = LANDING.computeIfAbsent(player.getUUID(), id -> new float[2]);
      boolean onGround = player.onGround();
      boolean wasAirborne = scratch[0] > 0.5F;
      float previousFall = scratch[1];
      if (onGround && wasAirborne && previousFall >= 3.0F) {
         bankCapped(player, data, (float)((double)previousFall * LANDING_PER_BLOCK.get()), 10.0F);
         KineticFx.landing(player, previousFall);
      }

      scratch[0] = onGround ? 0.0F : 1.0F;
      scratch[1] = (float)Math.min((double)player.fallDistance, 100.0);
   }

   public static void hitTaken(ServerPlayer player, DamageSource source, float amount) {
      if (source != null
         && !(amount <= 0.0F)
         && Foes.threat(source, player)
         && !source.is(DamageTypeTags.NO_KNOCKBACK)
         && !(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1.0)) {
         bank(player, PowerService.data(player), (float)HIT_DEPOSIT.get());
         KineticFx.bankFlash(player);
      }
   }

   private static double expectedSprintStride(ServerPlayer player) {
      return player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.8;
   }
}
