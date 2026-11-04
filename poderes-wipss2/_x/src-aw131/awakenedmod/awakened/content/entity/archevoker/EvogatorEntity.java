package com.awakenedmod.awakened.content.entity.archevoker;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class EvogatorEntity extends Monster {
   private static final int IDLE_LIFETIME = 900;
   private int lifeTicks = 900;

   public EvogatorEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      this.xpReward = 3;
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 34.0)
         .add(Attributes.ATTACK_DAMAGE, 6.0)
         .add(Attributes.MOVEMENT_SPEED, 0.32)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
         .add(Attributes.ARMOR, 3.0)
         .add(Attributes.FOLLOW_RANGE, 32.0)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.35, true));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal(this, Player.class, true));
   }

   public void aiStep() {
      super.aiStep();
      if (!this.level().isClientSide()) {
         if (this.getTarget() != null && this.getTarget().isAlive()) {
            this.lifeTicks = 900;
         } else if (--this.lifeTicks <= 0) {
            this.discard();
         }
      }
   }
}
