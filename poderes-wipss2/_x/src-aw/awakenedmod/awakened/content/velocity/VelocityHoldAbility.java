package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

abstract class VelocityHoldAbility extends Ability {
   private final Map<UUID, VelocityHoldAbility.Hold> holds = this.activeState();
   private final int capTicks;
   private final VelocityHoldAbility.Input input;

   protected VelocityHoldAbility(String id, int unlockLevel, int capTicks, VelocityHoldAbility.Input input) {
      super(id, 0.0F, unlockLevel);
      this.capTicks = capTicks;
      this.input = input;
   }

   protected VelocityHoldAbility.Hold newHold() {
      return new VelocityHoldAbility.Hold();
   }

   protected abstract boolean begin(ServerPlayer var1, VelocityHoldAbility.Hold var2);

   protected abstract void end(ServerPlayer var1, VelocityHoldAbility.Hold var2, VelocityHoldAbility.End var3);

   protected void tick(ServerPlayer player, VelocityHoldAbility.Hold hold) {
   }

   protected float drainPerSecond() {
      return 0.0F;
   }

   protected float drainPerSecond(Player player) {
      return this.drainPerSecond();
   }

   protected int requiredGear() {
      return 0;
   }

   protected boolean holdsMomentum() {
      return false;
   }

   protected boolean heldByViewer(VelocityVisual visual) {
      return false;
   }

   final float viewerDrain(Player viewer, VelocityVisual visual) {
      return this.heldByViewer(visual) ? this.drainPerSecond(viewer) : 0.0F;
   }

   protected float pressCost() {
      return this.staminaCost();
   }

   @Override
   public String costLabel() {
      return this.costLabel(this.drainPerSecond(), 0);
   }

   @Override
   public String costLabel(Player viewer) {
      return this.costLabel(viewer == null ? this.drainPerSecond() : this.drainPerSecond(viewer), Gearbox.extraForcedGears(viewer, this.requiredGear()));
   }

   private String costLabel(float ownDrain, int extraGears) {
      float forced = (float)extraGears * 5.0F;
      if (ownDrain + forced <= 0.0F) {
         return this.pressCost() > 0.0F ? String.valueOf(Math.round(this.billed(this.pressCost()))) : "hold";
      } else {
         float perSecond = this.billed(ownDrain) + StaminaSystem.effectiveCost(forced, OverdriveAbility.class.getSimpleName());
         String drain = Math.round(perSecond) + "/s";
         return this.pressCost() > 0.0F ? Math.round(this.billed(this.pressCost())) + " + " + drain : drain;
      }
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.holds.containsKey(player.getUUID())) {
         return this.input == VelocityHoldAbility.Input.TOGGLE ? this.close(player) : this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         VelocityHoldAbility.Hold hold = this.newHold();
         if (!this.begin(player, hold)) {
            if (hold.shell != null) {
               hold.shell.discard();
            }

            return false;
         } else {
            this.holds.put(player.getUUID(), hold);
            Gearbox.demand(player, this.id(), this.requiredGear());
            Momentum.holdBleed(player, this.id(), this.holdsMomentum());
            VelocityVisualState.sync(player);
            return true;
         }
      }
   }

   protected boolean close(ServerPlayer player) {
      this.stop(player, VelocityHoldAbility.End.RELEASED);
      return true;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      VelocityHoldAbility.Hold hold = this.holds.get(player.getUUID());
      if (hold != null) {
         if (++hold.ticks >= this.capTicks) {
            this.stop(player, VelocityHoldAbility.End.CAPPED);
         } else if (!VelocityStamina.drainPerSecond(player, this.drainPerSecond(player), this.balanceKey())) {
            this.stop(player, VelocityHoldAbility.End.RELEASED);
         } else {
            this.tick(player, hold);
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      if (this.input == VelocityHoldAbility.Input.HELD) {
         this.stop(player, VelocityHoldAbility.End.RELEASED);
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void onDimensionChange(ServerPlayer player, ServerLevel from) {
      this.stop(player, VelocityHoldAbility.End.TORN_DOWN);
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.stop(player, VelocityHoldAbility.End.TORN_DOWN);
      super.clearPlayer(player);
   }

   public boolean holding(ServerPlayer player) {
      return this.holds.containsKey(player.getUUID());
   }

   protected final VelocityHoldAbility.Hold hold(ServerPlayer player) {
      return this.holds.get(player.getUUID());
   }

   public final void stop(ServerPlayer player, VelocityHoldAbility.End reason) {
      VelocityHoldAbility.Hold hold = this.holds.remove(player.getUUID());
      if (hold != null) {
         Gearbox.release(player, this.id());
         Momentum.holdBleed(player, this.id(), false);
         if (hold.shell != null) {
            hold.shell.discard();
            hold.shell = null;
         }

         this.end(player, hold, reason);
         VelocityVisualState.sync(player);
      }
   }

   protected static enum End {
      RELEASED,
      CAPPED,
      TORN_DOWN;
   }

   protected static class Hold {
      int ticks;
      VfxEntity shell;
   }

   protected static enum Input {
      HELD,
      TOGGLE,
      TIMED;
   }
}
