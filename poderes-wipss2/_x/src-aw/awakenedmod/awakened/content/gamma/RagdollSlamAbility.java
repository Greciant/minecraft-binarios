package com.awakenedmod.awakened.content.gamma;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class RagdollSlamAbility extends Ability {
   private static final float COST = 28.0F;
   private static final double RANGE = 6.0;
   private static final double AIM_CONE_DEG = 25.0;

   public RagdollSlamAbility() {
      super("gamma_slam", 28.0F, 10);
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (SlamControl.isSlamming(player)) {
         return this.fail(player, "ability.awakened.gamma_slam.busy", new Object[0]);
      } else {
         LivingEntity victim = AimUtil.aimedLivingSoft(player, 6.0, 25.0, e -> !SlamControl.isVictim(e) && Harm.allowed(player, e));
         if (victim == null) {
            return this.fail(player, "ability.awakened.gamma_slam.no_target", new Object[0]);
         } else if (!Harm.controls(player, victim)) {
            return this.fail(player, "awakened.feedback.pvp_control_off", new Object[0]);
         } else {
            SlamControl.begin(player, victim, Rage.rage(PowerService.data(player)));
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      SlamControl.tick(player);
   }

   @Override
   public boolean isActive(ServerPlayer player) {
      return SlamControl.isSlamming(player) || super.isActive(player);
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      SlamControl.clearPlayer(player);
      super.clearPlayer(player);
   }
}
