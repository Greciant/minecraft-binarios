package com.awakenedmod.awakened.core.trade;

import com.awakenedmod.awakened.core.AwakenedConfigs;
import com.awakenedmod.awakened.core.net.SyncTradeRulePayload;
import java.util.Optional;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TradeRule {
   private static final String DATA_ID = "awakened_power_trading";
   private static volatile boolean clientAllowed = true;

   private TradeRule() {
   }

   public static void register(IEventBus gameBus) {
      AwakenedConfigs.onReload(TradeConfig.SPEC, server -> {
         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncTo(player, allowed(server));
         }
      });
      gameBus.addListener(e -> {
         TradeRule.State state = state(e.getServer());
         AwakenedConfigs.adopt(TradeConfig.ALLOW_POWER_TRADING, state.allowed.orElse(null), e.getServer(), state::forget);
      });
      gameBus.addListener(event -> {
         if (event.getEntity() instanceof ServerPlayer player) {
            syncTo(player, allowed(player.serverLevel().getServer()));
         }
      });
   }

   private static TradeRule.State state(MinecraftServer server) {
      return (TradeRule.State)server.overworld()
         .getDataStorage()
         .computeIfAbsent(new Factory(TradeRule.State::new, TradeRule.State::load, null), "awakened_power_trading");
   }

   public static boolean allowed(MinecraftServer server) {
      return server != null && TradeConfig.allowedByDefault();
   }

   public static AwakenedConfigs.Saved set(MinecraftServer server, boolean value) {
      return AwakenedConfigs.write(TradeConfig.ALLOW_POWER_TRADING, value, server);
   }

   private static void syncTo(ServerPlayer player, boolean value) {
      PacketDistributor.sendToPlayer(player, new SyncTradeRulePayload(value), new CustomPacketPayload[0]);
   }

   public static void setClient(boolean value) {
      clientAllowed = value;
   }

   public static boolean clientAllowed() {
      return clientAllowed;
   }

   public static final class State extends SavedData {
      private Optional<Boolean> allowed = Optional.empty();

      State() {
      }

      State(Optional<Boolean> allowed) {
         this.allowed = allowed;
      }

      static TradeRule.State load(CompoundTag tag, Provider provider) {
         return new TradeRule.State(tag.contains("allow_power_trading") ? Optional.of(tag.getBoolean("allow_power_trading")) : Optional.empty());
      }

      public CompoundTag save(CompoundTag tag, Provider provider) {
         this.allowed.ifPresent(value -> tag.putBoolean("allow_power_trading", value));
         return tag;
      }

      void forget() {
         if (this.allowed.isPresent()) {
            this.allowed = Optional.empty();
            this.setDirty();
         }
      }
   }
}
