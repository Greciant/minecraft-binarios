package com.awakenedmod.awakened.core.progression;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.relic.RelicSystem;
import com.awakenedmod.awakened.core.skill.Stat;
import net.minecraft.server.level.ServerPlayer;

public final class MasteryScaling {
   private static final double PER_LEVEL = 0.03;
   private static final double MAX_BONUS = 0.6;

   private MasteryScaling() {
   }

   public static float massBonus(int masteryLevel) {
      return (float)Math.min(0.6, (double)Math.max(0, masteryLevel) * 0.03);
   }

   public static float multiplier(int masteryLevel) {
      return 1.0F + massBonus(masteryLevel);
   }

   public static float scaled(float base, ServerPlayer player) {
      return base * fullMultiplier(player);
   }

   public static double scaled(double base, ServerPlayer player) {
      return base * (double)fullMultiplier(player);
   }

   public static int scaledTicks(int baseTicks, int maxTicks, ServerPlayer player) {
      float lift = multiplier(PowerService.data(player).masteryLevel()) * (1.0F + abilityPower(player));
      return Math.min(maxTicks, Math.round((float)baseTicks * lift));
   }

   public static float scaledDamage(float base, ServerPlayer player, String abilityKey, float... extraBonusFractions) {
      return base * damagePowerMultiplier(player, extraBonusFractions) * AbilityBalance.damageMultiplier(abilityKey) * BalanceConfig.damageMultiplier();
   }

   public static float scaledDamage(float base, int masteryLevel, float abilityPower, String abilityKey, float... extraBonusFractions) {
      return base
         * damagePowerMultiplier(masteryLevel, abilityPower, extraBonusFractions)
         * AbilityBalance.damageMultiplier(abilityKey)
         * BalanceConfig.damageMultiplier();
   }

   public static float configScaledDamage(float base, String abilityKey) {
      return base * AbilityBalance.damageMultiplier(abilityKey) * BalanceConfig.damageMultiplier();
   }

   public static double scaledDamage(double base, ServerPlayer player, String abilityKey, double... extraBonusFractions) {
      float[] fractions = new float[extraBonusFractions.length];

      for (int i = 0; i < fractions.length; i++) {
         fractions[i] = (float)extraBonusFractions[i];
      }

      return base
         * (double)damagePowerMultiplier(player, fractions)
         * (double)AbilityBalance.damageMultiplier(abilityKey)
         * (double)BalanceConfig.damageMultiplier();
   }

   public static float abilityPower(ServerPlayer player) {
      return AwakenedAttachments.skill(player).bonus(Stat.ABILITY_POWER) + RelicSystem.bonus(player, Stat.ABILITY_POWER);
   }

   public static float progressionBonus(ServerPlayer player) {
      return massBonus(PowerService.data(player).masteryLevel()) + abilityPower(player);
   }

   private static float fullMultiplier(ServerPlayer player) {
      int level = PowerService.data(player).masteryLevel();
      return multiplier(level) * (1.0F + abilityPower(player)) * BalanceConfig.damageMultiplier();
   }

   private static float damagePowerMultiplier(ServerPlayer player, float[] extraBonusFractions) {
      return damagePowerMultiplier(PowerService.data(player).masteryLevel(), abilityPower(player), extraBonusFractions);
   }

   private static float damagePowerMultiplier(int level, float apBonus, float[] extraBonusFractions) {
      float massBonus = massBonus(level);
      if (BalanceConfig.additiveDamageStacking()) {
         float total = massBonus + apBonus;

         for (float extra : extraBonusFractions) {
            total += extra;
         }

         return 1.0F + total;
      } else {
         float multiplier = (1.0F + massBonus) * (1.0F + apBonus);

         for (float extra : extraBonusFractions) {
            multiplier *= 1.0F + extra;
         }

         return multiplier;
      }
   }
}
