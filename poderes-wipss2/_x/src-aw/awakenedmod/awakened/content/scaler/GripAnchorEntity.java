package com.awakenedmod.awakened.content.scaler;

import com.awakenedmod.awakened.content.AwakenedEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class GripAnchorEntity extends Entity {
   public GripAnchorEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
   }

   public static GripAnchorEntity spawn(ServerLevel level, Vec3 pos) {
      GripAnchorEntity anchor = new GripAnchorEntity(AwakenedEntities.GRIP_ANCHOR.get(), level);
      anchor.setPos(pos.x, pos.y, pos.z);
      level.addFreshEntity(anchor);
      return anchor;
   }

   public static void mount(LivingEntity rider, GripAnchorEntity seat) {
      rider.startRiding(seat, true);
   }

   public static void detach(LivingEntity rider, GripAnchorEntity seat) {
      if (seat != null) {
         if (rider != null && rider.getVehicle() == seat) {
            rider.stopRiding();
         }

         seat.discard();
      }
   }

   public boolean shouldBeSaved() {
      return false;
   }

   public boolean isPickable() {
      return false;
   }

   public boolean shouldRiderSit() {
      return false;
   }

   public boolean hurt(DamageSource source, float amount) {
      return false;
   }

   protected void defineSynchedData(Builder builder) {
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }
}
