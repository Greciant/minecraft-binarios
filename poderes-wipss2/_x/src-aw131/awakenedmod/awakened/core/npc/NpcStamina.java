package com.awakenedmod.awakened.core.npc;

import com.awakenedmod.awakened.core.stamina.StaminaSystem;

public final class NpcStamina {
   private float stamina = StaminaSystem.baseMax();
   private long lastSpendTime = -4611686018427387904L;

   public float current() {
      return this.stamina;
   }

   public float max() {
      return StaminaSystem.baseMax();
   }

   public float fraction() {
      float max = this.max();
      return max <= 0.0F ? 0.0F : this.stamina / max;
   }

   public boolean canAfford(float rawCost, String balanceKey) {
      return this.stamina >= StaminaSystem.effectiveCost(rawCost, balanceKey);
   }

   public boolean spend(float rawCost, String balanceKey, long gameTime) {
      float cost = StaminaSystem.effectiveCost(rawCost, balanceKey);
      if (this.stamina < cost) {
         return false;
      } else {
         this.stamina -= cost;
         if (cost > 0.0F) {
            this.lastSpendTime = gameTime;
         }

         return true;
      }
   }

   public boolean drain(float rawPerTick, String balanceKey, long gameTime) {
      float cost = StaminaSystem.effectiveCost(rawPerTick, balanceKey);
      if (this.stamina < cost) {
         return false;
      } else {
         this.stamina -= cost;
         this.lastSpendTime = gameTime;
         return true;
      }
   }

   public void regenTick(long gameTime) {
      this.stamina = StaminaSystem.regenStep(this.stamina, this.lastSpendTime, gameTime, this.max(), StaminaSystem.baseRegenPerSecond());
   }

   public void load(float value) {
      this.stamina = Math.max(0.0F, Math.min(this.max(), value));
   }
}
