package com.awakenedmod.awakened.core.npc;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record NpcContext(
   PoweredNpc npc,
   NpcCaster caster,
   @Nullable LivingEntity target,
   double distance,
   boolean lineOfSight,
   Vec3 targetVelocity,
   @Nullable Vec3 lastSeenPos,
   int lastSeenTick,
   boolean fleeing,
   boolean hugging,
   float strain,
   float stamina,
   NpcSkill skill,
   NpcRead read
) {
   public static final int SIGHT_GRACE = 10;

   public boolean ready(String moveId) {
      return this.npc.npcBrain().ready(moveId, this);
   }

   public boolean usable(String moveId) {
      return this.npc.npcBrain().usable(moveId, this);
   }

   public float moveScore(String moveId) {
      return this.npc.npcBrain().moveScore(moveId, this);
   }

   public boolean idleHands() {
      return this.npc.npcBrain().running() == null;
   }

   @Nullable
   public Vec3 targetCenter() {
      return this.target == null ? null : this.target.position().add(0.0, (double)this.target.getBbHeight() * 0.5, 0.0);
   }

   public float healthFraction() {
      return this.npc.getHealth() / Math.max(1.0F, this.npc.getMaxHealth());
   }

   public int unseenTicks() {
      return this.npc.tickCount - this.lastSeenTick;
   }

   public boolean tracking() {
      return this.target != null && (this.lineOfSight || this.unseenTicks() <= 10);
   }

   public int hostilesNear(Vec3 center, double radius) {
      double r2 = radius * radius;
      return this.npc
         .level()
         .getEntitiesOfClass(
            LivingEntity.class, AABB.ofSize(center, radius * 2.0, radius * 2.0, radius * 2.0), e -> e.distanceToSqr(center) <= r2 && this.isHostile(e)
         )
         .size();
   }

   private boolean isHostile(LivingEntity e) {
      if (e != this.npc && e.isAlive() && this.caster.canHarm(e)) {
         if (e == this.target) {
            return true;
         } else {
            if (e instanceof Player p && !p.isCreative() && !p.isSpectator() || e instanceof Mob m && m.getTarget() == this.npc) {
               return true;
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public double targetSpeed() {
      return Math.sqrt(this.targetVelocity.x * this.targetVelocity.x + this.targetVelocity.z * this.targetVelocity.z);
   }

   @Nullable
   public Vec3 leadPoint(Vec3 origin, double speed, double gravity, int delayTicks) {
      Vec3 center = this.targetCenter();
      if (center == null) {
         return null;
      } else {
         double flight = center.distanceTo(origin) / Math.max(0.1, speed);
         double lead = (flight + (double)delayTicks) * (double)this.skill.leadAccuracy();
         Vec3 led = center.add(this.targetVelocity.x * lead, 0.0, this.targetVelocity.z * lead);
         return led.add(0.0, 0.5 * gravity * flight * flight, 0.0);
      }
   }
}
