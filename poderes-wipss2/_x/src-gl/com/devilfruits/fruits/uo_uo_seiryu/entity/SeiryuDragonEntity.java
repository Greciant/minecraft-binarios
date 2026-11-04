package com.devilfruits.fruits.uo_uo_seiryu.entity;

import com.devilfruits.core.body.BodyShell;
import com.devilfruits.core.body.SerpentBody;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.fruits.uo_uo_seiryu.DragonShells;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuEntities;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.neoforged.neoforge.entity.PartEntity;

public class SeiryuDragonEntity extends Entity implements BodyShell {
   public static final int MAX_PARTS = 64;
   public static final float THICKNESS = 1.3F;
   public static IntSupplier localPlayerId = () -> Integer.MIN_VALUE;
   private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(SeiryuDragonEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Byte> PART_COUNT = SynchedEntityData.defineId(SeiryuDragonEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(SeiryuDragonEntity.class, EntityDataSerializers.FLOAT);
   private final SeiryuBodyPart[] parts = new SeiryuBodyPart[64];
   private final double[] point = new double[3];
   @Nullable
   private SerpentBody body;
   @Nullable
   private UUID ownerUuid;
   private WeakReference<Player> ownerRef = new WeakReference<>(null);

   public SeiryuDragonEntity(EntityType<? extends SeiryuDragonEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
      this.noCulling = true;

      for (int i = 0; i < 64; i++) {
         this.parts[i] = new SeiryuBodyPart(this, i);
      }

      this.setId(ENTITY_COUNTER.getAndAdd(65) + 1);
   }

   public static SeiryuDragonEntity create(ServerPlayer owner) {
      SeiryuDragonEntity shell = new SeiryuDragonEntity(SeiryuEntities.DRAGON_BODY.get(), owner.level());
      shell.ownerUuid = owner.getUUID();
      shell.ownerRef = new WeakReference<>(owner);
      shell.entityData.set(OWNER_ID, owner.getId());
      shell.entityData.set(PART_COUNT, (byte)Mth.clamp((int)UoUoSeiryu.fruit().config().setting("dragonHitboxParts"), 1, 64));
      shell.entityData.set(LENGTH, (float)UoUoSeiryu.fruit().config().setting("dragonBodyLength"));
      shell.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
      return shell;
   }

   public void setId(int id) {
      super.setId(id);

      for (int i = 0; i < 64; i++) {
         this.parts[i].setId(id + i + 1);
      }
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(OWNER_ID, -1);
      builder.define(PART_COUNT, (byte)42);
      builder.define(LENGTH, 140.0F);
   }

   @Nullable
   @Override
   public UUID bodyOwner() {
      Player owner = this.owner();
      return owner != null ? owner.getUUID() : this.ownerUuid;
   }

   @Nullable
   @Override
   public Entity bodyOwnerEntity() {
      return this.owner();
   }

   @Nullable
   public Player owner() {
      if (!this.level().isClientSide && this.ownerUuid != null) {
         Player p = this.ownerRef.get();
         return p != null && !p.isRemoved() ? p : this.level().getPlayerByUUID(this.ownerUuid);
      } else {
         return this.level().getEntity((Integer)this.entityData.get(OWNER_ID)) instanceof Player p ? p : null;
      }
   }

   public int ownerId() {
      return (Integer)this.entityData.get(OWNER_ID);
   }

   public boolean ownedByLocalPlayer() {
      return (Integer)this.entityData.get(OWNER_ID) == localPlayerId.getAsInt();
   }

   public int partCount() {
      return Mth.clamp((Byte)this.entityData.get(PART_COUNT), 1, 64);
   }

   @Nullable
   public SerpentBody body() {
      return this.body;
   }

   public void tick() {
      super.tick();
      Player owner = this.owner();
      if (!this.level().isClientSide) {
         boolean dying = owner != null && owner.isDeadOrDying();
         if (owner == null || owner.isRemoved() || owner.level() != this.level() || !dying && !UoUoSeiryu.isIn(owner, "dragon")) {
            this.discard();
            return;
         }
      }

      if (owner != null) {
         if (this.level().isClientSide) {
            owner.noCulling = true;
         }

         this.setPos(owner.getX(), owner.getY(), owner.getZ());
         boolean flying = FormFlags.has(owner, 2);
         if (this.body == null) {
            this.body = new SerpentBody((Float)this.entityData.get(LENGTH), 1.3F);
            this.body.reset(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), flying);
         }

         if (!owner.isDeadOrDying()) {
            boolean twisting = FormFlags.has(owner, DragonShells.TWISTING);
            this.body
               .setTwisting(twisting, twisting ? (double)this.level().getHeight(Types.MOTION_BLOCKING_NO_LEAVES, owner.getBlockX(), owner.getBlockZ()) : 0.0);
            this.body.tick(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), flying);
         }

         this.placeParts();
      }
   }

   public void onClientRemoval() {
      Player owner = this.owner();
      if (owner != null) {
         owner.noCulling = false;
      }
   }

   private void placeParts() {
      int n = this.partCount();
      SerpentBody b = this.body;

      for (int i = 0; i < 64; i++) {
         SeiryuBodyPart part = this.parts[i];
         if (i >= n) {
            part.deactivate(this.getX(), this.getY(), this.getZ());
         } else {
            float t = ((float)i + 0.5F) / (float)n;
            b.point((double)(b.neck + b.length * t), 1.0F, this.point);
            part.place(this.point[0], this.point[1], this.point[2], b.radiusAt(t) * 1.7F);
         }
      }
   }

   boolean hurtFromPart(SeiryuBodyPart part, DamageSource source, float amount) {
      Player owner = this.owner();
      return owner != null && source.getEntity() != owner && source.getDirectEntity() != owner ? owner.hurt(source, amount) : false;
   }

   public boolean isMultipartEntity() {
      return true;
   }

   public PartEntity<?>[] getParts() {
      return this.parts;
   }

   public boolean hurt(DamageSource source, float amount) {
      return false;
   }

   public boolean isPickable() {
      return false;
   }

   public boolean isPushable() {
      return false;
   }

   public boolean canBeCollidedWith() {
      return false;
   }

   public boolean shouldBeSaved() {
      return false;
   }

   public boolean isAttackable() {
      return false;
   }

   public boolean is(Entity entity) {
      if (entity == this) {
         return true;
      } else {
         if (entity instanceof Player p && p.getId() == this.ownerId()) {
            return true;
         }

         return false;
      }
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }
}
