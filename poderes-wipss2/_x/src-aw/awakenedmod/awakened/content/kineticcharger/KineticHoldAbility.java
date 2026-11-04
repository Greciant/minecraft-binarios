package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

abstract class KineticHoldAbility extends Ability {
   protected static final int NO_POUR = -1;
   private final Map<UUID, KineticHoldAbility.Hold> holds = this.activeState();
   private final int capTicks;

   protected KineticHoldAbility(String id, float staminaCost, int unlockLevel, int capTicks) {
      super(id, staminaCost, unlockLevel);
      this.capTicks = capTicks;
   }

   protected KineticHoldAbility.Hold newHold() {
      return new KineticHoldAbility.Hold();
   }

   protected abstract boolean begin(ServerPlayer var1, KineticHoldAbility.Hold var2);

   protected abstract void end(ServerPlayer var1, KineticHoldAbility.Hold var2, KineticHoldAbility.End var3);

   protected void tick(ServerPlayer player, KineticHoldAbility.Hold hold) {
   }

   protected void tickOutsideHold(ServerPlayer player) {
   }

   protected int pourFromTick() {
      return -1;
   }

   protected float pourCap() {
      return 60.0F;
   }

   protected boolean cancelsOnSneak() {
      return false;
   }

   @Override
   public final boolean activate(ServerPlayer player) {
      if (this.holds.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         KineticHoldAbility.Hold hold = this.newHold();
         hold.cost = this.effectiveCost(player);
         hold.sneakArmed = !player.isShiftKeyDown();
         if (!this.begin(player, hold)) {
            return false;
         } else {
            this.holds.put(player.getUUID(), hold);
            return true;
         }
      }
   }

   @Override
   public final void serverTick(ServerPlayer player) {
      this.tickOutsideHold(player);
      KineticHoldAbility.Hold hold = this.holds.get(player.getUUID());
      if (hold != null) {
         hold.ticks++;
         if (this.cancelsOnSneak()) {
            if (!player.isShiftKeyDown()) {
               hold.sneakArmed = true;
            } else if (hold.sneakArmed) {
               this.stop(player, KineticHoldAbility.End.CANCELLED);
               return;
            }
         }

         int from = this.pourFromTick();
         if (from != -1 && hold.ticks > from) {
            hold.poured = KineticBank.pourTick(player, hold.poured, this.pourCap());
         }

         this.tick(player, hold);
         if (hold.ticks >= this.capTicks) {
            this.stop(player, KineticHoldAbility.End.CAPPED);
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.stop(player, KineticHoldAbility.End.RELEASED);
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.stop(player, KineticHoldAbility.End.TORN_DOWN);
      super.clearPlayer(player);
   }

   public final void stop(ServerPlayer player, KineticHoldAbility.End reason) {
      KineticHoldAbility.Hold hold = this.holds.remove(player.getUUID());
      if (hold != null) {
         PowerService.stopAnimation(player);
         if (reason == KineticHoldAbility.End.CANCELLED || reason == KineticHoldAbility.End.TORN_DOWN) {
            this.refund(player, hold);
         }

         this.end(player, hold, reason);
         KineticFx.holdEnd(player);
         KineticBank.spent(player);
      }
   }

   protected final void refund(ServerPlayer player, KineticHoldAbility.Hold hold) {
      this.refundPour(player, hold.poured);
      this.resource(player).refund(player, hold.cost);
      hold.poured = 0.0F;
      hold.cost = 0.0F;
   }

   protected final void refundPour(ServerPlayer player, float poured) {
      KineticBank.deposit(player, poured);
   }

   protected final KineticHoldAbility.Hold hold(ServerPlayer player) {
      return this.holds.get(player.getUUID());
   }

   final float liveSpend(ServerPlayer player) {
      KineticHoldAbility.Hold hold = this.holds.get(player.getUUID());
      if (hold == null) {
         return 0.0F;
      } else {
         float cost = PowerService.data(player).infiniteStamina() ? 0.0F : hold.cost;
         return Math.max(0.0F, cost + hold.poured);
      }
   }

   protected static enum End {
      RELEASED,
      CAPPED,
      CANCELLED,
      TORN_DOWN;
   }

   protected static class Hold {
      int ticks;
      float cost;
      float poured;
      boolean sneakArmed;
   }
}
