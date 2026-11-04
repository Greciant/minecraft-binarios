package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.core.rules.ServerRules;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public final class NearDeathAwaken {
   private static final String DATA_ID = "awakened_low_hp_trigger";

   private NearDeathAwaken() {
   }

   private static NearDeathAwaken.State state(MinecraftServer server) {
      return (NearDeathAwaken.State)server.overworld()
         .getDataStorage()
         .computeIfAbsent(new Factory(NearDeathAwaken.State::new, NearDeathAwaken.State::load, null), "awakened_low_hp_trigger");
   }

   public static boolean enabled(MinecraftServer server) {
      return ServerRules.nearDeath();
   }

   public static AwakenedConfigs.Saved set(MinecraftServer server, boolean value) {
      return AwakenedConfigs.write(ServerRules.NEAR_DEATH, value, server);
   }

   public static void migrateWorldFlag(MinecraftServer server) {
      NearDeathAwaken.State state = state(server);
      AwakenedConfigs.adopt(ServerRules.NEAR_DEATH, state.enabled() ? null : Boolean.FALSE, server, () -> state.set(true));
   }

   public static final class State extends SavedData {
      private boolean enabled;

      State() {
         this.enabled = true;
      }

      State(boolean enabled) {
         this.enabled = enabled;
      }

      static NearDeathAwaken.State load(CompoundTag tag, Provider provider) {
         return new NearDeathAwaken.State(tag.contains("enabled") ? tag.getBoolean("enabled") : true);
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
