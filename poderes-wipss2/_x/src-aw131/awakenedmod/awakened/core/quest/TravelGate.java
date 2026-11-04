package com.awakenedmod.awakened.core.quest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;

public final class TravelGate {
   private static final List<Predicate<ServerPlayer>> SUPPRESSORS = new CopyOnWriteArrayList<>();

   private TravelGate() {
   }

   public static void register(Predicate<ServerPlayer> suppressor) {
      SUPPRESSORS.add(suppressor);
   }

   public static boolean carried(ServerPlayer player) {
      for (Predicate<ServerPlayer> suppressor : SUPPRESSORS) {
         if (suppressor.test(player)) {
            return true;
         }
      }

      return false;
   }
}
