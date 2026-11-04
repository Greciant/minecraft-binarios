package com.awakenedmod.awakened.content.summoning;

import net.minecraft.server.level.ServerPlayer;

public final class SoulMenu {
   private SoulMenu() {
   }

   public static void handle(ServerPlayer player, int action, String type, int amount) {
      if (player != null) {
         SoulData souls = SummonAttachments.souls(player);
         if (action == 3) {
            souls.setBankLocked(!souls.bankLocked());
            player.setData(SummonAttachments.SOULS, souls);
         } else if (type != null && !type.isEmpty()) {
            switch (action) {
               case 0:
                  if (souls.count(type) > 0) {
                     souls.setActive(type);
                  }
                  break;
               case 1:
                  int current = souls.loadout().getOrDefault(type, 0);
                  int max = Math.max(souls.count(type), current);
                  souls.setLoadout(type, Math.max(0, Math.min(current + amount, max)));
                  break;
               case 2:
                  souls.discard(type, amount);
                  break;
               case 3:
               default:
                  return;
               case 4:
                  souls.addBlacklist(type);
                  break;
               case 5:
                  souls.removeBlacklist(type);
            }

            player.setData(SummonAttachments.SOULS, souls);
         }
      }
   }
}
