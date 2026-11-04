package com.awakenedmod.awakened.content.shadow;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class DevourAbility extends Ability {
   private static final float COST = 80.0F;
   private static final int UNLOCK = 10;
   private static final double AIM_RANGE = 28.0;
   private static final double RADIUS_DIM = 3.5;
   private static final double RADIUS_DARK = 7.0;
   private static final int TICKS_DIM = 80;
   private static final int TICKS_DARK = 160;
   private static final float DPS_DARK = 3.5F;

   public DevourAbility() {
      super("devour", 80.0F, 10);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().boostsRadius().build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 spot = AimUtil.aimedPosition(player, 28.0);
      if (spot == null) {
         return false;
      } else {
         float dark = ShadowDark.darkness(level, spot);
         float apScale = 1.0F + MasteryScaling.abilityPower(player);
         double radius = Mth.lerp((double)dark, 3.5, 7.0) * (double)apScale;
         int ticks = (int)Mth.lerp(dark, 80.0F, 160.0F);
         Vec3 center = spot.add(0.0, 0.2, 0.0);
         ShadowZones.open(player, level, center, radius, ticks, 3.5F, true, true, true, this.balanceKey());
         return true;
      }
   }
}
