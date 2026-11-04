package com.awakenedmod.awakened.content.teleporter;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class BlinkAbility extends Ability {
   private static final float COST = 12.0F;
   private static final double MAX_RANGE = 12.0;
   private static final double STEP = 0.5;
   private static final double MIN_DISTANCE = 1.5;
   private static final int MAX_DROP = 6;
   private static final int IFRAME_TICKS = 10;
   private static final float LOW_HEALTH = 6.0F;

   public BlinkAbility() {
      super("blink", 12.0F, 0);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().range(12.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      double range = MasteryScaling.scaled(12.0, player);
      Vec3 eye = player.getEyePosition();
      Vec3 aimPoint = player.pick(range, 1.0F, false).getLocation();
      Vec3 destination = TeleportSupport.findLandingToward(player, eye, aimPoint, 1.5, 0.5, 6);
      if (destination == null) {
         return this.fail(player, "awakened.teleporter.no_landing", new Object[0]);
      } else {
         PowerService.playAnimation(player, "blink");
         Vec3 fromCenter = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
         TeleFx.ghost(level, player);
         TeleportSupport.puff(level, fromCenter);
         player.teleportTo(destination.x, destination.y, destination.z);
         Vec3 velocity = player.getDeltaMovement();
         player.setDeltaMovement(velocity.x, Math.max(0.0, velocity.y), velocity.z);
         player.resetFallDistance();
         player.hurtMarked = true;
         player.invulnerableTime = Math.max(player.invulnerableTime, 10);
         Vec3 toCenter = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
         TeleportSupport.puff(level, toCenter);
         TeleFx.trail(level, fromCenter, toCenter);
         if (player.getHealth() <= 6.0F) {
            MasterySystem.award(player, MasteryReason.SURVIVAL);
         }

         return true;
      }
   }
}
