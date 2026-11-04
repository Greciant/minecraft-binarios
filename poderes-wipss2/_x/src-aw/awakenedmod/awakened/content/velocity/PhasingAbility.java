package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.core.PhaseState;
import com.awakenedmod.awakened.core.TeleportUtil;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class PhasingAbility extends VelocityHoldAbility {
   private static final int CAP_TICKS = 12000;
   private static final float DRAIN_PER_SECOND = 6.0F;
   private static final int GEAR = 2;
   private static final double AGGRO_CLEAR_RADIUS = 24.0;
   private static final int EFFECTIVE_TICKS = 20;
   private static final double EFFECTIVE_DISTANCE = 4.0;
   private static final double EXIT_RADIUS = 6.0;
   private static final int EXIT_MAX_DROP = 6;

   public PhasingAbility() {
      super("phasing", 3, 12000, VelocityHoldAbility.Input.HELD);
   }

   @Override
   protected VelocityHoldAbility.Hold newHold() {
      return new PhasingAbility.PhaseHold();
   }

   @Override
   protected float drainPerSecond() {
      return 6.0F;
   }

   @Override
   protected int requiredGear() {
      return 2;
   }

   @Override
   protected boolean begin(ServerPlayer player, VelocityHoldAbility.Hold hold) {
      ((PhasingAbility.PhaseHold)hold).from = player.position();
      VelocityVisualState.setPhasing(player, true);
      PhaseState.setServer(player.getUUID(), true);
      player.setInvulnerable(true);
      player.stopUsingItem();
      PhaseState.dropAggro(player, 24.0);
      return true;
   }

   @Override
   protected void tick(ServerPlayer player, VelocityHoldAbility.Hold hold) {
      PhaseState.setServer(player.getUUID(), true);
      player.setInvulnerable(true);
      PhaseState.dropAggro(player, 24.0);
   }

   @Override
   protected void end(ServerPlayer player, VelocityHoldAbility.Hold hold, VelocityHoldAbility.End end) {
      VelocityVisualState.setPhasing(player, false);
      PhaseState.setServer(player.getUUID(), false);
      player.setInvulnerable(false);
      release(player);
      if (end != VelocityHoldAbility.End.TORN_DOWN) {
         awardEffectiveUse(player, (PhasingAbility.PhaseHold)hold);
      }
   }

   private static void release(ServerPlayer player) {
      TeleportUtil.ejectFromWall(player);
      if (!TeleportUtil.safeToStand(player, player.position())) {
         Vec3 spot = TeleportUtil.nearestSafeSpot(player, player.position(), 6.0, 6);
         if (spot != null) {
            player.teleportTo(spot.x, spot.y, spot.z);
            player.resetFallDistance();
         }
      }
   }

   private static void awardEffectiveUse(ServerPlayer player, PhasingAbility.PhaseHold hold) {
      if (hold.ticks >= 20 && hold.from != null && player.position().distanceTo(hold.from) >= 4.0) {
         MasterySystem.awardUse(player);
      }
   }

   @Override
   protected boolean heldByViewer(VelocityVisual visual) {
      return visual.phasing();
   }

   private static final class PhaseHold extends VelocityHoldAbility.Hold {
      Vec3 from;
   }
}
