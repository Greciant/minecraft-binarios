package com.devilfruits.core.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;

@EventBusSubscriber(
   modid = "devilfruits"
)
public final class BlockRestoreData extends SavedData {
   public static final String NAME = "devilfruits_block_restore";
   private final Long2ObjectLinkedOpenHashMap<BlockRestoreData.Entry> entries = new Long2ObjectLinkedOpenHashMap();

   public static Factory<BlockRestoreData> factory() {
      return new Factory(BlockRestoreData::new, BlockRestoreData::load);
   }

   public static BlockRestoreData get(ServerLevel level) {
      return (BlockRestoreData)level.getDataStorage().computeIfAbsent(factory(), "devilfruits_block_restore");
   }

   public void absorb(BlockRestoreData other) {
      if (other != this && !other.entries.isEmpty()) {
         ObjectIterator var2 = other.entries.values().iterator();

         while (var2.hasNext()) {
            BlockRestoreData.Entry entry = (BlockRestoreData.Entry)var2.next();
            this.entries.putIfAbsent(entry.pos.asLong(), entry);
         }

         other.entries.clear();
         other.setDirty();
         this.setDirty();
      }
   }

   public void add(BlockPos pos, BlockState state, long restoreAt) {
      this.put(pos, state, restoreAt, null);
   }

   public void addTransform(BlockPos pos, BlockState original, BlockState to, long restoreAt) {
      this.put(pos, original, restoreAt, to);
   }

   private void put(BlockPos pos, BlockState state, long restoreAt, @Nullable BlockState expected) {
      long key = pos.asLong();
      BlockRestoreData.Entry previous = (BlockRestoreData.Entry)this.entries.get(key);
      if (previous != null) {
         this.entries.put(key, new BlockRestoreData.Entry(previous.pos, previous.state, Math.max(previous.restoreAt, restoreAt), expected));
      } else {
         this.entries.put(key, new BlockRestoreData.Entry(pos.immutable(), state, restoreAt, expected));
      }

      this.setDirty();
   }

   public boolean settle(ServerLevel level, BlockPos pos, Block current, BlockState back, int flags) {
      if (!level.getBlockState(pos).is(current)) {
         return false;
      } else {
         level.setBlock(pos, back, flags);
         long key = pos.asLong();
         BlockRestoreData.Entry entry = (BlockRestoreData.Entry)this.entries.get(key);
         if (entry != null) {
            if (entry.state.equals(back)) {
               this.entries.remove(key);
            } else {
               this.entries.put(key, new BlockRestoreData.Entry(entry.pos, entry.state, entry.restoreAt, back));
            }

            this.setDirty();
         }

         return true;
      }
   }

   public int size() {
      return this.entries.size();
   }

   @Nullable
   public BlockState pending(BlockPos pos) {
      BlockRestoreData.Entry entry = (BlockRestoreData.Entry)this.entries.get(pos.asLong());
      return entry == null ? null : entry.state;
   }

   public int restoreInside(ServerLevel level, AABB box) {
      return this.restore(level, Long.MAX_VALUE, box);
   }

   private int restore(ServerLevel level, long now, @Nullable AABB box) {
      if (this.entries.isEmpty()) {
         return 0;
      } else {
         int restored = 0;
         int sounds = 0;
         boolean changed = false;
         ObjectIterator<BlockRestoreData.Entry> it = this.entries.values().iterator();

         while (it.hasNext()) {
            BlockRestoreData.Entry entry = (BlockRestoreData.Entry)it.next();
            if (entry.restoreAt <= now && level.isLoaded(entry.pos) && (box == null || box.contains(Vec3.atCenterOf(entry.pos)))) {
               BlockState current = level.getBlockState(entry.pos);
               boolean free = entry.expected != null ? current.is(entry.expected.getBlock()) : current.canBeReplaced();
               if (!free) {
                  it.remove();
                  changed = true;
               } else if (entry.state.getCollisionShape(level, entry.pos).isEmpty()
                  || level.getEntitiesOfClass(LivingEntity.class, new AABB(entry.pos)).isEmpty()) {
                  level.setBlock(entry.pos, entry.state, 3);
                  if (sounds++ < 3 && !entry.state.isAir()) {
                     level.playSound(null, entry.pos, entry.state.getSoundType(level, entry.pos, null).getPlaceSound(), SoundSource.BLOCKS, 0.4F, 0.8F);
                  }

                  it.remove();
                  restored++;
                  changed = true;
               }
            }
         }

         if (changed) {
            this.setDirty();
         }

         return restored;
      }
   }

   @SubscribeEvent
   public static void onLevelTick(Post event) {
      if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 5L == 0L) {
         get(level).restore(level, level.getGameTime(), null);
      }
   }

   private static BlockRestoreData load(CompoundTag tag, Provider registries) {
      BlockRestoreData data = new BlockRestoreData();
      ListTag list = tag.getList("entries", 10);
      RegistryLookup<Block> blocks = registries.lookupOrThrow(Registries.BLOCK);

      for (int i = 0; i < list.size(); i++) {
         CompoundTag e = list.getCompound(i);
         BlockPos pos = BlockPos.of(e.getLong("pos"));
         BlockState expected = e.contains("expect", 10) ? NbtUtils.readBlockState(blocks, e.getCompound("expect")) : null;
         data.entries.put(pos.asLong(), new BlockRestoreData.Entry(pos, NbtUtils.readBlockState(blocks, e.getCompound("state")), e.getLong("at"), expected));
      }

      return data;
   }

   public CompoundTag save(CompoundTag tag, Provider registries) {
      ListTag list = new ListTag();
      ObjectIterator var4 = this.entries.values().iterator();

      while (var4.hasNext()) {
         BlockRestoreData.Entry entry = (BlockRestoreData.Entry)var4.next();
         CompoundTag e = new CompoundTag();
         e.putLong("pos", entry.pos.asLong());
         e.put("state", NbtUtils.writeBlockState(entry.state));
         e.putLong("at", entry.restoreAt);
         if (entry.expected != null) {
            e.put("expect", NbtUtils.writeBlockState(entry.expected));
         }

         list.add(e);
      }

      tag.put("entries", list);
      return tag;
   }

   private static record Entry(BlockPos pos, BlockState state, long restoreAt, @Nullable BlockState expected) {
   }
}
