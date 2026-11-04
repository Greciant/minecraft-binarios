package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.Config;
import com.awakenedmod.awakened.core.rules.ServerRules;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import net.neoforged.bus.api.IEventBus;

public final class UniquePowerRoll {
   private static final String DATA_ID = "awakened_unique_power_roll";

   private UniquePowerRoll() {
   }

   private static UniquePowerRoll.State state(MinecraftServer server) {
      return (UniquePowerRoll.State)server.overworld()
         .getDataStorage()
         .computeIfAbsent(new Factory(UniquePowerRoll.State::new, UniquePowerRoll.State::load, null), "awakened_unique_power_roll");
   }

   public static boolean enabled(MinecraftServer server) {
      return server != null && ServerRules.uniquePowers();
   }

   public static AwakenedConfigs.Saved set(MinecraftServer server, boolean value) {
      return AwakenedConfigs.write(ServerRules.UNIQUE_POWERS, value, server);
   }

   public static void register(IEventBus gameBus) {
      AwakenedConfigs.moved(ServerRules.STALE_AFTER_DAYS, Config.OLD_UNIQUE_POWERS_STALE_AFTER_DAYS);
      gameBus.addListener(e -> {
         UniquePowerRoll.State state = state(e.getServer());
         AwakenedConfigs.adopt(ServerRules.UNIQUE_POWERS, state.enabled() ? Boolean.TRUE : null, e.getServer(), () -> state.set(false));
      });
   }

   public static final class State extends SavedData {
      private boolean enabled;

      State() {
      }

      State(boolean enabled) {
         this.enabled = enabled;
      }

      static UniquePowerRoll.State load(CompoundTag tag, Provider provider) {
         return new UniquePowerRoll.State(tag.getBoolean("enabled"));
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
