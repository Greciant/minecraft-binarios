package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import com.awakenedmod.awakened.core.power.PowerResource;
import net.minecraft.server.level.ServerPlayer;

public final class KineticChargeResource implements PowerResource {
   public static final KineticChargeResource INSTANCE = new KineticChargeResource();

   private KineticChargeResource() {
   }

   @Override
   public float current(ServerPlayer player) {
      return KineticBank.value(PowerService.data(player));
   }

   @Override
   public float max(ServerPlayer player) {
      return KineticBank.max(player);
   }

   @Override
   public boolean canAfford(ServerPlayer player, float cost) {
      PowerData data = PowerService.data(player);
      return data.infiniteStamina() || KineticBank.canSpend(data, cost);
   }

   @Override
   public void spend(ServerPlayer player, float cost) {
      if (!PowerService.data(player).infiniteStamina()) {
         KineticBank.spend(player, cost);
      }
   }

   @Override
   public void sustain(ServerPlayer player, float cost) {
      this.spend(player, cost);
   }

   @Override
   public void refund(ServerPlayer player, float amount) {
      if (!PowerService.data(player).infiniteStamina()) {
         KineticBank.deposit(player, amount);
      }
   }

   @Override
   public void refuse(ServerPlayer player) {
      AbilityFeedback.fail(player, "awakened.kinetic_charger.no_charge");
   }

   @Override
   public boolean showsStaminaBar() {
      return false;
   }

   @Override
   public void regenTick(ServerPlayer player, PowerData data, float max) {
   }

   @Override
   public boolean affordable(PowerResource.Fill fill, float cost) {
      return fill.infinite() || fill.resourceFraction() * KineticBank.maxFrom(fill.staminaMax()) >= cost;
   }
}
