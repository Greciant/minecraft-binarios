package com.awakenedmod.awakened.content.reaper;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class WailingSoulsAbility extends Ability {
   private static final float COST = 18.0F;
   private static final int UNLOCK = 7;
   private static final double ACQUIRE_RANGE = 16.0;

   public WailingSoulsAbility() {
      super("wailing_souls", 18.0F, 7);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      LivingEntity target = AimUtil.aimedLivingSoft(player, 16.0, 12.0, e -> e != player && Harm.allowed(player, e));
      if (target == null) {
         return this.fail(player, "awakened.reaper.haunt_no_target", new Object[0]);
      } else {
         HauntControl.haunt(player, target, this.balanceKey());
         ReaperFx.hauntCast(player.serverLevel(), target.position().add(0.0, (double)target.getBbHeight() * 0.6, 0.0));
         MasterySystem.award(player, MasteryReason.HIT, target);
         return true;
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "death_mark";
   }
}
