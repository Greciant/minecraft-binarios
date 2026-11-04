package com.awakenedmod.awakened.content.entity.projectile;

import com.awakenedmod.awakened.content.summoning.MinionSupport;
import com.awakenedmod.awakened.core.DeflectedShots;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.OwnedProjectile;
import com.awakenedmod.awakened.core.npc.PoweredNpc;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class KineticProjectile extends ThrowableItemProjectile implements OwnedProjectile {
   private static final EntityDataAccessor<Byte> DATA_VISUAL_STYLE = SynchedEntityData.defineId(KineticProjectile.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Boolean> DATA_TRAIL = SynchedEntityData.defineId(KineticProjectile.class, EntityDataSerializers.BOOLEAN);
   private float impactDamage = 6.0F;
   @Nullable
   private ResourceKey<DamageType> damageType;
   private float aoeDamage = 2.0F;
   private float aoeRadius = 2.5F;
   private float knockback = 0.7F;
   private double knockbackLift = 0.3;
   private int igniteSeconds = 0;
   private int slowSeconds = 0;
   private double flameAura = 0.0;
   private ParticleOptions auraParticle = null;
   private double contactRadius = 0.0;
   private double gravityOverride = -1.0;
   private boolean customDetonation = false;
   private int homingTargetId = -1;
   private double homingStrength = 0.0;
   private double homingSpeed = 0.0;
   private Consumer<Vec3> onDetonate;
   private BiConsumer<Vec3, Vec3> onDetonateSurface;
   private Consumer<Entity> onDirectHit;
   private Consumer<Vec3> onTick;
   private Entity directHit;
   private Vec3 hitNormal;
   private Vec3 lastFlightDir = Vec3.ZERO;
   private boolean saving;

   public KineticProjectile(EntityType<? extends KineticProjectile> type, Level level) {
      super(type, level);
   }

   public KineticProjectile(EntityType<? extends KineticProjectile> type, LivingEntity owner, Level level, ItemStack item) {
      super(type, owner, level);
      this.setItem(item);
   }

   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_VISUAL_STYLE, (byte)0);
      builder.define(DATA_TRAIL, true);
   }

   public void setVisualStyle(int style) {
      this.entityData.set(DATA_VISUAL_STYLE, (byte)style);
   }

   public void setTrail(boolean trail) {
      this.entityData.set(DATA_TRAIL, trail);
   }

   public int visualStyle() {
      return (Byte)this.entityData.get(DATA_VISUAL_STYLE);
   }

   @Override
   public Entity shooter() {
      return this.getOwner();
   }

   @Override
   public float shotDamage() {
      return this.impactDamage;
   }

   public void configure(float impactDamage, float aoeDamage, float aoeRadius, float knockback) {
      this.impactDamage = impactDamage;
      this.aoeDamage = aoeDamage;
      this.aoeRadius = aoeRadius;
      this.knockback = knockback;
   }

   public void setKnockbackLift(double lift) {
      this.knockbackLift = lift;
   }

   public void setDamageType(ResourceKey<DamageType> type) {
      this.damageType = type;
   }

   private DamageSource hitSource(@Nullable Entity owner) {
      return this.damageType == null
         ? this.damageSources().thrown(this, owner)
         : new DamageSource(this.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(this.damageType), this, owner);
   }

   public void setIgnite(int seconds) {
      this.igniteSeconds = seconds;
   }

   public void setSlow(int seconds) {
      this.slowSeconds = seconds;
   }

   public void setFlameAura(double radius) {
      this.flameAura = radius;
   }

   public void setAuraParticle(ParticleOptions particle) {
      this.auraParticle = particle;
   }

   public void setContactRadius(double radius) {
      this.contactRadius = radius;
   }

   public void setOnDetonate(Consumer<Vec3> onDetonate) {
      this.onDetonate = onDetonate;
   }

   public void setOnDetonateSurface(BiConsumer<Vec3, Vec3> onDetonateSurface) {
      this.onDetonateSurface = onDetonateSurface;
   }

   public void setOnDirectHit(Consumer<Entity> onDirectHit) {
      this.onDirectHit = onDirectHit;
   }

   public Entity directHit() {
      return this.directHit;
   }

   public void setOnTick(Consumer<Vec3> onTick) {
      this.onTick = onTick;
   }

   public void setHoming(Entity target, double strength, double speed) {
      this.homingTargetId = target != null ? target.getId() : -1;
      this.homingStrength = strength;
      this.homingSpeed = speed;
   }

   public void setCustomDetonation(boolean custom) {
      this.customDetonation = custom;
   }

   protected Item getDefaultItem() {
      return Items.SNOWBALL;
   }

   public boolean canUsePortal(boolean allowPassengers) {
      return false;
   }

   public boolean shouldBeSaved() {
      return false;
   }

   public ItemStack getItem() {
      ItemStack item = super.getItem();
      return this.saving && item.isEmpty() ? new ItemStack(this.getDefaultItem()) : item;
   }

   public CompoundTag saveWithoutId(CompoundTag tag) {
      this.saving = true;

      CompoundTag var2;
      try {
         var2 = super.saveWithoutId(tag);
      } finally {
         this.saving = false;
      }

      return var2;
   }

   public void setGravity(double gravity) {
      this.gravityOverride = gravity;
   }

   protected double getDefaultGravity() {
      return this.gravityOverride >= 0.0 ? this.gravityOverride : 0.01;
   }

   private boolean isColdItem() {
      ItemStack s = this.getItem();
      return s.is(Items.PACKED_ICE) || s.is(Items.ICE) || s.is(Items.BLUE_ICE) || s.is(Items.SNOWBALL) || s.is(Items.SNOW_BLOCK);
   }

   public void tick() {
      Vec3 flight = this.getDeltaMovement();
      if (flight.lengthSqr() > 1.0E-6) {
         this.lastFlightDir = flight.normalize();
      }

      super.tick();
      label77:
      if (this.homingTargetId >= 0 && !this.isRemoved() && this.level() instanceof ServerLevel server) {
         if (server.getEntity(this.homingTargetId) instanceof LivingEntity living && living.isAlive()) {
            Vec3 toTarget = living.getBoundingBox().getCenter().subtract(this.position());
            double len = toTarget.length();
            if (len > 0.001) {
               Vec3 desired = toTarget.scale(1.0 / len);
               Vec3 cur = this.getDeltaMovement();
               double curLen = cur.length();
               Vec3 dir = curLen > 0.001 ? cur.scale(1.0 / curLen) : desired;
               Vec3 steered = dir.add(desired.subtract(dir).scale(this.homingStrength)).normalize();
               this.setDeltaMovement(steered.scale(this.homingSpeed > 0.0 ? this.homingSpeed : curLen));
            } else {
               this.homingTargetId = -1;
            }
            break label77;
         }

         this.homingTargetId = -1;
      }

      if (this.onTick != null && !this.isRemoved() && this.level() instanceof ServerLevel) {
         this.onTick.accept(this.position());
      }

      if (this.flameAura > 0.0 && this.level() instanceof ServerLevel server) {
         int points = Math.max(8, (int)(this.flameAura * 45.0));
         if (this.auraParticle != null) {
            Fx.sphereShell(server, this.auraParticle, this.position(), this.flameAura, points);
            Fx.sphereShell(server, this.auraParticle, this.position(), this.flameAura * 0.6, points / 2);
         } else {
            Fx.sphereShell(server, ParticleTypes.FLAME, this.position(), this.flameAura, points);
            Fx.sphereShell(server, ParticleTypes.SMALL_FLAME, this.position(), this.flameAura * 0.6, points / 2);
            Fx.burst(server, ParticleTypes.LAVA, this.position(), 1, this.flameAura * 0.4, 0.0);
         }
      } else if (this.level().isClientSide() && !this.getItem().isEmpty() && (Boolean)this.entityData.get(DATA_TRAIL)) {
         this.level().addParticle(this.isColdItem() ? ParticleTypes.SNOWFLAKE : ParticleTypes.WITCH, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      }

      if (this.contactRadius > 0.0 && !this.isRemoved() && this.level() instanceof ServerLevel serverx) {
         Entity owner = this.getOwner();
         boolean turned = DeflectedShots.turned(this);
         Iterator var22 = serverx.getEntitiesOfClass(
               LivingEntity.class,
               this.getBoundingBox().inflate(this.contactRadius),
               e -> e.isAlive() && (turned || e != owner) && !this.spares(e) && Harm.allowed(this, e)
            )
            .iterator();
         if (var22.hasNext()) {
            LivingEntity target = (LivingEntity)var22.next();
            this.impactOn(serverx, target);
            return;
         }
      }
   }

   private void impactOn(ServerLevel server, Entity target) {
      this.directHit = target;
      if (this.spares(target)) {
         this.detonate(server, target);
      } else {
         Entity owner = this.getOwner();
         if (this.igniteSeconds > 0 && Harm.allowed(this, target)) {
            target.igniteForSeconds((float)this.igniteSeconds);
         }

         boolean dealt = Dmg.hurt(target, server, this.hitSource(owner), this.impactDamage);
         if (!DeflectedShots.turnedThisTick(this)) {
            if (this.slowSeconds > 0 && target instanceof LivingEntity living) {
               Harm.effect(this, living, MobEffects.MOVEMENT_SLOWDOWN, this.slowSeconds * 20, 1);
            }

            if (dealt && owner instanceof ServerPlayer player) {
               MasterySystem.award(player, MasteryReason.HIT, target);
            }

            if (dealt && this.onDirectHit != null) {
               this.onDirectHit.accept(target);
            }

            this.detonate(server, target);
         }
      }
   }

   private boolean spares(Entity victim) {
      if (this.getOwner() instanceof PoweredNpc npc && victim != npc) {
         return !npc.caster().canHarm(victim);
      }

      if (this.getOwner() instanceof Mob shooter && victim instanceof LivingEntity living && MinionSupport.alliedSummonForbidden(shooter, living)) {
         return true;
      }

      return false;
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (this.level() instanceof ServerLevel server) {
         this.impactOn(server, result.getEntity());
      }
   }

   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      if (this.level() instanceof ServerLevel server) {
         Direction face = result.getDirection();
         this.hitNormal = new Vec3((double)face.getStepX(), (double)face.getStepY(), (double)face.getStepZ());
         this.detonate(server, null);
      }
   }

   public void detonateNow() {
      if (!this.isRemoved() && this.level() instanceof ServerLevel server) {
         this.detonate(server, null);
      }
   }

   private void detonate(ServerLevel server, @Nullable Entity directTarget) {
      Vec3 center = this.position();
      if (!this.customDetonation) {
         Fx.burst(server, ParticleTypes.EXPLOSION, center, 1, 0.0, 0.0);
         Fx.burst(server, this.igniteSeconds > 0 ? ParticleTypes.FLAME : ParticleTypes.WITCH, center, 16, 0.5, 0.2);
         Fx.sound(server, center, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 1.5F);
      }

      Entity owner = this.getOwner();

      for (LivingEntity victim : server.getEntitiesOfClass(
         LivingEntity.class, new AABB(center, center).inflate((double)this.aoeRadius), e -> e.isAlive() && e != owner && e != directTarget && !this.spares(e)
      )) {
         if (this.igniteSeconds > 0 && Harm.allowed(this, victim)) {
            victim.igniteForSeconds((float)this.igniteSeconds);
         }

         Dmg.hurt(victim, server, this.hitSource(owner), this.aoeDamage);
         if (this.slowSeconds > 0) {
            Harm.effect(this, victim, MobEffects.MOVEMENT_SLOWDOWN, this.slowSeconds * 20, 1);
         }

         Vec3 push = victim.position().subtract(center);
         if (push.lengthSqr() < 1.0E-4) {
            push = new Vec3(0.0, 0.0, 1.0);
         }

         push = push.normalize();
         Harm.push(this, victim, push.x * (double)this.knockback, this.knockbackLift, push.z * (double)this.knockback);
      }

      if (this.onDetonate != null) {
         this.onDetonate.accept(center);
      }

      if (this.onDetonateSurface != null) {
         Vec3 normal = this.hitNormal != null
            ? this.hitNormal
            : (this.lastFlightDir.lengthSqr() > 1.0E-6 ? this.lastFlightDir.scale(-1.0) : new Vec3(0.0, 1.0, 0.0));
         this.onDetonateSurface.accept(center, normal);
      }

      this.discard();
   }
}
