package com.awakenedmod.awakened.content.entity.archevoker;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class ArchEvokerCloneEntity extends Monster implements ArchEvokerActor {
   private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(ArchEvokerCloneEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Float> DATA_ACTION_PROGRESS = SynchedEntityData.defineId(ArchEvokerCloneEntity.class, EntityDataSerializers.FLOAT);
   private static final int CAST_WINDUP = 16;
   private static final int CAST_TOTAL = 28;
   private static final double CAST_MIN = 4.0;
   private static final double CAST_MAX = 24.0;
   private int lifeTicks = 240;
   private int castTick;
   private int nextCastAt;

   public ArchEvokerCloneEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      this.xpReward = 0;
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 1.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.ATTACK_DAMAGE, 1.0)
         .add(Attributes.FOLLOW_RANGE, 40.0)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_ACTION, (byte)0);
      builder.define(DATA_ACTION_PROGRESS, 0.0F);
   }

   @Override
   public byte getAction() {
      return (Byte)this.entityData.get(DATA_ACTION);
   }

   @Override
   public float getActionProgress() {
      return (Float)this.entityData.get(DATA_ACTION_PROGRESS);
   }

   private void setAction(byte action) {
      this.entityData.set(DATA_ACTION, action);
   }

   private void setActionProgress(float p) {
      this.entityData.set(DATA_ACTION_PROGRESS, p);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal(this, Player.class, true));
   }

   public void aiStep() {
      super.aiStep();
      if (this.level() instanceof ServerLevel server) {
         if (--this.lifeTicks <= 0) {
            this.discard();
         } else {
            LivingEntity target = this.getTarget();
            if (this.getAction() == 1) {
               this.castTick++;
               this.setActionProgress(Math.min(1.0F, (float)this.castTick / 28.0F));
               if (target != null) {
                  this.getLookControl().setLookAt(target, 30.0F, 30.0F);
               }

               if (this.castTick == 16 && target != null && target.isAlive()) {
                  ArchEvokerMoves.fangLineToward(server, this, this.position(), target.position(), 6, 2.0, 1.8, 1);
               }

               if (this.castTick >= 28) {
                  this.setAction((byte)0);
                  this.setActionProgress(0.0F);
                  this.castTick = 0;
                  this.nextCastAt = this.tickCount + 40 + this.getRandom().nextInt(30);
               }
            } else if (target != null && this.tickCount >= this.nextCastAt) {
               double dsq = this.distanceToSqr(target);
               if (dsq >= 16.0 && dsq <= 576.0) {
                  this.setAction((byte)1);
                  this.castTick = 0;
                  this.getLookControl().setLookAt(target, 30.0F, 30.0F);
               }
            }
         }
      }
   }
}
