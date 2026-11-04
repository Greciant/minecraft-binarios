package com.awakenedmod.awakened.content.command.vocab.trigger;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class AfterIKillTrigger extends RecentEventTrigger {
   private static final int WINDOW_TICKS = 80;

   public AfterIKillTrigger() {
      super(80);
   }

   @Override
   public String id() {
      return "after_i_kill";
   }

   @Override
   public int unlockLevel() {
      return 6;
   }

   @Override
   public void onCasterKill(ServerPlayer caster, LivingEntity victim) {
      this.mark(caster);
   }
}
