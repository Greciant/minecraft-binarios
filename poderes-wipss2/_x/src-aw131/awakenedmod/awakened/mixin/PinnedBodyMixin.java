package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.content.scaler.GripAnchorEntity;
import com.awakenedmod.awakened.core.BodyPinState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({Player.class})
public abstract class PinnedBodyMixin extends LivingEntity {
   protected PinnedBodyMixin(EntityType<? extends LivingEntity> type, Level level) {
      super(type, level);
   }

   public boolean isPushable() {
      return !this.awakened$pinned() && !this.awakened$gripped() && super.isPushable();
   }

   protected void pushEntities() {
      if (!this.awakened$pinned() && !this.awakened$gripped()) {
         super.pushEntities();
      }
   }

   private boolean awakened$gripped() {
      return this.getVehicle() instanceof GripAnchorEntity;
   }

   private boolean awakened$pinned() {
      return this.level().isClientSide() ? BodyPinState.clientPinned(this.getId()) : BodyPinState.serverPinned(this.getUUID());
   }
}
