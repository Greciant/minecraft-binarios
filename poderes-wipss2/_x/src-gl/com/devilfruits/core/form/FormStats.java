package com.devilfruits.core.form;

public record FormStats(
   int requiredMastery,
   int enterEnergyCost,
   double energyDrainPerSecond,
   int switchCooldownTicks,
   int castTicks,
   double regenMultiplier,
   double boostDrainPerSecond
) {
   public static final FormStats BASE = new FormStats(1, 0, 0.0, 0);

   public FormStats(int requiredMastery, int enterEnergyCost, double energyDrainPerSecond, int switchCooldownTicks) {
      this(requiredMastery, enterEnergyCost, energyDrainPerSecond, switchCooldownTicks, 0, 1.0, 0.0);
   }

   public FormStats withCastTicks(int ticks) {
      return new FormStats(
         this.requiredMastery, this.enterEnergyCost, this.energyDrainPerSecond, this.switchCooldownTicks, ticks, this.regenMultiplier, this.boostDrainPerSecond
      );
   }

   public FormStats withRegenMultiplier(double multiplier) {
      return new FormStats(
         this.requiredMastery, this.enterEnergyCost, this.energyDrainPerSecond, this.switchCooldownTicks, this.castTicks, multiplier, this.boostDrainPerSecond
      );
   }

   public FormStats withBoostDrain(double perSecond) {
      return new FormStats(
         this.requiredMastery, this.enterEnergyCost, this.energyDrainPerSecond, this.switchCooldownTicks, this.castTicks, this.regenMultiplier, perSecond
      );
   }
}
