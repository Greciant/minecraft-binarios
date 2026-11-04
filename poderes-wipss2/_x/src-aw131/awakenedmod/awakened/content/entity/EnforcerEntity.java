package com.awakenedmod.awakened.content.entity;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.content.summoning.PossessMelee;
import com.awakenedmod.awakened.core.AwakenedThreat;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.relic.RelicDrops;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class EnforcerEntity extends Monster implements Enemy, RangedAttackMob, AwakenedThreat, PossessMelee {
   public static final int BARRAGE_WINDUP_TICKS = 40;
   private static final int CHARGE_COOLDOWN = 80;
   private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(EnforcerEntity.class, EntityDataSerializers.BYTE);
   private int barrageWindup = 0;
   private int chargeCooldown = 80;
   private int chargeTell = 0;

   public EnforcerEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      this.xpReward = 25;
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 90.0)
         .add(Attributes.ATTACK_DAMAGE, 9.0)
         .add(Attributes.MOVEMENT_SPEED, 0.33)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
         .add(Attributes.ARMOR, 4.0)
         .add(Attributes.FOLLOW_RANGE, 36.0);
   }

   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_PHASE, (byte)0);
   }

   public byte getPhase() {
      return (Byte)this.entityData.get(DATA_PHASE);
   }

   private void setPhase(byte phase) {
      this.entityData.set(DATA_PHASE, phase);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
      this.goalSelector.addGoal(2, new RangedAttackGoal(this, 0.9, 40, 18.0F));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 20.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
   }

   public void setTarget(LivingEntity target) {
      LivingEntity prev = this.getTarget();
      super.setTarget(target);
      if (target != null && prev == null && this.level() instanceof ServerLevel server) {
         Fx.scanLock(server, this.position().add(0.0, (double)this.getBbHeight() * 0.8, 0.0), SoundEvents.BEACON_ACTIVATE, 1.2F, 0.9F);
      }
   }

   public void performRangedAttack(LivingEntity target, float velocity) {
      if (this.level() instanceof ServerLevel server) {
         Vec3 var8 = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);

         for (int i = 0; i < 2; i++) {
            KineticProjectile bolt = new KineticProjectile(AwakenedEntities.KINETIC_CHARGE.get(), this, server, new ItemStack(Items.FIRE_CHARGE));
            bolt.configure(5.0F, 1.5F, 1.8F, 0.35F);
            Vec3 aim = var8.subtract(bolt.position()).yRot((float)Math.toRadians(((double)i - 0.5) * 8.0));
            bolt.shoot(aim.x, aim.y, aim.z, 1.7F, 2.5F);
            Fx.burst(server, Fx.OPTIC_RED, bolt.position(), 6, 0.15, 0.04);
            server.addFreshEntity(bolt);
         }

         this.barrageWindup = 0;
         this.setPhase((byte)0);
         Fx.sound(server, this.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.0F, 0.9F);
      }
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      ServerLevel level = (ServerLevel)this.level();
      LivingEntity target = this.getTarget();
      if (target != null) {
         double dsq = this.distanceToSqr(target);
         if (this.chargeTell > 0) {
            if (--this.chargeTell <= 0) {
               ThreatMoves.leapToward(this, target, 1.3, 0.5);
               if (this.getPhase() == 3) {
                  this.setPhase((byte)0);
               }

               this.chargeCooldown = 80;
            }
         } else if (--this.chargeCooldown <= 0 && this.onGround() && dsq <= 256.0 && dsq >= 16.0 && this.getPhase() == 0) {
            this.setPhase((byte)3);
            this.chargeTell = 12;
            Fx.sound(level, this.position(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.9F, 0.8F);
            Fx.burst(level, Fx.OPTIC_RED, this.position().add(0.0, (double)this.getBbHeight() * 0.6, 0.0), 8, 0.4, 0.04);
         }
      }

      if (target != null && this.distanceToSqr(target) <= 324.0) {
         if (this.barrageWindup < 40) {
            this.barrageWindup++;
         }

         if (this.barrageWindup >= 39 && this.getPhase() == 0) {
            this.setPhase((byte)1);
         }
      } else if (this.barrageWindup > 0) {
         this.barrageWindup = 0;
         if (this.getPhase() == 1) {
            this.setPhase((byte)0);
         }
      }
   }

   public boolean doHurtTarget(Entity target) {
      ServerLevel level = (ServerLevel)this.level();
      boolean hit = super.doHurtTarget(target);
      if (hit) {
         Fx.burst(level, ParticleTypes.EXPLOSION, target.position().add(0.0, 0.5, 0.0), 2, 0.2, 0.04);
         Fx.sound(level, this.position(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.7F, 0.9F);
      }

      return hit;
   }

   @Override
   public double bonusMastery() {
      return 40.0;
   }

   @Override
   public double bonusXp() {
      return 120.0;
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.WARDEN_HEARTBEAT;
   }

   protected SoundEvent getHurtSound(DamageSource damageSource) {
      return SoundEvents.NETHERITE_BLOCK_HIT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.WARDEN_DEATH;
   }

   public void die(DamageSource cause) {
      super.die(cause);
      if (this.level() instanceof ServerLevel server) {
         Vec3 mid = this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0);
         Fx.burst(server, ParticleTypes.SCULK_SOUL, mid, 20, 0.6, 0.1);
         Fx.burst(server, ParticleTypes.SMOKE, mid, 16, 0.4, 0.05);
         if (this.getRandom().nextFloat() < 0.55F) {
            RelicDrops.dropAt(server, this.position().add(0.0, 1.0, 0.0), this.getRandom(), Rarity.COMMON, Rarity.RARE, false);
         }
      }
   }
}
