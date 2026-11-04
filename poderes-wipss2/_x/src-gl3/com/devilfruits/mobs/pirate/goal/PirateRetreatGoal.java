package com.devilfruits.mobs.pirate.goal;

import com.devilfruits.mobs.pirate.PirateConfig;
import com.devilfruits.mobs.pirate.entity.PirateFighter;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class PirateRetreatGoal extends Goal {
   private final PirateFighter mob;
   private int ticks;
   private long nextAllowed;

   public PirateRetreatGoal(PirateFighter mob) {
      this.mob = mob;
      this.setFlags(EnumSet.of(Flag.MOVE));
   }

   public boolean canUse() {
      LivingEntity target = this.mob.getTarget();
      return target != null
         && !this.mob.isActing()
         && this.mob.level().getGameTime() >= this.nextAllowed
         && (double)this.mob.getHealth() < (double)this.mob.getMaxHealth() * (Double)PirateConfig.DECKHAND_RETREAT_HEALTH.get()
         && this.mob.distanceToSqr(target) < 64.0;
   }

   public boolean canContinueToUse() {
      return this.ticks > 0 && this.mob.getTarget() != null;
   }

   public void start() {
      this.ticks = 60;
      this.nextAllowed = this.mob.level().getGameTime() + 260L;
      LivingEntity target = this.mob.getTarget();
      Vec3 away = target == null ? null : DefaultRandomPos.getPosAway(this.mob, 10, 4, target.position());
      if (away != null) {
         this.mob.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
      }
   }

   public void tick() {
      this.ticks--;
      if (this.mob.getNavigation().isDone() && this.ticks > 0) {
         this.ticks = Math.min(this.ticks, 10);
      }
   }

   public void stop() {
      this.ticks = 0;
      this.mob.getNavigation().stop();
   }
}
