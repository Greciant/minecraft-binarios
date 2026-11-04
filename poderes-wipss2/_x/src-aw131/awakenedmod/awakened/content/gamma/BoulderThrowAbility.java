package com.awakenedmod.awakened.content.gamma;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class BoulderThrowAbility extends Ability {
   private static final float COST = 15.0F;

   public BoulderThrowAbility() {
      super("gamma_boulder", 15.0F, 7);
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(16.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (BoulderControl.isThrowing(player)) {
         return this.fail(player, "ability.awakened.gamma_boulder.busy", new Object[0]);
      } else if (!player.onGround()) {
         return this.fail(player, "ability.awakened.gamma_boulder.airborne", new Object[0]);
      } else {
         float rage = Rage.rage(PowerService.data(player));
         float yawRad = player.getYRot() * (float) (Math.PI / 180.0);
         Vec3 ripPoint = player.position().add(-Math.sin((double)yawRad) * 1.1, 0.05, Math.cos((double)yawRad) * 1.1);
         BoulderControl.begin(player, ripPoint, rage);
         return true;
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      BoulderControl.release(player);
   }

   @Override
   public void serverTick(ServerPlayer player) {
      BoulderControl.tick(player);
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "gamma_boulder_lift";
   }

   @Override
   public boolean isActive(ServerPlayer player) {
      return BoulderControl.isThrowing(player) || super.isActive(player);
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      BoulderControl.clearPlayer(player);
      super.clearPlayer(player);
   }
}
