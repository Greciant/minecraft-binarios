package com.awakenedmod.awakened.content.pocket;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.TeleportGuard;
import com.awakenedmod.awakened.core.TeleportUtil;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class RiftBlinkAbility extends Ability {
   private static final float COST = 11.0F;
   private static final int UNLOCK = 3;
   private static final double MAX_RANGE = 14.0;
   private static final double STEP = 0.5;
   private static final double MIN_DISTANCE = 0.5;
   private static final double SURFACE_GAP = 0.55;
   private static final int IFRAME_TICKS = 10;
   private static final float LOW_HEALTH = 6.0F;

   public RiftBlinkAbility() {
      super("pocket_blink", 11.0F, 3);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().range(14.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      double range = MasteryScaling.scaled(14.0, player);
      Vec3 feet = player.position();
      Vec3 look = player.getLookAngle();
      HitResult hit = player.pick(range, 1.0F, false);
      Vec3 destination = null;
      if (hit instanceof BlockHitResult block && hit.getType() == Type.BLOCK) {
         Vec3 cand = Vec3.atBottomCenterOf(block.getBlockPos().relative(block.getDirection()));
         if (TeleportUtil.fits(player, cand) && !TeleportGuard.crosses(player, feet, cand)) {
            destination = cand;
         }
      }

      if (destination == null) {
         double aimDist = hit.getType() == Type.MISS ? range : Math.max(0.0, player.getEyePosition().distanceTo(hit.getLocation()) - 0.55);

         for (double d = aimDist; d >= 0.5; d -= 0.5) {
            Vec3 candidate = feet.add(look.scale(d));
            if (TeleportUtil.fits(player, candidate) && !TeleportGuard.crosses(player, feet, candidate)) {
               destination = candidate;
               break;
            }
         }
      }

      if (destination == null) {
         return this.fail(player, "awakened.pocket.blink_blocked", new Object[0]);
      } else {
         Vec3 originFeet = player.position();
         Vec3 dir = originToDir(originFeet, destination);
         RiftFx.blinkMaw(level, originFeet, dir);
         Fx.sound(level, originFeet, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 1.1F);
         player.teleportTo(destination.x, destination.y, destination.z);
         player.setDeltaMovement(0.0, 0.0, 0.0);
         player.hurtMarked = true;
         player.resetFallDistance();
         player.invulnerableTime = Math.max(player.invulnerableTime, 10);
         streak(level, originFeet.add(0.0, 1.0, 0.0), destination.add(0.0, 1.0, 0.0));
         RiftFx.blinkMaw(level, destination, dir);
         Fx.sound(level, destination, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 1.3F);
         if (player.getHealth() <= 6.0F) {
            MasterySystem.award(player, MasteryReason.SURVIVAL);
         }

         return true;
      }
   }

   private static Vec3 originToDir(Vec3 from, Vec3 to) {
      Vec3 d = new Vec3(to.x - from.x, 0.0, to.z - from.z);
      return d.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : d.normalize();
   }

   private static void streak(ServerLevel level, Vec3 from, Vec3 to) {
      int steps = (int)Math.max(6.0, from.distanceTo(to) * 3.0);

      for (int i = 0; i <= steps; i++) {
         Vec3 p = from.lerp(to, (double)i / (double)steps);
         Fx.burst(level, ParticleTypes.PORTAL, p, 2, 0.05, 0.0);
      }
   }
}
