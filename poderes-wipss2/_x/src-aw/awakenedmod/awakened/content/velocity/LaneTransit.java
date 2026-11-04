package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.content.brawler.BrawlerLift;
import com.awakenedmod.awakened.core.AbilityGate;
import com.awakenedmod.awakened.core.TeleportUtil;
import com.awakenedmod.awakened.core.net.LaneTransitPayload;
import com.awakenedmod.awakened.core.quest.TravelGate;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LaneTransit {
   public static final double TRANSIT_SPEED = 5.0;
   public static final int MIN_TICKS = 15;
   private static final int STREAM_LEAD_TICKS = 5;
   private static final double LOOSE_SEARCH_RADIUS = 8.0;
   private static final int LOOSE_MAX_DROP = 8;
   private static final double MOUTH_SEARCH_RADIUS = 2.0;
   private static final int MOUTH_MAX_DROP = 3;
   private static final double DRIFT_SNAP = 0.6;
   private static final Map<UUID, LaneTransit> RIDES = new ConcurrentHashMap<>();
   private final ServerLevel level;
   private final UUID rider;
   private final Vec3 from;
   private final Vec3 to;
   private final int duration;
   private final LaneTransit.Arrival arrival;
   private int ticks;
   private boolean streamed;

   private LaneTransit(ServerLevel level, UUID rider, Vec3 from, Vec3 to, int duration, LaneTransit.Arrival arrival) {
      this.level = level;
      this.rider = rider;
      this.from = from;
      this.to = to;
      this.duration = duration;
      this.arrival = arrival;
   }

   static void register(IEventBus gameBus) {
      gameBus.addListener(LaneTransit::onServerTick);
      gameBus.addListener(LaneTransit::onLoggedOut);
      gameBus.addListener(EventPriority.LOWEST, LaneTransit::onDeath);
      gameBus.addListener(LaneTransit::onDimensionChange);
      gameBus.addListener(LaneTransit::onLevelUnload);
      gameBus.addListener(LaneTransit::onServerStopped);
      AbilityGate.register(p -> RIDES.containsKey(p.getUUID()));
      TravelGate.register(p -> RIDES.containsKey(p.getUUID()));
   }

   static int duration(double blocks) {
      return Math.max(15, (int)Math.ceil(blocks / 5.0));
   }

   public static boolean riding(Entity entity) {
      return entity != null && RIDES.containsKey(entity.getUUID());
   }

   static boolean start(ServerPlayer player, ServerLevel level, Vec3 from, Vec3 to, LaneTransit.Arrival arrival) {
      UUID id = player.getUUID();
      if (!RIDES.containsKey(id) && !TunnelBody.inside(player)) {
         int duration = duration(from.distanceTo(to));
         RIDES.put(id, new LaneTransit(level, id, from, to, duration, arrival));
         TunnelBody.hide(player);
         TunnelBody.phase(player);
         TunnelBody.fly(player);
         VelocityVisualState.setTransit(player, true);
         player.stopRiding();
         player.setDeltaMovement(Vec3.ZERO);
         player.hurtMarked = true;
         player.resetFallDistance();
         send(player, LaneTransitPayload.begin(from, to, duration));
         return true;
      } else {
         return false;
      }
   }

   static void deliver(ServerPlayer player) {
      LaneTransit ride = RIDES.get(player.getUUID());
      if (ride != null) {
         ride.finish(player, ride.level == player.serverLevel() ? LaneTransit.End.RECALLED : LaneTransit.End.STRANDED);
      }
   }

   private boolean tick(ServerPlayer player) {
      TunnelBody.phase(player);
      this.ticks++;
      if (!this.streamed && this.ticks >= this.duration - 5) {
         this.streamed = true;
         TeleportUtil.streamIn(this.level, this.to);
      }

      if (this.ticks >= this.duration) {
         return false;
      } else {
         Vec3 mark = this.from.lerp(this.to, (double)this.ticks / (double)this.duration);
         Vec3 step = mark.subtract(player.position());
         double stride = this.from.distanceTo(this.to) / (double)this.duration;
         if (step.length() > stride * 1.6) {
            player.setDeltaMovement(Vec3.ZERO);
            player.teleportTo(mark.x, mark.y, mark.z);
         } else {
            player.setDeltaMovement(step);
            player.hurtMarked = true;
         }

         player.resetFallDistance();
         return true;
      }
   }

   private void finish(ServerPlayer player, LaneTransit.End end) {
      RIDES.remove(this.rider);
      TunnelBody.release(player);
      Vec3 at = player.position();
      if (end != LaneTransit.End.LOST) {
         Vec3 target = end.toFarMouth ? this.to : at;
         TeleportUtil.streamIn(player.serverLevel(), target);
         at = this.footing(player, target, end.toFarMouth);
         player.teleportTo(at.x, at.y, at.z);
         player.setDeltaMovement(Vec3.ZERO);
         player.hurtMarked = true;
         player.resetFallDistance();
         BrawlerLift.coverFall(player, 60);
      }

      send(player, LaneTransitPayload.end());
      if (this.arrival != null) {
         this.arrival.arrive(player, at, end.toFarMouth);
      }
   }

   private Vec3 footing(ServerPlayer player, Vec3 target, boolean vouched) {
      Vec3 near = TeleportUtil.nearestSafeSpot(player, target, vouched ? 2.0 : 8.0, vouched ? 3 : 8);
      if (near != null) {
         return near;
      } else if (vouched) {
         return target;
      } else {
         Vec3 surface = TeleportUtil.surfaceSpot(player, target.x, target.z);
         return surface != null ? surface : target;
      }
   }

   private static void send(ServerPlayer player, LaneTransitPayload payload) {
      if (player.connection != null) {
         PacketDistributor.sendToPlayer(player, payload, new CustomPacketPayload[0]);
      }
   }

   private static void onServerTick(Post event) {
      if (!RIDES.isEmpty()) {
         MinecraftServer server = event.getServer();

         for (Entry<UUID, LaneTransit> entry : new ArrayList<>(RIDES.entrySet())) {
            LaneTransit ride = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && !player.isRemoved()) {
               if (player.serverLevel() != ride.level) {
                  ride.finish(player, LaneTransit.End.STRANDED);
               } else if (!ride.tick(player)) {
                  ride.finish(player, LaneTransit.End.ARRIVED);
               }
            } else {
               RIDES.remove(entry.getKey());
            }
         }
      }
   }

   private static void onLoggedOut(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         LaneTransit ride = RIDES.get(player.getUUID());
         if (ride != null) {
            ride.finish(player, ride.level == player.serverLevel() ? LaneTransit.End.RECALLED : LaneTransit.End.STRANDED);
         }
      }
   }

   private static void onDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         LaneTransit ride = RIDES.get(player.getUUID());
         if (ride != null) {
            ride.finish(player, LaneTransit.End.LOST);
         }
      }
   }

   private static void onDimensionChange(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         LaneTransit ride = RIDES.get(player.getUUID());
         if (ride != null) {
            ride.finish(player, LaneTransit.End.STRANDED);
         }
      }
   }

   private static void onLevelUnload(Unload event) {
      if (!RIDES.isEmpty() && event.getLevel() instanceof ServerLevel unloading) {
         for (Entry<UUID, LaneTransit> entry : new ArrayList<>(RIDES.entrySet())) {
            LaneTransit ride = entry.getValue();
            if (ride.level == unloading) {
               ServerPlayer player = unloading.getServer().getPlayerList().getPlayer(entry.getKey());
               if (player != null) {
                  ride.finish(player, LaneTransit.End.STRANDED);
               } else {
                  RIDES.remove(entry.getKey());
               }
            }
         }
      }
   }

   private static void onServerStopped(ServerStoppedEvent event) {
      RIDES.clear();
   }

   @FunctionalInterface
   interface Arrival {
      void arrive(ServerPlayer var1, Vec3 var2, boolean var3);
   }

   private static enum End {
      ARRIVED(true),
      RECALLED(true),
      STRANDED(false),
      LOST(false);

      private final boolean toFarMouth;

      private End(boolean toFarMouth) {
         this.toFarMouth = toFarMouth;
      }
   }
}
