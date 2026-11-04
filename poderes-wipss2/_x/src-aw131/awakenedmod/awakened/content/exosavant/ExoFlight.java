package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.MoveKeys;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TempFlight;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.ExoFlightPrefSyncPayload;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import net.minecraft.core.particles.ParticleTypes;
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

public final class ExoFlight {
   private static final String K_FLYING = "exo_flying";
   private static final String K_JET = "exo_jet";
   private static final String K_SPUTTER = "exo_sputter";
   private static final String K_GRACE = "exo_fly_grace";
   private static final String K_CMD = "exo_cmd_speed";
   private static final String K_SPEED = "exo_jet_speed";
   private static final String K_HEAD_X = "exo_head_x";
   private static final String K_HEAD_Y = "exo_head_y";
   private static final String K_HEAD_Z = "exo_head_z";
   private static final String K_BOOST = "exo_boost";
   private static final String K_BOOST_CD = "exo_boost_cd";
   private static final String K_BOOM_ARMED = "exo_boom_armed";
   private static final String K_BRAKE = "exo_brake";
   private static final String K_PREF_JET = "exo_pref_jet";
   public static final int FLIGHT_UNLOCK = 8;
   private static final float HOVER_FLY_SPEED = 0.04F;
   private static final float DEFAULT_FLY_SPEED = 0.05F;
   private static final int BOB_PERIOD_TICKS = 50;
   private static final double BOB_MAX_VY = 0.012;
   private static final double BOB_IDLE_SPEED = 0.05;
   private static final double LAUNCH_IMPULSE = 0.9;
   private static final float LAUNCH_GRACE_TICKS = 6.0F;
   public static final double CRUISE_MAX = 1.7;
   public static final double AFTERBURN_SPEED = 2.6;
   private static final double THRUST_ACCEL = 0.05;
   private static final double COAST_DRAG = 0.03;
   private static final double BRAKE_DECEL = 0.08;
   private static final double HARD_BRAKE = 0.16;
   private static final double BRAKE_MIN_SPEED = 0.4;
   private static final float SOFT_BRAKE_LEVEL = 0.55F;
   private static final double ACCEL_STEP = 0.05;
   private static final double BOOST_ACCEL_STEP = 0.12;
   private static final double DECEL_STEP = 0.1;
   private static final double BRAKE_STEP = 0.16;
   private static final double TURN_RATE = 0.28;
   private static final double JUMP_ASCEND = 0.35;
   private static final double BOOM_ON = 2.0;
   private static final double BOOM_OFF = 1.6;
   private static final float BOOST_DURATION_TICKS = 60.0F;
   public static final float BOOST_RECHARGE_TICKS = 100.0F;
   public static final float HOVER_FUEL_PER_TICK = 0.12F;
   public static final float JET_FUEL_PER_TICK = 0.5F;
   private static final float CRUISE_FUEL_FLOOR = 0.12F;
   private static final float AFTERBURN_FUEL_PER_TICK = 1.0F;
   private static final float SPUTTER_TICKS = 25.0F;
   private static final double SPUTTER_DESCENT = 0.18;
   private static final int SYNC_INTERVAL = 5;

   private ExoFlight() {
   }

   public static boolean isFlying(PowerData data) {
      return data.var("exo_flying", 0.0F) != 0.0F;
   }

   public static boolean isFlying(ServerPlayer player) {
      return isFlying(PowerService.data(player));
   }

   public static boolean isJet(PowerData data) {
      return data.var("exo_jet", 0.0F) != 0.0F;
   }

   public static boolean isJet(ServerPlayer player) {
      return isJet(PowerService.data(player));
   }

   public static boolean isAfterburner(PowerData data) {
      return data.var("exo_boost", 0.0F) > 0.0F;
   }

   public static boolean isSputtering(PowerData data) {
      return data.var("exo_sputter", 0.0F) > 0.0F;
   }

   public static float flownSpeed(PowerData data) {
      return data.var("exo_jet_speed", 0.0F);
   }

   public static float brakeLevel(PowerData data) {
      return data.var("exo_brake", 0.0F);
   }

   public static float boostRechargeRemaining(PowerData data) {
      return Mth.clamp(data.var("exo_boost_cd", 0.0F) / 100.0F, 0.0F, 1.0F);
   }

