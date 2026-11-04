package com.awakenedmod.awakened.content.summoning;

import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

public final class WardenMinionGoal extends Goal {
   private final Warden warden;
   private final float speed;
   private static final int ANGER_LOCK = AngerLevel.ANGRY.getMinimumAnger() + 40;
   private static final double TELEPORT_DIST = 26.0;
   private static final int FOLLOW_CLOSE = 4;
   private static final int HOLD_CLOSE = 1;

   public WardenMinionGoal(Warden warden, double speed) {
      this.warden = warden;
      this.speed = (float)speed;
   }

   public boolean canUse() {
      return MinionSupport.owner(this.warden) != null;
   }

   public boolean canContinueToUse() {
      return this.canUse();
   }

   public boolean requiresUpdateEveryTick() {
      return true;
   }

   public void tick() {
      LivingEntity owner = MinionSupport.owner(this.warden);
      if (owner != null) {
         LivingEntity foe = MinionSupport.commandedOrAssistTarget(this.warden, owner);
         if (foe != null) {
            this.attack(foe);
         } else {
            this.standDown(owner);
         }
      }
   }

   private void attack(LivingEntity foe) {
      Brain<?> brain = this.warden.getBrain();
      if (this.warden.getEntityAngryAt().orElse(null) != foe || !this.warden.getAngerLevel().isAngry()) {
         this.warden.increaseAngerAt(foe, ANGER_LOCK, false);
      }

      brain.eraseMemory(MemoryModuleType.ROAR_TARGET);
      if (brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) != foe) {
         brain.setMemory(MemoryModuleType.ATTACK_TARGET, foe);
      }
   }

   private void standDown(LivingEntity owner) {
      Brain<?> brain = this.warden.getBrain();
      brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
      brain.eraseMemory(MemoryModuleType.ROAR_TARGET);
      brain.eraseMemory(MemoryModuleType.DISTURBANCE_LOCATION);
      brain.setMemoryWithExpiry(MemoryModuleType.SNIFF_COOLDOWN, Unit.INSTANCE, 40L);

      for (int i = 0; i < 4; i++) {
         LivingEntity angry = (LivingEntity)this.warden.getEntityAngryAt().orElse(null);
         if (angry == null) {
            break;
         }

         this.warden.clearAnger(angry);
      }

      Vec3 hold = MinionSupport.holdSpot(this.warden);
      if (hold != null) {
         brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(hold, this.speed, 1));
      } else {
         if (this.warden.distanceToSqr(owner) >= 676.0) {
            this.warden.teleportTo(owner.getX(), owner.getY(), owner.getZ());
         }

         brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(owner, this.speed, 4));
      }
   }
}
