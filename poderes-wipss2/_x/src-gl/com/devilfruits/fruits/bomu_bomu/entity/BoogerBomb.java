package com.devilfruits.fruits.bomu_bomu.entity;

import com.devilfruits.core.fx.ExplosionProfile;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.world.AbilityEntity;
import com.devilfruits.fruits.bomu_bomu.BomuAbilities;
import com.devilfruits.fruits.bomu_bomu.BomuEntities;
import com.devilfruits.fruits.bomu_bomu.BomuFx;
import com.devilfruits.fruits.bomu_bomu.BomuSounds;
import com.devilfruits.fruits.bomu_bomu.explosion.BombExplosionService;
import com.devilfruits.fruits.bomu_bomu.explosion.ExplosionSpec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class BoogerBomb extends ThrowableProjectile implements AbilityEntity {
   private static final EntityDataAccessor<Boolean> STUCK = SynchedEntityData.defineId(BoogerBomb.class, EntityDataSerializers.BOOLEAN);
   public static final double GRAVITY = 0.03;
   private static final int MAX_LIFE = 80;
   @Nullable
   private ExplosionSpec spec;
   private int fuse;
   private int life;
   private int stuckTicks = -1;
   private Vec3 normal = new Vec3(0.0, 1.0, 0.0);
   @Nullable
   private BlockPos stuckBlock;
   @Nullable
   private Entity stuckTo;
   private Vec3 stuckOffset = Vec3.ZERO;
   @Nullable
   private PhysicsStructure stuckHull;
   private Vec3 hullLocal = Vec3.ZERO;
   private Vec3 hullNormal = new Vec3(0.0, 1.0, 0.0);

   public BoogerBomb(EntityType<? extends BoogerBomb> type, Level level) {
      super(type, level);
   }

   public static BoogerBomb shoot(ServerPlayer owner, Vec3 from, Vec3 velocity, ExplosionSpec spec, int fuse) {
      BoogerBomb b = new BoogerBomb(BomuEntities.BOOGER.get(), owner.level());
      b.setOwner(owner);
      b.setPos(from);
      b.setDeltaMovement(velocity);
      b.spec = spec;
      b.fuse = fuse;
      owner.level().addFreshEntity(b);
      return b;
   }

   public static int active(ServerPlayer owner) {
      return owner.serverLevel()
         .getEntities((EntityTypeTest)BomuEntities.BOOGER.get(), owner.getBoundingBox().inflate(128.0), b -> b.getOwner() == owner)
         .size();
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(STUCK, false);
   }

   public boolean stuck() {
      return (Boolean)this.entityData.get(STUCK);
   }

   protected double getDefaultGravity() {
      return 0.03;
   }

   public void tick() {
      if (this.stuckTicks < 0 && !this.stuck()) {
         super.tick();
         if (!this.level().isClientSide() && ++this.life > 80) {
            this.detonate();
         }
      } else {
         this.xo = this.getX();
         this.yo = this.getY();
         this.zo = this.getZ();
         this.setDeltaMovement(Vec3.ZERO);
         if (!this.level().isClientSide()) {
            this.stuckTicks++;
            if (this.stuckTo != null) {
               if (!this.stuckTo.isAlive()) {
                  this.detonate();
                  return;
               }

               this.setPos(this.stuckTo.position().add(this.stuckOffset));
            } else if (this.stuckHull != null) {
               if (!this.stuckHull.isValid() || this.stuckBlock != null && this.level().getBlockState(this.stuckBlock).isAir()) {
                  this.detonate();
                  return;
               }

               this.setPos(this.stuckHull.toWorld(this.hullLocal));
               this.normal = this.stuckHull.dirToWorld(this.hullNormal);
            } else if (this.stuckBlock != null && this.level().getBlockState(this.stuckBlock).isAir()) {
               this.detonate();
               return;
            }

            if (this.stuckTicks >= this.fuse) {
               this.detonate();
            }
         }
      }
   }

   protected boolean canHitEntity(Entity target) {
      return !(target instanceof AbilityEntity) && super.canHitEntity(target);
   }

   protected void onHitBlock(BlockHitResult hit) {
      super.onHitBlock(hit);
      Vec3 n = PhysicsWorld.worldNormal(this.level(), hit);
      Vec3 at = PhysicsWorld.worldLocation(this.level(), hit).add(n.scale(0.06));
      PhysicsStructure hull = PhysicsWorld.structureAt(this.level(), hit.getBlockPos());
      if (hull != null) {
         this.stuckHull = hull;
         this.hullLocal = hull.toLocal(at);
         this.hullNormal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
      }

      this.stick(at, n);
      this.stuckBlock = hit.getBlockPos();
   }

   protected void onHitEntity(EntityHitResult hit) {
      super.onHitEntity(hit);
      Entity target = hit.getEntity();
      AABB box = target.getBoundingBox();
      Vec3 at = hit.getLocation();
      Vec3 inside = new Vec3(
         Math.max(box.minX, Math.min(box.maxX, at.x)), Math.max(box.minY, Math.min(box.maxY, at.y)), Math.max(box.minZ, Math.min(box.maxZ, at.z))
      );
      Vec3 n = this.getDeltaMovement().lengthSqr() > 1.0E-6 ? this.getDeltaMovement().normalize().scale(-1.0) : new Vec3(0.0, 1.0, 0.0);
      this.stick(inside, n);
      this.stuckTo = target;
      this.stuckOffset = inside.subtract(target.position());
   }

   private void stick(Vec3 at, Vec3 n) {
      this.setPos(at);
      this.setDeltaMovement(Vec3.ZERO);
      this.normal = n;
      this.stuckTicks = 0;
      if (!this.level().isClientSide()) {
         this.entityData.set(STUCK, true);
         this.level().playSound(null, at.x, at.y, at.z, BomuSounds.CANNON_SPLAT.get(), SoundSource.PLAYERS, 0.7F, 0.9F + this.random.nextFloat() * 0.3F);
         ServerFx.sendNear((ServerLevel)this.level(), BomuFx.SPLAT, this.getId(), at, n, 1.0F, this.random.nextLong(), 0, 48.0);
         if (this.fuse <= 0) {
            this.detonate();
         }
      }
   }

   private void detonate() {
      if (!this.isRemoved() && this.level() instanceof ServerLevel server) {
         this.discard();
         ServerPlayer owner = this.getOwner() instanceof ServerPlayer p ? p : null;
         ExplosionSpec s = this.spec != null
            ? this.spec
            : new ExplosionSpec(1.6F, 1.6F, 0.5F, 4.0F, 0.5F, 0.0F, ExplosionProfile.SMALL, BomuAbilities.CANNON.id());
         Vec3 at = this.position().add(this.normal.scale(0.15));
         if (this.stuckTo instanceof LivingEntity) {
            at = this.position();
         }

         BombExplosionService.detonate(server, at, this.normal, s, owner);
      }
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.spec != null) {
         tag.put("Spec", this.spec.save());
      }

      tag.putInt("Fuse", this.fuse);
      tag.putInt("Life", this.life);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.spec = tag.contains("Spec") ? ExplosionSpec.load(tag.getCompound("Spec")) : null;
      this.fuse = tag.getInt("Fuse");
      this.life = tag.getInt("Life");
   }
}