   public static boolean unlocked(PowerData data) {
      return data.masteryLevel() >= 8;
   }

   public static boolean defaultJet(PowerData data) {
      return data.var("exo_pref_jet", 0.0F) != 0.0F;
   }

   public static boolean defaultJet(ServerPlayer player) {
      return defaultJet(PowerService.data(player));
   }

   public static void toggleFlight(ServerPlayer player) {
      if (player != null && "exosavant".equals(PowerService.data(player).powerId())) {
         if (isFlying(player)) {
            disengage(player);
         } else {
            engage(player);
         }
      }
   }

   public static void engage(ServerPlayer player) {
      if (player != null && player.isAlive()) {
         PowerData data = PowerService.data(player);
         if (!isFlying(data)) {
            if (ExoSuit.isSuited(player)) {
               if (!ExoSuit.isAssembling(player)) {
                  if (!unlocked(data)) {
                     AbilityFeedback.fail(player, "awakened.exosavant.flight_locked", 8);
                  } else if (!ExoLoadout.hasPiece(player, ExoPiece.BOOTS) || !ExoLoadout.hasPiece(player, ExoPiece.LEGGINGS)) {
                     AbilityFeedback.fail(player, "awakened.exosavant.flight_no_legs");
                  } else if (!StaminaSystem.canAfford(data, billed(0.12F))) {
                     player.displayClientMessage(Component.translatable("awakened.exosavant.power_low"), true);
                  } else {
                     TempFlight.grant(player);
                     player.getAbilities().setFlyingSpeed(0.04F);
                     player.getAbilities().flying = true;
                     player.onUpdateAbilities();
                     data.setVar("exo_flying", 1.0F);
                     data.setVar("exo_jet", 0.0F);
                     data.setVar("exo_sputter", 0.0F);
                     data.setVar("exo_fly_grace", 6.0F);
                     Vec3 v = player.getDeltaMovement();
                     player.setDeltaMovement(v.x, 0.9, v.z);
                     player.hurtMarked = true;
                     player.resetFallDistance();
                     ServerLevel level = player.serverLevel();
                     Vec3 feet = player.position();
                     Vfx.dustDome(level, feet, 1.6, 8);
                     Vfx.ring(level, feet.add(0.0, 0.1, 0.0), 1.8, 10475775, 10);
                     Fx.burst(level, ExoFx.dust(), feet, 14, 0.4, 0.05);
                     Fx.burst(level, ParticleTypes.LARGE_SMOKE, feet, 10, 0.3, 0.03);
                     Fx.sound(level, feet, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.6F);
                     Fx.sound(level, feet, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.4F, 1.4F);
                     if (defaultJet(data)) {
                        setJet(player, true);
                     }

                     PowerService.sync(player);
                  }
               }
            }
         }
      }
   }

