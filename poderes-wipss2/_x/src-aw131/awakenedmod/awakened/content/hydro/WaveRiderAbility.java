package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.LoopSound;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.Terrain;
import com.awakenedmod.awakened.core.power.Ability;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class WaveRiderAbility extends Ability {
   private static final int UNLOCK = 5;
   private static final Lab.Value STAMINA_PER_TICK = Lab.SERVER.def("hydro.surf.stamina_per_tick", 0.3, 0.0, 2.0, 0.01);
   private static final float WATER_PER_TICK = 0.05F;
   private static final Lab.Value SPEED_LAND = Lab.SERVER.def("hydro.surf.speed_land", 0.65, 0.1, 2.5, 0.01);
   private static final Lab.Value SPEED_WATER = Lab.SERVER.def("hydro.surf.speed_water", 1.05, 0.1, 2.5, 0.01);
   private static final Lab.Value RAMP_TICKS = Lab.SERVER.def("hydro.surf.ramp_ticks", 8.0, 1.0, 60.0, 1.0);
   private static final int PUSH_GRACE = 8;
   private static final Lab.Value RIDE_HEIGHT = Lab.SERVER.def("hydro.surf.ride_height", 1.25, 0.0, 3.0, 0.05);
   private static final Lab.Value LIFT_GAIN = Lab.SERVER.def("hydro.surf.lift_gain", 0.4, 0.05, 1.5, 0.01);
   private static final Lab.Value MAX_LIFT = Lab.SERVER.def("hydro.surf.max_lift", 0.6, 0.05, 2.0, 0.01);
   private static final Lab.Value MAX_DROP = Lab.SERVER.def("hydro.surf.max_drop", 0.6, 0.05, 2.0, 0.01);
   private static final int SURFACE_UP = 3;
   private static final int SURFACE_DOWN = 6;
   private static final String SOUND_KEY = "hydro_surf";
   private static final Lab.Value RUSH_VOLUME_MIN = Lab.SERVER.def("hydro.surf.rush_volume_min", 0.3, 0.0, 2.0, 0.05);
   private static final Lab.Value RUSH_VOLUME_MAX = Lab.SERVER.def("hydro.surf.rush_volume_max", 0.8, 0.0, 2.0, 0.05);
   private static final Lab.Value RUSH_PITCH_MIN = Lab.SERVER.def("hydro.surf.rush_pitch_min", 0.75, 0.5, 2.0, 0.05);
   private static final Lab.Value RUSH_PITCH_MAX = Lab.SERVER.def("hydro.surf.rush_pitch_max", 1.3, 0.5, 2.0, 0.05);
   private final Map<UUID, WaveRiderAbility.Ride> riders = this.activeState();

   public WaveRiderAbility() {
      super("wave_rider", 5.0F, 5);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public String costLabel() {
      return Math.round(this.billed(STAMINA_PER_TICK.getF() * 20.0F)) + "/s";
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.riders.containsKey(player.getUUID())) {
         return true;
      } else {
         this.riders.put(player.getUUID(), new WaveRiderAbility.Ride());
         HydroVisual.setSurfing(player, true);
         Hydro.show(player, Hydro.peek(player), player.position());
         Fx.sound(player.serverLevel(), player.position(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.8F, 1.2F);
         return true;
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      WaveRiderAbility.Ride ride = this.riders.get(player.getUUID());
      if (ride != null) {
         if (player.isAlive() && !player.isShiftKeyDown()) {
            float staminaPerTick = this.billed(STAMINA_PER_TICK.getF());
            double speedLand = SPEED_LAND.get();
            double speedWater = SPEED_WATER.get();
            double rampTicks = RAMP_TICKS.get();
            double rideHeight = RIDE_HEIGHT.get();
            double liftGain = LIFT_GAIN.get();
            double maxLift = MAX_LIFT.get();
            double maxDrop = MAX_DROP.get();
            if (!this.resource(player).canAfford(player, staminaPerTick)) {
               HydroFeedback.tired(player);
               this.stop(player);
            } else {
               this.resource(player).sustain(player, staminaPerTick);
               boolean wet = Hydro.takeTick(player, 0.05F).wet();
               ServerLevel level = player.serverLevel();
               Vec3 look = player.getLookAngle();
               Vec3 flat = new Vec3(look.x, 0.0, look.z);
               flat = flat.lengthSqr() > 1.0E-4 ? flat.normalize() : new Vec3(0.0, 0.0, 1.0);
               double surfaceY = Math.max(
                  surface(level, player.getX(), player.getZ(), player.getY()),
                  surface(level, player.getX() + flat.x * 2.0, player.getZ() + flat.z * 2.0, player.getY())
               );
               double targetY = surfaceY + rideHeight;
               double dy = targetY - player.getY();
               double vy = Math.max(-maxDrop, Math.min(maxLift, dy * liftGain));
               if (player.horizontalCollision || wallAhead(level, player, flat)) {
                  vy = maxLift;
               }

               boolean onWater = player.isInWater()
                  || level.getFluidState(player.blockPosition().below()).is(FluidTags.WATER)
                  || level.isRainingAt(player.blockPosition());
               double speed = (onWater ? speedWater : speedLand) * Math.min(1.0, (double)(ride.ticks + 1) / rampTicks);
               Vec3 now = player.getDeltaMovement();
               boolean launched = ride.last != null
                  && now.distanceToSqr(ride.last) > 1.0E-4
                  && (now.y > maxLift + 0.05 || now.horizontalDistance() > speed * 1.3 + 0.05);
               if (launched) {
                  ride.grace = 8;
               }

               ride.last = now;
               if (ride.grace > 0) {
                  ride.grace--;
               } else {
                  player.setDeltaMovement(flat.x * speed, vy, flat.z * speed);
                  player.hurtMarked = true;
                  ride.last = player.getDeltaMovement();
               }

               player.resetFallDistance();
               float rush = (float)Math.min(1.0, player.getDeltaMovement().horizontalDistance() / speedWater);
               WaveRiderFx.wake(level, player.position(), surface(level, player.getX(), player.getZ(), player.getY()), flat, rush, ride.ticks, wet);
               LoopSound.keep(
                  player,
                  "hydro_surf",
                  SoundEvents.WEATHER_RAIN,
                  SoundSource.PLAYERS,
                  (float)Mth.lerp((double)rush, RUSH_VOLUME_MIN.get(), RUSH_VOLUME_MAX.get()),
                  (float)Mth.lerp((double)rush, RUSH_PITCH_MIN.get(), RUSH_PITCH_MAX.get())
               );
               ride.ticks++;
               if (ride.ticks % 5 == 0) {
                  PowerService.sync(player);
               }
            }
         } else {
            this.stop(player);
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.stop(player);
   }

   private void stop(ServerPlayer player) {
      if (this.riders.remove(player.getUUID()) != null) {
         endRide(player);
         HydroFx.splash(player.serverLevel(), player.position(), new Vec3(0.0, 1.0, 0.0), 0.8, HydroVisual.of(player).wet());
         Fx.sound(player.serverLevel(), player.position(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.6F, 0.9F);
         PowerService.sync(player);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.riders.containsKey(player.getUUID())) {
         endRide(player);
      }

      super.clearPlayer(player);
   }

   private static void endRide(ServerPlayer player) {
      HydroVisual.setSurfing(player, false);
      LoopSound.stop(player, "hydro_surf");
   }

   private static boolean wallAhead(ServerLevel level, ServerPlayer player, Vec3 flat) {
      double reach = (double)player.getBbWidth() * 0.5 + 0.35;

      for (double up : new double[]{0.2, 1.1}) {
         BlockPos at = BlockPos.containing(player.getX() + flat.x * reach, player.getY() + up, player.getZ() + flat.z * reach);
         if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private static double surface(ServerLevel level, double x, double z, double riderY) {
      double y = Terrain.floorNear(level, x, z, riderY, 3, 6, true);
      return Double.isNaN(y) ? riderY - 6.0 : y;
   }

   private static final class Ride {
      int ticks;
      int grace;
      Vec3 last;
   }
}
