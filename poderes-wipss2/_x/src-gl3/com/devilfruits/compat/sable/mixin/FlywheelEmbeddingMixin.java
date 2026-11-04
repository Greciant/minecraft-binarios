package com.devilfruits.compat.sable.mixin;

import com.devilfruits.compat.sable.client.SableShipLook;
import com.devilfruits.fruits.suke_suke.clear.ShipView;
import net.minecraft.core.Vec3i;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(
   targets = {"dev.engine_room.flywheel.backend.engine.embed.EmbeddedEnvironment"},
   remap = false
)
public abstract class FlywheelEmbeddingMixin {
   @Unique
   private static final Matrix4f devilfruits$NOWHERE = new Matrix4f().scale(0.0F);

   @Shadow
   public abstract Vec3i renderOrigin();

   @ModifyVariable(
      method = {"transforms"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private Matrix4fc devilfruits$hiddenShip(Matrix4fc pose) {
      return (Matrix4fc)(ShipView.any() && SableShipLook.hidesParts(this.renderOrigin()) ? devilfruits$NOWHERE : pose);
   }
}
