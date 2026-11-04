package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.core.net.SyncRealisticMovementPayload;
import com.awakenedmod.awakened.core.rules.ServerRules;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RealisticMovement {
   private static final String DATA_ID = "awakened_realistic_movement";
   private static final int COYOTE_TICKS = 6;
   private static final Map<UUID, Long> LAST_FOOTED = new ConcurrentHashMap<>();
   private static volatile boolean clientEnabled;

   private RealisticMovement() {
   }

   private static RealisticMovement.State state(MinecraftServer server) {
      return (RealisticMovement.State)server.overworld()
         .getDataStorage()
         .computeIfAbsent(new Factory(RealisticMovement.State::new, RealisticMovement.State::load, null), "awakened_realistic_movement");
   }

   public static boolean enabled(MinecraftServer server) {
      return server != null && ServerRules.realisticMovement();
   }

   public static boolean enabled(ServerPlayer player) {
      return enabled(player.serverLevel().getServer());
   }

   public static AwakenedConfigs.Saved set(MinecraftServer server, boolean value) {
      return AwakenedConfigs.write(ServerRules.REALISTIC_MOVEMENT, value, server);
   }

   public static void register(IEventBus gameBus) {
      AwakenedConfigs.onReload(ServerRules.SPEC, server -> {
         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncTo(player);
         }
      });
      gameBus.addListener(e -> {
         RealisticMovement.State state = state(e.getServer());
         AwakenedConfigs.adopt(ServerRules.REALISTIC_MOVEMENT, state.enabled() ? Boolean.TRUE : null, e.getServer(), () -> state.set(false));
      });
   }

   public static void syncTo(ServerPlayer player) {
      syncTo(player, enabled(player));
   }

   private static void syncTo(ServerPlayer player, boolean value) {
      PacketDistributor.sendToPlayer(player, new SyncRealisticMovementPayload(value), new CustomPacketPayload[0]);
   }

   public static void trackFooting(ServerPlayer player) {
      if (onSolidFooting(player)) {
         LAST_FOOTED.put(player.getUUID(), player.serverLevel().getGameTime());
      }
   }

   public static void forget(UUID id) {
      LAST_FOOTED.remove(id);
   }

   public static boolean hasFooting(ServerPlayer player) {
      if (onSolidFooting(player)) {
         return true;
      } else {
         Long last = LAST_FOOTED.get(player.getUUID());
         return last != null && player.serverLevel().getGameTime() - last <= 6L;
      }
   }

   private static boolean onSolidFooting(ServerPlayer player) {
      return player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable() || player.isPassenger();
   }

   public static boolean blocksAirLaunch(ServerPlayer player) {
      return enabled(player) && !hasFooting(player);
   }

   public static void setClient(boolean value) {
      clientEnabled = value;
   }

   public static boolean clientEnabled() {
      return clientEnabled;
   }

   public static final class State extends SavedData {
      private boolean enabled;

      State() {
      }

      State(boolean enabled) {
         this.enabled = enabled;
      }

      static RealisticMovement.State load(CompoundTag tag, Provider provider) {
         return new RealisticMovement.State(tag.getBoolean("enabled"));
      }

      public CompoundTag save(CompoundTag tag, Provider provider) {
         tag.putBoolean("enabled", this.enabled);
         return tag;
      }

      boolean enabled() {
         return this.enabled;
      }

      void set(boolean value) {
         if (value != this.enabled) {
            this.enabled = value;
            this.setDirty();
         }
      }
   }
}
