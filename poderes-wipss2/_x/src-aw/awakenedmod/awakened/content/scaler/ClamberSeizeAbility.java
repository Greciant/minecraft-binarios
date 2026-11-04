package com.awakenedmod.awakened.content.scaler;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Bosses;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class ClamberSeizeAbility extends Ability {
   public static final String ID = "clamber_seize";
   private static final int UNLOCK = 13;
   private static final float CLAMBER_COST = 8.0F;
   private static final float SEIZE_COST = 5.0F;
   private static final float THROW_COST = 7.5F;
   private static final double CLAMBER_RANGE = 10.0;
   private static final double SEIZE_RANGE = 9.0;
   private static final double AIM_CONE_DEG = 12.0;
   private static final double CLAMBER_MIN_HEIGHT_RATIO = 1.5;
   private static final double SEIZE_MAX_HEIGHT_RATIO = 0.6;

   public ClamberSeizeAbility() {
      super("clamber_seize", 8.0F, 13);
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      if (SeizeControl.isSeizing(player)) {
         return 0.0F;
      } else {
         float scale = Scaling.stage(PowerService.data(player)).scale();
         if (scale <= SizeStage.SMALL.scale()) {
            return 8.0F;
         } else {
            return scale >= SizeStage.BRUTE.scale() ? 5.0F : 0.0F;
         }
      }
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (SeizeControl.isSeizing(player)) {
         SeizeControl.drop(player);
         return true;
      } else if (SeizeControl.isClambering(player)) {
         return this.fail(player, "awakened.scaler.clamber_busy", new Object[0]);
      } else {
         float scale = Scaling.stage(PowerService.data(player)).scale();
         if (scale <= SizeStage.SMALL.scale()) {
            return this.tryClamber(player);
         } else {
            return scale >= SizeStage.BRUTE.scale() ? this.trySeize(player) : this.fail(player, "awakened.scaler.commit_violent", new Object[0]);
         }
      }
   }

   private boolean tryClamber(ServerPlayer player) {
      AimUtil.SoftPick pick = AimUtil.aimedLivingSoftPick(
         player, 10.0, 12.0, e -> (double)e.getBbHeight() > (double)player.getBbHeight() * 1.5 && Harm.allowed(player, e)
      );
      if (!pick.found()) {
         return this.fail(player, pick.rejected() ? "awakened.scaler.clamber_too_small" : "awakened.scaler.clamber_no_target", new Object[0]);
      } else {
         SeizeControl.beginClamber(player, pick.target());
         return true;
      }
   }

   private boolean trySeize(ServerPlayer player) {
      AimUtil.SoftPick pick = AimUtil.aimedLivingSoftPick(
         player, 9.0, 12.0, e -> (double)e.getBbHeight() < (double)player.getBbHeight() * 0.6 && Harm.allowed(player, e)
      );
      if (!pick.found()) {
         return this.fail(player, pick.rejected() ? "awakened.scaler.seize_too_big" : "awakened.scaler.seize_no_target", new Object[0]);
      } else {
         LivingEntity victim = pick.target();
         if (Bosses.is(victim)) {
            return this.fail(player, "awakened.scaler.boss_immune", new Object[0]);
         } else if (SeizeControl.isSeizedVictim(victim)) {
            return this.fail(player, "awakened.scaler.seize_taken", new Object[0]);
         } else {
            SeizeControl.beginSeize(player, victim);
            return true;
         }
      }
   }

   @Override
   public void onSecondaryAction(ServerPlayer player, int action) {
      if (action == 4 && SeizeControl.isSeizing(player)) {
         PowerData data = PowerService.data(player);
         float cost = this.billed(7.5F);
         if (!StaminaSystem.canAfford(data, cost)) {
            this.fail(player, "awakened.scaler.tired", new Object[0]);
         } else {
            if (SeizeControl.throwVictim(player) && !data.infiniteStamina()) {
               data.setStamina(data.stamina() - cost);
               data.setLastSpendTime(player.serverLevel().getGameTime());
            }
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      SeizeControl.releaseClamberKey(player);
   }

   @Override
   public void serverTick(ServerPlayer player) {
      SeizeControl.tick(player);
   }

   @Override
   public boolean isActive(ServerPlayer player) {
      return SeizeControl.isSeizing(player) || SeizeControl.isClambering(player) || super.isActive(player);
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      SeizeControl.clearPlayer(player);
      super.clearPlayer(player);
   }
}
