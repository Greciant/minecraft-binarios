package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.core.PhaseState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockStateBase.class})
public abstract class PhaseCollisionMixin {
   private static final double FEET_EPSILON = 1.0E-4;

   @Inject(
      method = {"getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$phaseCollision(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
      if (context instanceof EntityCollisionContext entityContext) {
         if (entityContext.getEntity() instanceof Player player && awakened$shouldPhase((BlockState)this, player, pos)) {
            cir.setReturnValue(Shapes.empty());
         }
      }
   }

   private static boolean awakened$shouldPhase(BlockState state, Player player, BlockPos pos) {
      boolean full = isFullPhasing(player);
      if (!full && !isPhasing(player)) {
         return false;
      } else if (state.is(PhaseState.UNPHASABLE)) {
         return false;
      } else {
         if (!full && !player.isShiftKeyDown()) {
            double feet = player.getY();
            if ((double)pos.getY() + 1.0 <= feet + 1.0E-4) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean isPhasing(Player p) {
      return p.level().isClientSide() ? PhaseState.clientPhasing(p.getId()) : PhaseState.serverPhasing(p.getUUID());
   }

   private static boolean isFullPhasing(Player p) {
      return p.level().isClientSide() ? PhaseState.clientFullPhasing(p.getId()) : PhaseState.serverFullPhasing(p.getUUID());
   }
}
