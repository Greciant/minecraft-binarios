package com.devilfruits.fruits.doru_doru.atelier;

import com.devilfruits.fruits.doru_doru.wax.WaxMask;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayDeque;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class WaxIntegrity {
   private static final Direction[] DIRS = Direction.values();

   public static WaxIntegrity.Result check(WaxIntegrity.View view, LongCollection seeds, WaxIntegrity.Rules rules) {
      Long2LongOpenHashMap blocks = new Long2LongOpenHashMap();
      LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
      LongIterator truncated = seeds.iterator();

      while (truncated.hasNext()) {
         long s = (Long)truncated.next();
         long m = view.mask(s);
         if (m != 0L && !blocks.containsKey(s)) {
            blocks.put(s, m);
            queue.enqueue(s);
         }
      }

      boolean truncatedx = false;

      while (!queue.isEmpty()) {
         long p = queue.dequeueLong();
         long m = blocks.get(p);

         for (Direction d : DIRS) {
            long q = BlockPos.offset(p, d);
            if (!blocks.containsKey(q) && WaxMask.touches(m, d)) {
               long n = view.mask(q);
               if (n != 0L && WaxMask.touches(n, d.getOpposite())) {
                  if (blocks.size() >= rules.maxBlocks()) {
                     truncatedx = true;
                  } else {
                     blocks.put(q, n);
                     queue.enqueue(q);
                  }
               }
            }
         }
      }

      Long2IntOpenHashMap stress = new Long2IntOpenHashMap();
      if (truncatedx) {
         return new WaxIntegrity.Result(stress, true);
      } else {
         Long2IntOpenHashMap cluster = new Long2IntOpenHashMap();
         cluster.defaultReturnValue(-1);
         LongArrayList anchored = new LongArrayList();
         LongOpenHashSet anchoredSet = new LongOpenHashSet();
         ObjectIterator var37 = blocks.long2LongEntrySet().iterator();

         while (var37.hasNext()) {
            Entry e = (Entry)var37.next();
            long p = e.getLongKey();
            long m = e.getLongValue();

            for (Direction dx : DIRS) {
               long q = BlockPos.offset(p, dx);
               if (!blocks.containsKey(q) && view.mask(q) == 0L && WaxMask.touches(m, dx) && view.anchor(q, dx.getOpposite())) {
                  anchored.add(p);
                  anchoredSet.add(p);
                  break;
               }
            }
         }

         int clusters = 0;
         LongListIterator var40 = anchored.iterator();

         while (var40.hasNext()) {
            long a = (Long)var40.next();
            if (cluster.get(a) < 0) {
               int id = clusters++;
               cluster.put(a, id);
               queue.enqueue(a);

               while (!queue.isEmpty()) {
                  long p = queue.dequeueLong();

                  for (Direction dxx : DIRS) {
                     long q = BlockPos.offset(p, dxx);
                     if (cluster.get(q) < 0 && anchoredSet.contains(q)) {
                        cluster.put(q, id);
                        queue.enqueue(q);
                     }
                  }
               }
            }
         }

         Long2IntOpenHashMap d1 = new Long2IntOpenHashMap();
         Long2IntOpenHashMap l1 = new Long2IntOpenHashMap();
         Long2IntOpenHashMap d2 = new Long2IntOpenHashMap();
         Long2IntOpenHashMap l2 = new Long2IntOpenHashMap();
         d1.defaultReturnValue(Integer.MAX_VALUE);
         d2.defaultReturnValue(Integer.MAX_VALUE);
         l1.defaultReturnValue(-1);
         l2.defaultReturnValue(-1);
         ArrayDeque<long[]> deque = new ArrayDeque<>();
         LongListIterator var51 = anchored.iterator();

         while (var51.hasNext()) {
            long a = (Long)var51.next();
            d1.put(a, 0);
            l1.put(a, cluster.get(a));
            deque.add(new long[]{a, 0L, (long)cluster.get(a)});
         }

         while (!deque.isEmpty()) {
            long[] entry = deque.pollFirst();
            long p = entry[0];
            int dist = (int)entry[1];
            int label = (int)entry[2];
            boolean current = dist == d1.get(p) && label == l1.get(p) || dist == d2.get(p) && label == l2.get(p);
            if (current) {
               for (Direction dxxx : DIRS) {
                  long q = BlockPos.offset(p, dxxx);
                  if (blocks.containsKey(q) && WaxMask.touches(blocks.get(p), dxxx) && WaxMask.touches(blocks.get(q), dxxx.getOpposite())) {
                     int cost = dxxx == Direction.UP ? 0 : 1;
                     int nd = dist + cost;
                     int ql1 = l1.get(q);
                     if (ql1 != -1 && (nd >= d1.get(q) || ql1 != label)) {
                        if (nd < d1.get(q)) {
                           d2.put(q, d1.get(q));
                           l2.put(q, ql1);
                           d1.put(q, nd);
                           l1.put(q, label);
                        } else {
                           if (label == ql1 || nd >= d2.get(q)) {
                              continue;
                           }

                           d2.put(q, nd);
                           l2.put(q, label);
                        }
                     } else {
                        d1.put(q, nd);
                        l1.put(q, label);
                     }

                     long[] next = new long[]{q, (long)nd, (long)label};
                     if (cost == 0) {
                        deque.addFirst(next);
                     } else {
                        deque.addLast(next);
                     }
                  }
               }
            }
         }

         ObjectIterator var53 = blocks.long2LongEntrySet().iterator();

         while (var53.hasNext()) {
            Entry e = (Entry)var53.next();
            long p = e.getLongKey();
            int dist = d1.get(p);
            WaxIntegrity.Stress s;
            if (dist == Integer.MAX_VALUE) {
               s = WaxIntegrity.Stress.UNSUPPORTED;
            } else {
               double allow = Math.min(rules.maxReach(), rules.baseCantilever() + 2.0 * thickness(blocks, p));
               if (d2.get(p) != Integer.MAX_VALUE) {
                  allow *= 2.0;
               }

               double ratio = (double)dist / Math.max(0.5, allow);
               s = ratio > 1.0 ? WaxIntegrity.Stress.UNSUPPORTED : (ratio > 0.75 ? WaxIntegrity.Stress.STRESSED : WaxIntegrity.Stress.OK);
            }

            stress.put(p, s.ordinal());
         }

         return new WaxIntegrity.Result(stress, false);
      }
   }

   static double thickness(Long2LongOpenHashMap blocks, long p) {
      int x = BlockPos.getX(p);
      int y = BlockPos.getY(p);
      int z = BlockPos.getZ(p);
      int n = 0;

      for (int dx = -1; dx <= 1; dx++) {
         for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
               if (blocks.containsKey(BlockPos.asLong(x + dx, y + dy, z + dz))) {
                  n++;
               }
            }
         }
      }

      return Math.min(3.0, (double)n / 9.0);
   }

   private WaxIntegrity() {
   }

   public static record Result(Long2IntOpenHashMap stress, boolean truncated) {
      public WaxIntegrity.Stress of(long pos) {
         return WaxIntegrity.Stress.values()[this.stress.getOrDefault(pos, WaxIntegrity.Stress.OK.ordinal())];
      }

      public boolean anyUnsupported() {
         return this.stress.containsValue(WaxIntegrity.Stress.UNSUPPORTED.ordinal());
      }

      public int count(WaxIntegrity.Stress s) {
         int n = 0;
         IntIterator var3 = this.stress.values().iterator();

         while (var3.hasNext()) {
            int v = (Integer)var3.next();
            if (v == s.ordinal()) {
               n++;
            }
         }

         return n;
      }
   }

   public static record Rules(double baseCantilever, double maxReach, int maxBlocks) {
   }

   public static enum Stress {
      OK,
      STRESSED,
      UNSUPPORTED;
   }

   public interface View {
      long mask(long var1);

      boolean anchor(long var1, Direction var3);
   }
}
