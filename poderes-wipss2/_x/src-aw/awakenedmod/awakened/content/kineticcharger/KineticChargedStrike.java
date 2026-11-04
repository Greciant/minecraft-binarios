package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

abstract class KineticChargedStrike extends KineticHoldAbility {
   private final int windupTicks;
   private final float maxSpend;
   private final Map<UUID, KineticChargedStrike.Chamber> chambered = this.activeState();
   private final Map<UUID, KineticChargedStrike.Chamber> swinging = this.activeState();
   protected final TimedBlows blows = new TimedBlows(this.playerState());

   protected KineticChargedStrike(String id, float staminaCost, int unlockLevel, int windupTicks, float maxSpend, int capTicks) {
      super(id, staminaCost, unlockLevel, capTicks);
      this.windupTicks = windupTicks;
      this.maxSpend = maxSpend;
   }

   protected abstract void windupFx(ServerPlayer var1, float var2);

   protected abstract void pourFx(ServerPlayer var1, int var2, float var3);

   protected abstract void releaseFx(ServerPlayer var1);

   protected abstract void strike(ServerPlayer var1, float var2);

   protected int swingTicks() {
      return 2;
   }

   protected void tickAftermath(ServerPlayer player) {
   }

   protected boolean canBegin(ServerPlayer player) {
      return true;
   }

   protected static Vec3 outward(ServerPlayer player, Vec3 origin, LivingEntity target) {
      Vec3 flat = new Vec3(target.getX() - origin.x, 0.0, target.getZ() - origin.z);
      return flat.lengthSqr() < 1.0E-4 ? AimUtil.lookFlat(player) : flat.normalize();
   }

   protected final float maxSpend() {
      return this.maxSpend;
   }

   protected final int windupTicks() {
      return this.windupTicks;
   }

   protected final float fillOf(float spent) {
      return Math.min(1.0F, spent / this.maxSpend);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   protected final int pourFromTick() {
      return this.windupTicks;
   }

   @Override
   protected final float pourCap() {
      return this.maxSpend;
   }

   @Override
   protected final boolean begin(ServerPlayer player, KineticHoldAbility.Hold hold) {
      if (this.chambered.containsKey(player.getUUID()) || this.swinging.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else if (!this.canBegin(player)) {
         return false;
      } else {
         this.windupFx(player, 0.0F);
         return true;
      }
   }

   @Override
   protected final void tick(ServerPlayer player, KineticHoldAbility.Hold hold) {
      if (hold.ticks < this.windupTicks) {
         this.windupFx(player, (float)hold.ticks / (float)this.windupTicks);
      } else {
         this.pourFx(player, hold.ticks, hold.poured / this.maxSpend);
      }
   }

   @Override
   protected final void end(ServerPlayer player, KineticHoldAbility.Hold hold, KineticHoldAbility.End reason) {
      if (reason != KineticHoldAbility.End.TORN_DOWN && reason != KineticHoldAbility.End.CANCELLED) {
         int left = this.windupTicks - hold.ticks;
         if (left > 0) {
            this.chambered.put(player.getUUID(), new KineticChargedStrike.Chamber(left, hold.poured));
         } else {
            this.release(player, hold.poured);
         }
      }
   }

   private void release(ServerPlayer player, float spent) {
      this.releaseFx(player);
      int swing = this.swingTicks();
      if (swing <= 0) {
         this.strike(player, spent);
      } else {
         this.swinging.put(player.getUUID(), new KineticChargedStrike.Chamber(swing, spent));
      }
   }

   @Override
   protected final void tickOutsideHold(ServerPlayer player) {
      this.blows.tick(player);
      this.tickSwing(player);
      this.tickChamber(player);
      this.tickAftermath(player);
   }

   private void tickSwing(ServerPlayer player) {
      KineticChargedStrike.Chamber swing = this.swinging.get(player.getUUID());
      if (swing != null) {
         if (--swing.left <= 0) {
            this.swinging.remove(player.getUUID());
            this.strike(player, swing.spent);
         }
      }
   }

   private void tickChamber(ServerPlayer player) {
      KineticChargedStrike.Chamber chamber = this.chambered.get(player.getUUID());
      if (chamber != null) {
         if (--chamber.left > 0) {
            this.windupFx(player, 1.0F - (float)chamber.left / (float)this.windupTicks);
         } else {
            this.chambered.remove(player.getUUID());
            this.release(player, chamber.spent);
         }
      }
   }

   private static final class Chamber {
      private int left;
      private final float spent;

      private Chamber(int left, float spent) {
         this.left = left;
         this.spent = spent;
      }
   }
}
