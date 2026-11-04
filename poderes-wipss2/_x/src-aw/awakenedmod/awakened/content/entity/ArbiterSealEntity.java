package com.awakenedmod.awakened.content.entity;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Bosses;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ArbiterSealEntity extends Mob implements Enemy, Foes.Inert, Bosses.Part {
   public static final int MAX_HITS = 3;
   private static final double[] SLOT_RADIUS = new double[]{13.5, 8.5, 6.5, 14.5, 10.0, 11.5};
   public static final double MIN_ORBIT_RADIUS = 6.5;
   public static final double MAX_ORBIT_RADIUS = 14.5;
   private static final double[] SLOT_HEIGHT = new double[]{1.5, 4.7, 2.3, 6.5, 1.9, 8.0};
   private static final double MIN_HEIGHT = 1.5;
   private static final double MAX_HEIGHT = 8.0;
   public static final int ORBIT_PERIOD = 800;
   private static final double ORBIT_SPEED = 0.007853981633974483;
   private static final double BOB = 0.22;
   private static final int HIT_COOLDOWN_TICKS = 8;
   private static final EntityDataAccessor<Integer> DATA_OWNER_ID = SynchedEntityData.defineId(ArbiterSealEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Byte> DATA_ORBIT_INDEX = SynchedEntityData.defineId(ArbiterSealEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Byte> DATA_HITS_TAKEN = SynchedEntityData.defineId(ArbiterSealEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Byte> DATA_ORBIT_DIR = SynchedEntityData.defineId(ArbiterSealEntity.class, EntityDataSerializers.BYTE);
   private static final DustParticleOptions SEAL_VIOLET = Fx.dust(11544319, 1.2F);
   private long hitOpenAt;

   public static double orbitRadius(int index) {
      return SLOT_RADIUS[Math.floorMod(index, SLOT_RADIUS.length)];
   }

   private static void checkTable(String name, double[] table, double min, double max) {
      double lo = Double.MAX_VALUE;
      double hi = -Double.MAX_VALUE;

      for (double v : table) {
         lo = Math.min(lo, v);
         hi = Math.max(hi, v);
      }

      if (lo == min && hi == max) {
         if (table.length != 6) {
            throw new IllegalStateException(name + " has " + table.length + " entries for 6 seals");
         }
      } else {
         throw new IllegalStateException(name + " spans " + lo + ".." + hi + " but its bounds say " + min + ".." + max);
      }
   }

   private static double orbitHeight(int index) {
      return SLOT_HEIGHT[Math.floorMod(index, SLOT_HEIGHT.length)];
   }

   public ArbiterSealEntity(EntityType<? extends Mob> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
      this.setNoAi(true);
      this.xpReward = 0;
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 1024.0)
         .add(Attributes.MOVEMENT_SPEED, 0.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.FOLLOW_RANGE, 0.0);
   }

   public void bind(ArbiterEntity owner, int orbitIndex) {
      this.entityData.set(DATA_OWNER_ID, owner.getId());
      this.entityData.set(DATA_ORBIT_INDEX, (byte)orbitIndex);
      this.entityData.set(DATA_ORBIT_DIR, (byte)orbitDirection(owner.escalationPhase()));
      this.setPos(this.orbitPos(owner));
   }

   public static int orbitDirection(int tier) {
      return (tier & 1) == 0 ? -1 : 1;
   }

   public static double orbitAngle(int index, long clock, int direction) {
      return (double)index * (Math.PI / 3) + (double)clock * 0.007853981633974483 * (double)direction;
   }

   public ArbiterEntity owner() {
      return this.level().getEntity((Integer)this.entityData.get(DATA_OWNER_ID)) instanceof ArbiterEntity arbiter ? arbiter : null;
   }

   public int orbitIndex() {
      return (Byte)this.entityData.get(DATA_ORBIT_INDEX);
   }

   public int hitsTaken() {
      return (Byte)this.entityData.get(DATA_HITS_TAKEN);
   }

   public void tick() {
      super.tick();
      ArbiterEntity owner = this.owner();
      if (owner != null && owner.isAlive()) {
         this.setPos(this.orbitPos(owner));
         if (this.level() instanceof ServerLevel server) {
            this.tickWear(server);
         }
      } else {
         if (!this.level().isClientSide()) {
            this.discardOrphan();
         }
      }
   }

   private void tickWear(ServerLevel level) {
      int hits = this.hitsTaken();
      if (hits > 0) {
         boolean last = hits >= 2;
         int period = last ? 10 : 20;
         if ((level.getGameTime() + (long)this.orbitIndex() * 3L) % (long)period == 0L) {
            Vec3 core = this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0);
            Vfx.ring(level, core, last ? 1.15 : 0.85, last ? 15244287 : 11544319, last ? 8 : 6);
            Fx.burst(level, SEAL_VIOLET, core, last ? 6 : 3, 0.22, 0.01);
         }
      }
   }

   private Vec3 orbitPos(ArbiterEntity owner) {
      int index = this.orbitIndex();
      long clock = this.level().getGameTime();
      double angle = orbitAngle(index, clock, (Byte)this.entityData.get(DATA_ORBIT_DIR));
      double radius = orbitRadius(index);
      double height = orbitHeight(index) + (double)Mth.sin((float)clock * 0.06F + (float)index * 1.1F) * 0.22;
      return new Vec3(owner.getX() + Math.cos(angle) * radius, owner.getY() + height, owner.getZ() + Math.sin(angle) * radius);
   }

   private boolean takeHit(ServerLevel level, DamageSource source) {
      if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         this.discardOrphan();
         return true;
      } else if (source.getEntity() instanceof LivingEntity attacker) {
         Vec3 var7 = this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0);
         if (level.getGameTime() >= this.hitOpenAt && !this.isRemoved()) {
            this.hitOpenAt = level.getGameTime() + 8L;
            int hits = this.hitsTaken() + 1;
            this.entityData.set(DATA_HITS_TAKEN, (byte)hits);
            ArbiterEntity owner = this.owner();
            if (owner != null) {
               owner.onSealStruck(attacker);
            }

            if (hits >= 3) {
               this.shatter(level, var7);
            } else {
               Fx.burst(level, SEAL_VIOLET, var7, 10, 0.3, 0.02);
               Vfx.sphere(level, var7, 0.85, 15244287, 4);
               Fx.sound(level, var7, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.0F, 0.7F + (float)hits * 0.15F);
            }

            return true;
         } else {
            Fx.sound(level, var7, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 0.55F, 0.5F);
            Fx.burst(level, SEAL_VIOLET, var7, 2, 0.12, 0.005);
            return true;
         }
      } else {
         return false;
      }
   }

   private void shatter(ServerLevel level, Vec3 core) {
      Vfx.sphere(level, core, 0.9, 11544319, 7);
      Vfx.ring(level, core, 1.4, 15244287, 9);
      Fx.burst(level, SEAL_VIOLET, core, 24, 0.45, 0.05);
      Fx.sound(level, core, (SoundEvent)AwakenedSounds.ARBITER_SEAL_SHATTER.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
      ArbiterEntity owner = this.owner();
      if (owner != null) {
         owner.onSealShattered(this);
      }

      this.discard();
   }

   private void discardOrphan() {
      ArbiterEntity owner = this.owner();
      if (owner != null) {
         owner.onSealDiscarded(this);
      }

      this.discard();
   }

   public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
      if (this.owner() == null) {
         super.lerpTo(x, y, z, yRot, xRot, steps);
      }
   }

   public boolean isPickable() {
      return true;
   }

   public boolean isPushable() {
      return false;
   }

   public boolean canBeAffected(MobEffectInstance effect) {
      return false;
   }

   public boolean removeWhenFarAway(double distance) {
      return false;
   }

   public boolean shouldBeSaved() {
      return false;
   }

   protected SoundEvent getAmbientSound() {
      return null;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return null;
   }

   protected SoundEvent getDeathSound() {
      return null;
   }

   public boolean hurt(DamageSource source, float amount) {
      return this.level() instanceof ServerLevel serverLevel ? this.takeHit(serverLevel, source) : true;
   }

   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_OWNER_ID, -1);
      builder.define(DATA_ORBIT_INDEX, (byte)0);
      builder.define(DATA_HITS_TAKEN, (byte)0);
      builder.define(DATA_ORBIT_DIR, (byte)1);
   }

   static {
      checkTable("SLOT_RADIUS", SLOT_RADIUS, 6.5, 14.5);
      checkTable("SLOT_HEIGHT", SLOT_HEIGHT, 1.5, 8.0);
   }
}
