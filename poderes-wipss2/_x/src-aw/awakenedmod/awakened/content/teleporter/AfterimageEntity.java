package com.awakenedmod.awakened.content.teleporter;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

public final class AfterimageEntity extends Entity implements IEntityWithComplexSpawn {
   private UUID playerId = new UUID(0L, 0L);
   private float yawDeg;
   private int lifetime = 14;

   public AfterimageEntity(EntityType<? extends AfterimageEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
   }

   public void configure(UUID playerId, float yawDeg, int lifetime) {
      this.playerId = playerId != null ? playerId : new UUID(0L, 0L);
      this.yawDeg = yawDeg;
      this.lifetime = lifetime;
   }

   public UUID playerId() {
      return this.playerId;
   }

   public float yawDeg() {
      return this.yawDeg;
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

   public boolean hurt(DamageSource source, float amount) {
      return false;
   }

   protected void defineSynchedData(Builder builder) {
   }

   public void writeSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeUUID(this.playerId);
      buf.writeFloat(this.yawDeg);
      buf.writeVarInt(this.lifetime);
   }

   public void readSpawnData(RegistryFriendlyByteBuf buf) {
      this.playerId = buf.readUUID();
      this.yawDeg = buf.readFloat();
      this.lifetime = buf.readVarInt();
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }
}
