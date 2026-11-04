package com.awakenedmod.awakened.content.aero;

import com.awakenedmod.awakened.core.data.AeroVisual;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class AeroVisualState {
   private AeroVisualState() {
   }

   public static AeroVisual get(Player player) {
      return (AeroVisual)player.getData(AwakenedAttachments.AERO_VISUAL);
   }

   public static void sync(ServerPlayer player) {
      if (player != null) {
         apply(player, new AeroVisual(GaleControl.isGliding(player)));
      }
   }

   public static void clear(ServerPlayer player) {
      if (player != null) {
         apply(player, AeroVisual.NONE);
      }
   }

   private static void apply(ServerPlayer player, AeroVisual next) {
      if (!get(player).equals(next)) {
         player.setData(AwakenedAttachments.AERO_VISUAL, next);
      }
   }
}
