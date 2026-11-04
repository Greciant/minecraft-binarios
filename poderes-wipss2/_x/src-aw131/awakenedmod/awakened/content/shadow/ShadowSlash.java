package com.awakenedmod.awakened.content.shadow;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

public final class ShadowSlash {
   private static final int[] TIER_UNLOCK = new int[]{0, 0, 4, 8, 12};
   private static final float[] TIER_DAMAGE = new float[]{0.0F, 4.0F, 6.5F, 9.0F, 13.0F};
   private static final double[] TIER_RADIUS = new double[]{0.0, 0.5, 0.8, 1.2, 1.8};
   private static final int[] TIER_PIERCE = new int[]{0, 1, 2, 3, 5};
   private static final float VELOCITY = 2.3F;
   private static final double SPAWN_AHEAD = 0.3;

   private ShadowSlash() {
   }

   public static int masteryCap(PowerData data) {
      int level = data.masteryLevel();
      int cap = 1;

      for (int tier = 2; tier <= 4; tier++) {
         if (level >= TIER_UNLOCK[tier]) {
            cap = tier;
         }
      }

      return cap;
   }

   public static int tier(ServerPlayer player) {
      int fromDark = 1 + Math.round(ShadowDark.darkness(player) * 3.0F);
      return Math.min(fromDark, masteryCap(PowerService.data(player)));
   }

   public static void shoot(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      int tier = tier(player);
      Vec3 origin = AimUtil.castOrigin(player);
      Vec3 dir = AimUtil.throwDirection(player, origin, 64.0);
      ShadowBolt slash = new ShadowBolt(AwakenedEntities.SHADOW_BOLT.get(), player, level);
      Vec3 start = origin.add(dir.scale(0.3));
      slash.setPos(start.x, start.y, start.z);
      slash.configure(MasteryScaling.scaledDamage(TIER_DAMAGE[tier], player, "ShadowBladeAbility"), 0.0F, 0.0F, TIER_RADIUS[tier]);
      slash.setPierce(TIER_PIERCE[tier]);
      slash.setCrescent(true);
      slash.setMaxLife(40);
      slash.setNoGravity(true);
      slash.shoot(dir.x, dir.y, dir.z, 2.3F, 0.0F);
      level.addFreshEntity(slash);
      ShadowFx.slashLaunch(level, origin);
      player.swing(InteractionHand.MAIN_HAND);
      PowerService.playAnimation(player, "shadow_slash");
   }
}
