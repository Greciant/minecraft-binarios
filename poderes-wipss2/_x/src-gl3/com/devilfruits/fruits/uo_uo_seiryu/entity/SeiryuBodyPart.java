package com.devilfruits.fruits.uo_uo_seiryu.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;

public class SeiryuBodyPart extends PartEntity<SeiryuDragonEntity> {
   public final int index;
   private EntityDimensions size = EntityDimensions.scalable(0.1F, 0.1F);
   private boolean active;

   SeiryuBodyPart(SeiryuDragonEntity parent, int index) {
      super(parent);
      this.index = index;
      this.noPhysics = true;
      this.refreshDimensions();
   }

   void place(double x, double y, double z, float edge) {
      if (!this.active || Math.abs(this.size.width() - edge) > 0.05F) {
         this.size = EntityDimensions.scalable(edge, edge);
         this.refreshDimensions();
      }

      this.active = true;
      this.xo = this.getX();
      this.yo = this.getY();
      this.zo = this.getZ();
      this.setPos(x, y - (double)edge * 0.5, z);
   }

   void deactivate(double x, double y, double z) {
      if (this.active) {
         this.active = false;
         this.size = EntityDimensions.scalable(0.1F, 0.1F);
         this.refreshDimensions();
      }

      this.setPos(x, y, z);
   }

   public boolean active() {
      return this.active;
   }

   public EntityDimensions getDimensions(Pose pose) {
      return this.size;
   }

   public boolean isPickable() {
      return !this.active ? false : !this.level().isClientSide || !((SeiryuDragonEntity)this.getParent()).ownedByLocalPlayer();
   }

   public boolean hurt(DamageSource source, float amount) {
      return this.active && !this.isInvulnerableTo(source) && ((SeiryuDragonEntity)this.getParent()).hurtFromPart(this, source, amount);
   }

   public boolean is(Entity entity) {
      return entity == this || ((SeiryuDragonEntity)this.getParent()).is(entity);
   }

   @Nullable
   public ItemStack getPickResult() {
      return null;
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
