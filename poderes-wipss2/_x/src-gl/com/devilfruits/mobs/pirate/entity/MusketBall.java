package com.devilfruits.mobs.pirate.entity;

import com.devilfruits.core.npc.FactionFighter;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.physics.StructureForces;
import com.devilfruits.mobs.pirate.PirateConfig;
import com.devilfruits.mobs.pirate.Pirates;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MusketBall extends ThrowableItemProjectile {
   public static final double GRAVITY = 0.006;
   private float damage = 6.0F;
   private float piercing = 0.25F;
   private boolean heavy;
   private int life;

   public MusketBall(EntityType<? extends MusketBall> type, Level level) {
      super(type, level);
   }

   public MusketBall(Level level, LivingEntity shooter) {
      super(Pirates.MUSKET_BALL_ENTITY.get(), shooter, level);
   }

   public static MusketBall fire(LivingEntity shooter, Vec3 muzzle, Vec3 dir, float speed, float inaccuracy, float damage, float piercing, boolean musket) {
      MusketBall ball = new MusketBall(shooter.level(), shooter);
      ball.setPos(muzzle);
      ball.shoot(dir.x, dir.y, dir.z, speed, inaccuracy);
      ball.damage = damage;
      ball.piercing = piercing;
      ball.heavy = musket;
      shooter.level().addFreshEntity(ball);
      Level level = shooter.level();
      level.playSound(
         null,
         muzzle.x,
         muzzle.y,
         muzzle.z,
         (musket ? Pirates.MUSKET_FIRE : Pirates.PISTOL_FIRE).get(),
         SoundSource.HOSTILE,
         musket ? 2.2F : 1.6F,
         0.9F + level.random.nextFloat() * 0.15F
      );
      level.playSound(null, muzzle.x, muzzle.y, muzzle.z, Pirates.GUN_BOOM.get(), SoundSource.HOSTILE, musket ? 1.4F : 0.8F, musket ? 0.55F : 0.75F);
      if (level instanceof ServerLevel server) {
         Vec3 d = dir.normalize();
         server.sendParticles(
            Pirates.MUZZLE_FLASH.get(), muzzle.x + d.x * 0.3, muzzle.y + d.y * 0.3, muzzle.z + d.z * 0.3, 0, musket ? 1.1 : 0.75, 0.0, 0.0, 1.0
         );
         server.sendParticles(ParticleTypes.FLAME, muzzle.x + d.x * 0.15, muzzle.y + d.y * 0.15, muzzle.z + d.z * 0.15, 3, 0.03, 0.03, 0.03, 0.01);

         for (int i = 0; i < (musket ? 10 : 6); i++) {
            double k = 0.05 + (double)i * 0.03;
            server.sendParticles(
               ParticleTypes.SMOKE,
               muzzle.x + d.x * (double)i * 0.12,
               muzzle.y + d.y * (double)i * 0.12,
               muzzle.z + d.z * (double)i * 0.12,
               1,
               0.04,
               0.04,
               0.04,
               k * 0.1
            );
         }

         server.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, musket ? 3 : 1, 0.08, 0.08, 0.08, 0.01);
      }

      return ball;
   }

   protected Item getDefaultItem() {
      return (Item)Pirates.MUSKET_BALL.get();
   }

   protected double getDefaultGravity() {
      return 0.006;
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         if (this.tickCount > 1) {
            this.level().addParticle(ParticleTypes.SMOKE, this.xo, this.yo, this.zo, 0.0, 0.005, 0.0);
         }
      } else if (++this.life > 80) {
         this.discard();
      }
   }

   protected boolean canHitEntity(Entity target) {
      return !PirateConfig.FRIENDLY_FIRE.get()
            && this.getOwner() instanceof FactionFighter owner
            && target instanceof FactionFighter other
            && owner.isAlliedTo(other)
         ? false
         : super.canHitEntity(target);
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      Entity target = result.getEntity();
      LivingEntity owner = this.getOwner() instanceof LivingEntity l ? l : null;
      DamageSource source = this.damageSources().mobProjectile(this, owner);
      float amount = this.damage;
      if (target instanceof LivingEntity living && this.piercing > 0.0F && amount > 0.0F) {
         float armor = (float)living.getAttributeValue(Attributes.ARMOR);
         float toughness = (float)living.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
         float after = CombatRules.getDamageAfterAbsorb(living, amount, source, armor, toughness);
         if (after > 0.01F && after < amount) {
            amount *= 1.0F - this.piercing + this.piercing * amount / after;
         }
      }

      if (target.hurt(source, amount) && target instanceof LivingEntity livingx) {
         Vec3 v = this.getDeltaMovement().normalize();
         livingx.knockback(this.heavy ? 0.5 : 0.25, -v.x, -v.z);
      }

      this.level().playSound(null, this.getX(), this.getY(), this.getZ(), Pirates.BALL_HIT.get(), SoundSource.HOSTILE, 0.6F, 1.4F);
   }

   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      if (this.level() instanceof ServerLevel server) {
         BlockState var6 = this.level().getBlockState(result.getBlockPos());
         Vec3 at = result.getLocation();
         PhysicsStructure structure = PhysicsWorld.structureAt(this.level(), result.getBlockPos());
         if (structure != null) {
            at = structure.toWorld(at);
            if ((Boolean)PirateConfig.BALL_PUSHES_STRUCTURES.get()) {
               StructureForces.apply(structure, at, this.getDeltaMovement().normalize().scale(this.heavy ? 6.0 : 3.0));
            }
         }

         server.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.05, 0.05, 0.05, 0.25);
         server.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 3, 0.05, 0.05, 0.05, 0.01);
         if (!var6.isAir()) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, var6), at.x, at.y, at.z, 6, 0.08, 0.08, 0.08, 0.1);
         }

         this.level().playSound(null, at.x, at.y, at.z, Pirates.BALL_HIT.get(), SoundSource.BLOCKS, 0.8F, 0.9F + this.level().random.nextFloat() * 0.3F);
      }
   }

   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!this.level().isClientSide()) {
         this.discard();
      }
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putFloat("Damage", this.damage);
      tag.putFloat("Piercing", this.piercing);
      tag.putBoolean("Heavy", this.heavy);
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.damage = tag.getFloat("Damage");
      this.piercing = Mth.clamp(tag.getFloat("Piercing"), 0.0F, 1.0F);
      this.heavy = tag.getBoolean("Heavy");
   }
}
