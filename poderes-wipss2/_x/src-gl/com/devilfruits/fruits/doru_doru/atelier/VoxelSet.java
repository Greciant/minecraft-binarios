package com.devilfruits.fruits.doru_doru.atelier;

import com.devilfruits.fruits.doru_doru.wax.WaxMask;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.core.BlockPos;

public final class VoxelSet {
   private final Long2LongOpenHashMap blocks = new Long2LongOpenHashMap();
   private int count;

   public static int block(int sub) {
      return sub >> 2;
   }

   public static int cell(int sub) {
      return sub & 3;
   }

   public void add(int sx, int sy, int sz) {
      long key = BlockPos.asLong(sx >> 2, sy >> 2, sz >> 2);
      long m = this.blocks.get(key);
      long n = WaxMask.with(m, sx & 3, sy & 3, sz & 3);
      if (n != m) {
         this.blocks.put(key, n);
         this.count++;
      }
   }

   public boolean has(int sx, int sy, int sz) {
      long m = this.blocks.get(BlockPos.asLong(sx >> 2, sy >> 2, sz >> 2));
      return m != 0L && WaxMask.has(m, sx & 3, sy & 3, sz & 3);
   }

   public void remove(int sx, int sy, int sz) {
      long key = BlockPos.asLong(sx >> 2, sy >> 2, sz >> 2);
      long m = this.blocks.get(key);
      long n = WaxMask.without(m, sx & 3, sy & 3, sz & 3);
      if (n != m) {
         this.count--;
         if (n == 0L) {
            this.blocks.remove(key);
         } else {
            this.blocks.put(key, n);
         }
      }
   }

   public void addMask(long blockKey, long mask) {
      long m = this.blocks.get(blockKey);
      long n = m | mask;
      if (n != m) {
         this.count = this.count + (WaxMask.count(n) - WaxMask.count(m));
         this.blocks.put(blockKey, n);
      }
   }

   public void setMask(long blockKey, long mask) {
      long m = this.blocks.get(blockKey);
      this.count = this.count + (WaxMask.count(mask) - WaxMask.count(m));
      if (mask == 0L) {
         this.blocks.remove(blockKey);
      } else {
         this.blocks.put(blockKey, mask);
      }
   }

   public long mask(long blockKey) {
      return this.blocks.get(blockKey);
   }

   public void addAll(VoxelSet other) {
      ObjectIterator var2 = other.blocks.long2LongEntrySet().iterator();

      while (var2.hasNext()) {
         Entry e = (Entry)var2.next();
         this.addMask(e.getLongKey(), e.getLongValue());
      }
   }

   public Long2LongOpenHashMap blocks() {
      return this.blocks;
   }

   public int size() {
      return this.count;
   }

   public boolean isEmpty() {
      return this.count == 0;
   }

   public float blockEquivalents() {
      return (float)this.count / 64.0F;
   }

   public void clear() {
      this.blocks.clear();
      this.count = 0;
   }
}
