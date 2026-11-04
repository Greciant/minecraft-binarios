package com.devilfruits.fruits.yomi_yomi.entity;

import com.devilfruits.fruits.yomi_yomi.soul.SoulPhase;
import com.devilfruits.fruits.yomi_yomi.soul.YomiSoul;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PlayerCorpse extends Entity {
   private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(PlayerCorpse.class, EntityDataSerializers.OPTIONAL_UUID);
   private static final EntityDataAccessor<String> DATA_NAME = SynchedEntityData.defineId(PlayerCorpse.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(PlayerCorpse.class, EntityDataSerializers.FLOAT);
   private int hurtTicks;

   public PlayerCorpse(EntityType<? extends PlayerCorpse> type, Level level) {
      super(type, level);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_OWNER, Optional.empty());
      builder.define(DATA_NAME, "");
      builder.define(DATA_HEALTH, 20.0F);
   }

   public void setup(ServerPlayer owner, float health) {
      this.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.yBodyRot, 0.0F);
      this.entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
      this.entityData.set(DATA_NAME, owner.getGameProfile().getName());
      this.entityData.set(DATA_HEALTH, health);
   }

   @Nullable
   public UUID ownerId() {
      return (UUID)((Optional)this.entityData.get(DATA_OWNER)).orElse(null);
   }

   public String ownerName() {
      return (String)this.entityData.get(DATA_NAME);
   }

   public float health() {
      return (Float)this.entityData.get(DATA_HEALTH);
   }

   public int hurtTicks() {
      return this.hurtTicks;
   }

   public Component getName() {
      return Component.translatable("entity.devilfruits.yomi_yomi.player_corpse.of", new Object[]{this.ownerName()});
   }

   public InteractionResult interact(Player player, InteractionHand hand) {
      if (hand == InteractionHand.MAIN_HAND && player.getUUID().equals(this.ownerId())) {
         if (player instanceof ServerPlayer soul) {
            SoulPhase.requestPossession(soul, this);
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide());
      } else {
         return InteractionResult.PASS;
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      if (!this.level().isClientSide() && !this.isRemoved() && !this.isInvulnerableTo(source)) {
         if (source.getEntity() != null && source.getEntity().getUUID().equals(this.ownerId())) {
            return false;
         } else {
            boolean always = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
            if (!always && !(source.getEntity() instanceof Player) && !source.is(DamageTypeTags.IS_EXPLOSION) && !source.is(DamageTypeTags.IS_FIRE)) {
               return false;
            } else {
               float left = this.health() - amount;
               this.entityData.set(DATA_HEALTH, Math.max(0.0F, left));
               this.hurtTicks = 0;
               this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.6F, 0.7F);
               if (left <= 0.0F) {
                  this.destroy();
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   private void destroy() {
      ServerLevel level = (ServerLevel)this.level();
      level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.3, this.getZ(), 24, 0.6, 0.2, 0.6, 0.02);
      level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SKELETON_DEATH, SoundSource.PLAYERS, 1.0F, 0.6F);
      UUID owner = this.ownerId();
      this.discard();
      SoulPhase.onCorpseDestroyed(level, owner);
   }

   public boolean isPickable() {
      return !this.isRemoved();
   }

   public boolean isPushable() {
      return false;
   }

   public boolean canBeHitByProjectile() {
      return false;
   }

   public void tick() {
      super.tick();
      this.hurtTicks++;
      if (!this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0).scale(0.98));
         this.move(MoverType.SELF, this.getDeltaMovement());
      } else {
         this.setDeltaMovement(Vec3.ZERO);
      }

      if (!this.level().isClientSide() && this.getY() < (double)(this.level().getMinBuildHeight() - 32)) {
         this.destroy();
      } else if (!this.level().isClientSide() && this.tickCount % 100 == 0 && this.orphaned()) {
         this.discard();
      }
   }

   private boolean orphaned() {
      UUID owner = this.ownerId();
      ServerPlayer player = owner == null ? null : ((ServerLevel)this.level()).getServer().getPlayerList().getPlayer(owner);
      return owner == null ? true : player != null && !this.getUUID().equals(YomiSoul.of(player).corpseId());
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(DATA_OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
      this.entityData.set(DATA_NAME, tag.getString("OwnerName"));
      this.entityData.set(DATA_HEALTH, tag.contains("Health") ? tag.getFloat("Health") : 20.0F);
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      UUID owner = this.ownerId();
      if (owner != null) {
         tag.putUUID("Owner", owner);
      }

      tag.putString("OwnerName", this.ownerName());
      tag.putFloat("Health", this.health());
   }

   public boolean shouldBeSaved() {
      return !this.isRemoved();
   }
}
