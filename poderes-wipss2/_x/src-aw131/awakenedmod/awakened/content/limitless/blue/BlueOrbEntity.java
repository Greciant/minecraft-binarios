package com.awakenedmod.awakened.content.limitless.blue;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.limitless.LimitlessFx;
import com.awakenedmod.awakened.content.vfx.AbstractFxEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class BlueOrbEntity extends AbstractFxEntity {
   private static final EntityDataAccessor<Integer> DATA_RELEASE_AT = SynchedEntityData.defineId(BlueOrbEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_ENDING = SynchedEntityData.defineId(BlueOrbEntity.class, EntityDataSerializers.INT);
   public static final int OPEN = Integer.MAX_VALUE;
   private int born;
   private long seed;
   private boolean collapsed;

   public BlueOrbEntity(EntityType<? extends BlueOrbEntity> type, Level level) {
      super(type, level);
   }

   public static BlueOrbEntity spawn(ServerLevel level, Vec3 at, boolean held) {
      BlueOrbEntity orb = new BlueOrbEntity(AwakenedEntities.LIMITLESS_BLUE_ORB.get(), level);
      orb.setPos(at.x, at.y, at.z);
      orb.born = (int)level.getGameTime();
      orb.seed = level.random.nextLong();
      orb.entityData.set(DATA_RELEASE_AT, held ? Integer.MAX_VALUE : 40);
      orb.entityData.set(DATA_ENDING, (held ? BlueEnd.FADE : BlueEnd.IMPLODE).ordinal());
      level.addFreshEntity(orb);
      LimitlessFx.orbOpened(level, at, held);
      return orb;
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_RELEASE_AT, 40);
      builder.define(DATA_ENDING, BlueEnd.FADE.ordinal());
   }

   public float age(float partialTick) {
      return (float)((int)this.level().getGameTime() - this.born) + partialTick;
   }

   public int releaseAt() {
      return (Integer)this.entityData.get(DATA_RELEASE_AT);
   }

   public long seed() {
      return this.seed;
   }

   public BlueEnd ending() {
      return BlueEnd.of((Integer)this.entityData.get(DATA_ENDING));
   }

   public void letGo() {
      int age = (int)this.age(0.0F);
      if (!this.level().isClientSide() && this.releaseAt() > age) {
         this.entityData.set(DATA_RELEASE_AT, Math.max(8, age));
         this.entityData.set(DATA_ENDING, BlueEnd.FADE.ordinal());
      }
   }

   void pop() {
      int age = (int)this.age(0.0F);
      if (!this.level().isClientSide() && this.releaseAt() > age) {
         this.entityData.set(DATA_RELEASE_AT, age);
         this.entityData.set(DATA_ENDING, BlueEnd.POP.ordinal());
      }
   }

   @Override
   public void tick() {
      this.baseTick();
      if (this.level() instanceof ServerLevel server) {
         float age = this.age(0.0F);
         int release = this.releaseAt();
         if (age >= (float)release) {
            if (!this.collapsed) {
               this.collapsed = true;
               LimitlessFx.orbCollapsed(server, this);
            }

            if (age >= (float)(release + 12)) {
               this.discard();
            }
         } else {
            LimitlessFx.orbHum(this, Mth.clamp(age / 8.0F, 0.0F, 1.0F));
         }
      }
   }

   @Override
   protected void writeExtraSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeInt(this.born);
      buf.writeLong(this.seed);
   }

   @Override
   protected void readExtraSpawnData(RegistryFriendlyByteBuf buf) {
      this.born = buf.readInt();
      this.seed = buf.readLong();
   }
}
