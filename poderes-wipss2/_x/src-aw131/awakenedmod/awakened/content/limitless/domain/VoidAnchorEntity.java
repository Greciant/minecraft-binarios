package com.awakenedmod.awakened.content.limitless.domain;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.vfx.AbstractFxEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class VoidAnchorEntity extends AbstractFxEntity {
   private static final EntityDataAccessor<Integer> DATA_CRACK = SynchedEntityData.defineId(VoidAnchorEntity.class, EntityDataSerializers.INT);
   public static final int OPEN = 1048576;
   private int castTick;
   private float radius;
   private float yaw;
   private long seed;
   private int casterId = -1;

   public VoidAnchorEntity(EntityType<? extends VoidAnchorEntity> type, Level level) {
      super(type, level);
   }

   public static VoidAnchorEntity spawn(ServerLevel level, Vec3 centre, double radius, float yaw, int casterId) {
      VoidAnchorEntity anchor = new VoidAnchorEntity(AwakenedEntities.LIMITLESS_VOID_ANCHOR.get(), level);
      anchor.setPos(centre.x, centre.y, centre.z);
      anchor.castTick = (int)level.getGameTime();
      anchor.radius = (float)radius;
      anchor.yaw = yaw;
      anchor.seed = level.random.nextLong();
      anchor.casterId = casterId;
      level.addFreshEntity(anchor);
      return anchor;
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_CRACK, 1048576);
   }

   public float since(float partialTick) {
      return (float)((int)this.level().getGameTime() - this.castTick) + partialTick;
   }

   public int castTick() {
      return this.castTick;
   }

   public int crackAt() {
      return (Integer)this.entityData.get(DATA_CRACK);
   }

   public void crackNow() {
      int at = Math.max((int)this.level().getGameTime() - this.castTick, 30);
      if (at < this.crackAt()) {
         this.entityData.set(DATA_CRACK, at);
      }
   }

   public float radius() {
      return this.radius;
   }

   public float radiusAt(float t) {
      return this.radius * VoidTimings.shell(VoidTimings.lookAge(t), (float)(this.crackAt() - 20));
   }

   public float yaw() {
      return this.yaw;
   }

   public Vec3 forward() {
      double r = Math.toRadians((double)this.yaw);
      return new Vec3(-Math.sin(r), 0.0, Math.cos(r));
   }

   public long seed() {
      return this.seed;
   }

   public int casterId() {
      return this.casterId;
   }

   public boolean viewUp(float t) {
      return t >= 20.0F && t < (float)(this.crackAt() + 24);
   }

   public boolean zoneUp(float t) {
      return t >= 0.0F && t < (float)(this.crackAt() + 24);
   }

   @Override
   public void tick() {
      this.baseTick();
   }

   public boolean isAlwaysTicking() {
      return true;
   }

   public void checkDespawn() {
      if (!this.level().isClientSide() && this.since(0.0F) > (float)(this.crackAt() + 36 + 2)) {
         this.discard();
      }
   }

   @Override
   protected void writeExtraSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeInt(this.castTick);
      buf.writeFloat(this.radius);
      buf.writeFloat(this.yaw);
      buf.writeLong(this.seed);
      buf.writeVarInt(this.casterId);
   }

   @Override
   protected void readExtraSpawnData(RegistryFriendlyByteBuf buf) {
      this.castTick = buf.readInt();
      this.radius = buf.readFloat();
      this.yaw = buf.readFloat();
      this.seed = buf.readLong();
      this.casterId = buf.readVarInt();
   }
}
