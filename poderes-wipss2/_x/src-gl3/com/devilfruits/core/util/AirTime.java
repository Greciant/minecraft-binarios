package com.devilfruits.core.util;

import com.devilfruits.mixin.ServerGamePacketListenerImplAccessor;
import net.minecraft.server.level.ServerPlayer;

public final class AirTime {
   public static void keepAirborne(ServerPlayer player) {
      if (player.connection != null) {
         ((ServerGamePacketListenerImplAccessor)player.connection).devilfruits$setAboveGroundTickCount(0);
      }
   }

   private AirTime() {
   }
}
