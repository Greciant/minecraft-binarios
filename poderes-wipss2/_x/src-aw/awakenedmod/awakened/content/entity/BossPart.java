package com.awakenedmod.awakened.content.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.entity.PartEntity;

public final class BossPart<T extends LivingEntity & MultipartThreat> extends PartEntity<T> {
   private final String name;
   private final EntityDimensions size;

   public BossPart(T parent, String name, float width, float height) {
      super(parent);
      this.name = name;
      this.size = EntityDimensions.scalable(width, height);
      this.refreshDimensions();
   }

   public String partName() {
      return this.name;
   }

   public boolean hurt(DamageSource source, float amount) {
      return !this.isInvulnerableTo(source) && this.level() instanceof ServerLevel level
         ? ((MultipartThreat)((LivingEntity)this.getParent())).hurtPart(this, level, source, amount)
         : false;
   }

   public boolean isPickable() {
      return true;
   }

   public boolean is(Entity entity) {
      return this == entity || this.getParent() == entity;
   }

   public EntityDimensions getDimensions(Pose pose) {
      return this.size;
   }

   public boolean shouldBeSaved() {
      return false;
   }

   protected void defineSynchedData(Builder builder) {
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }
}
