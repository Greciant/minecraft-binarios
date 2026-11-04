package com.devilfruits.fruits.kage_kage.shadow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public final class ShadowVictims extends SavedData {
   private static final String NAME = "devilfruits_kage_kage_victims";
   private final Map<UUID, UUID> owners = new HashMap<>();
   private final Map<UUID, UUID> zombies = new HashMap<>();

   public static ShadowVictims get(MinecraftServer server) {
      return (ShadowVictims)server.overworld()
         .getDataStorage()
         .computeIfAbsent(new Factory(ShadowVictims::new, ShadowVictims::load, null), "devilfruits_kage_kage_victims");
   }

   @Nullable
   public UUID owner(UUID victim) {
      return this.owners.get(victim);
   }

   public boolean isVictim(UUID victim) {
      return this.owners.containsKey(victim);
   }

   public void put(UUID victim, UUID owner) {
      this.owners.put(victim, owner);
      this.setDirty();
   }

   public void remove(UUID victim) {
      if (this.owners.remove(victim) != null) {
         this.setDirty();
      }
   }

   public List<UUID> releaseAll(UUID owner) {
      List<UUID> out = new ArrayList<>();
      this.owners.entrySet().removeIf(e -> {
         if (e.getValue().equals(owner)) {
            out.add(e.getKey());
            return true;
         } else {
            return false;
         }
      });
      if (!out.isEmpty()) {
         this.setDirty();
      }

      return out;
   }

   public int count(UUID owner) {
      return (int)this.owners.values().stream().filter(owner::equals).count();
   }

   public void putZombie(UUID zombie, UUID owner) {
      this.zombies.put(zombie, owner);
      this.setDirty();
   }

   public void removeZombie(UUID zombie) {
      if (this.zombies.remove(zombie) != null) {
         this.setDirty();
      }
   }

   public int zombies(UUID owner) {
      return (int)this.zombies.values().stream().filter(owner::equals).count();
   }

   private static ShadowVictims load(CompoundTag tag, Provider registries) {
      ShadowVictims v = new ShadowVictims();

      for (Tag t : tag.getList("victims", 10)) {
         CompoundTag e = (CompoundTag)t;
         if (e.hasUUID("victim") && e.hasUUID("owner")) {
            v.owners.put(e.getUUID("victim"), e.getUUID("owner"));
         }
      }

      for (Tag tx : tag.getList("zombies", 10)) {
         CompoundTag e = (CompoundTag)tx;
         if (e.hasUUID("zombie") && e.hasUUID("owner")) {
            v.zombies.put(e.getUUID("zombie"), e.getUUID("owner"));
         }
      }

      return v;
   }

   public CompoundTag save(CompoundTag tag, Provider registries) {
      ListTag list = new ListTag();
      this.owners.forEach((victim, owner) -> {
         CompoundTag e = new CompoundTag();
         e.putUUID("victim", victim);
         e.putUUID("owner", owner);
         list.add(e);
      });
      tag.put("victims", list);
      ListTag zl = new ListTag();
      this.zombies.forEach((zombie, owner) -> {
         CompoundTag e = new CompoundTag();
         e.putUUID("zombie", zombie);
         e.putUUID("owner", owner);
         zl.add(e);
      });
      tag.put("zombies", zl);
      return tag;
   }
}
