package com.awakenedmod.awakened.content.reaper;

import com.awakenedmod.awakened.core.PhaseState;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.net.ReaperDriftPayload;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SpectralDriftAbility extends Ability {
   private static final float COST = 16.0F;
   private static final int UNLOCK = 3;
   private static final int DRIFT_TICKS = 5;
   private static final double DISTANCE = 8.0;
   private static final double SPEED = 1.6;
   private static final float LOW_HEALTH = 6.0F;
   private final Map<UUID, SpectralDriftAbility.Drift> drifting = this.activeState();

   public SpectralDriftAbility() {
      super("spectral_drift", 16.0F, 3);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      Vec3 aim = player.getLookAngle().normalize();
      this.drifting.put(player.getUUID(), new SpectralDriftAbility.Drift(aim));
      PhaseState.setServer(player.getUUID(), true);
      player.setInvulnerable(true);
      sendDrift(player, true);
      ServerLevel level = player.serverLevel();
      ReaperFx.driftDissolve(level, player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0));
      this.step(player, aim);
      if (player.getHealth() <= 6.0F) {
         MasterySystem.award(player, MasteryReason.SURVIVAL);
      }

      return true;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      SpectralDriftAbility.Drift drift = this.drifting.get(player.getUUID());
      if (drift != null) {
         drift.ticks--;
         if (drift.ticks <= 0) {
            this.reform(player);
         } else {
            this.step(player, drift.aim);
            ReaperFx.driftTrail(player.serverLevel(), player.position());
         }
      }
   }

   private void step(ServerPlayer player, Vec3 aim) {
      player.setDeltaMovement(aim.scale(1.6));
      player.hurtMarked = true;
      player.resetFallDistance();
   }

   private void reform(ServerPlayer player) {
      if (this.drifting.remove(player.getUUID()) != null) {
         this.endPhase(player);
         ServerLevel level = player.serverLevel();
         ReaperFx.driftReform(level, player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0));
         PowerService.stopAnimation(player);
         PowerService.sync(player);
      }
   }

   private void endPhase(ServerPlayer player) {
      PhaseState.setServer(player.getUUID(), false);
      player.setInvulnerable(false);
      sendDrift(player, false);
   }

   private static void sendDrift(ServerPlayer player, boolean on) {
      if (player.connection != null) {
         PacketDistributor.sendToPlayer(player, new ReaperDriftPayload(on), new CustomPacketPayload[0]);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.drifting.remove(player.getUUID()) != null) {
         this.endPhase(player);
         PowerService.stopAnimation(player);
      }

      super.clearPlayer(player);
   }

   @Override
   public void onDimensionChange(ServerPlayer player, ServerLevel from) {
      if (this.drifting.remove(player.getUUID()) != null) {
         this.endPhase(player);
         PowerService.stopAnimation(player);
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "spectral_drift";
   }

   private static final class Drift {
      private final Vec3 aim;
      private int ticks = 5;

      private Drift(Vec3 aim) {
         this.aim = aim;
      }
   }
}
