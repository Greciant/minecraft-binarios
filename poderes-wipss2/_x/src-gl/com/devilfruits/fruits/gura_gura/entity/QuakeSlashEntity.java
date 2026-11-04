package com.devilfruits.fruits.gura_gura.entity;

import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.gura_gura.GuraAbilities;
import com.devilfruits.fruits.gura_gura.GuraEntities;
import com.devilfruits.fruits.gura_gura.GuraFx;
import com.devilfruits.fruits.gura_gura.GuraGura;
import com.devilfruits.fruits.gura_gura.GuraParticles;
import com.devilfruits.fruits.gura_gura.ability.QuakeDeflect;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class QuakeSlashEntity extends Projectile {
   private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(QuakeSlashEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_ROLL = SynchedEntityData.defineId(QuakeSlashEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_LIFETIME = SynchedEntityData.defineId(QuakeSlashEntity.class, EntityDataSerializers.INT);
   private float damage;
   private float knockback;
   private final IntSet hit = new IntOpenHashSet();
   private CastBudget budget = new CastBudget(12, 6);
   private final Set<UUID> structuresHit = new HashSet<>();
   private float destruction = 1.0F;

   public QuakeSlashEntity(EntityType<? extends QuakeSlashEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   public QuakeSlashEntity(
      ServerLevel level, ServerPlayer owner, Vec3 pos, Vec3 dir, float speed, float width, float roll, float damage, float knockback, float range
   ) {
      this(GuraEntities.QUAKE_SLASH.get(), level);
      this.setOwner(owner);
      this.setPos(pos);
      Vec3 velocity = dir.normalize().scale((double)speed);
      this.setDeltaMovement(velocity);
      this.setYRot((float)(Mth.atan2(velocity.x, velocity.z) * 180.0F / (float)Math.PI));
      this.setXRot((float)(Mth.atan2(velocity.y, velocity.horizontalDistance()) * 180.0F / (float)Math.PI));
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
      this.damage = damage;
      this.knockback = knockback;
      this.budget = CastBudget.fromConfig();
      this.entityData.set(DATA_WIDTH, width);
      this.entityData.set(DATA_ROLL, roll);
      this.entityData.set(DATA_LIFETIME, Math.max(4, Mth.ceil(range / speed)));
   }

   public float width() {
      return (Float)this.entityData.get(DATA_WIDTH);
   }

   public float roll() {
      return (Float)this.entityData.get(DATA_ROLL);
   }

   public int lifetime() {
      return (Integer)this.entityData.get(DATA_LIFETIME);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_WIDTH, 3.0F);
      builder.define(DATA_ROLL, 0.0F);
      builder.define(DATA_LIFETIME, 20);
   }

   public void tick() {
      super.tick();
      Vec3 start = this.position();
      Vec3 motion = this.getDeltaMovement();
      Vec3 end = start.add(motion);
      if (this.level() instanceof ServerLevel level) {
         if (this.tickCount > this.lifetime() || !(this.getOwner() instanceof ServerPlayer owner)) {
            this.discard();
            return;
         }

         BlockHitResult blockHit = PhysicsWorld.clip(level, new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, this));
         if (blockHit.getType() == Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            if (PhysicsWorld.structureAt(level, pos) != null) {
               BlockImpactService.strike(
                  owner,
                  GuraAbilities.QUAKE_INFUSION,
                  ImpactShape.sphere(blockHit.getLocation(), Math.max(1.0, (double)this.width() * 0.5)),
                  motion,
                  40.0 * ((double)this.damage + 6.0 * (double)this.knockback) * 0.8,
                  0.0,
                  this.budget,
                  this.structuresHit
               );
            }

            if (!TerrainDestruction.destructionAllowed(level)
               || !TerrainDestruction.tearOut(level, owner, pos, motion.scale(0.25).add(0.0, 0.25, 0.0), this.budget, level.random.nextInt(40), true)) {
               ServerFx.sendNear(level, GuraFx.SLASH_IMPACT, this.getId(), blockHit.getLocation(), motion.normalize(), this.width(), 0L, 0, 64.0);
               this.discard();
               return;
            }

            this.bore(level, owner, pos, motion);
         }

         float half = this.width() * 0.5F;
         QuakeDeflect.sweep(owner, ImpactShape.capsule(start, end, (double)half + 0.4), motion);
         AABB box = new AABB(start, end).inflate((double)half, (double)half, (double)half);

         for (Entity target : Targeting.inBox(level, this, box, e -> !this.hit.contains(e.getId()) && Targeting.canPush(owner, e))) {
            this.hit.add(target.getId());
            Targeting.hurt(owner, target, this.damage, GuraGura.QUAKE_DAMAGE);
            Targeting.launch(target, motion.normalize().scale((double)this.knockback).add(0.0, 0.25, 0.0));
         }
      } else {
         for (int i = 0; i < 2; i++) {
            float a = (this.random.nextFloat() - 0.5F) * this.width();
            this.level()
               .addParticle(
                  (ParticleOptions)GuraParticles.GLASS_SHARD.get(),
                  this.getX() + (double)(a * Mth.cos(this.getYRot() * (float) (Math.PI / 180.0))),
                  this.getY() + (double)(a * Mth.sin(this.roll())) * 0.5,
                  this.getZ() + (double)(a * Mth.sin(this.getYRot() * (float) (Math.PI / 180.0))),
                  motion.x * 0.1,
                  0.02,
                  motion.z * 0.1
               );
         }
      }

      this.setPos(end);
   }

   protected boolean canHitEntity(Entity target) {
      return false;
   }

   public boolean shouldBeSaved() {
      return false;
   }

   public boolean isPickable() {
      return false;
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   public QuakeSlashEntity withDestruction(float destruction) {
      this.destruction = destruction;
      this.budget = new CastBudget(GuraGura.blockBudget(destruction), (Integer)CoreConfig.MAX_DEBRIS_ENTITIES_PER_CAST.get());
      return this;
   }

   private void bore(ServerLevel level, ServerPlayer owner, BlockPos hit, Vec3 motion) {
      float radius = 1.2F * ((float)Math.sqrt((double)this.destruction) - 1.0F);
      int r = (int)Math.ceil((double)radius);

      for (int dx = -r; dx <= r; dx++) {
         for (int dy = -r; dy <= r; dy++) {
            for (int dz = -r; dz <= r; dz++) {
               if ((dx != 0 || dy != 0 || dz != 0) && (float)(dx * dx + dy * dy + dz * dz) <= radius * radius + 0.25F) {
                  TerrainDestruction.tearOut(
                     level, owner, hit.offset(dx, dy, dz), motion.scale(0.2).add(0.0, 0.25, 0.0), this.budget, level.random.nextInt(40), true, false
                  );
               }
            }
         }
      }
   }
}
