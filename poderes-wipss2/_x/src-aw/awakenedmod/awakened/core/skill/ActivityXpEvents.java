package com.awakenedmod.awakened.core.skill;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.terrain.TempTerrain;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;

public final class ActivityXpEvents {
   private static final double XP_PER_MINE = 0.5;
   private static final double XP_PER_PLACE = 0.4;
   private static final double FLUSH_AT = 5.0;
   private static final Map<UUID, Double> PENDING = new ConcurrentHashMap<>();
   private static final MiningThrottle MINE_THROTTLE = new MiningThrottle(1);
   private static final MiningThrottle PLACE_THROTTLE = new MiningThrottle(1);

   private ActivityXpEvents() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ActivityXpEvents::onBlockBreak);
      gameBus.addListener(ActivityXpEvents::onBlockPlace);
      gameBus.addListener(ActivityXpEvents::onLogout);
   }

   private static void onBlockBreak(BreakEvent event) {
      if (!TempTerrain.isProbe(event)) {
         if (event.getPlayer() instanceof ServerPlayer player && MINE_THROTTLE.accept(player)) {
            accrue(player, 0.5);
         }
      }
   }

   private static void onBlockPlace(EntityPlaceEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && PLACE_THROTTLE.accept(player)) {
         accrue(player, 0.4);
      }
   }

   private static void accrue(ServerPlayer player, double amount) {
      if (AwakenedAttachments.get(player).awakened()) {
         double pending = PENDING.merge(player.getUUID(), amount, Double::sum);
         if (pending >= 5.0) {
            int whole = (int)pending;
            PENDING.put(player.getUUID(), pending - (double)whole);
            SkillSystem.award(player, (double)whole);
         }
      }
   }

   private static void onLogout(PlayerLoggedOutEvent event) {
      UUID id = event.getEntity().getUUID();
      PENDING.remove(id);
      MINE_THROTTLE.clear(id);
      PLACE_THROTTLE.clear(id);
   }
}
