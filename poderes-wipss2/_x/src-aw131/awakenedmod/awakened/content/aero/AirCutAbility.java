package com.awakenedmod.awakened.content.aero;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class AirCutAbility extends Ability {
   private static final float COST = 10.0F;
   private static final float BASE_DAMAGE = 7.0F;
   private static final double RANGE = 40.0;
   private static final int PIERCE_MASTERY_LEVEL = 12;

   public AirCutAbility() {
      super("aero_air_cut", 10.0F, 4);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(7.0).build();
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "aero_slash";
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 origin = AimUtil.castOrigin(player);
      Vec3 dir = player.getLookAngle().normalize();
      float damage = MasteryScaling.scaledDamage(7.0F, player, this.balanceKey());
      PowerData data = PowerService.data(player);
      boolean pierce = data.masteryLevel() >= 12;
      AeroCrescentEntity crescent = new AeroCrescentEntity(AwakenedEntities.AERO_CRESCENT.get(), player, level);
      crescent.setPos(origin.x, origin.y, origin.z);
      int lifeTicks = (int)Math.ceil(25.0) + 2;
      crescent.configure(damage, pierce, lifeTicks);
      crescent.shoot(dir.x, dir.y, dir.z, 1.6F, 0.0F);
      level.addFreshEntity(crescent);
      AeroVfx.crescent(level, origin, dir, 40.0, AeroVfx.rollCrescentBank(level.random));
      return true;
   }
}
