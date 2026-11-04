package com.awakenedmod.awakened.content.entity;

import com.awakenedmod.awakened.content.brawler.BrawlerTargets;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class VerdictHookEntity extends Entity {
   public static final byte MODE_FLYING = 0;
   public static final byte MODE_BITING = 1;
   public static final byte MODE_REELING = 2;
   public static final byte MODE_EMBEDDED = 3;
   public static final byte MODE_RETURNING = 4;
   public static final byte MODE_STRAINING = 5;
   public static final double SPEED = 1.4;
   public static final double RANGE = 16.0;
   private static final double FLAT_BLOCKS = 12.0;
   private static final double DROP = 0.045;
   private static final double HOMING_TURN = 0.22;
   private static final double BITE_RADIUS = 1.0;
   public static final float THROW_DAMAGE = 7.0F;
   private static final int HAUL_TICKS = 12;
   private static final int BITE_HOLD = 2;
   private static final double RELEASE = 2.0;
   private static final double HAUL_MIN = 0.35;
   private static final double HAUL_MAX = 1.1;
   private static final int EMBED_TICKS = 6;
   private static final double RETURN_SPEED = 2.0;
   private static final double CAUGHT = 1.5;
   private static final int STAGGER_TICKS = 20;
   private static final int STRAIN_TICKS = 8;
   private static final float BITE_KICK = 0.1F;
   private static final int MAX_LIFE = 140;
   private static final EntityDataAccessor<Integer> DATA_OWNER_ID = SynchedEntityData.defineId(VerdictHookEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Byte> DATA_MODE = SynchedEntityData.defineId(VerdictHookEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Integer> DATA_RIDE_ID = SynchedEntityData.defineId(VerdictHookEntity.class, EntityDataSerializers.INT);
   public static final double RIDE_HEIGHT = 0.55;
   private Vec3 heading = Vec3.ZERO;
   private double travelled;
   private double fall;
   private LivingEntity homing;
   private LivingEntity hooked;
   private int haulLeft;
   private int embedLeft;
   private int biteLeft;
   private ItemStack weapon = ItemStack.EMPTY;
   private Vec3 clientPrev;
   private Vec3 clientNow;

   public VerdictHookEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
   }

   public void launch(ServerPlayer thrower, Vec3 from, Vec3 dir, LivingEntity homing, ItemStack weapon) {
      this.entityData.set(DATA_OWNER_ID, thrower.getId());
      this.heading = dir.normalize();
      this.homing = homing;
      this.weapon = weapon;
      this.setPos(from);
   }

   public byte mode() {
      return (Byte)this.entityData.get(DATA_MODE);
   }

   private void setMode(byte mode) {
      this.entityData.set(DATA_MODE, mode);
   }

   public Player owner() {
      return this.level().getEntity((Integer)this.entityData.get(DATA_OWNER_ID)) instanceof Player p ? p : null;
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

   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         this.clientPrev = this.clientNow;
         this.clientNow = this.position();
      } else if (this.level() instanceof ServerLevel server) {
         if (this.owner() instanceof ServerPlayer thrower
            && thrower.isAlive()
            && !thrower.isRemoved()
            && thrower.level() == this.level()
            && this.tickCount <= 140) {
            switch (this.mode()) {
               case 0:
                  this.tickFlight(server, thrower);
                  break;
               case 1:
               case 2:
                  this.tickHaul(server, thrower);
                  break;
               case 3:
                  if (--this.embedLeft <= 0) {
                     this.setMode((byte)4);
                  }
                  break;
               case 4:
               default:
                  this.tickReturn(server, thrower);
                  break;
               case 5:
                  this.tickStrain(server);
            }

            return;
         }

         this.releaseHooked();
         this.discard();
      }
   }

   private void tickFlight(ServerLevel server, ServerPlayer thrower) {
      if (this.homing != null) {
         if (this.homing.isAlive() && !this.homing.isRemoved() && this.homing.level() == server) {
            Vec3 want = this.homing.position().add(0.0, (double)this.homing.getBbHeight() * 0.5, 0.0).subtract(this.position());
            if (want.lengthSqr() > 1.0E-4) {
               this.heading = this.heading.add(want.normalize().subtract(this.heading).scale(0.22)).normalize();
            }
         } else {
            this.homing = null;
         }
      }

      if (this.travelled > 12.0) {
         this.fall += 0.045;
      }

      Vec3 from = this.position();
      Vec3 step = this.heading.scale(1.4).add(0.0, -this.fall, 0.0);
      Vec3 to = from.add(step);
      LivingEntity bit = this.firstBody(server, thrower, from, to);
      BlockHitResult wall = server.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, this));
      boolean hitWall = wall.getType() == Type.BLOCK;
      if (bit == null || hitWall && !(from.distanceToSqr(bit.position()) < from.distanceToSqr(wall.getLocation()))) {
         if (hitWall) {
            this.setPos(wall.getLocation().subtract(step.normalize().scale(0.2)));
            this.embed(server, wall);
         } else {
            this.setPos(to);
            this.travelled = this.travelled + step.length();
            if (this.travelled >= 16.0) {
               Fx.sound(server, this.position(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.8F, 1.1F);
               this.setMode((byte)4);
            }
         }
      } else {
         this.setPos(bit.position().add(0.0, (double)bit.getBbHeight() * 0.55, 0.0));
         this.bite(server, thrower, bit);
      }
   }

   private LivingEntity firstBody(ServerLevel server, ServerPlayer thrower, Vec3 from, Vec3 to) {
      AABB sweep = new AABB(from, to).inflate(1.0);
      LivingEntity best = null;
      double bestDist = Double.MAX_VALUE;

      for (LivingEntity e : server.getEntitiesOfClass(
         LivingEntity.class, sweep, c -> c != thrower && c.isAlive() && c.attackable() && !c.isSpectator() && Harm.allowed(thrower, c)
      )) {
         if (!e.getBoundingBox().inflate(0.5).clip(from, to).isEmpty()) {
            double d = from.distanceToSqr(e.position());
            if (d < bestDist) {
               bestDist = d;
               best = e;
            }
         }
      }

      return best;
   }

   private void bite(ServerLevel server, ServerPlayer thrower, LivingEntity target) {
      this.setMode((byte)1);
      float damage = Dmg.weaponBonus(server, this.weapon, target, server.damageSources().playerAttack(thrower), 7.0F);
      Dmg.hurt(target, server, server.damageSources().playerAttack(thrower), damage);
      Fx.sound(server, this.position(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.2F, 0.7F);
      Fx.burst(server, ParticleTypes.CRIT, this.position(), 10, 0.3, 0.05);
      Fx.burst(server, ParticleTypes.WITCH, this.position(), 6, 0.25, 0.04);
      Fx.shakeFor(thrower, thrower.position(), 4.0, 0.1F, 0, 6);
      this.entityData.set(DATA_RIDE_ID, target.getId());
      if (BrawlerTargets.unliftable(target)) {
         Harm.effect(thrower, target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
         this.hooked = target;
         this.embedLeft = 8;
         this.setMode((byte)5);
      } else {
         this.hooked = target;
         this.haulLeft = 12;
         this.biteLeft = 2;
      }
   }

   private void tickStrain(ServerLevel server) {
      LivingEntity body = this.hooked;
      if (body != null && body.isAlive() && !body.isRemoved() && body.level() == server) {
         this.setPos(body.position().add(0.0, (double)body.getBbHeight() * 0.55, 0.0));
      }

      if (--this.embedLeft <= 0 || body == null || !body.isAlive()) {
         this.releaseHooked();
         Fx.sound(server, this.position(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 0.7F);
         this.setMode((byte)4);
      }
   }

   private void tickHaul(ServerLevel server, ServerPlayer thrower) {
      LivingEntity victim = this.hooked;
      if (victim != null && victim.isAlive() && !victim.isRemoved() && victim.level() == server) {
         this.setPos(victim.position().add(0.0, (double)victim.getBbHeight() * 0.55, 0.0));
         if (this.biteLeft > 0) {
            if (--this.biteLeft == 0) {
               this.setMode((byte)2);
            }
         } else {
            Vec3 pull = thrower.position()
               .add(0.0, (double)thrower.getBbHeight() * 0.5, 0.0)
               .subtract(victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0));
            double gap = pull.length();
            if (!(gap <= 2.0) && --this.haulLeft > 0) {
               double speed = Math.max(0.35, Math.min(1.1, (gap - 2.0) / (double)Math.max(1, this.haulLeft)));
               Harm.setMotion(thrower, victim, pull.normalize().scale(speed).add(0.0, 0.06, 0.0));
               victim.hurtMarked = true;
               victim.resetFallDistance();
               if (this.haulLeft % 4 == 0) {
                  Fx.sound(server, this.position(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.7F, 0.6F);
               }
            } else {
               this.releaseHooked();
               Fx.sound(server, this.position(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 1.0F, 0.8F);
               this.setMode((byte)4);
            }
         }
      } else {
         this.releaseHooked();
         this.setMode((byte)4);
      }
   }

   private void releaseHooked() {
      this.hooked = null;
      this.entityData.set(DATA_RIDE_ID, -1);
   }

   private void embed(ServerLevel server, BlockHitResult wall) {
      this.embedLeft = 6;
      this.setMode((byte)3);
      Fx.sound(server, this.position(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1.1F, 0.8F);
      Fx.burst(server, ParticleTypes.CRIT, this.position(), 6, 0.2, 0.02);
      Fx.blockSpall(server, wall.getLocation(), server.getBlockState(wall.getBlockPos()), 10);
   }

   private void tickReturn(ServerLevel server, ServerPlayer thrower) {
      Vec3 hand = thrower.position().add(0.0, (double)thrower.getBbHeight() * 0.7, 0.0);
      Vec3 span = hand.subtract(this.position());
      double gap = span.length();
      if (gap <= 1.5) {
         Fx.sound(server, this.position(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.8F, 1.2F);
         this.discard();
      } else {
         this.setPos(this.position().add(span.scale(Math.min(2.0, gap) / gap)));
      }
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

   public void remove(RemovalReason reason) {
      if (!this.level().isClientSide()) {
         this.releaseHooked();
      }

      super.remove(reason);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_OWNER_ID, -1);
      builder.define(DATA_MODE, (byte)0);
      builder.define(DATA_RIDE_ID, -1);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }
}
