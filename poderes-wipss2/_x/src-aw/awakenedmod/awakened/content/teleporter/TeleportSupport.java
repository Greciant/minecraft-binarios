package com.awakenedmod.awakened.content.teleporter;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.TeleportUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class TeleportSupport {
   private TeleportSupport() {
   }

   public static void puff(ServerLevel level, Vec3 pos) {
      TeleFx.rift(level, pos);
      Fx.sound(level, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.3F);
   }

   public static boolean fits(Entity entity, Vec3 feet) {
      return TeleportUtil.fits(entity, feet);
   }

   public static Vec3 findLanding(ServerPlayer player, Vec3 origin, Vec3 dir, double maxRange, double step, double minDist) {
      return TeleportUtil.findLanding(player, origin, dir, maxRange, step, minDist);
   }

   public static Vec3 findLandingToward(ServerPlayer player, Vec3 eye, Vec3 aimPoint, double minDist, double step, int maxDrop) {
      return TeleportUtil.findLandingToward(player, eye, aimPoint, minDist, step, maxDrop);
   }

   public static LivingEntity aimedEnemy(ServerPlayer player, double range) {
      return AimUtil.aimedLivingEntity(player, range, e -> Harm.allowed(player, e));
   }

   public static Vec3 horizontalDir(Vec3 from, Vec3 to) {
      return AimUtil.horizontalDir(from, to);
   }
}
