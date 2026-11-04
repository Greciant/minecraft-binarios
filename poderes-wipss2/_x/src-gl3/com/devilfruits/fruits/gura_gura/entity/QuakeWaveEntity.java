package com.devilfruits.fruits.gura_gura.entity;

import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.AbilityEntity;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.gura_gura.GuraAbilities;
import com.devilfruits.fruits.gura_gura.GuraEntities;
import com.devilfruits.fruits.gura_gura.GuraGura;
import com.devilfruits.fruits.gura_gura.GuraParticles;
import com.devilfruits.fruits.gura_gura.ability.QuakeDeflect;
import com.devilfruits.registry.DFParticles;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class QuakeWaveEntity extends Entity implements AbilityEntity {
   private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(QuakeWaveEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(QuakeWaveEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> DATA_WATER = SynchedEntityData.defineId(QuakeWaveEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_LIFETIME = SynchedEntityData.defineId(QuakeWaveEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(QuakeWaveEntity.class, EntityDataSerializers.FLOAT);
   @Nullable
   private UUID ownerId;
   private float damage;
   private float knockback;
   private int landTicks;
   private final IntSet hit = new IntOpenHashSet();
   private final Set<UUID> structuresHit = new HashSet<>();
   private CastBudget budget = new CastBudget(0, 0);
   private float destruction = 1.0F;
   public static BooleanSupplier vanillaParticles = () -> true;

   public QuakeWaveEntity(EntityType<? extends QuakeWaveEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   public QuakeWaveEntity(
      ServerLevel level,
      ServerPlayer owner,
      Vec3 pos,
      Vec3 dir,
      boolean water,
      float width,
      float height,
      float speed,
      float range,
      float damage,
      float knockback
   ) {
      this(GuraEntities.QUAKE_WAVE.get(), level);
      this.ownerId = owner.getUUID();
      this.setPos(pos);
      this.setYRot((float)(Mth.atan2(dir.x, dir.z) * 180.0F / (float)Math.PI));
      this.yRotO = this.getYRot();
      this.damage = damage;
      this.knockback = knockback;
      this.budget = CastBudget.fromConfig();
      this.entityData.set(DATA_WIDTH, width);
      this.entityData.set(DATA_HEIGHT, height);
      this.entityData.set(DATA_WATER, water);
      this.entityData.set(DATA_SPEED, speed);
      this.entityData.set(DATA_LIFETIME, Math.max(10, Mth.ceil(range / speed)));
   }

   public float width() {
      return (Float)this.entityData.get(DATA_WIDTH);
   }

   public float height() {
      return (Float)this.entityData.get(DATA_HEIGHT);
   }

   public boolean isWater() {
      return (Boolean)this.entityData.get(DATA_WATER);
   }

   public int lifetime() {
      return (Integer)this.entityData.get(DATA_LIFETIME);
   }

   public float speed() {
      return (Float)this.entityData.get(DATA_SPEED);
   }

   public Vec3 direction() {
      float yaw = this.getYRot() * (float) (Math.PI / 180.0);
      return new Vec3((double)Mth.sin(yaw), 0.0, (double)Mth.cos(yaw));
   }

   public float envelope(float partialTick) {
      float age = (float)this.tickCount + partialTick;
      float rise = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
      float fall = Mth.clamp(((float)this.lifetime() - age) / 8.0F, 0.0F, 1.0F);
      return rise * fall;
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_WIDTH, 8.0F);
      builder.define(DATA_HEIGHT, 4.0F);
      builder.define(DATA_WATER, true);
      builder.define(DATA_LIFETIME, 30);
      builder.define(DATA_SPEED, 0.7F);
   }

   public void tick() {
      super.tick();
      Vec3 dir = this.direction();
      Vec3 next = this.position().add(dir.scale((double)this.speed()));
      if (this.level() instanceof ServerLevel level) {
         if (this.tickCount > this.lifetime()) {
            this.discard();
            return;
         }

         ServerPlayer owner = this.ownerId == null ? null : (ServerPlayer)level.getPlayerByUUID(this.ownerId);
         if (owner == null) {
            this.discard();
            return;
         }

         double y = this.followSurface(level, next);
         this.setPos(next.x, y, next.z);
         this.hitEntities(level, owner, dir);
         this.deflect(owner, dir);
         this.strikeStructures(owner, dir);
         if (!this.isWater()) {
            this.popBlocks(level, owner, dir);
         } else if (this.landTicks > 10) {
            this.discard();
         }
      } else {
         this.setPos(next);
         if (vanillaParticles.getAsBoolean()) {
            this.spawnClientParticles(dir);
         }
      }
   }

   private double followSurface(ServerLevel level, Vec3 at) {
      int x = Mth.floor(at.x);
      int z = Mth.floor(at.z);
      int baseY = Mth.floor(this.getY());
      if (this.isWater()) {
         MutableBlockPos pos = new MutableBlockPos(x, baseY + 2, z);

         for (int i = 0; i < 7; i++) {
            if (level.getFluidState(pos).is(FluidTags.WATER) && !level.getFluidState(pos.above()).is(FluidTags.WATER)) {
               this.landTicks = 0;
               return (double)pos.getY() + 0.9;
            }

            pos.move(0, -1, 0);
         }

         this.landTicks++;
      }

      BlockPos surface = TerrainDestruction.findSurface(level, x, baseY, z, 3, 5);
      return surface != null ? (double)surface.getY() + 1.0 : this.getY();
   }

   private void hitEntities(ServerLevel level, ServerPlayer owner, Vec3 dir) {
      float envelope = this.envelope(0.0F);
      if (!(envelope < 0.2F)) {
         Vec3 perp = new Vec3(-dir.z, 0.0, dir.x);
         double half = (double)this.width() * 0.5;
         Vec3 a = this.position().add(perp.scale(half)).add(dir.scale(-1.0));
         Vec3 b = this.position().add(perp.scale(-half)).add(dir.scale(2.0)).add(0.0, (double)(this.height() * envelope + 1.0F), 0.0);
         AABB box = new AABB(a, b).inflate(0.2, 0.5, 0.2);

         for (Entity target : Targeting.inBox(level, this, box, e -> !this.hit.contains(e.getId()) && Targeting.canPush(owner, e))) {
            Vec3 rel = target.position().subtract(this.position());
            if (!(Math.abs(rel.dot(perp)) > half + 0.6)) {
               this.hit.add(target.getId());
               Targeting.hurt(owner, target, this.damage, GuraGura.QUAKE_DAMAGE);
               Vec3 launch = dir.scale((double)this.knockback * (this.isWater() ? 1.4 : 0.8)).add(0.0, this.isWater() ? 0.6 : 0.85, 0.0);
               Targeting.launch(target, launch);
               if (this.isWater()) {
                  target.clearFire();
               }
            }
         }
      }
   }

   private void strikeStructures(ServerPlayer owner, Vec3 dir) {
      float envelope = this.envelope(0.0F);
      if (!(envelope < 0.2F) && PhysicsWorld.active()) {
         Vec3 perp = new Vec3(-dir.z, 0.0, dir.x);
         double half = (double)this.width() * 0.5;
         double h = (double)(this.height() * envelope + 1.0F);
         Vec3 mid = this.position().add(dir.scale(0.5)).add(0.0, h * 0.5 - 0.5, 0.0);
         ImpactShape front = ImpactShape.capsule(mid.add(perp.scale(half)), mid.add(perp.scale(-half)), Math.max(1.2, h * 0.5));
         double strength = 40.0 * ((double)this.damage + 6.0 * (double)this.knockback) * (this.isWater() ? 1.4 : 1.0) * Math.sqrt((double)this.destruction);
         Vec3 heave = dir.add(0.0, this.isWater() ? 0.35 : 0.15, 0.0);
         BlockImpactService.strike(
            owner, GuraAbilities.KAISHO, front, heave, strength, this.isWater() ? 0.05 : 0.15 * (double)this.destruction, this.budget, this.structuresHit
         );
      }
   }

   private void deflect(ServerPlayer owner, Vec3 dir) {
      Vec3 perp = new Vec3(-dir.z, 0.0, dir.x);
      double half = (double)this.width() * 0.5;
      double h = (double)(this.height() * this.envelope(0.0F) + 1.0F);
      Vec3 mid = this.position().add(dir.scale(0.5)).add(0.0, h * 0.5 - 0.5, 0.0);
      QuakeDeflect.sweep(owner, ImpactShape.capsule(mid.add(perp.scale(half)), mid.add(perp.scale(-half)), Math.max(1.4, h * 0.5)), dir.add(0.0, 0.2, 0.0));
   }

   public QuakeWaveEntity withDestruction(float destruction) {
      this.destruction = destruction;
      this.budget = new CastBudget(GuraGura.blockBudget(destruction), (Integer)CoreConfig.MAX_DEBRIS_ENTITIES_PER_CAST.get());
      return this;
   }

   private void popBlocks(ServerLevel level, ServerPlayer owner, Vec3 dir) {
      if (TerrainDestruction.destructionAllowed(level) && this.budget.hasBlocks()) {
         Vec3 perp = new Vec3(-dir.z, 0.0, dir.x);
         int half = Mth.floor(this.width() * 0.5F);
         float expected = 0.28F * this.destruction;

         for (int i = -half; i <= half; i++) {
            int n = (int)expected + (level.random.nextFloat() < expected - (float)((int)expected) ? 1 : 0);
            Vec3 at = this.position().add(perp.scale((double)i)).add(dir.scale(1.5));

            for (int k = 0; k < n; k++) {
               BlockPos surface = TerrainDestruction.findSurface(level, Mth.floor(at.x), Mth.floor(this.getY()), Mth.floor(at.z), 2, 3 + k);
               if (surface == null) {
                  break;
               }

               Vec3 velocity = new Vec3(dir.x * 0.15, 0.45 + (double)level.random.nextFloat() * 0.35 + 0.1 * (double)k, dir.z * 0.15);
               TerrainDestruction.tearOut(level, owner, surface, velocity, this.budget, level.random.nextInt(60), true);
            }
         }
      }
   }

   private void spawnClientParticles(Vec3 dir) {
      float envelope = this.envelope(0.0F);
      Vec3 perp = new Vec3(-dir.z, 0.0, dir.x);
      int count = Mth.ceil(this.width() * 1.2F);

      for (int i = 0; i < count; i++) {
         float side = (this.random.nextFloat() - 0.5F) * this.width();
         Vec3 base = this.position().add(perp.scale((double)side));
         if (this.isWater()) {
            double crest = (double)(this.height() * envelope) * (1.0 - Math.pow((double)Math.abs(side) / ((double)this.width() * 0.5), 2.0) * 0.5);
            this.level()
               .addParticle(
                  (ParticleOptions)GuraParticles.WATER_SPRAY.get(),
                  base.x,
                  base.y + crest,
                  base.z,
                  dir.x * 0.3 + ((double)this.random.nextFloat() - 0.5) * 0.2,
                  0.15 + (double)this.random.nextFloat() * 0.25,
                  dir.z * 0.3 + ((double)this.random.nextFloat() - 0.5) * 0.2
               );
            if (this.random.nextInt(3) == 0) {
               this.level().addParticle(ParticleTypes.SPLASH, base.x, base.y + 0.2, base.z, 0.0, 0.3, 0.0);
            }
         } else {
            BlockState below = this.level().getBlockState(BlockPos.containing(base.x, this.getY() - 0.5, base.z));
            if (!below.isAir()) {
               this.level()
                  .addParticle(
                     new BlockParticleOption(DFParticles.BLOCK_DEBRIS.get(), below),
                     base.x,
                     this.getY() + 0.1,
                     base.z,
                     dir.x * 0.2 + ((double)this.random.nextFloat() - 0.5) * 0.2,
                     0.3 + (double)this.random.nextFloat() * 0.4,
                     dir.z * 0.2 + ((double)this.random.nextFloat() - 0.5) * 0.2
                  );
            }

            if (this.random.nextInt(2) == 0) {
               this.level().addParticle((ParticleOptions)DFParticles.DUST_PUFF.get(), base.x, this.getY() + 0.2, base.z, dir.x * 0.05, 0.05, dir.z * 0.05);
            }
         }
      }
   }

   public boolean shouldBeSaved() {
      return false;
   }

   public boolean isPickable() {
      return false;
   }

   public AABB getBoundingBoxForCulling() {
      double horizontal = Math.max((double)this.width() * 0.5, (double)this.height() * 2.2) + 1.0;
      return this.getBoundingBox().inflate(horizontal, (double)(this.height() + 1.0F), horizontal);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }
}
