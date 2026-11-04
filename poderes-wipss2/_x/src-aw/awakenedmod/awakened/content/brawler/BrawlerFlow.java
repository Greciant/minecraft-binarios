package com.awakenedmod.awakened.content.brawler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

final class BrawlerFlow {
   private static final Map<UUID, Long> HELD = new ConcurrentHashMap<>();

   private BrawlerFlow() {
   }

   static boolean active(ServerPlayer player) {
      Long until = HELD.get(player.getUUID());
      if (until != null) {
         if (player.serverLevel().getGameTime() < until) {
            return true;
         }

         HELD.remove(player.getUUID());
      }

      return BrawlerCombo.tier(player) >= 3;
   }

   static void hold(ServerPlayer player, int ticks) {
      if (ticks > 0) {
         HELD.merge(player.getUUID(), player.serverLevel().getGameTime() + (long)ticks, Math::max);
      }
   }

   static float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      return !(amount <= 0.0F) && melee(source) && active(player) ? amount * 0.7F : amount;
   }

   static boolean holdsGround(ServerPlayer player) {
      return active(player);
   }

   private static boolean melee(DamageSource source) {
      Entity direct = source.getDirectEntity();
      return direct instanceof LivingEntity && direct == source.getEntity();
   }

   static void clearPlayer(ServerPlayer player) {
      HELD.remove(player.getUUID());
   }

   static void clearAll() {
      HELD.clear();
   }
}
