package com.awakenedmod.awakened.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;

public final class SprintBlock {
   private static final List<Predicate<Player>> RULES = new CopyOnWriteArrayList<>();

   private SprintBlock() {
   }

   public static void add(Predicate<Player> rule) {
      RULES.add(rule);
   }

   public static boolean blocked(Player player) {
      for (Predicate<Player> rule : RULES) {
         if (rule.test(player)) {
            return true;
         }
      }

      return false;
   }
}
