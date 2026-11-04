package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.core.PhaseState;
import com.awakenedmod.awakened.core.TempFlight;
import com.awakenedmod.awakened.core.net.VelocityScreenFxPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

final class TunnelBody {
   static final int FALL_GRACE_TICKS = 60;
   private static final double AGGRO_RADIUS = 32.0;

   private TunnelBody() {
   }

   static void hide(ServerPlayer player) {
      player.setInvisible(true);
      VelocityVisualState.setTunnel(player, true);
      PhaseState.dropAggro(player, 32.0);
      screenFx(player, true);
   }

   static void phase(ServerPlayer player) {
      PhaseState.setServerFull(player.getUUID(), true);
      player.setInvulnerable(true);
   }

   static void fly(ServerPlayer player) {
      if (canFlyAnyway(player)) {
         player.resetFallDistance();
      } else {
         TempFlight.grant(player);
      }
   }

   static void release(ServerPlayer player) {
      PhaseState.setServerFull(player.getUUID(), false);
      player.setInvulnerable(false);
      player.setInvisible(false);
      TempFlight.revoke(player, false);
      VelocityVisualState.setDiving(player, false);
      VelocityVisualState.setTunnel(player, false);
      VelocityVisualState.setTransit(player, false);
      screenFx(player, false);
   }

   static boolean inside(ServerPlayer player) {
      return VelocityVisualState.of(player).tunnelling();
   }

   private static boolean canFlyAnyway(ServerPlayer player) {
      return player.isCreative() || player.isSpectator();
   }

   private static void screenFx(ServerPlayer player, boolean on) {
      if (player.connection != null) {
         PacketDistributor.sendToPlayer(player, VelocityScreenFxPayload.tunnel(on), new CustomPacketPayload[0]);
      }
   }
}
