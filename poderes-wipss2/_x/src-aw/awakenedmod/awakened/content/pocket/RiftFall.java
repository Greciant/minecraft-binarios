package com.awakenedmod.awakened.content.pocket;

import com.awakenedmod.awakened.core.Fx;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

public final class RiftFall {
   private static final int FALL_OUT_Y = RiftSpace.ENTRY.getY() - 48;

   private RiftFall() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(RiftFall::onServerTick);
   }

   private static void onServerTick(Post event) {
      ServerLevel rift = RiftSpace.riftLevel(event.getServer());
      if (rift != null) {
         for (ServerPlayer player : List.copyOf(rift.players())) {
            if (player.getY() < (double)FALL_OUT_Y) {
               fallOut(player);
            }
         }
      }
   }

   private static void fallOut(ServerPlayer player) {
      Fx.burst(player.serverLevel(), ParticleTypes.REVERSE_PORTAL, player.getEyePosition(), 30, 0.5, 0.3);
      Fx.sound(player.serverLevel(), player.position(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.3F, 1.2F);
      if (RiftSpace.returnHome(player)) {
         player.displayClientMessage(Component.translatable("awakened.pocket.fell_out"), false);
      }
   }
}
