package com.awakenedmod.awakened.content.entity;

import com.awakenedmod.awakened.content.entity.ai.StalkerPounceGoal;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class StalkerEntity extends Monster {
   public StalkerEntity(EntityType<? extends StalkerEntity> type, Level level) {
      super(type, level);
      this.xpReward = 6;
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 26.0)
         .add(Attributes.MOVEMENT_SPEED, 0.37)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
         .add(Attributes.FOLLOW_RANGE, 32.0)
         .add(Attributes.ATTACK_DAMAGE, 6.0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new StalkerPounceGoal(this));
      this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, false));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
   }

   public void setTarget(LivingEntity target) {
      LivingEntity prev = this.getTarget();
      super.setTarget(target);
      if (target != null && prev == null && this.level() instanceof ServerLevel server) {
         Fx.scanLock(server, this.position().add(0.0, (double)this.getBbHeight() * 0.9, 0.0), SoundEvents.BEACON_ACTIVATE, 0.9F, 1.3F);
      }
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      ServerLevel level = (ServerLevel)this.level();
      if (this.getTarget() != null && this.getDeltaMovement().horizontalDistanceSqr() > 0.012 && this.random.nextInt(3) == 0) {
         Fx.burst(level, Fx.OPTIC_RED, this.position().add(0.0, 0.1, 0.0), 2, 0.2, 0.0);
      }
   }

   public boolean doHurtTarget(Entity target) {
      ServerLevel level = (ServerLevel)this.level();
      boolean hurt = super.doHurtTarget(target);
      if (hurt) {
         Vec3 bite = target.position().add(0.0, (double)target.getBbHeight() * 0.6, 0.0);
         Fx.burst(level, Fx.OPTIC_RED, bite, 8, 0.25, 0.05);
         Fx.sound(level, this.position(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.7F, 0.7F);
      }

      return hurt;
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      return SoundEvents.WARDEN_NEARBY_CLOSE;
   }

   @Nullable
   protected SoundEvent getHurtSound(DamageSource damageSource) {
      return SoundEvents.NETHERITE_BLOCK_HIT;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return SoundEvents.NETHERITE_BLOCK_BREAK;
   }

   public void die(DamageSource cause) {
      if (this.level() instanceof ServerLevel server) {
         Vec3 mid = this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0);
         Fx.burst(server, Fx.OPTIC_RED, mid, 24, 0.4, 0.1);
         Fx.burst(server, ParticleTypes.SMOKE, mid, 16, 0.3, 0.04);
      }

      super.die(cause);
   }
}
