package com.awakenedmod.awakened.content.command.vocab.trigger;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class JustAfterIHitTrigger extends RecentEventTrigger {
   private static final int WINDOW_TICKS = 60;

   public JustAfterIHitTrigger() {
      super(60);
   }

   @Override
   public String id() {
      return "just_after_i_hit";
   }

   @Override
   public int unlockLevel() {
      return 5;
   }

   @Override
   public void onCasterHit(ServerPlayer caster, LivingEntity victim) {
      this.mark(caster);
   }
}
