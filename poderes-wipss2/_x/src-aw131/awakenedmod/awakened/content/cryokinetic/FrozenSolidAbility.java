package com.awakenedmod.awakened.content.cryokinetic;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

public final class FrozenSolidAbility extends Ability {
   private static final int UNLOCK = 5;
   private static final double RANGE = 18.0;
   private static final int FREEZE_TICKS = 52;
   private static final int MAX_FREEZE_TICKS = 100;
   private static final float DAMAGE = 4.0F;
   private final IceHold hold = new IceHold(this.activeState());

   public FrozenSolidAbility() {
      super("frozen_solid", 30.0F, 5);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(4.0).durationTicks(52, 100).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      LivingEntity target = AimUtil.aimedLivingEntity(player, 18.0, e -> Harm.allowed(player, e));
      if (target == null) {
         return this.fail(player, "awakened.cryokinetic.no_target", new Object[0]);
      } else if (!Harm.controls(player, target)) {
         return this.fail(player, "awakened.feedback.pvp_control_off", new Object[0]);
      } else if (this.hold.holds(player, target)) {
         return this.fail(player, "awakened.cryokinetic.already_frozen", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         this.hold.encase(player, level, target, MasteryScaling.scaledTicks(52, 100, player));
         Dmg.hurt(target, level, Dmg.by(player, DamageTypes.FREEZE), MasteryScaling.scaledDamage(4.0F, player, this.balanceKey()));
         PowerService.playAnimation(player, "frozen_solid");
         MasterySystem.award(player, MasteryReason.HIT, target);
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.hold.tick(player);
   }

   @Override
   public boolean occupiesHands(ServerPlayer player) {
      return false;
   }

   @Override
   public boolean ownsBody(ServerPlayer player) {
      return false;
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.hold.releaseAll(player);
      super.clearPlayer(player);
   }
}
