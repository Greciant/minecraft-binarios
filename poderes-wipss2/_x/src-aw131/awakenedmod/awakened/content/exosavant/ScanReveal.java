package com.awakenedmod.awakened.content.exosavant;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class ScanReveal {
   private static final int REVEAL_LIFETIME_TICKS = 180;
   private static final ExpiringTargetSet REVEALED = new ExpiringTargetSet(56.0);

   private ScanReveal() {
   }

   public static void reveal(ServerPlayer player, LivingEntity target) {
      if (player != null && target != null && target != player && ExoTags.taggable(target, player)) {
         REVEALED.add(player, target, 180);
      }
   }

   public static List<LivingEntity> revealed(ServerPlayer player) {
      return REVEALED.live(player);
   }

   public static void tick(ServerPlayer player) {
      REVEALED.prune(player);
   }

   public static void clear(ServerPlayer player) {
      REVEALED.clear(player);
   }
}
