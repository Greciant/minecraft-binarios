package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.content.summoning.PossessControl;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerList.class})
public abstract class PossessSoundReachMixin {
   @Shadow
   @Final
   private List<ServerPlayer> players;

   @Inject(
      method = {"broadcast(Lnet/minecraft/world/entity/player/Player;DDDDLnet/minecraft/resources/ResourceKey;Lnet/minecraft/network/protocol/Packet;)V"},
      at = {@At("HEAD")}
   )
   private void awakened$soundToPilot(
      Player except, double x, double y, double z, double radius, ResourceKey<Level> dimension, Packet<?> packet, CallbackInfo ci
   ) {
      if (PossessControl.anyPiloting()) {
         double r2 = radius * radius;

         for (ServerPlayer sp : this.players) {
            if (sp != except && sp.level().dimension() == dimension) {
               double dx = x - sp.getX();
               double dy = y - sp.getY();
               double dz = z - sp.getZ();
               if (!(dx * dx + dy * dy + dz * dz < r2)) {
                  Vec3 view = PossessControl.pilotViewPos(sp);
                  if (view != null && view.distanceToSqr(x, y, z) < r2) {
                     sp.connection.send(packet);
                  }
               }
            }
         }
      }
   }
}
