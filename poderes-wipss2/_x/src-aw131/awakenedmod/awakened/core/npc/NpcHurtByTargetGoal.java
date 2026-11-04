package com.awakenedmod.awakened.core.npc;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;

final class NpcHurtByTargetGoal extends HurtByTargetGoal {
   NpcHurtByTargetGoal(PathfinderMob mob) {
      super(mob, new Class[0]);
   }

   public void start() {
      super.start();
      this.unseenMemoryTicks = 600;
   }
}
