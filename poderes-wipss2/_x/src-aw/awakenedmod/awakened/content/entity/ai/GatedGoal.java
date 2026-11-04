package com.awakenedmod.awakened.content.entity.ai;

import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.ai.goal.Goal;

public final class GatedGoal extends Goal {
   private final Goal delegate;
   private final BooleanSupplier allowed;

   public GatedGoal(Goal delegate, BooleanSupplier allowed) {
      this.delegate = delegate;
      this.allowed = allowed;
      this.setFlags(delegate.getFlags());
   }

   public boolean canUse() {
      return this.allowed.getAsBoolean() && this.delegate.canUse();
   }

   public boolean canContinueToUse() {
      return this.allowed.getAsBoolean() && this.delegate.canContinueToUse();
   }

   public boolean isInterruptable() {
      return this.delegate.isInterruptable();
   }

   public boolean requiresUpdateEveryTick() {
      return this.delegate.requiresUpdateEveryTick();
   }

   public void start() {
      this.delegate.start();
   }

   public void stop() {
      this.delegate.stop();
   }

   public void tick() {
      this.delegate.tick();
   }
}
