package com.devilfruits.fruits.kage_kage.entity;

import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.npc.Locomotion;
import com.devilfruits.fruits.kage_kage.KageFx;
import com.devilfruits.fruits.kage_kage.ability.DoppelmanAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Doppelman extends ShadowServant {
   public static final int RISE_TICKS = 40;
   public static final int SINK_TICKS = 20;
   public static final int ATTACK_TICKS = 12;
   public static final int ATTACK_HIT = 5;
   public static final int STATE_STANDING = 0;
   public static final int STATE_RISING = 1;
   public static final int STATE_SINKING = 2;
   private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(Doppelman.class, EntityDataSerializers.BYTE);
   private int stateTicks;
   private int ownerMissing;
   private double leash = 24.0;

   public Doppelman(EntityType<? extends Doppelman> type, Level level) {
      super(type, level);
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 80.0)
         .add(Attributes.ATTACK_DAMAGE, 3.5)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.FOLLOW_RANGE, 32.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_STATE, (byte)1);
   }

   @Override
   public int rank() {
      return 1;
   }

   @Override
   protected int variantCount() {
      return 1;
   }

   @Override
   public Locomotion.Speeds locomotionSpeeds() {
      return new Locomotion.Speeds(1.6F, 4.5F);
   }

   @Override
   public int attackTicks() {
      return 12;
   }

   @Override
   public int attackHit() {
      return 5;
   }

   public void setOwner(ServerPlayer owner, double leashBlocks) {
      this.setOwner(owner);
      this.leash = leashBlocks;
   }

   public int state() {
      return (Byte)this.entityData.get(DATA_STATE);
   }

   private void setState(int state) {
      this.entityData.set(DATA_STATE, (byte)state);
      this.stateTicks = 0;
   }

   public int stateTicks() {
      return this.stateTicks;
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (DATA_STATE.equals(key)) {
         this.stateTicks = 0;
      }
   }

   @Override
   public boolean standing() {
      return this.state() == 0 && this.isAlive();
   }

   public void recall() {
      if (this.state() != 2 && this.isAlive()) {
         this.setState(2);
         this.setTarget(null);
         this.getNavigation().stop();
         ServerFx.send(this, KageFx.DOPPEL_POOL, this.position(), Vec3.ZERO, 1.1F, this.getRandom().nextLong(), 26);
      }
   }

   @Override
   public void tick() {
      super.tick();
      this.stateTicks++;
      if (!this.level().isClientSide()) {
         int state = this.state();
         if (state == 1 && this.stateTicks >= 40) {
            this.setState(0);
         } else if (state == 2 && this.stateTicks >= 20) {
            this.discard();
            return;
         }

         ServerPlayer owner = this.owner();
         if (owner != null && owner.level() == this.level() && owner.isAlive()) {
            this.ownerMissing = 0;
            if (state == 0 && this.distanceToSqr(owner) > this.leash * this.leash) {
               this.stepBack(owner);
            }
         } else {
            if (++this.ownerMissing > 40) {
               this.discard();
            }
         }
      }
   }

   private void stepBack(ServerPlayer owner) {
      for (int i = 0; i < 12; i++) {
         double a = this.getRandom().nextDouble() * Math.PI * 2.0;
         BlockPos at = BlockPos.containing(owner.getX() + Math.cos(a) * 2.5, owner.getY(), owner.getZ() + Math.sin(a) * 2.5);
         if (this.level().noCollision(this, this.getBoundingBox().move(Vec3.atBottomCenterOf(at).subtract(this.position())))) {
            ServerFx.send(this, KageFx.DOPPEL_POOL, this.position(), Vec3.ZERO, 1.0F, this.getRandom().nextLong(), 14);
            this.teleportTo((double)at.getX() + 0.5, (double)at.getY(), (double)at.getZ() + 0.5);
            this.getNavigation().stop();
            ServerFx.send(this, KageFx.DOPPEL_POOL, this.position(), Vec3.ZERO, 1.0F, this.getRandom().nextLong(), 14);
            return;
         }
      }
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      return this.state() == 1 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) ? false : super.hurt(source, amount);
   }

   @Override
   public void die(DamageSource source) {
      boolean first = !this.dead && !this.isRemoved();
      super.die(source);
      if (first && !this.level().isClientSide()) {
         ServerFx.send(this, KageFx.DOPPEL_POOL, this.position(), Vec3.ZERO, 1.1F, this.getRandom().nextLong(), 34);
         ServerPlayer owner = this.owner();
         if (owner != null) {
            DoppelmanAbility.onDeath(owner);
         }
      }
   }

   protected SoundEvent getAmbientSound() {
      return null;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.ENDERMAN_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ENDERMAN_DEATH;
   }

   public float getVoicePitch() {
      return 0.6F;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putDouble("Leash", this.leash);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.leash = tag.contains("Leash") ? tag.getDouble("Leash") : 24.0;
      this.entityData.set(DATA_STATE, (byte)0);
   }
}
