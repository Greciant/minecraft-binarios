package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.Config;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.rules.ServerRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public final class PowerLimits {
   private static final String DATA_ID = "awakened_power_limits";
   public static final int UNLIMITED = 0;
   private static final int NO_DEFAULT = -1;
   private static final ConfigOverrides<Integer> CONFIGURED = new ConfigOverrides<>("uniquePowers.limits", ServerRules::limits, text -> {
      int value = Integer.parseInt(text);
      return value < 0 ? null : value;
   }, () -> PowerRegistry.all().stream().map(Power::id));

   private PowerLimits() {
   }

   private static PowerLimits.State state(MinecraftServer server) {
      return (PowerLimits.State)server.overworld()
         .getDataStorage()
         .computeIfAbsent(new Factory(PowerLimits.State::new, PowerLimits.State::load, null), "awakened_power_limits");
   }

   public static int limit(String powerId) {
      Integer configured = CONFIGURED.get(powerId, null);
      return configured != null ? configured : defaultLimit();
   }

   public static boolean atCapacity(String powerId, int holders) {
      int limit = limit(powerId);
      return limit != 0 && holders >= limit;
   }

   public static int defaultLimit() {
      return Math.max(0, ServerRules.defaultLimit());
   }

   public static boolean hasOwnLimit(String powerId) {
      return CONFIGURED.has(powerId);
   }

   public static AwakenedConfigs.Saved setDefault(MinecraftServer server, int limit) {
      return AwakenedConfigs.write(ServerRules.DEFAULT_LIMIT, Math.max(0, limit), server);
   }

   public static AwakenedConfigs.Saved set(MinecraftServer server, String powerId, Integer limit) {
      Map<String, Integer> change = new HashMap<>();
      change.put(powerId, limit == null ? null : Math.max(0, limit));
      return AwakenedConfigs.write(ServerRules.LIMITS, withLimits(change), server);
   }

   private static List<String> withLimits(Map<String, Integer> changes) {
      List<String> out = new ArrayList<>();

      for (String entry : ServerRules.limits()) {
         String name = AwakenedConfigs.nameOf(entry);
         if (name == null || !changes.containsKey(name)) {
            out.add(entry);
         }
      }

      changes.forEach((power, count) -> {
         if (count != null) {
            out.add(power + "=" + count);
         }
      });
      return out;
   }

   public static void register(IEventBus gameBus) {
      AwakenedConfigs.moved(ServerRules.DEFAULT_LIMIT, Config.OLD_UNIQUE_POWERS_DEFAULT_LIMIT);
      AwakenedConfigs.moved(ServerRules.LIMITS, Config.OLD_UNIQUE_POWERS_LIMITS);
      gameBus.addListener(PowerLimits::adoptWorldLimits);
   }

   private static void adoptWorldLimits(ServerStartedEvent event) {
      MinecraftServer server = event.getServer();
      PowerLimits.State state = state(server);
      AwakenedConfigs.adopt(ServerRules.DEFAULT_LIMIT, state.defaultLimit == -1 ? null : Math.max(0, state.defaultLimit), server, state::forgetDefault);
      if (!state.limits.isEmpty()) {
         Map<String, Integer> own = new TreeMap<>();
         state.limits.forEach((power, count) -> own.put(power, Math.max(0, count)));
         AwakenedConfigs.adopt(ServerRules.LIMITS, withLimits(own), server, state::forgetLimits);
      }
   }

   static final class State extends SavedData {
      private final Map<String, Integer> limits;
      private int defaultLimit;

      State() {
         this(Map.of(), -1);
      }

      State(Map<String, Integer> limits, int defaultLimit) {
         this.limits = new HashMap<>(limits);
         this.defaultLimit = defaultLimit;
      }

      static PowerLimits.State load(CompoundTag tag, Provider provider) {
         Map<String, Integer> limits = new HashMap<>();
         CompoundTag limitsTag = tag.getCompound("limits");

         for (String key : limitsTag.getAllKeys()) {
            limits.put(key, limitsTag.getInt(key));
         }

         int defaultLimit = tag.contains("defaultLimit") ? tag.getInt("defaultLimit") : -1;
         return new PowerLimits.State(limits, defaultLimit);
      }

      public CompoundTag save(CompoundTag tag, Provider provider) {
         CompoundTag limitsTag = new CompoundTag();
         this.limits.forEach(limitsTag::putInt);
         tag.put("limits", limitsTag);
         tag.putInt("defaultLimit", this.defaultLimit);
         return tag;
      }

      void forgetLimits() {
         if (!this.limits.isEmpty()) {
            this.limits.clear();
            this.setDirty();
         }
      }

      void forgetDefault() {
         if (this.defaultLimit != -1) {
            this.defaultLimit = -1;
            this.setDirty();
         }
      }
   }
}
