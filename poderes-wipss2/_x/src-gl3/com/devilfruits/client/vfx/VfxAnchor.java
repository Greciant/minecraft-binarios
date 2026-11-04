package com.devilfruits.client.vfx;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record VfxAnchor(int entityId, Vec3 fallback) {
   public static VfxAnchor of(@Nullable Entity entity, Vec3 fallback) {
      return new VfxAnchor(entity != null ? entity.getId() : -1, fallback);
   }

   @Nullable
   public Entity entity(ClientLevel level) {
      return this.entityId >= 0 ? level.getEntity(this.entityId) : null;
   }
}
