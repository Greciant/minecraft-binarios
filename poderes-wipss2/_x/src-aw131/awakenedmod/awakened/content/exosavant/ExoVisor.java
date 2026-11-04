package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class ExoVisor {
   private static final Map<UUID, Boolean> OPEN = new ConcurrentHashMap<>();

   private ExoVisor() {
   }

   public static void toggle(ServerPlayer player) {
      if (player != null) {
         PowerData data = PowerService.data(player);
         if ("exosavant".equals(data.powerId())) {
            if (ExoSuit.isSuited(player)) {
               if ((ExoLoadout.equippedMask(player) & ExoPiece.HELMET.bit()) == 0) {
                  AbilityFeedback.fail(player, "awakened.exosavant.not_built", ExoPiece.HELMET.displayName());
               } else {
                  boolean nowOpen = !isOpen(player);
                  OPEN.put(player.getUUID(), nowOpen);
                  player.serverLevel()
                     .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        nowOpen ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE,
                        SoundSource.PLAYERS,
                        0.6F,
                        1.6F
                     );
               }
            }
         }
      }
   }

   public static boolean isOpen(ServerPlayer player) {
      return Boolean.TRUE.equals(OPEN.get(player.getUUID()));
   }

   public static void clear(ServerPlayer player) {
      OPEN.remove(player.getUUID());
   }
}
