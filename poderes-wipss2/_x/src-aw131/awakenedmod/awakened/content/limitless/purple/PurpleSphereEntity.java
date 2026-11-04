package com.awakenedmod.awakened.content.limitless.purple;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.limitless.LimitlessBufs;
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

public final class PurpleSphereEntity extends AbstractFxEntity {
   private static final EntityDataAccessor<Integer> DATA_GONE = SynchedEntityData.defineId(PurpleSphereEntity.class, EntityDataSerializers.INT);
   private static final float GRIND_FULL = 40.0F;
   private static final float GRIND_DECAY = 0.8F;
   private int casterId = -1;
   private int releaseTick;
   private int chargeStart;
   private Vec3 merge = Vec3.ZERO;
   private Vec3 fwd = new Vec3(0.0, 0.0, 1.0);
   private long seed;
   private PurpleStrength strength = PurpleStrength.NORMAL;
   private boolean quiet;
   private float grind;

   public PurpleSphereEntity(EntityType<? extends PurpleSphereEntity> type, Level level) {
      super(type, level);
   }

   public static PurpleSphereEntity spawn(
      ServerLevel level, int casterId, int chargeStart, int releaseTick, Vec3 merge, Vec3 fwd, long seed, PurpleStrength strength
   ) {
      PurpleSphereEntity e = new PurpleSphereEntity(AwakenedEntities.LIMITLESS_PURPLE_SPHERE.get(), level);
      e.setPos(merge.x, merge.y, merge.z);
      e.casterId = casterId;
      e.chargeStart = chargeStart;
      e.releaseTick = releaseTick;
      e.merge = merge;
      e.fwd = fwd;
      e.seed = seed;
      e.strength = strength;
      e.entityData.set(DATA_GONE, strength.gone());
      e.lifetime = strength.gone() - 50 + strength.afterLife() + 5;
      level.addFreshEntity(e);
      return e;
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_GONE, PurpleStrength.NORMAL.gone());
   }

   public int casterId() {
      return this.casterId;
   }

   public int releaseTick() {
      return this.releaseTick;
   }

   public Vec3 merge() {
      return this.merge;
   }

   public Vec3 fwd() {
      return this.fwd;
   }

   public long seed() {
      return this.seed;
   }

   public PurpleStrength strength() {
      return this.strength;
   }

   public int gone() {
      return (Integer)this.entityData.get(DATA_GONE);
   }

   public float motion(float partialTick) {
      return (float)((int)this.level().getGameTime() - this.chargeStart) + partialTick;
   }

   public float age(float partialTick) {
      return (float)(50 + ((int)this.level().getGameTime() - this.releaseTick)) + partialTick;
   }

   public Vec3 centre(float partialTick) {
      return this.strength.centre(this.merge, this.fwd, (double)this.age(partialTick), this.gone());
   }

   public void cutShort() {
      int age = (int)this.age(0.0F);
      if (!this.level().isClientSide() && age < this.gone()) {
         this.entityData.set(DATA_GONE, Math.max(50, age));
      }
   }

   void erased(int cells) {
      this.grind = Math.min(1.0F, this.grind + (float)cells / 40.0F);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel level) {
         Vec3 var5 = this.centre(0.0F);
         this.setPos(var5.x, var5.y, var5.z);
         int age = (int)this.age(0.0F);
         if (age < this.gone()) {
            float flown = (float)(age - 50) / (float)Math.max(1, this.gone() - 50);
            PurpleVoices.flying(this, flown, this.grind, this.tickCount);
            this.grind *= 0.8F;
         } else {
            if (!this.quiet) {
               this.quiet = true;
               PurpleVoices.gone(level, this);
            }

            PurpleVoices.after(level, this, age - this.gone());
         }
      }
   }

   @Override
   protected void writeExtraSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeVarInt(this.casterId);
      buf.writeInt(this.releaseTick);
      buf.writeInt(this.chargeStart);
      LimitlessBufs.writeVec(buf, this.merge);
      LimitlessBufs.writeVec(buf, this.fwd);
      buf.writeLong(this.seed);
      buf.writeVarInt(this.strength.tier());
   }

   @Override
   protected void readExtraSpawnData(RegistryFriendlyByteBuf buf) {
      this.casterId = buf.readVarInt();
      this.releaseTick = buf.readInt();
      this.chargeStart = buf.readInt();
      this.merge = LimitlessBufs.readVec(buf);
      this.fwd = LimitlessBufs.readVec(buf);
      this.seed = buf.readLong();
      this.strength = PurpleStrength.of(buf.readVarInt());
   }
}
