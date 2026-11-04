package com.devilfruits.fruits.yomi_yomi.entity;

import com.devilfruits.fruits.yomi_yomi.Vessel;
import com.devilfruits.fruits.yomi_yomi.soul.SoulProjection;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SoulShell extends Entity {
   private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(SoulShell.class, EntityDataSerializers.OPTIONAL_UUID);
   private static final EntityDataAccessor<Integer> DATA_VESSEL = SynchedEntityData.defineId(SoulShell.class, EntityDataSerializers.INT);
   private int hurtTicks = 100;

   public SoulShell(EntityType<? extends SoulShell> type, Level level) {
      super(type, level);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_OWNER, Optional.empty());
      builder.define(DATA_VESSEL, -1);
   }

   public void setup(ServerPlayer owner, @Nullable Vessel vessel) {
      this.entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
      this.entityData.set(DATA_VESSEL, vessel == null ? -1 : vessel.ordinal());
   }

   @Nullable
   public UUID ownerId() {
      return (UUID)((Optional)this.entityData.get(DATA_OWNER)).orElse(null);
   }

   @Nullable
   public Vessel vessel() {
      int v = (Integer)this.entityData.get(DATA_VESSEL);
      return v >= 0 && v < Vessel.values().length ? Vessel.values()[v] : null;
   }

   public int hurtTicks() {
      return this.hurtTicks;
   }

   public boolean hurt(DamageSource source, float amount) {
      if (!this.level().isClientSide() && !this.isRemoved() && !this.isInvulnerableTo(source)) {
         if (SoulProjection.forward(this, source, amount) <= 0.0F) {
            return false;
         } else {
            this.level().broadcastEntityEvent(this, (byte)2);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 0.5F, 0.6F);
            return true;
         }
      } else {
         return false;
      }
   }

   public void handleEntityEvent(byte id) {
      if (id == 2) {
         this.hurtTicks = 0;
      } else {
         super.handleEntityEvent(id);
      }
   }

   public boolean isPickable() {
      return !this.isRemoved();
   }

   public boolean isPushable() {
      return false;
   }

   public boolean canBeHitByProjectile() {
      return true;
   }

   public void tick() {
      super.tick();
      this.hurtTicks++;
      if (!this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0).scale(0.98));
         this.move(MoverType.SELF, this.getDeltaMovement());
      } else {
         this.setDeltaMovement(Vec3.ZERO);
      }

      if (!this.level().isClientSide() && this.tickCount % 10 == 0 && !SoulProjection.ownsShell((ServerLevel)this.level(), this)) {
         this.discard();
      }
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   public boolean shouldBeSaved() {
      return false;
   }
}
