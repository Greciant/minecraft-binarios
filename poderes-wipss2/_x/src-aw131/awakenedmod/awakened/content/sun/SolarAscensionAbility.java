package com.awakenedmod.awakened.content.sun;

import com.awakenedmod.awakened.content.AwakenedParticles;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class SolarAscensionAbility extends Ability {
   private static final float STEP_COST = 6.0F;
   private static final int RAMP_INTERVAL = 8;
   private final Map<UUID, Integer> ramp = this.activeState();

   public SolarAscensionAbility() {
      super("solar_ascension", 6.0F, 0);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      if (!hasSun(player, data)) {
         return this.fail(player, "awakened.sun.need_sun", new Object[0]);
      } else if (Solar.steps(data) >= 4) {
         return this.fail(player, "awakened.sun.ascend_max", new Object[0]);
      } else {
         this.ramp.put(player.getUUID(), 1);
         PowerService.playAnimation(player, "solar_ascension");
         this.raiseStep(player, data);
         flourish(player, true);
         return true;
      }
   }

   @Override
   public boolean usesSneakActivate() {
      return true;
   }

   @Override
   public void onSecondaryAction(ServerPlayer player, int action) {
      if (action == 3) {
         this.ramp.put(player.getUUID(), -1);
         PowerService.stopAnimation(player);
         PowerData data = PowerService.data(player);
         if (Solar.steps(data) > 0) {
            Solar.ascend(data, -1);
            flourish(player, false);
            PowerService.sync(player);
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      Integer dir = this.ramp.get(player.getUUID());
      if (dir != null) {
         PowerData data = PowerService.data(player);
         if (dir > 0) {
            if (!hasSun(player, data)) {
               this.stop(player);
               return;
            }

            if (Solar.steps(data) >= 4) {
               this.stop(player);
               return;
            }

            chargeAura(player);
            if (player.tickCount % 8 == 0) {
               if (this.raiseStep(player, data)) {
                  flourish(player, true);
                  PowerService.sync(player);
               } else {
                  this.stop(player);
               }
            }
         } else if (player.tickCount % 8 == 0) {
            if (Solar.steps(data) > 0) {
               Solar.ascend(data, -1);
               flourish(player, false);
               PowerService.sync(player);
            } else {
               this.stop(player);
            }
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.stop(player);
   }

   private void stop(ServerPlayer player) {
      if (this.ramp.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
      }
   }

   private boolean raiseStep(ServerPlayer player, PowerData data) {
      if (Solar.steps(data) >= 4) {
         return true;
      } else if (!this.charge(player, 6.0F)) {
         return false;
      } else {
         Solar.ascend(data, 1);
         return true;
      }
   }

   private static boolean hasSun(ServerPlayer player, PowerData data) {
      return Solar.realAmbient(player) > 0.02F || Solar.personalSunOnYou(player, data);
   }

   private static void chargeAura(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Random rnd = new Random((long)player.getId() * 31L + (long)player.tickCount);
      Vec3 c = player.position();

      for (int i = 0; i < 6; i++) {
         double ang = rnd.nextDouble() * Math.PI * 2.0;
         double rr = 0.25 + rnd.nextDouble() * 0.5;
         double px = c.x + Math.cos(ang) * rr;
         double pz = c.z + Math.sin(ang) * rr;
         double upv = 0.25 + rnd.nextDouble() * 0.3;
         level.sendParticles(AwakenedParticles.SUN_MOTE.get(), px, c.y + 0.1, pz, 0, 0.0, upv, 0.0, 1.0);
      }
   }

   private static void flourish(ServerPlayer player, boolean up) {
      ServerLevel level = player.serverLevel();
      if (up) {
         Fx.burst(level, (ParticleOptions)AwakenedParticles.SUN_MOTE.get(), player.position().add(0.0, 1.0, 0.0), 28, 0.5, 0.1);
         Fx.sound(level, player.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.4F);
      } else {
         Fx.burst(level, ParticleTypes.SMOKE, player.position().add(0.0, 1.0, 0.0), 10, 0.4, 0.02);
         Fx.sound(level, player.position(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.2F);
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      PowerService.stopAnimation(player);
      Solar.clear(player);
      super.clearPlayer(player);
   }
}
