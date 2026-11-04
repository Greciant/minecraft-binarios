package com.awakenedmod.awakened.content.aero;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TempFlight;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.AeroFlightSyncPayload;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GaleControl {
   private static final String K_ARMED = "aero_armed";
   private static final String K_FLYING = "aero_flying";
   private static final String K_FORCED_GLIDE = "aero_forced_glide";
   private static final String K_GRACE = "aero_fly_grace";
   private static final String K_FLIGHT_DIST = "aero_flight_dist";
   public static final int FLIGHT_MASTERY = 12;
   public static final int GLIDE_MASTERY = 5;
   private static final double GLIDE_FALL_CAP = 0.02;
   private static final double GLIDE_SINK_EASE = 0.04;
   private static final double SURF_CRUISE = 0.8;
   private static final double SURF_MIN_SPEED = 0.45;
   private static final double SURF_MAX_SPEED = 1.6;
   private static final double SURF_DIVE_GAIN = 0.06;
   private static final double SURF_DIVE_SINK = 0.6;
   private static final double SURF_BLEED = 0.012;
   private static final double SURF_TURN_DEG_SLOW = 13.0;
   private static final double SURF_TURN_DEG_FAST = 9.0;
   private static final double SURF_CLIMB_RATE = 0.45;
   private static final double SURF_CLIMB_COST = 0.05;
   private static final double SURF_CLIMB_EASE = 0.16;
   private static final float GLIDE_DRAIN_PER_TICK = 0.025F;
   public static final float FLIGHT_DRAIN_PER_TICK = 0.1F;
   public static final double DASH_SPEED = 1.7;
   public static final float DASH_COST = 10.0F;
   private static final int SURF_GROUNDED_NET_TICKS = 10;
   private static final String K_SURF_GROUNDED = "aero_surf_grounded";
   private static final float FLIGHT_FLY_SPEED = 0.05F;
   private static final float DEFAULT_FLY_SPEED = 0.05F;
   private static final float LAUNCH_GRACE_TICKS = 6.0F;
   private static final double FLIGHT_MASTERY_STEP = 24.0;
   private static final int SYNC_INTERVAL = 5;
   private static final Set<UUID> GLIDING = ConcurrentHashMap.newKeySet();
   private static final Map<UUID, Vec3> LAST_POS = new ConcurrentHashMap<>();
   private static final double MOVE_DELTA_MAX = 8.0;

   private GaleControl() {
   }

   private static Vec3 sampleMove(ServerPlayer player) {
      Vec3 pos = player.position();
      Vec3 prev = LAST_POS.put(player.getUUID(), pos);
      if (prev == null) {
         return Vec3.ZERO;
      } else {
         Vec3 move = pos.subtract(prev);
         return move.lengthSqr() > 64.0 ? Vec3.ZERO : move;
      }
   }

   public static boolean isArmed(PowerData data) {
      return data.var("aero_armed", 0.0F) != 0.0F;
   }

   public static boolean isFlying(PowerData data) {
      return data.var("aero_flying", 0.0F) != 0.0F;
   }

   public static boolean isFlying(ServerPlayer player) {
      return isFlying(PowerService.data(player));
   }

   public static boolean isGliding(ServerPlayer player) {
      return GLIDING.contains(player.getUUID());
   }

   public static void toggleArmed(ServerPlayer player) {
      if (player != null && player.isAlive() && "aerokinetic".equals(PowerService.data(player).powerId())) {
         PowerData data = PowerService.data(player);
         if (data.masteryLevel() < 12) {
            AbilityFeedback.fail(player, "awakened.aero.flight_locked", 12);
         } else {
            boolean on = !isArmed(data);
            data.setVar("aero_armed", on ? 1.0F : 0.0F);
            player.displayClientMessage(Component.translatable(on ? "awakened.aero.flight_armed" : "awakened.aero.flight_disarmed"), true);
            pushSync(player);
         }
      }
   }

   public static void toggleFlight(ServerPlayer player) {
      if (player != null && player.isAlive() && "aerokinetic".equals(PowerService.data(player).powerId())) {
         if (isFlying(player)) {
            disengage(player);
         } else {
            engage(player);
         }
      }
   }

   private static void engage(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      if (!isFlying(data) && isArmed(data) && data.masteryLevel() >= 12) {
         if (!StaminaSystem.canAfford(data, billed(0.1F))) {
            player.displayClientMessage(Component.translatable("awakened.feedback.no_stamina"), true);
         } else {
            endGlide(player, data);
            TempFlight.grant(player);
            player.getAbilities().setFlyingSpeed(0.05F);
            player.onUpdateAbilities();
            data.setVar("aero_flying", 1.0F);
            data.setVar("aero_fly_grace", 6.0F);
            data.setVar("aero_flight_dist", 0.0F);
            player.resetFallDistance();
            ServerLevel level = player.serverLevel();
            Vec3 feet = player.position();
            AeroVfx.flightBurst(level, feet.add(0.0, 1.0, 0.0));
            Fx.sound(level, feet, (SoundEvent)SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.1F, 0.9F);
            pushSync(player);
            PowerService.sync(player);
         }
      }
   }

   public static void disengage(ServerPlayer player) {
      if (player != null) {
         PowerData data = PowerService.data(player);
         if (isFlying(data)) {
            teardownFlight(player, data);
            Fx.sound(player.serverLevel(), player.position(), (SoundEvent)SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.5F, 1.1F);
            pushSync(player);
            PowerService.sync(player);
         }
      }
   }

   private static void teardownFlight(ServerPlayer player, PowerData data) {
      TempFlight.revoke(player, false);
      player.getAbilities().setFlyingSpeed(0.05F);
      player.onUpdateAbilities();
      data.setVar("aero_flying", 0.0F);
      data.setVar("aero_fly_grace", 0.0F);
      data.setVar("aero_flight_dist", 0.0F);
      LAST_POS.remove(player.getUUID());
   }

   public static void tick(ServerPlayer player, PowerData data) {
      if (isFlying(data) && player.isAlive()) {
         if (!player.getAbilities().mayfly) {
            resetVars(data);
            endGlide(player, data);
         } else {
            long gameTime = player.serverLevel().getGameTime();
            float grace = data.var("aero_fly_grace", 0.0F);
            if (grace > 0.0F) {
               data.setVar("aero_fly_grace", grace - 1.0F);
            }

            float remaining = StaminaSystem.drainSustained(data, gameTime, billed(0.1F));
            if (remaining <= 0.0F && !data.infiniteStamina()) {
               beginForcedGlide(player, data);
            } else {
               player.resetFallDistance();
               Vec3 move = sampleMove(player);
               accumulateFlightDistance(player, data, move.horizontalDistance());
               AeroVfx.flightTrail(player.serverLevel(), player.position(), move, gameTime);
               if (grace <= 0.0F && player.onGround()) {
                  disengage(player);
               } else {
                  if (player.tickCount % 5 == 0) {
                     PowerService.sync(player);
                  }
               }
            }
         }
      }
   }

   private static void accumulateFlightDistance(ServerPlayer player, PowerData data, double dist) {
      float acc = data.var("aero_flight_dist", 0.0F) + (float)dist;
      if ((double)acc >= 24.0) {
         acc -= 24.0F;
         MasterySystem.awardUse(player);
      }

      data.setVar("aero_flight_dist", acc);
   }

   private static void beginForcedGlide(ServerPlayer player, PowerData data) {
      teardownFlight(player, data);
      data.setVar("aero_forced_glide", 1.0F);
      beginGlide(player, data);
      player.displayClientMessage(Component.translatable("awakened.feedback.no_stamina"), true);
      pushSync(player);
      PowerService.sync(player);
   }

   public static void tickForcedGlide(ServerPlayer player, PowerData data) {
      if (data.var("aero_forced_glide", 0.0F) != 0.0F) {
         glideStep(player);
         if (player.onGround() || player.isInWater() || player.isPassenger()) {
            data.setVar("aero_forced_glide", 0.0F);
            endGlide(player, data);
         }
      }
   }

   public static boolean isForcedGliding(PowerData data) {
      return data.var("aero_forced_glide", 0.0F) != 0.0F;
   }

   public static Vec3 surfVelocity(Vec3 v, Vec3 look, float speedMult) {
      double minSpeed = 0.45 * (double)speedMult;
      double cruise = 0.8 * (double)speedMult;
      double maxSpeed = 1.6 * (double)speedMult;
      Vec3 flatLook = new Vec3(look.x, 0.0, look.z);
      flatLook = flatLook.lengthSqr() > 1.0E-4 ? flatLook.normalize() : new Vec3(0.0, 0.0, 1.0);
      Vec3 curFlat = new Vec3(v.x, 0.0, v.z);
      double speed = Math.max(curFlat.length(), minSpeed);
      double nose = Math.max(0.0, -look.y);
      if (nose > 0.05) {
         speed += 0.06 * nose;
      } else if (speed > cruise) {
         speed -= 0.012;
      }

      double climb = 0.0;
      if (look.y > 0.15 && speed > cruise) {
         double lift = Math.min(1.0, (speed - cruise) / (maxSpeed - cruise));
         climb = 0.45 * look.y * lift;
         speed -= 0.05 * look.y;
      }

      speed = Mth.clamp(speed, minSpeed, maxSpeed);
      double frac = (speed - minSpeed) / (maxSpeed - minSpeed);
      double maxTurn = Math.toRadians(13.0 + -4.0 * frac);
      double targetYaw = Math.atan2(flatLook.z, flatLook.x);
      double curYaw = curFlat.lengthSqr() > 1.0E-6 ? Math.atan2(curFlat.z, curFlat.x) : targetYaw;
      double d = Math.atan2(Math.sin(targetYaw - curYaw), Math.cos(targetYaw - curYaw));
      double newYaw = curYaw + Mth.clamp(d, -maxTurn, maxTurn);
      Vec3 steered = new Vec3(Math.cos(newYaw) * speed, 0.0, Math.sin(newYaw) * speed);
      double vy = climb > 0.0 ? Math.min(v.y + 0.16, climb) : Math.max(v.y - (0.04 + nose * 0.03), -(0.02 + nose * 0.6));
      return new Vec3(steered.x, vy, steered.z);
   }

   public static void glideStep(ServerPlayer player) {
      player.resetFallDistance();
   }

   public static void handleSurf(ServerPlayer player, int action) {
      if (player != null && player.isAlive() && "aerokinetic".equals(PowerService.data(player).powerId())) {
         PowerData data = PowerService.data(player);
         switch (action) {
            case 0:
               startSurf(player, data);
               break;
            case 1:
               endSurfIntent(player, data);
               break;
            case 2:
               dash(player, data);
         }
      }
   }

   private static void startSurf(ServerPlayer player, PowerData data) {
      if (data.masteryLevel() >= 5 && !isFlying(data)) {
         data.setVar("aero_surf_grounded", 0.0F);
         beginGlide(player, data);
      }
   }

   private static void endSurfIntent(ServerPlayer player, PowerData data) {
      if (!isForcedGliding(data)) {
         endGlide(player, data);
      }
   }

   private static void dash(ServerPlayer player, PowerData data) {
      if (data.masteryLevel() >= 5) {
         if (!data.infiniteStamina()) {
            float cost = dashCost();
            if (!StaminaSystem.canAfford(data, cost)) {
               AbilityFeedback.fail(player, "awakened.feedback.no_stamina");
               return;
            }

            StaminaSystem.drainSustained(data, player.serverLevel().getGameTime(), cost);
         }

         player.resetFallDistance();
         AeroVfx.flightBurst(player.serverLevel(), player.position().add(0.0, 1.0, 0.0));
         AeroVfx.leapTrail(player);
         PowerService.sync(player);
      }
   }

   public static void tickSurf(ServerPlayer player, PowerData data) {
      if (isGliding(player) && !isForcedGliding(data)) {
         player.resetFallDistance();
         if (!data.infiniteStamina()) {
            float drain = billed(0.025F);
            if (data.stamina() < drain) {
               endGlide(player, data);
               PowerService.sync(player);
               return;
            }

            StaminaSystem.drainSustained(data, player.serverLevel().getGameTime(), drain);
         }

         if (player.onGround()) {
            float grounded = data.var("aero_surf_grounded", 0.0F) + 1.0F;
            data.setVar("aero_surf_grounded", grounded);
            if (grounded >= 10.0F) {
               endGlide(player, data);
            }
         } else {
            data.setVar("aero_surf_grounded", 0.0F);
         }
      }
   }

   public static void beginGlide(ServerPlayer player, PowerData data) {
      if (GLIDING.add(player.getUUID())) {
         AeroVisualState.sync(player);
         PowerService.playAnimation(player, "aero_glide");
         AeroVfx.glideCatch(player.serverLevel(), player.position());
         pushSync(player);
      }
   }

   public static void endGlide(ServerPlayer player, PowerData data) {
      if (GLIDING.remove(player.getUUID())) {
         LAST_POS.remove(player.getUUID());
         data.setVar("aero_forced_glide", 0.0F);
         data.setVar("aero_surf_grounded", 0.0F);
         AeroVisualState.clear(player);
         PowerService.stopAnimation(player);
         pushSync(player);
      }
   }

   public static float glideDrainPerTick() {
      return 0.025F;
   }

   public static float dashCost() {
      return billed(10.0F);
   }

   private static float billed(float rawCost) {
      return StaminaSystem.effectiveCost(rawCost, GaleWindsAbility.class.getSimpleName());
   }

   public static void pushSync(ServerPlayer player) {
      if (player != null) {
         PowerData data = PowerService.data(player);
         PacketDistributor.sendToPlayer(
            player, new AeroFlightSyncPayload(isArmed(data), isFlying(data), isGliding(player), isForcedGliding(data)), new CustomPacketPayload[0]
         );
      }
   }

   private static void resetVars(PowerData data) {
      data.setVar("aero_flying", 0.0F);
      data.setVar("aero_fly_grace", 0.0F);
      data.setVar("aero_flight_dist", 0.0F);
      data.setVar("aero_forced_glide", 0.0F);
   }

   public static void clear(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      if (isFlying(data)) {
         teardownFlight(player, data);
      }

      endGlide(player, data);
      data.setVar("aero_armed", 0.0F);
      resetVars(data);
      GLIDING.remove(player.getUUID());
      LAST_POS.remove(player.getUUID());
      AeroVisualState.clear(player);
   }
}
