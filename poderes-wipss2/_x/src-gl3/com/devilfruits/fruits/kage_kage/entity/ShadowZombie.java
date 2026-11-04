package com.devilfruits.fruits.kage_kage.entity;

import com.devilfruits.core.npc.Locomotion;
import com.devilfruits.fruits.kage_kage.KageItems;
import com.devilfruits.fruits.kage_kage.shadow.ShadowVictims;
import com.devilfruits.fruits.kage_kage.shadow.Shadows;
import com.devilfruits.fruits.kage_kage.shadow.StolenShadow;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class ShadowZombie extends ShadowServant {
   public static final int RISE_TICKS = 30;
   public static final int ATTACK_TICKS = 12;
   public static final int ATTACK_HIT = 6;
   public static final int SKINS = 12;
   private static final EntityDataAccessor<Boolean> DATA_RISING = SynchedEntityData.defineId(ShadowZombie.class, EntityDataSerializers.BOOLEAN);
   @Nullable
   private StolenShadow shadow;
   private int risingTicks;
   private boolean fell;

   public ShadowZombie(EntityType<? extends ShadowZombie> type, Level level) {
      super(type, level);
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 20.0)
         .add(Attributes.ATTACK_DAMAGE, 3.0)
         .add(Attributes.MOVEMENT_SPEED, 0.25)
         .add(Attributes.FOLLOW_RANGE, 32.0)
         .add(Attributes.ARMOR, 2.0);
   }

   public static float[] stats(StolenShadow shadow) {
      float health = Mth.clamp(10.0F + shadow.maxHealth() * 0.75F, 16.0F, 150.0F);
      float attack = Mth.clamp(2.0F + shadow.attack() * 0.8F, 3.0F, 16.0F);
      return new float[]{health, attack};
   }

   public void raise(ServerPlayer owner, StolenShadow shadow) {
      this.setOwner(owner);
      this.shadow = shadow;
      float[] s = stats(shadow);
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue((double)s[0]);
      this.setHealth(s[0]);
      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue((double)s[1]);
      this.setVariant(this.getRandom().nextInt(12));
      ShadowVictims.get(owner.server).putZombie(this.getUUID(), owner.getUUID());
   }

   @Nullable
   public StolenShadow shadow() {
      return this.shadow;
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_RISING, true);
   }

   public boolean rising() {
      return (Boolean)this.entityData.get(DATA_RISING);
   }

   public int risingTicks() {
      return this.risingTicks;
   }

   @Override
   public boolean standing() {
      return !this.rising() && this.isAlive();
   }

   @Override
   public int rank() {
      return 0;
   }

   @Override
   protected int variantCount() {
      return 12;
   }

   @Override
   public Locomotion.Speeds locomotionSpeeds() {
      return new Locomotion.Speeds(1.2F, 3.5F);
   }

   @Override
   public int attackTicks() {
      return 12;
   }

   @Override
   public int attackHit() {
      return 6;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.rising()) {
         this.risingTicks++;
         if (!this.level().isClientSide() && this.risingTicks >= 30) {
            this.entityData.set(DATA_RISING, false);
         }
      }

      if (!this.level().isClientSide() && this.isAlive() && this.tickCount % 20 == 7 && this.level() instanceof ServerLevel server) {
         UUID owner = this.ownerId();
         if (this.shadow == null || owner == null || !owner.equals(ShadowVictims.get(server.getServer()).owner(this.shadow.victim()))) {
            this.collapse();
         }
      }
   }

   public void collapse() {
      if (this.isAlive()) {
         this.hurt(this.damageSources().genericKill(), Float.MAX_VALUE);
      }
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      return this.rising() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) ? false : super.hurt(source, amount);
   }

   @Override
   public void die(DamageSource source) {
      boolean first = !this.dead && !this.isRemoved();
      super.die(source);
      if (first && this.level() instanceof ServerLevel server) {
         this.fall(server);
      }
   }

   private void fall(ServerLevel server) {
      if (!this.fell) {
         this.fell = true;
         ShadowVictims.get(server.getServer()).removeZombie(this.getUUID());
         if (this.shadow != null && this.ownerId() != null && this.ownerId().equals(ShadowVictims.get(server.getServer()).owner(this.shadow.victim()))) {
            Shadows.returnHome(server.getServer(), this.shadow.victim());
         }

         server.addFreshEntity(new ItemEntity(server, this.getX(), this.getY() + 0.3, this.getZ(), new ItemStack((ItemLike)KageItems.HOLLOW_CORPSE.get())));
      }
   }

   public void remove(RemovalReason reason) {
      if (reason.shouldDestroy() && this.level() instanceof ServerLevel server) {
         ShadowVictims.get(server.getServer()).removeZombie(this.getUUID());
      }

      super.remove(reason);
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.ZOMBIE_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.ZOMBIE_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ZOMBIE_DEATH;
   }

   public float getVoicePitch() {
      return 0.8F;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.shadow != null) {
         StolenShadow.CODEC.encodeStart(NbtOps.INSTANCE, this.shadow).result().ifPresent(t -> tag.put("Shadow", t));
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.shadow = tag.contains("Shadow") ? (StolenShadow)StolenShadow.CODEC.parse(NbtOps.INSTANCE, tag.get("Shadow")).result().orElse(null) : null;
      this.entityData.set(DATA_RISING, false);
   }
}
