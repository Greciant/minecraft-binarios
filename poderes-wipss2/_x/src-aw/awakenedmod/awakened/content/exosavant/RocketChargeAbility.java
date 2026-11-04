package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.power.Ability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class RocketChargeAbility extends Ability {
   private static final int UNLOCK = 3;
   private static final double THRUST = 1.7;
   private static final float DASH_COST = 8.0F;

   public RocketChargeAbility() {
      super("rocket_charge", 8.0F, 3);
   }

   @Override
   public boolean requiresFootingWhenRealistic() {
      return false;
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return ExoFlight.isFlying(player) ? 0.0F : 8.0F;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (!ExoLoadout.usable(player, ExoPiece.BOOTS)) {
         return false;
      } else if (ExoFlight.isFlying(player)) {
         ExoFlight.toggleJet(player);
         return true;
      } else {
         Vec3 dir = player.getLookAngle();
         Vec3 velocity = new Vec3(dir.x * 1.7, Math.max(0.35, dir.y * 1.7 + 0.2), dir.z * 1.7);
         player.setDeltaMovement(velocity);
         player.hurtMarked = true;
         ExoFx.dashKick(player.serverLevel(), player.position(), dir);
         player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.7F);
         return true;
      }
   }
}
