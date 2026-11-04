package com.awakenedmod.awakened.content.earth;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import com.awakenedmod.awakened.core.team.Teams;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class StoneMechEntity extends Monster {
   private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(StoneMechEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Float> DATA_PROGRESS = SynchedEntityData.defineId(StoneMechEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> DATA_ASSEMBLING = SynchedEntityData.defineId(StoneMechEntity.class, EntityDataSerializers.BOOLEAN);
   private static final float STAMINA_PER_DAMAGE = 0.5F;

   public StoneMechEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 200.0)
         .add(Attributes.MOVEMENT_SPEED, 0.22)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.ATTACK_DAMAGE, 16.0)
         .add(Attributes.ARMOR, 14.0)
         .add(Attributes.STEP_HEIGHT, 1.5);
   }

   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_ACTION, (byte)0);
      builder.define(DATA_PROGRESS, 0.0F);
      builder.define(DATA_ASSEMBLING, false);
   }

   protected void registerGoals() {
   }

   public byte getAction() {
      return (Byte)this.entityData.get(DATA_ACTION);
   }

   public void setAction(byte action) {
      this.entityData.set(DATA_ACTION, action);
   }

   public float getActionProgress() {
      return (Float)this.entityData.get(DATA_PROGRESS);
   }

   public void setActionProgress(float progress) {
      this.entityData.set(DATA_PROGRESS, progress);
   }

   public boolean isAssembling() {
      return (Boolean)this.entityData.get(DATA_ASSEMBLING);
   }

   public void setAssembling(boolean assembling) {
      this.entityData.set(DATA_ASSEMBLING, assembling);
   }

   public void checkDespawn() {
   }

   public boolean hurt(DamageSource source, float amount) {
      if (this.level() instanceof ServerLevel level) {
         ServerPlayer owner = StoneMechControl.ownerOf(this);
         return owner != null && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
            ? absorbAsStamina(level, owner, this, source, amount, this.position())
            : super.hurt(source, amount);
      } else {
         return false;
      }
   }

   static boolean absorbAsStamina(ServerLevel level, ServerPlayer owner, StoneMechEntity mech, DamageSource source, float amount, Vec3 fxPos) {
      if (source.is(DamageTypeTags.IS_FALL) || source.getEntity() == owner || source.getEntity() == mech) {
         return false;
      } else if (Teams.harmForbidden(source.getEntity(), mech)) {
         return false;
      } else {
         PowerData data = PowerService.data(owner);
         if (!data.infiniteStamina()) {
            float remaining = data.stamina() - StaminaSystem.effectiveCost(amount * 0.5F, StoneMechAbility.class.getSimpleName());
            if (remaining <= 0.0F) {
               data.setStamina(0.0F);
               PowerService.sync(owner);
               EarthFx.mechHit(level, fxPos);
               return false;
            }

            data.setStamina(remaining);
            PowerService.sync(owner);
         }

         EarthFx.mechHit(level, fxPos);
         return false;
      }
   }

   public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
      return false;
   }
}
