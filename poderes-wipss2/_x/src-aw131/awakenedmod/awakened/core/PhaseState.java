package com.awakenedmod.awakened.core;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(
   modid = "awakened"
)
public final class PhaseState {
   public static final TagKey<Block> UNPHASABLE = BlockTags.create(AwId.of("unphasable"));
   private static final Set<Integer> CLIENT = ConcurrentHashMap.newKeySet();
   private static final Set<UUID> SERVER = ConcurrentHashMap.newKeySet();
   private static final Set<Integer> CLIENT_FULL = ConcurrentHashMap.newKeySet();
   private static final Map<UUID, Set<String>> SERVER_FULL = new ConcurrentHashMap<>();
   private static final String DEFAULT_SOURCE = "default";
   private static final List<PhaseState.ClientSource> CLIENT_SOURCES = new CopyOnWriteArrayList<>();
   private static final List<Predicate<Player>> CLIENT_FULL_SOURCES = new CopyOnWriteArrayList<>();

   private PhaseState() {
   }

   public static void registerClientSource(Predicate<Player> phasing, int fogColor) {
      registerClientSource(phasing, p -> fogColor);
   }

   public static void registerClientSource(Predicate<Player> phasing, ToIntFunction<Player> fogColor) {
      CLIENT_SOURCES.add(new PhaseState.ClientSource(phasing, fogColor));
   }

   @Nullable
   public static PhaseState.ClientSource clientSource(Player player) {
      for (PhaseState.ClientSource source : CLIENT_SOURCES) {
         if (source.phasing().test(player)) {
            return source;
         }
      }

      return null;
   }

   public static void setClient(int entityId, boolean phasing) {
      if (phasing) {
         CLIENT.add(entityId);
      } else {
         CLIENT.remove(entityId);
      }
   }

   public static boolean clientPhasing(int entityId) {
      return CLIENT.contains(entityId);
   }

   public static void setServer(UUID id, boolean phasing) {
      if (phasing) {
         SERVER.add(id);
      } else {
         SERVER.remove(id);
      }
   }

   public static boolean serverPhasing(UUID id) {
      return SERVER.contains(id);
   }

   public static void registerClientFullSource(Predicate<Player> phasing) {
      CLIENT_FULL_SOURCES.add(phasing);
   }

   public static boolean clientFullSource(Player player) {
      for (Predicate<Player> source : CLIENT_FULL_SOURCES) {
         if (source.test(player)) {
            return true;
         }
      }

      return false;
   }

   public static void setClientFull(int entityId, boolean phasing) {
      if (phasing) {
         CLIENT_FULL.add(entityId);
      } else {
         CLIENT_FULL.remove(entityId);
      }
   }

   public static boolean clientFullPhasing(int entityId) {
      return CLIENT_FULL.contains(entityId);
   }

   public static void setServerFull(UUID id, boolean phasing) {
      setServerFull(id, "default", phasing);
   }

   public static void setServerFull(UUID id, String source, boolean phasing) {
      if (phasing) {
         SERVER_FULL.computeIfAbsent(id, k -> ConcurrentHashMap.newKeySet()).add(source);
      } else {
         SERVER_FULL.computeIfPresent(id, (k, set) -> {
            set.remove(source);
            return set.isEmpty() ? null : set;
         });
      }
   }

   public static boolean serverFullPhasing(UUID id) {
      return SERVER_FULL.containsKey(id);
   }

   public static void dropAggro(ServerPlayer player, double radius) {
      ServerLevel level = player.serverLevel();

      for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius), m -> m.getTarget() == player)) {
         mob.setTarget(null);
      }
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      SERVER.clear();
      SERVER_FULL.clear();
   }

   public static void clearClient() {
      CLIENT.clear();
      CLIENT_FULL.clear();
   }

   public static record ClientSource(Predicate<Player> phasing, ToIntFunction<Player> fogColor) {
      public int fogColor(Player player) {
         return this.fogColor.applyAsInt(player);
      }
   }
}
