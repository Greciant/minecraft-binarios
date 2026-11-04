package com.devilfruits.fruits.horo_horo.entity;

import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.fruits.horo_horo.ghost.GhostForm;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GhostBody extends Entity {
   private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(GhostBody.class, EntityDataSerializers.OPTIONAL_UUID);
   private int hurtTicks = 100;

   public GhostBody(EntityType<? extends GhostBody> type, Level level) {
      super(type, level);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_OWNER, Optional.empty());
   }

   public void setup(ServerPlayer owner) {
      this.entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
   }

   @Nullable
   public UUID ownerId() {
      return (UUID)((Optional)this.entityData.get(DATA_OWNER)).orElse(null);
   }

   public int hurtTicks() {
      return this.hurtTicks;
   }

   public boolean hurt(DamageSource source, float amount) {
      if (!this.level().isClientSide() && !this.isRemoved() && !this.isInvulnerableTo(source)) {
         if (GhostForm.forward(this, source, amount) <= 0.0F) {
            return false;
         } else {
            this.level().broadcastEntityEvent(this, (byte)2);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.6F, 0.8F);
            return true;
         }
      } else {
         return false;
      }
   }

   public InteractionResult interact(Player player, InteractionHand hand) {
      if (this.level().isClientSide()) {
         return player.getUUID().equals(this.ownerId()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
      } else {
         if (player instanceof ServerPlayer sp && GhostForm.requestPossession(sp, this)) {
            return InteractionResult.CONSUME;
         }

         return InteractionResult.PASS;
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
      Vec3 carried = this.level().isClientSide() ? Vec3.ZERO : PhysicsWorld.carrierVelocity(this);
      if (!this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0).scale(0.98));
      } else {
         this.setDeltaMovement(Vec3.ZERO);
      }

      Vec3 move = this.getDeltaMovement().add(carried);
      if (move.lengthSqr() > 1.0E-8) {
         this.move(MoverType.SELF, move);
      }

      if (!this.level().isClientSide() && this.tickCount % 10 == 0 && !GhostForm.ownsBody((ServerLevel)this.level(), this)) {
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
