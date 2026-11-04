package com.awakenedmod.awakened.content.reaper;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class DeathMarkAbility extends Ability {
   private static final float COST = 22.0F;
   private static final int UNLOCK = 10;
   private static final double ACQUIRE_RANGE = 16.0;

   public DeathMarkAbility() {
      super("death_mark", 22.0F, 10);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      LivingEntity target = AimUtil.aimedLivingSoft(player, 16.0, 12.0, e -> e != player && Harm.allowed(player, e));
      if (target == null) {
         return this.fail(player, "awakened.reaper.mark_no_target", new Object[0]);
      } else {
         DeathMarkControl.mark(player, target);
         ReaperFx.deathMarkCast(player.serverLevel(), target.position().add(0.0, (double)target.getBbHeight() * 0.6, 0.0), target.getYRot());
         MasterySystem.award(player, MasteryReason.HIT, target);
         return true;
      }
   }
}
