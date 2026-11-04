package com.awakenedmod.awakened.core.terrain;

import com.awakenedmod.awakened.core.TeleportUtil;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongBidirectionalIterator;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

final class PlanResolver {
   private PlanResolver() {
   }

   static PlanResolver.Resolved resolve(ServerLevel level, @Nullable ServerPlayer actor, CarvePlan plan, @Nullable CarveReads given) {
      CarveReads reads = given != null && given.isFor(level, actor) ? given : new CarveReads(level, actor);
      Long2IntLinkedOpenHashMap waveOf = new Long2IntLinkedOpenHashMap();

      for (int w = 0; w < plan.waves.size(); w++) {
         for (long pos : plan.waves.get(w).positions()) {
            if (!plan.keep.contains(pos)) {
               waveOf.putIfAbsent(pos, w);
            }
         }
      }

      Long2IntOpenHashMap tops = columnTops(waveOf);
      addLooseTops(level, reads, plan.keep, waveOf, tops);
      addPartners(reads, plan.keep, waveOf);
      LongSet skipped = skipped(level, reads, plan.keep, waveOf);
      Long2IntOpenHashMap floors = new Long2IntOpenHashMap();
      floors.defaultReturnValue(Integer.MIN_VALUE);
      LongSet floorSources = new LongOpenHashSet(skipped);
      floorSources.addAll(plan.keep);
      floorSources.forEach(posx -> raise(floors, posx));
      guardColumns(level, reads, waveOf, skipped, floors);
      List<LongArrayList> kept = new ArrayList<>();
      plan.waves.forEach(w -> kept.add(new LongArrayList()));
      ObjectBidirectionalIterator var11 = waveOf.long2IntEntrySet().iterator();

      while (var11.hasNext()) {
         Entry entry = (Entry)var11.next();
         long posx = entry.getLongKey();
         if (!skipped.contains(posx) && BlockPos.getY(posx) >= floors.get(TerrainColumns.key(posx)) && !reads.state(BlockPos.of(posx)).isAir()) {
            kept.get(entry.getIntValue()).add(posx);
         }
      }

      return pack(plan, kept, floors);
   }

   private static Long2IntOpenHashMap columnTops(Long2IntLinkedOpenHashMap waveOf) {
      Long2IntOpenHashMap tops = new Long2IntOpenHashMap();
      tops.defaultReturnValue(Integer.MIN_VALUE);
      waveOf.keySet().forEach(pos -> {
         long column = TerrainColumns.key(pos);
         tops.put(column, Math.max(tops.get(column), BlockPos.getY(pos)));
      });
      return tops;
   }

   private static void addLooseTops(ServerLevel level, CarveReads reads, LongSet keep, Long2IntLinkedOpenHashMap waveOf, Long2IntOpenHashMap tops) {
      ObjectIterator var5 = tops.long2IntEntrySet().iterator();

      while (var5.hasNext()) {
         Entry column = (Entry)var5.next();
         BlockPos top = BlockPos.of(column.getLongKey()).atY(column.getIntValue());
         BlockPos above = top.above();
         long packed = above.asLong();
         if (!waveOf.containsKey(packed) && !keep.contains(packed)) {
            BlockState state = reads.state(above);
            if (!state.isAir() && state.getCollisionShape(level, above).isEmpty()) {
               waveOf.put(packed, waveOf.get(top.asLong()));
               column.setValue(above.getY());
            }
         }
      }
   }

   private static void addPartners(CarveReads reads, LongSet keep, Long2IntLinkedOpenHashMap waveOf) {
      for (long pos : waveOf.keySet().toLongArray()) {
         BlockPos at = BlockPos.of(pos);
         BlockState state = reads.state(at);
         if (TerrainSafety.isDouble(state)) {
            long partner = TerrainSafety.partner(at, state).asLong();
            if (!keep.contains(partner)) {
               int wave = waveOf.containsKey(partner) ? Math.min(waveOf.get(partner), waveOf.get(pos)) : waveOf.get(pos);
               waveOf.put(pos, wave);
               waveOf.put(partner, wave);
            }
         }
      }
   }

