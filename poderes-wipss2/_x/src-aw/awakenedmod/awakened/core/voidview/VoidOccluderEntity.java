package com.awakenedmod.awakened.core.voidview;

import com.awakenedmod.awakened.content.vfx.AbstractFxEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class VoidOccluderEntity extends AbstractFxEntity {
   private float radius = 1.0F;

   public VoidOccluderEntity(EntityType<? extends VoidOccluderEntity> type, Level level) {
      super(type, level);
      this.lifetime = Integer.MAX_VALUE;
   }

   public float radius() {
      return this.radius;
   }

   public void setRadius(float radius) {
      this.radius = radius;
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      return true;
   }

   @Override
   protected void writeExtraSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeFloat(this.radius);
   }

   @Override
   protected void readExtraSpawnData(RegistryFriendlyByteBuf buf) {
      this.radius = buf.readFloat();
   }
}
