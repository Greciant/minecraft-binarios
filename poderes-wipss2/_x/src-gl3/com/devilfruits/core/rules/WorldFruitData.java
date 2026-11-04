package com.devilfruits.core.rules;

import com.devilfruits.core.fruit.DevilFruit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public final class WorldFruitData extends SavedData {
   public static final String NAME = "devilfruits_world_fruits";
   private final Map<ResourceLocation, WorldFruitData.Entry> fruits = new LinkedHashMap<>();
   private boolean fresh;

   public static WorldFruitData get(MinecraftServer server) {
      return (WorldFruitData)server.overworld()
         .getDataStorage()
         .computeIfAbsent(new Factory(WorldFruitData::create, WorldFruitData::load), "devilfruits_world_fruits");
   }

   private static WorldFruitData create() {
      WorldFruitData data = new WorldFruitData();
      data.fresh = true;
      return data;
   }

   public boolean isFresh() {
      return this.fresh;
   }

   private WorldFruitData.Entry entry(ResourceLocation fruit) {
      return this.fruits.computeIfAbsent(fruit, k -> new WorldFruitData.Entry());
   }

   public WorldFruitData.State state(DevilFruit fruit) {
      return this.state(fruit.id());
   }

   public WorldFruitData.State state(ResourceLocation fruit) {
      WorldFruitData.Entry e = this.fruits.get(fruit);
      return e == null ? WorldFruitData.State.FREE : e.state;
   }

   public Optional<UUID> instance(DevilFruit fruit) {
      WorldFruitData.Entry e = this.fruits.get(fruit.id());
      return Optional.ofNullable(e == null ? null : e.instance);
   }

   public Optional<UUID> owner(DevilFruit fruit) {
      WorldFruitData.Entry e = this.fruits.get(fruit.id());
      return Optional.ofNullable(e == null ? null : e.owner);
   }

   public String ownerName(DevilFruit fruit) {
      WorldFruitData.Entry e = this.fruits.get(fruit.id());
      return e == null ? "" : e.ownerName;
   }

   public void setItem(DevilFruit fruit, UUID instance) {
      this.set(fruit.id(), WorldFruitData.State.ITEM, instance, null, "");
   }

   public void setEaten(DevilFruit fruit, UUID owner, String name) {
      this.set(fruit.id(), WorldFruitData.State.EATEN, null, owner, name);
   }

   public void setFree(DevilFruit fruit) {
      this.set(fruit.id(), WorldFruitData.State.FREE, null, null, "");
   }

   public void set(ResourceLocation fruit, WorldFruitData.State state, @Nullable UUID instance, @Nullable UUID owner, String ownerName) {
      WorldFruitData.Entry e = this.entry(fruit);
      e.state = state;
      e.instance = instance;
      e.owner = owner;
      e.ownerName = ownerName;
      this.setDirty();
   }

   private static WorldFruitData load(CompoundTag tag, Provider registries) {
      WorldFruitData data = new WorldFruitData();
      CompoundTag list = tag.getCompound("fruits");

      for (String key : list.getAllKeys()) {
         ResourceLocation id = ResourceLocation.tryParse(key);
         if (id != null) {
            CompoundTag t = list.getCompound(key);
            WorldFruitData.Entry e = data.entry(id);

            try {
               e.state = WorldFruitData.State.valueOf(t.getString("state"));
            } catch (IllegalArgumentException var10) {
               e.state = WorldFruitData.State.FREE;
            }

            e.instance = t.hasUUID("instance") ? t.getUUID("instance") : null;
            e.owner = t.hasUUID("owner") ? t.getUUID("owner") : null;
            e.ownerName = t.getString("owner_name");
         }
      }

      return data;
   }

   public CompoundTag save(CompoundTag tag, Provider registries) {
      CompoundTag list = new CompoundTag();
      this.fruits.forEach((id, e) -> {
         CompoundTag t = new CompoundTag();
         t.putString("state", e.state.name());
         if (e.instance != null) {
            t.putUUID("instance", e.instance);
         }

         if (e.owner != null) {
            t.putUUID("owner", e.owner);
         }

         t.putString("owner_name", e.ownerName);
         list.put(id.toString(), t);
      });
      tag.put("fruits", list);
      return tag;
   }

   private static final class Entry {
      WorldFruitData.State state = WorldFruitData.State.FREE;
      @Nullable
      UUID instance;
      @Nullable
      UUID owner;
      String ownerName = "";
   }

   public static enum State {
      FREE,
      ITEM,
      EATEN;
   }
}
