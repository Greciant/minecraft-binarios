package com.awakenedmod.awakened.content.entity;

import com.awakenedmod.awakened.content.earth.StoneConstructEntity;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ArbiterWhipEntity extends Entity {
   public static final int TRAVEL_TICKS = 6;
   public static final int LINGER_TICKS = 14;
   private static final double ARC_HEIGHT = 3.2;
   private static final EntityDataAccessor<Integer> DATA_OWNER_ID = SynchedEntityData.defineId(ArbiterWhipEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Byte> DATA_CHAIN_MODE = SynchedEntityData.defineId(ArbiterWhipEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Integer> DATA_RIDE_ID = SynchedEntityData.defineId(ArbiterWhipEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_ROOT_STAND = SynchedEntityData.defineId(ArbiterWhipEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_SCRIPT = SynchedEntityData.defineId(ArbiterWhipEntity.class, EntityDataSerializers.INT);
   public static final byte CHAIN_FROM_YOKE = 0;
   public static final byte CHAIN_FROM_GROUND = 1;
   public static final byte CHAIN_PENDULUM = 2;
   public static final byte CHAIN_REEL = 3;
   public static final byte CHAIN_SWEEP_HIGH = 4;
   public static final int ROOT_RISE_TICKS = 4;
   public static final int ROOT_BREACH_TICKS = 2;
   public static final int ROOT_LINGER_TICKS = 12;
   public static final int ROOT_SINK_TICKS = 6;
   public static final int ROOT_SETTLE_TICKS = 3;
   private static final double ROOT_DEPTH = 2.2;
   private static final double ROOT_HEIGHT = 2.9;
   private static final double ROOT_OVERSHOOT = 0.45;
   private static final int DRIVEN_MAX_LIFE = 400;
   public static final double RIDE_HEIGHT = 0.55;
   private Vec3 from = Vec3.ZERO;
   private Vec3 to = Vec3.ZERO;
   private Vec3 clientPrev;
   private Vec3 clientNow;
   private boolean driven;
   private boolean root;
   private int sinkStart = -1;
   private double sinkFromY;
   private boolean solidRoot;
   private StoneConstructEntity collider;

   public ArbiterWhipEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
   }

   public void launch(ArbiterEntity owner, Vec3 from, Vec3 to) {
      this.entityData.set(DATA_OWNER_ID, owner.getId());
      this.from = from;
      this.to = to;
      this.setPos(from);
   }

   public void bindDriven(ArbiterEntity owner, Vec3 start, byte chainMode) {
      this.entityData.set(DATA_OWNER_ID, owner.getId());
      this.entityData.set(DATA_CHAIN_MODE, chainMode);
      this.driven = true;
      this.setPos(start);
   }

   public void launchRoot(ArbiterEntity owner, Vec3 burstPoint, int standTicks, boolean solid) {
      this.entityData.set(DATA_OWNER_ID, owner.getId());
      this.entityData.set(DATA_CHAIN_MODE, (byte)1);
      this.entityData.set(DATA_ROOT_STAND, standTicks);
      this.root = true;
      this.solidRoot = solid;
      this.from = burstPoint.add(0.0, -2.2, 0.0);
      this.to = burstPoint.add(0.0, 2.9, 0.0);
      this.setPos(this.from);
   }

   public int rootStandTicks() {
      return (Integer)this.entityData.get(DATA_ROOT_STAND);
   }

   public boolean isStandingRoot() {
      return this.root && this.rootStandTicks() > 12 && !this.isSinkingRoot();
   }

   public boolean isSinkingRoot() {
      return this.root && (this.sinkStart >= 0 || this.tickCount > 4 + this.rootStandTicks());
   }

   public byte chainMode() {
      return (Byte)this.entityData.get(DATA_CHAIN_MODE);
   }

   public int scriptTicks() {
      return Math.max(1, (Integer)this.entityData.get(DATA_SCRIPT));
   }

   public void setScriptTicks(int ticks) {
      this.entityData.set(DATA_SCRIPT, ticks);
   }

   public void rideOn(Entity rider) {
      this.entityData.set(DATA_RIDE_ID, rider.getId());
   }

   private Entity rider() {
      int id = (Integer)this.entityData.get(DATA_RIDE_ID);
      return id < 0 ? null : this.level().getEntity(id);
   }

   public Vec3 renderPosition(float partialTick) {
      Entity rider = this.rider();
      if (rider != null) {
         return rider.getPosition(partialTick).add(0.0, (double)rider.getBbHeight() * 0.55, 0.0);
      } else {
         return this.clientPrev != null && this.clientNow != null ? this.clientPrev.lerp(this.clientNow, (double)partialTick) : this.position();
      }
   }

   public ArbiterEntity owner() {
      return this.level().getEntity((Integer)this.entityData.get(DATA_OWNER_ID)) instanceof ArbiterEntity arbiter ? arbiter : null;
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         this.clientPrev = this.clientNow;
         this.clientNow = this.position();
      } else {
         int maxLife = this.driven ? 400 : (this.root ? 4 + this.rootStandTicks() + 6 : 20);
         ArbiterEntity owner = this.owner();
         if (owner == null || !owner.isAlive() && !this.root || this.tickCount > maxLife) {
            this.discard();
         } else if (!this.driven) {
            if (this.root) {
               this.tickRoot();
            } else {
               if (this.tickCount <= 6) {
                  float t = (float)this.tickCount / 6.0F;
                  Vec3 p = this.from.add(this.to.subtract(this.from).scale((double)t));
                  this.setPos(p.x, p.y + (double)Mth.sin(t * (float) Math.PI) * 3.2, p.z);
               }
            }
         }
      }
   }

   private void tickRoot() {
      double surface = this.to.y - 2.9;
      double peak = this.to.y + 0.45;
      int t = this.tickCount;
      int stand = this.rootStandTicks();
      if (this.sinkStart >= 0) {
         this.sink(t, this.sinkStart, this.sinkFromY);
      } else if (t <= 2) {
         this.setPos(this.from.x, Mth.lerp((double)t / 2.0, this.from.y, surface), this.from.z);
      } else if (t <= 4) {
         double k = easeOutCubic((double)(t - 2) / 2.0);
         this.setPos(this.from.x, Mth.lerp(k, surface, peak), this.from.z);
      } else if (t <= 7) {
         double k = easeOutCubic((double)(t - 4) / 3.0);
         this.setPos(this.from.x, Mth.lerp(k, peak, this.to.y), this.from.z);
      } else if (t > 4 + stand) {
         this.sink(t, 4 + stand, this.to.y);
         return;
      }

      this.tryHarden(t);
   }

   private void tryHarden(int t) {
      if (this.solidRoot && this.collider == null && this.sinkStart < 0 && this.level() instanceof ServerLevel server) {
         if (t >= 5) {
            double half = 0.225F;
            Vec3 feet = new Vec3(this.from.x, this.to.y - 2.9, this.from.z);
            AABB box = new AABB(feet.x - half, feet.y, feet.z - half, feet.x + half, feet.y + 3.2F, feet.z + half);
            if (server.getEntities(this, box, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator()).isEmpty()) {
               this.collider = StoneConstructEntity.collider(server, feet, 0.45F, 3.2F, this.rootStandTicks() + 6 + 20);
            }
         }
      }
   }

   private void dropCollider() {
      if (this.collider != null) {
         this.collider.discard();
         this.collider = null;
      }
   }

   private void sink(int t, int since, double topY) {
      this.dropCollider();
      if (t == since + 1 && topY > this.to.y - 2.9) {
         Fx.sound(this.level(), this.position(), SoundEvents.CHAIN_STEP, SoundSource.HOSTILE, 1.0F, 0.6F);
      }

      double k = Math.min(1.0, (double)(t - since) / 6.0);
      this.setPos(this.from.x, Mth.lerp(k * k, topY, this.from.y), this.from.z);
      if (k >= 1.0) {
         this.discard();
      }
   }

   public void sinkRoot() {
      if (this.root && this.sinkStart < 0 && !this.level().isClientSide()) {
         if (this.tickCount <= 4 + this.rootStandTicks()) {
            this.sinkStart = this.tickCount;
            this.sinkFromY = this.getY();
            this.dropCollider();
         }
      }
   }

   public void remove(RemovalReason reason) {
      this.dropCollider();
      super.remove(reason);
   }

   private static double easeOutCubic(double t) {
      double u = 1.0 - Mth.clamp(t, 0.0, 1.0);
      return 1.0 - u * u * u;
   }

   public boolean isPickable() {
      return false;
   }

   public boolean hurt(DamageSource source, float amount) {
      return false;
   }

   public boolean shouldBeSaved() {
      return false;
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_OWNER_ID, -1);
      builder.define(DATA_CHAIN_MODE, (byte)0);
      builder.define(DATA_RIDE_ID, -1);
      builder.define(DATA_SCRIPT, 4);
      builder.define(DATA_ROOT_STAND, 12);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }
}
