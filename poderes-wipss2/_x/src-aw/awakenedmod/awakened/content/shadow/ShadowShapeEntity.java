package com.awakenedmod.awakened.content.shadow;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

public final class ShadowShapeEntity extends Entity implements IEntityWithComplexSpawn {
   private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(ShadowShapeEntity.class, EntityDataSerializers.FLOAT);
   private ShadowShapeEntity.Shape shape = ShadowShapeEntity.Shape.SPHERE;
   private int color = 9064447;
   private int alpha = 150;
   private int lifetime = 0;
   private float height = 0.0F;

   public ShadowShapeEntity(EntityType<? extends ShadowShapeEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
   }

   public void configure(ShadowShapeEntity.Shape shape, float radius, int color, int alpha, int lifetime) {
      this.shape = shape;
      this.color = color;
      this.alpha = alpha;
      this.lifetime = lifetime;
      this.setRadius(radius);
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public float height() {
      return this.height;
   }

   public void setRadius(float radius) {
      this.entityData.set(RADIUS, radius);
   }

   public float radius() {
      return (Float)this.entityData.get(RADIUS);
   }

   public ShadowShapeEntity.Shape shape() {
      return this.shape;
   }

   public int color() {
      return this.color;
   }

   public int alpha() {
      return this.alpha;
   }

   public int lifetime() {
      return this.lifetime;
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.lifetime > 0 && this.tickCount >= this.lifetime) {
         this.discard();
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      return false;
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(RADIUS, 1.0F);
   }

   public void writeSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeVarInt(this.shape.ordinal());
      buf.writeInt(this.color);
      buf.writeVarInt(this.alpha);
      buf.writeVarInt(this.lifetime);
      buf.writeFloat(this.height);
   }

   public void readSpawnData(RegistryFriendlyByteBuf buf) {
      this.shape = ShadowShapeEntity.Shape.values()[buf.readVarInt()];
      this.color = buf.readInt();
      this.alpha = buf.readVarInt();
      this.lifetime = buf.readVarInt();
      this.height = buf.readFloat();
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   public static enum Shape {
      SPHERE,
      DISC,
      COILS;
   }
}
