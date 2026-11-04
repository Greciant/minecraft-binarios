package com.awakenedmod.awakened.core.rules;

import com.awakenedmod.awakened.core.NearDeathAwaken;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

public final class AwakeningRules {
   private static final int START_DELAY_TICKS = 40;
   private static final Map<UUID, Long> MISSED_AT = new ConcurrentHashMap<>();

   private AwakeningRules() {
   }

   static void register(IEventBus gameBus) {
      gameBus.addListener(AwakeningRules::onPlayerTick);
      gameBus.addListener(e -> MISSED_AT.remove(e.getEntity().getUUID()));
   }

   public static boolean mayAwaken(ServerPlayer player, DamageSource source) {
      if (!NearDeathAwaken.enabled(player.serverLevel().getServer())) {
         return false;
      } else if (!ServerRules.fromPlayerDamage() && PvpRules.byAnotherPlayer(source.getEntity(), player)) {
         return false;
      } else {
         double playedMinutes = (double)player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) / 20.0 / 60.0;
         if (playedMinutes < ServerRules.graceMinutes()) {
            return false;
         } else {
            double chance = ServerRules.awakenChance();
            if (chance >= 1.0) {
               return true;
            } else {
               long now = player.serverLevel().getGameTime();
               Long missed = MISSED_AT.get(player.getUUID());
               if (missed != null && now >= missed && now - missed < ServerRules.retryTicks()) {
                  return false;
               } else if (player.getRandom().nextDouble() < chance) {
                  MISSED_AT.remove(player.getUUID());
                  return true;
               } else {
                  MISSED_AT.put(player.getUUID(), now);
                  return false;
               }
            }
         }
      }
   }

   private static void onPlayerTick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player
         && player.tickCount >= 40
         && player.tickCount % 20 == 0
         && ServerRules.startAwakened()
         && player.isAlive()
         && !player.isSpectator()
         && !PowerService.data(player).awakened()
         && !PowerService.powerChangePending(player)
         && !PowerRegistry.rollable().isEmpty()) {
         PowerService.awaken(player);
      }
   }
}
