package com.devilfruits.core.world;

import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.registry.DFEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DebrisEntity extends Entity {
   private static final EntityDataAccessor<BlockState> DATA_STATE = SynchedEntityData.defineId(DebrisEntity.class, EntityDataSerializers.BLOCK_STATE);
   private static final int MAX_AGE = 100;
   public final float spinX;
   public final float spinZ;

   public DebrisEntity(EntityType<? extends DebrisEntity> type, Level level) {
      super(type, level);
      this.spinX = (level.getRandom().nextFloat() - 0.5F) * 40.0F;
      this.spinZ = (level.getRandom().nextFloat() - 0.5F) * 40.0F;
   }

   public DebrisEntity(ServerLevel level, double x, double y, double z, BlockState state, Vec3 velocity) {
      this(DFEntities.DEBRIS.get(), level);
      this.setPos(x, y, z);
      this.setDeltaMovement(velocity);
      this.xo = x;
      this.yo = y;
      this.zo = z;
      this.entityData.set(DATA_STATE, state);
   }

   public BlockState getBlockState() {
      return (BlockState)this.entityData.get(DATA_STATE);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_STATE, Blocks.STONE.defaultBlockState());
   }

   protected double getDefaultGravity() {
      return 0.05;
   }

   public void tick() {
      super.tick();
      this.applyGravity();
      this.move(MoverType.SELF, this.getDeltaMovement());
      this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
      if (!this.level().isClientSide()) {
         boolean landed = this.tickCount > 4 && (this.onGround() || this.horizontalCollision);
         if (landed || this.isInWater() || this.tickCount > 100) {
            ServerFx.blockBurst((ServerLevel)this.level(), this.position().add(0.0, 0.5, 0.0), this.getBlockState(), landed ? 1.0F : 0.5F);
            this.discard();
         }
      }
   }

   public boolean shouldBeSaved() {
      return false;
   }

   public boolean isPickable() {
      return false;
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.contains("state")) {
         this.entityData.set(DATA_STATE, NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), tag.getCompound("state")));
      }
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.put("state", NbtUtils.writeBlockState(this.getBlockState()));
   }
}
