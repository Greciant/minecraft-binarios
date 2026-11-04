package com.awakenedmod.awakened.content.aero;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.RealisticMovement;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class GaleWindsAbility extends Ability {
   private static final float LEAP_COST = 10.0F;
   private static final double LEAP_SPEED = 1.8;
   private static final double LEAP_UP = 0.62;
   private static final double LEAP_MASTERY_DISTANCE = 5.0;
   private static final int LEAP_CHECK_TICKS = 12;
   private final Map<UUID, GaleWindsAbility.LeapTrack> leapChecks = this.playerState();

   public GaleWindsAbility() {
      super("aero_gale_winds", 10.0F, 2);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boosts("glide_speed").build();
   }

   @Override
   public boolean clientDriven() {
      return true;
   }

   @Override
   public boolean requiresFootingWhenRealistic() {
      return false;
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      return GaleControl.isFlying(player) ? this.burstDash(player) : this.leap(player);
   }

   private boolean leap(ServerPlayer player) {
      if (RealisticMovement.blocksAirLaunch(player)) {
         return this.fail(player, "awakened.feedback.no_footing", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         Vec3 feet = player.position();
         Vec3 impulse = player.getLookAngle().scale(1.8);
         if (impulse.y < 0.62) {
            impulse = new Vec3(impulse.x, 0.62, impulse.z);
         }

         player.setDeltaMovement(impulse);
         player.hurtMarked = true;
         player.resetFallDistance();
         AeroVfx.leapRing(level, feet);
         AeroVfx.leapTrail(player);
         this.leapChecks.put(player.getUUID(), new GaleWindsAbility.LeapTrack(feet, level.getGameTime() + 12L));
         return true;
      }
   }

   private boolean burstDash(ServerPlayer player) {
      Vec3 look = player.getLookAngle();
      Vec3 v = player.getDeltaMovement();
      player.setDeltaMovement(v.add(look.scale(1.7)));
      player.hurtMarked = true;
      player.resetFallDistance();
      AeroVfx.flightBurst(player.serverLevel(), player.position().add(0.0, 1.0, 0.0));
      AeroVfx.leapTrail(player);
      return true;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      GaleControl.tickSurf(player, data);
      GaleControl.tickForcedGlide(player, data);
      GaleWindsAbility.LeapTrack track = this.leapChecks.get(player.getUUID());
      if (track != null && player.serverLevel().getGameTime() >= track.checkAtTick()) {
         this.leapChecks.remove(player.getUUID());
         if (player.position().distanceTo(track.origin()) >= 5.0) {
            MasterySystem.awardUse(player);
         }
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      GaleControl.clear(player);
      super.clearPlayer(player);
   }

   private static record LeapTrack(Vec3 origin, long checkAtTick) {
   }
}
