package com.awakenedmod.awakened.content.entity;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.ai.RangedKiteGoal;
import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.content.summoning.PossessMelee;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SuppressorEntity extends Monster implements RangedAttackMob, PossessMelee {
   private static final int CHARGE_TICKS = 10;
   public static final float IMPACT_DAMAGE = 4.0F;
   private static final float SPEED = 1.6F;
   private static final float SPREAD = 6.0F;
   private int chargeTicks;
   private float attackProgress;
   private float attackProgressO;

   public SuppressorEntity(EntityType<? extends SuppressorEntity> type, Level level) {
      super(type, level);
      this.moveControl = new FlyingMoveControl(this, 20, true);
      this.xpReward = 5;
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 30.0)
         .add(Attributes.MOVEMENT_SPEED, 0.25)
         .add(Attributes.FLYING_SPEED, 0.45)
         .add(Attributes.FOLLOW_RANGE, 36.0)
         .add(Attributes.ATTACK_DAMAGE, 4.0);
   }

   protected PathNavigation createNavigation(Level level) {
      FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
      nav.setCanOpenDoors(false);
      nav.setCanFloat(true);
      return nav;
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new RangedKiteGoal(this, 6.0, 1.2));
      this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 35, 16.0F));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal(this, Player.class, true));
   }

   public void setTarget(LivingEntity target) {
      LivingEntity prev = this.getTarget();
      super.setTarget(target);
      if (target != null && prev == null && this.level() instanceof ServerLevel server) {
         Fx.scanLock(server, this.position().add(0.0, (double)this.getBbHeight() * 0.7, 0.0), SoundEvents.BEACON_ACTIVATE, 0.7F, 1.7F);
      }
   }

   public void tick() {
      this.noPhysics = true;
      super.tick();
      this.noPhysics = false;
      this.setNoGravity(true);
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      ServerLevel level = (ServerLevel)this.level();
      this.attackProgressO = this.attackProgress;
      if (this.chargeTicks > 0) {
         this.chargeTicks--;
         this.attackProgress = 1.0F - (float)this.chargeTicks / 10.0F;
         this.spawnChargeParticles(level);
         if (this.chargeTicks == 0) {
            this.releaseBolt(level);
         }
      } else if (this.attackProgress > 0.0F) {
         this.attackProgress = Math.max(0.0F, this.attackProgress - 0.2F);
      }
   }

   public void performRangedAttack(LivingEntity target, float velocity) {
      if (this.chargeTicks <= 0 && this.attackProgress <= 0.0F) {
         this.chargeTicks = 10;
         if (this.level() instanceof ServerLevel server) {
            Fx.sound(server, this.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.8F, 1.4F);
         }
      }
   }

   private void releaseBolt(ServerLevel server) {
      LivingEntity target = this.getTarget();
      if (target != null && target.isAlive()) {
         server.addFreshEntity(bolt(this, server, target, 6.0F));
         Fx.sound(server, this.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.0F, 1.6F);
      }
   }

   public static KineticProjectile bolt(LivingEntity from, ServerLevel server, LivingEntity target, float spread) {
      KineticProjectile bolt = new KineticProjectile(AwakenedEntities.KINETIC_CHARGE.get(), from, server, new ItemStack(Items.FIRE_CHARGE));
      bolt.configure(4.0F, 1.0F, 1.5F, 0.3F);
      Vec3 aim = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0).subtract(bolt.position());
      bolt.shoot(aim.x, aim.y, aim.z, 1.6F, spread);
      return bolt;
   }

   private void spawnChargeParticles(ServerLevel server) {
      Vec3 base = this.position().add(0.0, (double)this.getBbHeight() * 0.4, 0.0);

      for (int i = 0; i < 3; i++) {
         double ang = Math.toRadians(120.0 * (double)i + (double)this.tickCount * 6.0);
         double r = 0.35;
         Vec3 tip = base.add(Math.cos(ang) * r, -0.3, Math.sin(ang) * r);
         Fx.burst(server, Fx.OPTIC_RED, tip, 1, 0.02, 0.0);
      }
   }

   public float getAttackProgress(float partialTick) {
      return Mth.lerp(partialTick, this.attackProgressO, this.attackProgress);
   }

   public float getSidewaysSpeed(float partialTick) {
      Vec3 v = this.getDeltaMovement();
      float yaw = this.yBodyRot * (float) (Math.PI / 180.0);
      double right = v.x * (double)Mth.cos(yaw) + v.z * (double)Mth.sin(yaw);
      return (float)right;
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.BEACON_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource damageSource) {
      return SoundEvents.NETHERITE_BLOCK_HIT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.BEACON_DEACTIVATE;
   }

   public void die(DamageSource cause) {
      super.die(cause);
      if (this.level() instanceof ServerLevel server) {
         Vec3 mid = this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0);
         Fx.burst(server, Fx.OPTIC_RED, mid, 18, 0.3, 0.05);
         Fx.burst(server, ParticleTypes.SMOKE, mid, 14, 0.25, 0.02);
      }
   }

   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }
}
