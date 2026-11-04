package com.awakenedmod.awakened.core.stamina;

import com.awakenedmod.awakened.core.PowerSuppression;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.progression.AbilityBalance;
import com.awakenedmod.awakened.core.progression.BalanceConfig;
import com.awakenedmod.awakened.core.relic.RelicSystem;
import com.awakenedmod.awakened.core.skill.Stat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class StaminaSystem {
   private StaminaSystem() {
   }

   public static float baseMax() {
      return (float)BalanceConfig.staminaMax();
   }

   public static float baseRegenPerSecond() {
      return (float)BalanceConfig.regenPerSecond();
   }

   public static long regenDelayTicks() {
      return (long)(BalanceConfig.regenDelaySeconds() * 20.0);
   }

   public static float effectiveCost(float rawCost, String balanceKey) {
      return rawCost * BalanceConfig.staminaMultiplier() * AbilityBalance.staminaMultiplier(balanceKey);
   }

   public static float regenStep(float stamina, long lastSpendTime, long gameTime, float max, float regenPerSecond) {
      return !(stamina >= max) && gameTime - lastSpendTime >= regenDelayTicks() ? Math.min(max, stamina + regenPerSecond / 20.0F) : stamina;
   }

   public static float max(Player player) {
      return baseMax() + AwakenedAttachments.skill(player).bonus(Stat.MAX_STAMINA) + RelicSystem.bonus(player, Stat.MAX_STAMINA) + powerBonus(player, true);
   }

   public static float regenPerSecond(Player player) {
      return baseRegenPerSecond()
         + AwakenedAttachments.skill(player).bonus(Stat.STAMINA_REGEN)
         + RelicSystem.bonus(player, Stat.STAMINA_REGEN)
         + powerBonus(player, false);
   }

   private static float powerBonus(Player player, boolean max) {
      Power power = PowerRegistry.get(AwakenedAttachments.get(player).powerId());
      if (power != null && !PowerSuppression.isSuppressed(player)) {
         return max ? power.bonusMaxStamina(player) : power.bonusStaminaRegen(player);
      } else {
         return 0.0F;
      }
   }

   public static float powerMaxBonus(Player player) {
      return powerBonus(player, true);
   }

   public static float locked(ServerPlayer player, PowerData data, Power power) {
      return power != null && !data.infiniteStamina() && !PowerSuppression.isSuppressed(player) ? Math.max(0.0F, power.lockedStamina(player, data)) : 0.0F;
   }

   public static boolean canAfford(PowerData data, float cost) {
      return data.infiniteStamina() || data.stamina() >= cost;
   }

   public static float drainSustained(PowerData data, long gameTime, float amount) {
      if (data.infiniteStamina()) {
         return data.stamina();
      } else {
         float remaining = Math.max(0.0F, data.stamina() - amount);
         data.setStamina(remaining);
         data.setLastSpendTime(gameTime);
         return remaining;
      }
   }

   public static void suppressRegen(PowerData data, long gameTime) {
      data.setLastSpendTime(gameTime);
   }

   public static boolean regenTick(PowerData data, long gameTime, float max, float regenPerSecond) {
      if (data.infiniteStamina()) {
         if (data.stamina() < max) {
            data.setStamina(max);
            return true;
         } else {
            return false;
         }
      } else {
         float next = regenStep(data.stamina(), data.lastSpendTime(), gameTime, max, regenPerSecond);
         if (next == data.stamina()) {
            return false;
         } else {
            data.setStamina(next);
            return true;
         }
      }
   }
}
