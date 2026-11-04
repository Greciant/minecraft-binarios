package com.awakenedmod.awakened.core.stamina;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.PowerResource;
import net.minecraft.server.level.ServerPlayer;

public final class StaminaResource implements PowerResource {
   public static final StaminaResource INSTANCE = new StaminaResource();

   private StaminaResource() {
   }

   @Override
   public float current(ServerPlayer player) {
      return PowerService.data(player).stamina();
   }

   @Override
   public float max(ServerPlayer player) {
      return StaminaSystem.max(player);
   }

   @Override
   public boolean canAfford(ServerPlayer player, float cost) {
      return StaminaSystem.canAfford(PowerService.data(player), cost);
   }

   @Override
   public void spend(ServerPlayer player, float cost) {
      PowerData data = PowerService.data(player);
      if (!data.infiniteStamina()) {
         data.setStamina(data.stamina() - cost);
      }

      if (cost > 0.0F) {
         data.setLastSpendTime(player.serverLevel().getGameTime());
      }
   }

   @Override
   public void sustain(ServerPlayer player, float cost) {
      StaminaSystem.drainSustained(PowerService.data(player), player.serverLevel().getGameTime(), cost);
   }

   @Override
   public void refund(ServerPlayer player, float amount) {
      if (!(amount <= 0.0F)) {
         PowerData data = PowerService.data(player);
         float max = StaminaSystem.max(player);
         if (!(data.stamina() >= max)) {
            data.setStamina(Math.min(max, data.stamina() + amount));
            PowerService.sync(player);
         }
      }
   }

   @Override
   public void refuse(ServerPlayer player) {
      PowerService.feedback(player, 0);
   }

   @Override
   public boolean showsStaminaBar() {
      return true;
   }

   @Override
   public void regenTick(ServerPlayer player, PowerData data, float max) {
      StaminaSystem.regenTick(data, player.serverLevel().getGameTime(), max, StaminaSystem.regenPerSecond(player));
   }

   @Override
   public boolean affordable(PowerResource.Fill fill, float cost) {
      return fill.infinite() || fill.stamina() >= cost;
   }
}
