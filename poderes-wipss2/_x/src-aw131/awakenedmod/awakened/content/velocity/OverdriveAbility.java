package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import com.awakenedmod.awakened.core.power.PassiveToggleService;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import net.minecraft.server.level.ServerPlayer;

public final class OverdriveAbility extends Ability {
   public static final PassiveToggle TOGGLE = new PassiveToggle(
      "velocity_overdrive", "awakened.screen.setting_velocity_overdrive", "awakened.screen.setting_velocity_overdrive_desc", true
   );

   public OverdriveAbility() {
      super("overdrive", 0.0F, 0);
   }

   public static boolean isOn(PowerData data) {
      return TOGGLE.enabled(data);
   }

   public static void set(ServerPlayer player, boolean on) {
      if (player != null && isOn(PowerService.data(player)) != on) {
         PassiveToggleService.toggle(player, TOGGLE.key());
      }
   }

   @Override
   public boolean activate(ServerPlayer player) {
      PassiveToggleService.toggle(player, TOGGLE.key());
      PowerData data = PowerService.data(player);
      OverdriveControl.tick(player, data);
      if (isOn(data)) {
         VelocityFx.gearBurst(player.serverLevel(), player, Momentum.chosenGear(data));
      }

      VelocityVisualState.sync(player);
      return true;
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public String costLabel() {
      return "free";
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boosts("speed_bonus").build();
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      OverdriveControl.clear(player);
      Gearbox.clearDemands(player);
      VelocityPower.forgetMomentumSync(player);
      VelocityVisualState.clear(player);
      EchoCadence.forget(player);
      super.clearPlayer(player);
   }
}
