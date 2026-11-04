package com.awakenedmod.awakened.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;

@EventBusSubscriber(
   modid = "awakened"
)
public final class MoveKeys {
   private static final Map<UUID, MoveKeys.Keys> RELAYED = new ConcurrentHashMap<>();

   private MoveKeys() {
   }

   public static MoveKeys.Keys of(ServerPlayer player) {
      return RELAYED.getOrDefault(player.getUUID(), MoveKeys.Keys.NONE);
   }

   public static void relay(ServerPlayer player, MoveKeys.Keys keys) {
      if (player != null) {
         RELAYED.put(player.getUUID(), keys);
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent event) {
      RELAYED.remove(event.getEntity().getUUID());
   }

   public static record Keys(boolean forward, boolean backward, boolean left, boolean right, boolean jump) {
      public static final MoveKeys.Keys NONE = new MoveKeys.Keys(false, false, false, false, false);

      public boolean moving() {
         return this.forward || this.backward || this.left || this.right;
      }

      public byte bits() {
         return (byte)((this.forward ? 1 : 0) | (this.backward ? 2 : 0) | (this.left ? 4 : 0) | (this.right ? 8 : 0) | (this.jump ? 16 : 0));
      }

      public static MoveKeys.Keys fromBits(byte bits) {
         return new MoveKeys.Keys((bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0, (bits & 16) != 0);
      }
   }
}