   public static void disengage(ServerPlayer player) {
      if (player != null) {
         PowerData data = PowerService.data(player);
         if (isFlying(data)) {
            teardown(player, data);
            ServerLevel level = player.serverLevel();
            Fx.sound(level, player.position(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5F, 1.2F);
            Fx.burst(level, ParticleTypes.SMOKE, player.position().add(0.0, 0.4, 0.0), 6, 0.3, 0.02);
            PowerService.sync(player);
         }
      }
   }

   public static void toggleJet(ServerPlayer player) {
      if (isFlying(player)) {
         setJet(player, !isJet(player));
      }
   }

   public static void setJet(ServerPlayer player, boolean on) {
      PowerData data = PowerService.data(player);
      if (isFlying(data)) {
         data.setVar("exo_jet", on ? 1.0F : 0.0F);
         if (on) {
            double speed = horizontalSpeed(player);
            data.setVar("exo_jet_speed", (float)speed);
            data.setVar("exo_cmd_speed", (float)Mth.clamp(speed, 0.0, 1.7));
            seedHeading(player, data);
            data.setVar("exo_boost", 0.0F);
            data.setVar("exo_boom_armed", 1.0F);
            PowerService.playAnimation(player, "jet_fly");
         } else {
            if (data.var("exo_boost", 0.0F) > 0.0F) {
               data.setVar("exo_boost_cd", 100.0F);
            }

            data.setVar("exo_boost", 0.0F);
            data.setVar("exo_jet_speed", 0.0F);
            data.setVar("exo_boom_armed", 0.0F);
            stopJetPose(player);
         }

         ServerLevel level = player.serverLevel();
         Vec3 at = player.position().add(0.0, 0.9, 0.0);
         if (on) {
            Fx.sound(level, at, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
            Fx.sound(level, at, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.3F, 1.7F);
            Vfx.sphere(level, at, 0.6, 10475775, 5);
         } else {
            Fx.sound(level, at, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5F, 1.3F);
         }

         PowerService.sync(player);
      }
   }

   public static void toggleDefaultFlight(ServerPlayer player) {
      if (player != null && "exosavant".equals(PowerService.data(player).powerId())) {
         PowerData data = PowerService.data(player);
         boolean on = !defaultJet(data);
         data.setVar("exo_pref_jet", on ? 1.0F : 0.0F);
         pushPref(player);
         player.displayClientMessage(Component.translatable(on ? "awakened.exosavant.flight_default_jet" : "awakened.exosavant.flight_default_hover"), true);
      }
   }

   public static void pushPref(ServerPlayer player) {
      if (player != null) {
         PacketDistributor.sendToPlayer(player, new ExoFlightPrefSyncPayload(defaultJet(player)), new CustomPacketPayload[0]);
      }
   }

   public static void igniteAfterburner(ServerPlayer player) {
      if (player != null) {
         PowerData data = PowerService.data(player);
         if (isFlying(data) && isJet(data)) {
            if (!(data.var("exo_boost", 0.0F) > 0.0F) && !(data.var("exo_boost_cd", 0.0F) > 0.0F)) {
               data.setVar("exo_boost", 60.0F);
               ServerLevel level = player.serverLevel();
               Vec3 at = player.position().add(0.0, 0.9, 0.0);
               Fx.sound(level, at, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.3F);
               Fx.sound(level, at, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.35F, 1.8F);
               PowerService.sync(player);
            }
         }
      }
   }

   public static void tick(ServerPlayer player, PowerData data) {
      if (isFlying(data) && player.isAlive()) {
         if (!ExoSuit.isSuited(player)) {
            disengage(player);
         } else if (!player.getAbilities().mayfly) {
            stopJetPose(player);
            resetVars(data);
         } else {
            long gameTime = player.serverLevel().getGameTime();
            float grace = data.var("exo_fly_grace", 0.0F);
            if (grace > 0.0F) {
               data.setVar("exo_fly_grace", grace - 1.0F);
            }

            if (data.var("exo_sputter", 0.0F) > 0.0F) {
               float s = data.var("exo_sputter", 0.0F) - 1.0F;
               data.setVar("exo_sputter", s);
               Vec3 v = player.getDeltaMovement();
               double sinkY = Math.max(-0.18, v.y - 0.06);
               player.setDeltaMovement(v.x * 0.6, sinkY, v.z * 0.6);
               player.hurtMarked = true;
               sputterFx(player);
               if (s <= 0.0F || grace <= 0.0F && player.onGround()) {
                  disengage(player);
               }
            } else {
               boolean jet = isJet(data);
               boolean brake = false;
               boolean afterburn = false;
               double target = 0.0;
               float burn;
               if (jet) {
                  float fwd = forwardAxis(player);
                  double cmd = updateCommandedSpeed(data, fwd, brake);
                  afterburn = updateBoost(data);
                  if (afterburn && (brake || fwd < 0.0F)) {
                     cancelAfterburner(data);
                     afterburn = false;
                  }

                  if (afterburn) {
                     target = 2.6;
                     burn = 1.0F;
                  } else {
                     target = cmd;
                     burn = cruiseBurn(cmd);
                  }

                  updateBrakeLevel(data, brake, fwd, afterburn);
               } else {
                  burn = 0.12F;
                  data.setVar("exo_brake", 0.0F);
               }

               float remaining = StaminaSystem.drainSustained(data, gameTime, billed(burn));
               if (remaining <= 0.0F && !data.infiniteStamina()) {
                  beginSputter(player, data);
               } else {
                  ServerLevel level = player.serverLevel();
                  if (jet) {
                     double speed = easeSpeed(data, target, brake, afterburn);
                     Vec3 dir = easeDirection(player, data);
                     Vec3 vel = dir.scale(speed);
                     if (jumpHeld(player)) {
                        vel = vel.add(0.0, 0.35, 0.0);
                     } else if (player.isShiftKeyDown()) {
                        vel = vel.add(0.0, -0.35, 0.0);
                     } else if (speed < 0.05) {
                        double phase = (double)(player.tickCount % 50) * 0.12566370614359174 + (double)(player.getId() & 0xFF);
                        vel = vel.add(0.0, 0.012 * Math.cos(phase), 0.0);
                     }

                     player.setDeltaMovement(vel);
                     player.hurtMarked = true;
                     player.resetFallDistance();
                     maybeSonicBoom(level, player, data);
                     if (afterburn && player.tickCount % 3 == 0) {
                        Fx.shake(level, player.position(), 8.0, 0.05F, 2, 2);
                     }
                  } else {
                     player.resetFallDistance();
                  }

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
   }

   private static float forwardAxis(ServerPlayer player) {
      MoveKeys.Keys in = MoveKeys.of(player);
      return in.forward() == in.backward() ? 0.0F : (in.forward() ? 1.0F : -1.0F);
   }

   private static boolean jumpHeld(ServerPlayer player) {
      return MoveKeys.of(player).jump();
   }

   private static double updateCommandedSpeed(PowerData data, float forwardAxis, boolean brake) {
      double cmd = (double)data.var("exo_cmd_speed", 0.0F);
      if (brake) {
         cmd = Math.max(0.0, cmd - 0.16);
      } else if (forwardAxis > 0.0F) {
         cmd = Math.min(1.7, cmd + 0.05);
      } else if (forwardAxis < 0.0F) {
         cmd = Math.max(0.0, cmd - 0.08);
      } else {
         cmd = Math.max(0.0, cmd - 0.03);
      }

      data.setVar("exo_cmd_speed", (float)cmd);
      return cmd;
   }

   private static boolean updateBoost(PowerData data) {
      float boost = data.var("exo_boost", 0.0F);
      if (boost > 0.0F) {
         data.setVar("exo_boost", --boost);
         if (boost <= 0.0F) {
            data.setVar("exo_boost_cd", 100.0F);
         }

         return true;
      } else {
         float cd = data.var("exo_boost_cd", 0.0F);
         if (cd > 0.0F) {
            data.setVar("exo_boost_cd", cd - 1.0F);
         }

         return false;
      }
   }

   private static void cancelAfterburner(PowerData data) {
      if (data.var("exo_boost", 0.0F) > 0.0F) {
         data.setVar("exo_boost", 0.0F);
         data.setVar("exo_boost_cd", 100.0F);
      }
   }

   private static void updateBrakeLevel(PowerData data, boolean brake, float forwardAxis, boolean afterburn) {
      float level;
      if (afterburn || (double)data.var("exo_jet_speed", 0.0F) <= 0.4) {
         level = 0.0F;
      } else if (brake) {
         level = 1.0F;
      } else if (forwardAxis < 0.0F) {
         level = 0.55F;
      } else {
         level = 0.0F;
      }

      data.setVar("exo_brake", level);
   }

   private static float billed(float fuel) {
      return StaminaSystem.effectiveCost(fuel, RocketChargeAbility.class.getSimpleName());
   }

   private static float cruiseBurn(double speed) {
      float frac = (float)Mth.clamp(speed / 1.7, 0.0, 1.0);
      return Mth.lerp(frac, 0.12F, 0.5F);
   }

   private static double easeSpeed(PowerData data, double target, boolean brake, boolean afterburn) {
      double cur = (double)data.var("exo_jet_speed", 0.0F);
      double accel = afterburn ? 0.12 : 0.05;
      double step = target >= cur ? accel : (brake ? 0.16 : 0.1);
      cur += Mth.clamp(target - cur, -step, step);
      data.setVar("exo_jet_speed", (float)cur);
      return cur;
   }

   private static Vec3 easeDirection(ServerPlayer player, PowerData data) {
      Vec3 look = player.getLookAngle();
      Vec3 dir = storedHeading(data);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = look;
      }

      Vec3 nd = dir.add(look.subtract(dir).scale(0.28));
      if (nd.lengthSqr() < 1.0E-6) {
         nd = look;
      }

      nd = nd.normalize();
      storeHeading(data, nd);
      return nd;
   }

   private static void seedHeading(ServerPlayer player, PowerData data) {
      Vec3 v = player.getDeltaMovement();
      storeHeading(data, v.lengthSqr() > 1.0E-4 ? v.normalize() : player.getLookAngle());
   }

   private static Vec3 storedHeading(PowerData data) {
      return new Vec3((double)data.var("exo_head_x", 0.0F), (double)data.var("exo_head_y", 0.0F), (double)data.var("exo_head_z", 0.0F));
   }

   private static void storeHeading(PowerData data, Vec3 dir) {
      data.setVar("exo_head_x", (float)dir.x);
      data.setVar("exo_head_y", (float)dir.y);
      data.setVar("exo_head_z", (float)dir.z);
   }

   private static double horizontalSpeed(ServerPlayer player) {
      Vec3 v = player.getDeltaMovement();
      return Math.sqrt(v.x * v.x + v.z * v.z);
   }

   private static void maybeSonicBoom(ServerLevel level, ServerPlayer player, PowerData data) {
      boolean afterburn = isAfterburner(data);
      double horiz = horizontalSpeed(player);
      boolean armed = data.var("exo_boom_armed", 0.0F) != 0.0F;
      if (afterburn && armed && horiz >= 2.0) {
         data.setVar("exo_boom_armed", 0.0F);
         ExoFx.sonicBoom(level, player.position().add(0.0, 0.9, 0.0), player.getYRot());
      } else if (!armed && (!afterburn || horiz < 1.6)) {
         data.setVar("exo_boom_armed", 1.0F);
      }
   }

   private static void stopJetPose(ServerPlayer player) {
      PowerService.stopAnimation(player);
   }

   private static void beginSputter(ServerPlayer player, PowerData data) {
      stopJetPose(player);
      data.setVar("exo_jet", 0.0F);
      data.setVar("exo_jet_speed", 0.0F);
      data.setVar("exo_brake", 0.0F);
      data.setVar("exo_boom_armed", 0.0F);
      data.setVar("exo_sputter", 25.0F);
      player.displayClientMessage(Component.translatable("awakened.exosavant.power_low"), true);
      ServerLevel level = player.serverLevel();
      Fx.sound(level, player.position(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 0.7F);
      PowerService.sync(player);
   }

   private static void sputterFx(ServerPlayer player) {
      if (player.tickCount % 3 == 0) {
         ServerLevel level = player.serverLevel();
         Vec3 at = player.position().add(0.0, 0.4, 0.0);
         Fx.burst(level, ParticleTypes.LARGE_SMOKE, at, 4, 0.25, 0.01);
         Fx.burst(level, ParticleTypes.SMOKE, at, 2, 0.2, 0.0);
      }
   }

   private static void teardown(ServerPlayer player, PowerData data) {
      stopJetPose(player);
      player.getAbilities().setFlyingSpeed(0.05F);
      TempFlight.revoke(player, false);
      player.onUpdateAbilities();
      resetVars(data);
   }

   private static void resetVars(PowerData data) {
      data.setVar("exo_flying", 0.0F);
      data.setVar("exo_jet", 0.0F);
      data.setVar("exo_sputter", 0.0F);
      data.setVar("exo_fly_grace", 0.0F);
      data.setVar("exo_cmd_speed", 0.0F);
      data.setVar("exo_jet_speed", 0.0F);
      data.setVar("exo_head_x", 0.0F);
      data.setVar("exo_head_y", 0.0F);
      data.setVar("exo_head_z", 0.0F);
      data.setVar("exo_boost", 0.0F);
      data.setVar("exo_boost_cd", 0.0F);
      data.setVar("exo_boom_armed", 0.0F);
      data.setVar("exo_brake", 0.0F);
   }

   public static void clear(ServerPlayer player) {
      teardown(player, PowerService.data(player));
   }
}
