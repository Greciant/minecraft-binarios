package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import net.minecraft.server.level.ServerPlayer;

public final class VelocityStamina {
   private static final float TICKS_PER_SECOND = 20.0F;

   private VelocityStamina() {
   }

   public static boolean drainPerSecond(ServerPlayer player, float perSecond, String balanceKey) {
      if (player != null && !(perSecond <= 0.0F)) {
         PowerData data = PowerService.data(player);
         return data.infiniteStamina()
            ? true
            : StaminaSystem.drainSustained(data, player.serverLevel().getGameTime(), StaminaSystem.effectiveCost(perSecond / 20.0F, balanceKey)) > 0.0F;
      } else {
         return true;
      }
   }
}
