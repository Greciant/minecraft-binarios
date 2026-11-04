package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import net.minecraft.server.level.ServerPlayer;

public final class CommandStamina {
   private CommandStamina() {
   }

   public static boolean canAfford(PowerData data, float points) {
      return data.infiniteStamina() || data.stamina() >= points;
   }

   public static float spend(ServerPlayer player, PowerData data, float points) {
      if (!(points <= 0.0F) && !data.infiniteStamina()) {
         float taken = Math.min(points, Math.max(0.0F, data.stamina()));
         data.setStamina(Math.max(0.0F, data.stamina() - points));
         data.setLastSpendTime(player.serverLevel().getGameTime());
         return taken;
      } else {
         return 0.0F;
      }
   }

   public static void hold(ServerPlayer player) {
      PowerService.sync(player);
   }

   public static float activeSum(ServerPlayer player, CommandData rules) {
      float total = rules.lockedTotal();

      for (Landings.Landing landing : Landings.of(player.getUUID())) {
         total += landing.paid;
      }

      return total;
   }

   public static void release(ServerPlayer player) {
      PowerService.sync(player);
   }

   public static void relock(ServerPlayer player, PowerData data, CommandData rules) {
      float held = activeSum(player, rules);
      if (!(held <= 0.0F) && !data.infiniteStamina()) {
         data.setStamina(Math.max(0.0F, data.stamina() - held));
         PowerService.sync(player);
      }
   }
}
