package com.awakenedmod.awakened.content.stormcaller;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TempFlight;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class UpdraftAbility extends Ability {
   private static final float UPFRONT_COST = 20.0F;
   private static final int UNLOCK = 5;
   private static final float DRAIN_PER_TICK = 0.32F;
   private static final double LIFT_VELOCITY = 0.5;
   private static final int SYNC_INTERVAL = 5;
   private final Map<UUID, Boolean> flights = this.playerState();

   public UpdraftAbility() {
      super("updraft", 20.0F, 5);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.flights.containsKey(player.getUUID())) {
         this.end(player);
         return true;
      } else {
         PowerData data = PowerService.data(player);
         float upfront = this.billed(20.0F);
         if (!data.infiniteStamina() && data.stamina() < upfront) {
            return this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
         } else {
            if (!data.infiniteStamina()) {
               data.setStamina(data.stamina() - upfront);
            }

            TempFlight.grant(player);
            player.setDeltaMovement(player.getDeltaMovement().x, 0.5, player.getDeltaMovement().z);
            player.hurtMarked = true;
            this.flights.put(player.getUUID(), Boolean.TRUE);
            ServerLevel level = player.serverLevel();
            updraftClouds(level, player, 40, 0.6, 0.05);
            Fx.sound(level, player.position(), SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, 0.8F, 0.8F);
            PowerService.playAnimation(player, "updraft");
            if (player.getHealth() <= 6.0F) {
               MasterySystem.award(player, MasteryReason.SURVIVAL);
            }

            PowerService.sync(player);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      if (this.flights.containsKey(player.getUUID())) {
         ServerLevel level = player.serverLevel();
         updraftClouds(level, player, 6, 0.5, 0.01);
         player.resetFallDistance();
         PowerData data = PowerService.data(player);
         if (!data.infiniteStamina()) {
            float remaining = StaminaSystem.drainSustained(data, level.getGameTime(), this.billed(0.32F));
            if (remaining <= 0.0F) {
               this.end(player);
               return;
            }

            if (player.tickCount % 5 == 0) {
               PowerService.sync(player);
            }
         }
      }
   }

   private void end(ServerPlayer player) {
      if (this.flights.remove(player.getUUID()) != null) {
         TempFlight.revoke(player);
         ServerLevel level = player.serverLevel();
         updraftClouds(level, player, 20, 0.5, 0.03);
         Fx.sound(level, player.position(), SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, 0.5F, 1.2F);
         PowerService.sync(player);
      }
   }

   private static void updraftClouds(ServerLevel level, ServerPlayer caster, int count, double spread, double speed) {
      Vec3 feet = caster.position();
      sendCloud(level, caster, feet.x, feet.y, feet.z, count, spread, spread, speed);
      double rangeSqr = 2304.0;

      for (ServerPlayer viewer : level.players()) {
         if (viewer != caster && !(viewer.distanceToSqr(feet) > rangeSqr)) {
            sendCloud(level, viewer, feet.x, feet.y - 0.4, feet.z, count, spread, 0.1, speed);
         }
      }
   }

   private static void sendCloud(ServerLevel level, ServerPlayer to, double x, double y, double z, int count, double xzSpread, double yDist, double speed) {
      level.sendParticles(to, ParticleTypes.CLOUD, false, x, y, z, count, xzSpread, yDist, xzSpread, speed);
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.flights.containsKey(player.getUUID())) {
         TempFlight.revoke(player);
      }

      super.clearPlayer(player);
   }
}
