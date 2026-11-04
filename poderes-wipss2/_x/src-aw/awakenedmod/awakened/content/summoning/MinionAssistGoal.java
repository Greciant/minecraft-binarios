package com.awakenedmod.awakened.content.summoning;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

public final class MinionAssistGoal extends TargetGoal {
   private LivingEntity wanted;

   public MinionAssistGoal(Mob mob) {
      super(mob, false);
   }

   public boolean canUse() {
      LivingEntity owner = MinionSupport.owner(this.mob);
      if (owner == null) {
         return false;
      } else {
         this.wanted = MinionSupport.commandedOrAssistTarget(this.mob, owner);
         return this.wanted != null;
      }
   }

   public void start() {
      this.mob.setTarget(this.wanted);
      super.start();
   }

   public boolean canContinueToUse() {
      LivingEntity target = this.mob.getTarget();
      if (target == null || !target.isAlive()) {
         return false;
      } else if (!MinionSupport.canPressAttack(this.mob)) {
         return false;
      } else {
         LivingEntity owner = MinionSupport.owner(this.mob);
         return owner != null && target != owner ? !SummonAttachments.minion(target).isOwnedBy(owner.getUUID()) : false;
      }
   }
}
