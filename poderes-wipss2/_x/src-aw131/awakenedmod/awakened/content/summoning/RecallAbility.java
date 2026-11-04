package com.awakenedmod.awakened.content.summoning;

import com.awakenedmod.awakened.core.power.Ability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

public final class RecallAbility extends Ability {
   private static final double AIM_RANGE = 32.0;

   public RecallAbility() {
      super("recall", 0.0F, 0);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      Mob target = MinionSupport.aimedMinion(player, 32.0);
      if (target == null) {
         target = MinionSupport.nearestMinion(player);
      }

      if (target == null) {
         return this.fail(player, "awakened.summon.no_minions", new Object[0]);
      } else {
         SummonFx.cancel(target);
         MinionSupport.recall(player, target);
         return true;
      }
   }

   @Override
   public boolean usesSneakActivate() {
      return true;
   }

   @Override
   public void onSecondaryAction(ServerPlayer player, int action) {
      if (action == 3) {
         for (Mob m : MinionSupport.minionsOf(player)) {
            SummonFx.cancel(m);
            MinionSupport.recall(player, m);
         }
      }
   }
}
