package com.devilfruits.core.fruit;

public record EnergyRules(int baseEnergy, int energyPerLevel, double regenInCombat, double regenOutOfCombat) {
   public static final EnergyRules DEFAULT = new EnergyRules(100, 10, 2.5, 7.0);
}
