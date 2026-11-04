package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class HydroLance extends Projectile {
   public static final int STYLE_WET = 16;
   public static final int STYLE_LEFT_ARM = 32;
   public static final float STUCK_BACKOFF = 0.3F;
   public static final int LINGER_TICKS = 16;
   private static final EntityDataAccessor<Byte> DATA_STYLE = SynchedEntityData.defineId(HydroLance.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Boolean> DATA_STUCK = SynchedEntityData.defineId(HydroLance.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Float> DATA_GRAVITY = SynchedEntityData.defineId(HydroLance.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_LAUNCH_X = SynchedEntityData.defineId(HydroLance.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_LAUNCH_Y = SynchedEntityData.defineId(HydroLance.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_LAUNCH_Z = SynchedEntityData.defineId(HydroLance.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_LAUNCH_TIME = SynchedEntityData.defineId(HydroLance.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Byte> DATA_FACE = SynchedEntityData.defineId(HydroLance.class, EntityDataSerializers.BYTE);
   private static final double DRAIN_TAP = 1.2;
   private static final double DRAIN_FULL_EXTRA = 0.8;
   private static final int DRAIN_REACH_TICKS = 6;
   private static final double DRAG = 0.995;
   public static final double RANGE = 128.0;
   private static final int MAX_CATCH_UP = 200;
   private float damage;
   private double knock;
   private int pierce;
   private double remaining = 128.0;
   private final Set<Integer> hit = new HashSet<>();
   private int speared;
   private boolean skewerPaid;
   private int linger = -1;
   private Vec3 landedAt;
   private int stuckAt = -1;
   private boolean clientStopped;
   private int ageOffset;
   private float drainDrop = -1.0F;

   public HydroLance(EntityType<? extends HydroLance> type, Level level) {
      super(type, level);
   }

   public static HydroLance launch(ServerPlayer owner, Vec3 from, Vec3 velocity, double gravity, float damage, double knock, int pierce, int style) {
      HydroLance lance = new HydroLance(AwakenedEntities.HYDRO_LANCE.get(), owner.level());
      lance.setOwner(owner);
      lance.setPos(from.x, from.y, from.z);
      lance.setDeltaMovement(velocity);
      lance.face(velocity);
      lance.yRotO = lance.getYRot();
      lance.xRotO = lance.getXRot();
      lance.damage = damage;
      lance.knock = knock;
      lance.pierce = pierce;
      lance.entityData.set(DATA_STYLE, (byte)style);
      lance.entityData.set(DATA_GRAVITY, (float)gravity);
      lance.entityData.set(DATA_LAUNCH_X, (float)velocity.x);
      lance.entityData.set(DATA_LAUNCH_Y, (float)velocity.y);
      lance.entityData.set(DATA_LAUNCH_Z, (float)velocity.z);
      lance.entityData.set(DATA_LAUNCH_TIME, (int)owner.level().getGameTime());
      owner.level().addFreshEntity(lance);
      return lance;
   }

   public static int style(float charge, boolean wet, boolean leftArm) {
      int q = Mth.clamp(Math.round(charge * 15.0F), 0, 15);
      return q | (wet ? 16 : 0) | (leftArm ? 32 : 0);
   }

   public static float charge(int style) {
      return (float)(style & 15) / 15.0F;
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_STYLE, (byte)0);
      builder.define(DATA_STUCK, false);
      builder.define(DATA_GRAVITY, 0.02F);
      builder.define(DATA_LAUNCH_X, 0.0F);
      builder.define(DATA_LAUNCH_Y, 0.0F);
      builder.define(DATA_LAUNCH_Z, 0.0F);
      builder.define(DATA_LAUNCH_TIME, 0);
      builder.define(DATA_FACE, (byte)-1);
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (this.level().isClientSide()) {
         if (!DATA_LAUNCH_X.equals(key) && !DATA_LAUNCH_Y.equals(key) && !DATA_LAUNCH_Z.equals(key) && !DATA_LAUNCH_TIME.equals(key)) {
            if (DATA_STUCK.equals(key) && this.isStuck() && this.stuckAt < 0) {
               this.clientStopped = false;
               this.stuckAt = this.tickCount;
               this.setDeltaMovement(Vec3.ZERO);
            }
         } else if (!this.isStuck() && this.tickCount <= 1) {
            this.catchUp();
         }
      }
   }

   private void catchUp() {
      int elapsed = Mth.clamp((int)this.level().getGameTime() - (Integer)this.entityData.get(DATA_LAUNCH_TIME), 0, 200);
      double g = (double)((Float)this.entityData.get(DATA_GRAVITY)).floatValue();
      Vec3 v = new Vec3(
         (double)((Float)this.entityData.get(DATA_LAUNCH_X)).floatValue(),
         (double)((Float)this.entityData.get(DATA_LAUNCH_Y)).floatValue(),
         (double)((Float)this.entityData.get(DATA_LAUNCH_Z)).floatValue()
      );

      for (int i = 0; i < elapsed; i++) {
         v = v.add(0.0, -g, 0.0).scale(0.995);
      }

      this.setDeltaMovement(v);
      this.ageOffset = Math.max(0, elapsed - 1);
   }

   public float flightAge(float partialTick) {
      return (float)(this.tickCount + this.ageOffset) + partialTick;
   }

   public int style() {
      return (Byte)this.entityData.get(DATA_STYLE);
   }

   public boolean isStuck() {
      return (Boolean)this.entityData.get(DATA_STUCK);
   }

   public Direction wallFace() {
      int id = (Byte)this.entityData.get(DATA_FACE);
      Direction face = id < 0 ? null : Direction.from3DDataValue(id);
      return face != null && face.getAxis().isHorizontal() ? face : null;
   }

   public Vec3 contact(float partialTick) {
      return this.getPosition(partialTick).add(Vec3.directionFromRotation(this.getXRot(), this.getYRot()).scale(0.3F));
   }

   public float drainLength() {
      Direction face = this.wallFace();
      if (face == null) {
         return 0.0F;
      } else {
         double reach = 1.2 + (double)charge(this.style()) * 0.8;
         if (this.drainDrop < 0.0F) {
            Vec3 from = this.contact(1.0F).add((double)face.getStepX() * 0.08, 0.0, (double)face.getStepZ() * 0.08);
            BlockHitResult down = this.level().clip(new ClipContext(from, from.add(0.0, -reach, 0.0), Block.COLLIDER, Fluid.NONE, this));
            this.drainDrop = (float)(down.getType() == Type.BLOCK ? from.y - down.getLocation().y : reach);
         }

         return (float)Math.min(reach, (double)this.drainDrop);
      }
   }

   public float drained(float partialTick) {
      return this.stuckAt < 0 ? 0.0F : Mth.clamp(((float)(this.tickCount - this.stuckAt) + partialTick) / 16.0F, 0.0F, 1.0F);
   }

   public void tick() {
      super.tick();
      if (!this.isStuck() && !this.clientStopped) {
         Level level = this.level();
         Vec3 vel = this.getDeltaMovement().add(0.0, (double)(-(Float)this.entityData.get(DATA_GRAVITY)), 0.0).scale(0.995);
         double step = vel.length();
         ServerLevel server = level instanceof ServerLevel s ? s : null;
         if (server == null || !(step <= 1.0E-4) && !(this.remaining <= 0.0)) {
            Vec3 pos = this.position();
            Vec3 dir = step > 1.0E-6 ? vel.scale(1.0 / step) : new Vec3(0.0, 0.0, 1.0);
            Vec3 next = pos.add(vel);
            if (server == null || !(next.y < (double)level.getMinBuildHeight()) && server.isPositionEntityTicking(BlockPos.containing(next))) {
               BlockHitResult clip = this.clipPastLeaves(level, pos, next, dir);
               boolean wall = clip.getType() == Type.BLOCK;
               if (wall) {
                  next = clip.getLocation();
               }

               if (server != null && this.getOwner() instanceof ServerPlayer player) {
                  this.strike(server, player, pos, next, dir);
               }

               this.setPos(next.x, next.y, next.z);
               this.setDeltaMovement(vel);
               this.face(vel);
               if (server == null) {
                  if (wall) {
                     this.clientStopped = true;
                     this.setDeltaMovement(Vec3.ZERO);
                  } else {
                     this.spray(dir);
                  }
               } else {
                  this.remaining -= step;
                  if (wall) {
                     this.land(server, next, clip.getDirection());
                  } else if (this.pierce <= 0) {
                     this.spent(server, next, dir);
                  } else if (this.remaining <= 0.0) {
                     this.discard();
                  }
               }
            } else {
               this.discard();
            }
         } else {
            this.discard();
         }
      } else {
         this.tickStuck();
      }
   }

   private void spray(Vec3 dir) {
      Level level = this.level();
      double len = 1.7 + (double)charge(this.style()) * 0.7;
      int n = 1 + this.random.nextInt(2);

      for (int i = 0; i < n; i++) {
         Vec3 p = this.position().subtract(dir.scale(len * (0.6 + 0.4 * this.random.nextDouble())));
         level.addParticle(ParticleTypes.SPLASH, p.x, p.y, p.z, 0.0, 0.0, 0.0);
      }

      if ((this.style() & 16) != 0 && this.tickCount % 2 == 0) {
         Vec3 p = this.position().subtract(dir.scale(len * this.random.nextDouble()));
         level.addParticle(ParticleTypes.BUBBLE_POP, p.x, p.y, p.z, 0.0, 0.0, 0.0);
      }
   }

   private void tickStuck() {
      if (this.level() instanceof ServerLevel server) {
         if (this.linger == 10) {
            this.drainPuddle(server);
         }

         if (this.linger-- <= 0) {
            this.discard();
         }
      } else {
         if (this.random.nextInt(3) == 0) {
            Vec3 back = Vec3.directionFromRotation(this.getXRot(), this.getYRot()).scale(-0.4 - this.random.nextDouble());
            Vec3 p = this.position().add(back);
            this.level().addParticle(ParticleTypes.DRIPPING_WATER, p.x, p.y, p.z, 0.0, 0.0, 0.0);
         }

         Direction face = this.wallFace();
         if (face != null && this.random.nextInt(2) == 0) {
            Vec3 p = this.contact(1.0F).add((double)face.getStepX() * 0.08, -0.05, (double)face.getStepZ() * 0.08);
            this.level().addParticle(ParticleTypes.FALLING_WATER, p.x, p.y, p.z, 0.0, 0.0, 0.0);
         }
      }
   }

   private void drainPuddle(ServerLevel level) {
      Direction face = this.wallFace();
      if (face != null && this.landedAt != null) {
         float f = charge(this.style());
         Vec3 from = this.landedAt.add((double)face.getStepX() * 0.08, 0.0, (double)face.getStepZ() * 0.08);
         double reach = 1.2 + (double)f * 0.8 + 0.3;
         BlockHitResult down = level.clip(new ClipContext(from, from.add(0.0, -reach, 0.0), Block.COLLIDER, Fluid.NONE, this));
         if (down.getType() == Type.BLOCK && down.getDirection() == Direction.UP) {
            boolean wet = (this.style() & 16) != 0;
            Vec3 at = down.getLocation().add((double)face.getStepX() * 0.25, 0.01, (double)face.getStepZ() * 0.25);
            HydroFx.puddle(level, at, new Vec3(0.0, 1.0, 0.0), 0.4 + (double)f * 0.3, 22, wet);
            Fx.burst(level, ParticleTypes.SPLASH, at, 5, 0.2, 0.05);
         }
      }
   }

   private void strike(ServerLevel level, ServerPlayer player, Vec3 pos, Vec3 next, Vec3 dir) {
      if (this.pierce > 0) {
         AABB search = new AABB(pos, next).inflate(0.6);
         List<LivingEntity> along = new ArrayList<>();

         for (LivingEntity e : level.getEntitiesOfClass(
            LivingEntity.class, search, c -> c != player && c.isAlive() && !this.hit.contains(c.getId()) && Harm.allowed(player, c)
         )) {
            if (e.getBoundingBox().inflate(0.3).clip(pos, next).isPresent()) {
               along.add(e);
            }
         }

         along.sort(Comparator.comparingDouble(ex -> ex.distanceToSqr(pos)));
         boolean wet = (this.style() & 16) != 0;
         float f = charge(this.style());
         boolean foeStruck = false;

         for (LivingEntity ex : along) {
            if (this.pierce <= 0) {
               break;
            }

            Hydro.hit(player, ex, this.damage);
            ex.knockback(this.knock, -dir.x, -dir.z);
            Vec3 at = ex.position().add(0.0, (double)ex.getBbHeight() * 0.5, 0.0);
            HydroFx.splash(level, at, dir.scale(-1.0), 0.35 + (double)f * 0.3, wet);
            HydroFx.impactRing(level, at.add(dir.scale(0.5)), dir, 0.5 + (double)f * 0.4, wet);
            HydroFx.droplets(level, at, 20, 0.45, wet);
            HydroFx.sound(level, at, SoundEvents.GENERIC_SPLASH, 0.7F, 0.9F);
            if (f >= 0.99F) {
               Fx.shake(level, at, 6.0, 0.06F, 0, 3);
            }

            Fx.soundOnlyFor(player, player.getEyePosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.6F, wet ? 0.9F : 1.25F);
            this.hit.add(ex.getId());
            if (Foes.counts(player, ex)) {
               this.speared++;
               foeStruck = true;
            }

            this.pierce--;
         }

         if (foeStruck) {
            MasterySystem.award(player, MasteryReason.HIT);
         }

         if (!this.skewerPaid && this.speared >= 3) {
            QuestSystem.feat(player, "hydro_skewer");
            this.skewerPaid = true;
         }
      }
   }

   private void land(ServerLevel level, Vec3 at, Direction face) {
      Vec3 normal = new Vec3((double)face.getStepX(), (double)face.getStepY(), (double)face.getStepZ());
      float f = charge(this.style());
      boolean wet = (this.style() & 16) != 0;
      HydroFx.splash(level, at, normal, 0.45 + (double)f * 0.45, wet);
      HydroFx.impactRing(level, at.add(normal.scale(0.03)), normal, 0.8 + (double)f * 0.9, wet);
      if (face == Direction.UP) {
         HydroFx.puddle(level, at.add(0.0, 0.01, 0.0), normal, 0.55 + (double)f * 0.5, 26, wet);
      }

      HydroFx.sound(level, at, SoundEvents.TRIDENT_HIT_GROUND, 0.5F, 1.4F);
      Vec3 back = at.subtract(Vec3.directionFromRotation(this.getXRot(), this.getYRot()).scale(0.3F));
      this.setPos(back.x, back.y, back.z);
      landShake(level, at, f);
      this.setDeltaMovement(Vec3.ZERO);
      this.linger = 16;
      this.landedAt = at;
      this.entityData.set(DATA_FACE, (byte)face.get3DDataValue());
      this.entityData.set(DATA_STUCK, true);
   }

   private static void landShake(ServerLevel level, Vec3 at, float f) {
      if (f > 0.5F) {
         Fx.shake(level, at, 10.0, 0.05F + 0.1F * f, 0, 6);
      }
   }

   private void spent(ServerLevel level, Vec3 at, Vec3 dir) {
      boolean wet = (this.style() & 16) != 0;
      float f = charge(this.style());
      HydroFx.splash(level, at, dir.scale(-1.0), 0.4 + (double)f * 0.4, wet);
      landShake(level, at, f);
      this.discard();
   }

   private void face(Vec3 v) {
      double h = Math.sqrt(v.x * v.x + v.z * v.z);
      if (!(h < 1.0E-6) || !(Math.abs(v.y) < 1.0E-6)) {
         this.setYRot((float)Math.toDegrees(Math.atan2(-v.x, v.z)));
         this.setXRot((float)Math.toDegrees(Math.atan2(-v.y, h)));
      }
   }

   private BlockHitResult clipPastLeaves(Level level, Vec3 from, Vec3 to, Vec3 dir) {
      Vec3 start = from;

      for (int i = 0; i < 8; i++) {
         BlockHitResult hitResult = level.clip(new ClipContext(start, to, Block.COLLIDER, Fluid.NONE, this));
         if (hitResult.getType() != Type.BLOCK || !level.getBlockState(hitResult.getBlockPos()).is(BlockTags.LEAVES)) {
            return hitResult;
         }

         start = hitResult.getLocation().add(dir.scale(0.05));
      }

      return BlockHitResult.miss(to, Direction.UP, BlockPos.containing(to));
   }

   public boolean isPickable() {
      return false;
   }

   public boolean isPushedByFluid() {
      return false;
   }

   public boolean canUsePortal(boolean allowPassengers) {
      return false;
   }

   public boolean shouldBeSaved() {
      return false;
   }
}
