package com.awakenedmod.awakened.core.power;

import com.awakenedmod.awakened.core.net.AbilityFeedbackPayload;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AbilityFeedback {
   private static final Set<UUID> SPOKEN = ConcurrentHashMap.newKeySet();

   private AbilityFeedback() {
   }

   public static void fail(ServerPlayer player, String langKey, Object... args) {
      SPOKEN.add(player.getUUID());
      PacketDistributor.sendToPlayer(player, AbilityFeedbackPayload.reason(langKey, args), new CustomPacketPayload[0]);
   }

   public static void beginActivation(ServerPlayer player) {
      SPOKEN.remove(player.getUUID());
   }

   public static void spoke(ServerPlayer player) {
      SPOKEN.add(player.getUUID());
   }

   public static boolean consumeSpoken(ServerPlayer player) {
      return SPOKEN.remove(player.getUUID());
   }
}
