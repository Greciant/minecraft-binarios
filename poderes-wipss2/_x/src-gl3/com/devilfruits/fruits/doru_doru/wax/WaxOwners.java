package com.devilfruits.fruits.doru_doru.wax;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public final class WaxOwners extends SavedData {
   public static final String NAME = "devilfruits_doru_doru_wax";
   public static final int NONE = -1;
   private final List<UUID> owners = new ArrayList<>();
   private final IntArrayList counts = new IntArrayList();
   private final Object2IntOpenHashMap<UUID> index = new Object2IntOpenHashMap();

   private WaxOwners() {
      this.index.defaultReturnValue(-1);
   }

   public static WaxOwners get(MinecraftServer server) {
      return (WaxOwners)server.overworld().getDataStorage().computeIfAbsent(new Factory(WaxOwners::new, WaxOwners::load, null), "devilfruits_doru_doru_wax");
   }

   public int index(UUID owner) {
      int i = this.index.getInt(owner);
      if (i == -1) {
         i = this.owners.size();
         this.owners.add(owner);
         this.counts.add(0);
         this.index.put(owner, i);
         this.setDirty();
      }

      return i;
   }

   public int find(UUID owner) {
      return this.index.getInt(owner);
   }

   @Nullable
   public UUID owner(int i) {
      return i >= 0 && i < this.owners.size() ? this.owners.get(i) : null;
   }

   public int count(UUID owner) {
      int i = this.index.getInt(owner);
      return i == -1 ? 0 : this.counts.getInt(i);
   }

   public void add(int owner, int delta) {
      if (owner >= 0 && owner < this.counts.size() && delta != 0) {
         this.counts.set(owner, Math.max(0, this.counts.getInt(owner) + delta));
         this.setDirty();
      }
   }

   public CompoundTag save(CompoundTag tag, Provider registries) {
      ListTag list = new ListTag();

      for (int i = 0; i < this.owners.size(); i++) {
         CompoundTag e = new CompoundTag();
         e.put("id", NbtUtils.createUUID(this.owners.get(i)));
         e.putInt("count", this.counts.getInt(i));
         list.add(e);
      }

      tag.put("owners", list);
      return tag;
   }

   private static WaxOwners load(CompoundTag tag, Provider registries) {
      WaxOwners data = new WaxOwners();

      for (Tag t : tag.getList("owners", 10)) {
         CompoundTag e = (CompoundTag)t;
         UUID id = NbtUtils.loadUUID(e.get("id"));
         data.index.put(id, data.owners.size());
         data.owners.add(id);
         data.counts.add(e.getInt("count"));
      }

      return data;
   }
}
