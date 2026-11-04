package com.awakenedmod.awakened.content.vfx;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

public abstract class AbstractFxEntity extends Entity implements IEntityWithComplexSpawn {
   protected int lifetime = 20;

   protected AbstractFxEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
   }

   public int lifetime() {
      return this.lifetime;
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.tickCount >= this.lifetime) {
         this.discard();
      }
   }

   public boolean shouldBeSaved() {
      return false;
   }

   public final boolean hurt(DamageSource source, float amount) {
      return false;
   }

   protected void defineSynchedData(Builder builder) {
   }

   public final void writeSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeVarInt(this.lifetime);
      this.writeExtraSpawnData(buf);
   }

   public final void readSpawnData(RegistryFriendlyByteBuf buf) {
      this.lifetime = buf.readVarInt();
      this.readExtraSpawnData(buf);
   }

   protected abstract void writeExtraSpawnData(RegistryFriendlyByteBuf var1);

   protected abstract void readExtraSpawnData(RegistryFriendlyByteBuf var1);

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }
}