   private static LongSet skipped(ServerLevel level, CarveReads reads, LongSet keep, Long2IntLinkedOpenHashMap waveOf) {
      int[] box = bounds(waveOf.keySet());
      LongSet supports = HangingSupports.in(
         level, new AABB((double)box[0], (double)box[1], (double)box[2], (double)(box[3] + 1), (double)(box[4] + 1), (double)(box[5] + 1))
      );
      Long2IntOpenHashMap solidTops = new Long2IntOpenHashMap();
      solidTops.defaultReturnValue(Integer.MIN_VALUE);
      LongBidirectionalIterator skipped = waveOf.keySet().iterator();

      while (skipped.hasNext()) {
         long pos = (Long)skipped.next();
         if (!reads.state(BlockPos.of(pos)).isAir()) {
            long column = TerrainColumns.key(pos);
            solidTops.put(column, Math.max(solidTops.get(column), BlockPos.getY(pos)));
         }
      }

      LongSet skippedx = new LongOpenHashSet();
      LongBidirectionalIterator var15 = waveOf.keySet().iterator();

      while (var15.hasNext()) {
         long pos = (Long)var15.next();
         BlockPos at = BlockPos.of(pos);
         BlockState state = reads.state(at);
         if (!state.isAir()) {
            BlockPos top = at.atY(solidTops.get(TerrainColumns.key(pos)));
            if (supports.contains(pos) || !TerrainSafety.canTake(level, at, state) || !reads.allows(top)) {
               skippedx.add(pos);
               if (TerrainSafety.isDouble(state)) {
                  skippedx.add(TerrainSafety.partner(at, state).asLong());
               }
            } else if (TerrainSafety.isDouble(state) && keep.contains(TerrainSafety.partner(at, state).asLong())) {
               skippedx.add(pos);
            }
         }
      }

      return skippedx;
   }

   private static void guardColumns(ServerLevel level, CarveReads reads, Long2IntLinkedOpenHashMap waveOf, LongSet skipped, Long2IntOpenHashMap floors) {
      Long2IntOpenHashMap tops = new Long2IntOpenHashMap();
      Long2IntOpenHashMap bottoms = new Long2IntOpenHashMap();
      tops.defaultReturnValue(Integer.MIN_VALUE);
      bottoms.defaultReturnValue(Integer.MAX_VALUE);
      LongBidirectionalIterator worldBottom = waveOf.keySet().iterator();

      while (worldBottom.hasNext()) {
         long pos = (Long)worldBottom.next();
         long column = TerrainColumns.key(pos);
         int y = BlockPos.getY(pos);
         if (!skipped.contains(pos) && y >= floors.get(column) && !reads.state(BlockPos.of(pos)).isAir()) {
            tops.put(column, Math.max(tops.get(column), y));
            bottoms.put(column, Math.min(bottoms.get(column), y));
         }
      }

      int worldBottomx = TeleportUtil.minY(level);
      MutableBlockPos probe = new MutableBlockPos();
      ObjectIterator var9 = tops.long2IntEntrySet().iterator();

      while (var9.hasNext()) {
         Entry entry = (Entry)var9.next();
         long column = entry.getLongKey();
         BlockPos top = BlockPos.of(column).atY(entry.getIntValue());
         int bottom = bottoms.get(column);
         boolean unsafe = reads.state(top.above()).getBlock() instanceof FallingBlock || bottom - worldBottomx < 4;
         if (!unsafe) {
            boolean floor = false;

            for (int d = 1; d <= 4 && !floor; d++) {
               probe.set(top.getX(), bottom - d, top.getZ());
               floor = !reads.state(probe).getCollisionShape(level, probe).isEmpty();
            }

            unsafe = !floor;
         }

         if (unsafe) {
            raise(floors, top.asLong());
         }
      }
   }

   static void raise(Long2IntOpenHashMap floors, long pos) {
      long column = TerrainColumns.key(pos);
      floors.put(column, Math.max(floors.get(column), BlockPos.getY(pos) + 1));
   }

   private static PlanResolver.Resolved pack(CarvePlan plan, List<LongArrayList> kept, Long2IntOpenHashMap floors) {
      int[] ticks = new int[kept.size()];
      long[][] waves = new long[kept.size()][];
      LongOpenHashSet all = new LongOpenHashSet();

      for (int w = 0; w < kept.size(); w++) {
         ticks[w] = plan.waves.get(w).atTick();
         LongArrayList list = kept.get(w);
         list.sort((a, b) -> Integer.compare(BlockPos.getY(b), BlockPos.getY(a)));
         waves[w] = list.toLongArray();
         all.addAll(list);
      }

      int[] box = bounds(all);
      return new PlanResolver.Resolved(ticks, waves, floors, all, all.size(), box[0], box[1], box[2], box[3], box[4], box[5]);
   }

   private static int[] bounds(LongCollection positions) {
      if (positions.isEmpty()) {
         return new int[6];
      } else {
         int[] box = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
         LongIterator it = positions.iterator();

         while (it.hasNext()) {
            long pos = it.nextLong();
            box[0] = Math.min(box[0], BlockPos.getX(pos));
            box[1] = Math.min(box[1], BlockPos.getY(pos));
            box[2] = Math.min(box[2], BlockPos.getZ(pos));
            box[3] = Math.max(box[3], BlockPos.getX(pos));
            box[4] = Math.max(box[4], BlockPos.getY(pos));
            box[5] = Math.max(box[5], BlockPos.getZ(pos));
         }

         return box;
      }
   }

   static record Resolved(
      int[] waveTicks, long[][] waves, Long2IntOpenHashMap floors, LongSet positions, int size, int minX, int minY, int minZ, int maxX, int maxY, int maxZ
   ) {
      boolean isEmpty() {
         return this.size == 0;
      }
   }
}
