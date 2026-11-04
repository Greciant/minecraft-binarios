package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.core.voidview.VoidZones;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Item.class})
public abstract class VoidReachMixin {
   @Inject(
      method = {"getPlayerPOVHitResult"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private static void awakened$voidReach(Level level, Player holder, Fluid fluid, CallbackInfoReturnable<BlockHitResult> cir) {
      if (VoidZones.inside(holder)) {
         Vec3 eye = holder.getEyePosition();
         Vec3 end = eye.add(holder.getViewVector(1.0F).scale(holder.blockInteractionRange()));
         cir.setReturnValue(BlockHitResult.miss(end, holder.getDirection(), BlockPos.containing(end)));
      }
   }
}
