package com.awakenedmod.awakened.core.quest;

import com.awakenedmod.awakened.core.skill.MiningThrottle;
import com.awakenedmod.awakened.core.terrain.TempTerrain;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

public final class QuestActivityEvents {
   private static final double TELEPORT_CUTOFF = 16.0;
   private static final long DAY_TICKS = 24000L;
   private static final int POLL_INTERVAL_TICKS = 20;
   private static final Map<UUID, QuestActivityEvents.Tracker> TRACKERS = new ConcurrentHashMap<>();
   private static final MiningThrottle MINE_THROTTLE = new MiningThrottle(1);

   private QuestActivityEvents() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(QuestActivityEvents::onBlockBreak);
      gameBus.addListener(QuestActivityEvents::onCraft);
      gameBus.addListener(QuestActivityEvents::onPlayerTick);
      gameBus.addListener(QuestActivityEvents::onLogout);
   }

   private static void onBlockBreak(BreakEvent event) {
      if (!TempTerrain.isProbe(event)) {
         if (event.getPlayer() instanceof ServerPlayer player && MINE_THROTTLE.accept(player)) {
            QuestSystem.report(player, QuestTrigger.MINE_BLOCKS, 1);
         }
      }
   }

   private static void onCraft(ItemCraftedEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && !event.getCrafting().isEmpty()) {
         QuestSystem.onItemCrafted(player, BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem()).toString());
      }
   }

   private static void onPlayerTick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         QuestActivityEvents.Tracker var12 = TRACKERS.computeIfAbsent(player.getUUID(), k -> new QuestActivityEvents.Tracker());
         ServerLevel level = player.serverLevel();
         if (++var12.pollTicks >= 20) {
            var12.pollTicks = 0;
            QuestSystem.poll(player);
         }

         Vec3 pos = player.position();
         if (var12.hasLast) {
            double dx = pos.x - var12.lastX;
            double dz = pos.z - var12.lastZ;
            double step = Math.sqrt(dx * dx + dz * dz);
            if (step > 0.0 && step <= 16.0 && !TravelGate.carried(player)) {
               var12.distance += step;
               if (var12.distance >= 1.0) {
                  int whole = (int)var12.distance;
                  var12.distance -= (double)whole;
                  QuestSystem.report(player, QuestTrigger.TRAVEL_DISTANCE, whole);
               }
            }
         }

         var12.lastX = pos.x;
         var12.lastZ = pos.z;
         var12.hasLast = true;
         long dayIndex = level.getGameTime() / 24000L;
         if (!var12.hasDay) {
            var12.lastDay = dayIndex;
            var12.hasDay = true;
         } else if (dayIndex > var12.lastDay) {
            QuestSystem.report(player, QuestTrigger.SURVIVE_NIGHTS, (int)(dayIndex - var12.lastDay));
            var12.lastDay = dayIndex;
         }
      }
   }

   private static void onLogout(PlayerLoggedOutEvent event) {
      UUID id = event.getEntity().getUUID();
      TRACKERS.remove(id);
      MINE_THROTTLE.clear(id);
      QuestSystem.forget(id);
   }

   private static final class Tracker {
      private boolean hasLast;
      private double lastX;
      private double lastZ;
      private double distance;
      private boolean hasDay;
      private long lastDay;
      private int pollTicks;
   }
}
