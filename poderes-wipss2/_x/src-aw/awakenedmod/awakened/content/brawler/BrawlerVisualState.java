package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.BrawlerVisual;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class BrawlerVisualState {
   private BrawlerVisualState() {
   }

   public static BrawlerVisual get(Player player) {
      return (BrawlerVisual)player.getData(AwakenedAttachments.BRAWLER_VISUAL);
   }

   private static void apply(ServerPlayer player, BrawlerVisual next) {
      if (!get(player).equals(next)) {
         player.setData(AwakenedAttachments.BRAWLER_VISUAL, next);
      }
   }

   private static int now(ServerPlayer player) {
      return (int)player.serverLevel().getGameTime();
   }

   public static void extend(ServerPlayer player, LivingEntity target) {
      BrawlerVisual current = get(player);
      int since = current.phase() == 1 ? current.since() : now(player);
      apply(player, new BrawlerVisual(1, target.getId(), 0.0, 0.0, 0.0, since));
   }

   public static void hold(ServerPlayer player, LivingEntity target) {
      BrawlerVisual current = get(player);
      int since = current.phase() == 2 ? current.since() : now(player);
      apply(player, new BrawlerVisual(2, target.getId(), 0.0, 0.0, 0.0, since));
   }

   public static void retract(ServerPlayer player) {
      BrawlerVisual current = get(player);
      if (current.attached()) {
         Entity target = player.serverLevel().getEntity(current.target());
         if (target != null && target.isAlive()) {
            Vec3 tip = target.position().add(0.0, (double)target.getBbHeight() * 0.55, 0.0);
            apply(player, new BrawlerVisual(3, -1, tip.x, tip.y, tip.z, now(player)));
         } else {
            clear(player);
         }
      }
   }

   public static void tickRetract(ServerPlayer player) {
      BrawlerVisual current = get(player);
      if (current.phase() == 3 && now(player) - current.since() >= 3) {
         clear(player);
      }
   }

   public static void clear(ServerPlayer player) {
      apply(player, BrawlerVisual.IDLE);
   }
}
