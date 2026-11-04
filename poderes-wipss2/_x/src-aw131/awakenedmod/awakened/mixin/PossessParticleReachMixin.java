package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.content.summoning.PossessControl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerLevel.class})
public abstract class PossessParticleReachMixin {
   @Inject(
      method = {"sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void awakened$particlesToPilot(
      ServerPlayer player, boolean longDistance, double x, double y, double z, Packet<?> packet, CallbackInfoReturnable<Boolean> cir
   ) {
      if (!cir.getReturnValueZ()) {
         if (player.serverLevel() == this) {
            Vec3 view = PossessControl.pilotViewPos(player);
            if (view != null) {
               double r = longDistance ? 512.0 : 32.0;
               if (view.distanceToSqr(x, y, z) < r * r) {
                  player.connection.send(packet);
                  cir.setReturnValue(true);
               }
            }
         }
      }
   }
}
