package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.core.rules.ServerRules;
import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public final class PowerOwnershipRegistry {
   private static final String DATA_ID = "awakened_power_ownership";
   public static final long NO_MAX_AGE = 0L;

   private PowerOwnershipRegistry() {
   }

   private static PowerOwnershipRegistry.State state(MinecraftServer server) {
      return (PowerOwnershipRegistry.State)server.overworld()
         .getDataStorage()
         .computeIfAbsent(new Factory(PowerOwnershipRegistry.State::new, PowerOwnershipRegistry.State::load, null), "awakened_power_ownership");
   }

   public static void set(MinecraftServer server, UUID player, String powerId) {
      state(server).set(player, powerId);
   }

   public static void clear(MinecraftServer server, UUID player) {
      state(server).clear(player);
   }

   public static void renameAll(MinecraftServer server, String from, String to) {
      if (server != null) {
         state(server).rename(from, to);
      }
   }

   public static long staleAfterMillis() {
      double days = ServerRules.staleAfterDays();
      return days <= 0.0 ? 0L : (long)(days * 24.0 * 60.0 * 60.0 * 1000.0);
   }

   public static boolean isStale(MinecraftServer server, UUID player) {
      long maxAgeMillis = staleAfterMillis();
      if (server != null && maxAgeMillis > 0L) {
         PowerOwnershipRegistry.Entry entry = state(server).owners.get(player);
         return entry != null && System.currentTimeMillis() - entry.lastSeenMillis() > maxAgeMillis;
      } else {
         return false;
      }
   }

   public static Map<UUID, String> all(MinecraftServer server) {
      return all(server, 0L);
   }

   public static Map<UUID, String> all(MinecraftServer server, long maxAgeMillis) {
      if (server == null) {
         return Map.of();
      } else {
         Map<UUID, PowerOwnershipRegistry.Entry> owners = state(server).owners;
         long now = System.currentTimeMillis();
         Map<UUID, String> result = new HashMap<>();
         owners.forEach((uuid, entry) -> {
            if (maxAgeMillis <= 0L || now - entry.lastSeenMillis() <= maxAgeMillis) {
               result.put(uuid, entry.powerId());
            }
         });
         return Collections.unmodifiableMap(result);
      }
   }

   public static record Entry(String powerId, long lastSeenMillis) {
   }

   public static final class State extends SavedData {
      private static final Codec<UUID> UUID_STRING_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
      private final Map<UUID, PowerOwnershipRegistry.Entry> owners;

      State() {
         this(Map.of());
      }

      State(Map<UUID, PowerOwnershipRegistry.Entry> owners) {
         this.owners = new HashMap<>(owners);
      }

      static PowerOwnershipRegistry.State load(CompoundTag tag, Provider provider) {
         Map<UUID, PowerOwnershipRegistry.Entry> owners = new HashMap<>();
         CompoundTag ownersTag = tag.getCompound("owners");

         for (String key : ownersTag.getAllKeys()) {
            CompoundTag entryTag = ownersTag.getCompound(key);
            owners.put(UUID.fromString(key), new PowerOwnershipRegistry.Entry(entryTag.getString("powerId"), entryTag.getLong("lastSeenMillis")));
         }

         return new PowerOwnershipRegistry.State(owners);
      }

      public CompoundTag save(CompoundTag tag, Provider provider) {
         CompoundTag ownersTag = new CompoundTag();
         this.owners.forEach((uuid, entry) -> {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("powerId", entry.powerId());
            entryTag.putLong("lastSeenMillis", entry.lastSeenMillis());
            ownersTag.put(uuid.toString(), entryTag);
         });
         tag.put("owners", ownersTag);
         return tag;
      }

      void set(UUID player, String powerId) {
         this.owners.put(player, new PowerOwnershipRegistry.Entry(powerId, System.currentTimeMillis()));
         this.setDirty();
      }

      void clear(UUID player) {
         if (this.owners.remove(player) != null) {
            this.setDirty();
         }
      }

      void rename(String from, String to) {
         boolean changed = false;

         for (Map.Entry<UUID, PowerOwnershipRegistry.Entry> owner : this.owners.entrySet()) {
            PowerOwnershipRegistry.Entry entry = owner.getValue();
            if (entry.powerId().equals(from)) {
               owner.setValue(new PowerOwnershipRegistry.Entry(to, entry.lastSeenMillis()));
               changed = true;
            }
         }

         if (changed) {
            this.setDirty();
         }
      }
   }
}
