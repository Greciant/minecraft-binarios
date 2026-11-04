package com.awakenedmod.awakened.content.sun;

import com.awakenedmod.awakened.content.vfx.AbstractFxEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class SunOrbEntity extends AbstractFxEntity {
   private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(SunOrbEntity.class, EntityDataSerializers.FLOAT);

   public SunOrbEntity(EntityType<? extends SunOrbEntity> type, Level level) {
      super(type, level);
   }

   public void configure(float radius, int lifetime) {
      this.setRadius(radius);
      this.lifetime = lifetime;
   }

   public void setRadius(float radius) {
      this.entityData.set(RADIUS, radius);
   }

   public float radius() {
      return (Float)this.entityData.get(RADIUS);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(RADIUS, 4.0F);
   }

   @Override
   protected void writeExtraSpawnData(RegistryFriendlyByteBuf buf) {
   }

   @Override
   protected void readExtraSpawnData(RegistryFriendlyByteBuf buf) {
   }
}
