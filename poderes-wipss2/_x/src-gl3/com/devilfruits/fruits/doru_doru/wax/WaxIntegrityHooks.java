package com.devilfruits.fruits.doru_doru.wax;

import com.devilfruits.fruits.doru_doru.ability.WaxAtelierAbility;
import com.devilfruits.fruits.doru_doru.atelier.BuildEngine;
import com.devilfruits.fruits.doru_doru.atelier.WaxIntegrity;
import com.devilfruits.fruits.doru_doru.atelier.WaxViews;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class WaxIntegrityHooks {
   private static final Map<ResourceKey<Level>, LongOpenHashSet> PENDING = new HashMap<>();
   private static final int CHECKS_PER_TICK = 6;

   static void supportChanged(ServerLevel level, BlockPos pos, BlockPos from) {
      if (!(level.getBlockState(from).getBlock() instanceof WaxBlock)) {
         PENDING.computeIfAbsent(level.dimension(), k -> new LongOpenHashSet()).add(pos.asLong());
      }
   }

   static void removed(ServerLevel level, BlockPos pos) {
      LongOpenHashSet set = PENDING.computeIfAbsent(level.dimension(), k -> new LongOpenHashSet());

      for (Direction d : Direction.values()) {
         set.add(pos.relative(d).asLong());
      }
   }

   public static void tick(ServerLevel level) {
      LongOpenHashSet pending = PENDING.get(level.dimension());
      if (pending != null && !pending.isEmpty() && BuildEngine.running() <= 0) {
         LongSet visited = new LongOpenHashSet();
         int checks = 0;
         long now = level.getGameTime();
         LongIterator it = pending.iterator();

         while (it.hasNext() && checks < 6) {
            long seed = it.nextLong();
            it.remove();
            if (!visited.contains(seed) && level.getBlockEntity(BlockPos.of(seed)) instanceof WaxBlockEntity) {
               checks++;
               WaxIntegrity.Result result = WaxIntegrity.check(WaxViews.of(level, null, null), LongSet.of(seed), WaxAtelierAbility.rules());
               ObjectIterator var10 = result.stress().long2IntEntrySet().iterator();

               while (var10.hasNext()) {
                  Entry e = (Entry)var10.next();
                  visited.add(e.getLongKey());
                  if (e.getIntValue() == WaxIntegrity.Stress.UNSUPPORTED.ordinal()) {
                     BlockEntity var13 = level.getBlockEntity(BlockPos.of(e.getLongKey()));
                     if (var13 instanceof WaxBlockEntity) {
                        WaxBlockEntity be = (WaxBlockEntity)var13;
                        if (be.expires() == 0L || be.expires() > now) {
                           be.setExpires(now);
                           WaxHeatService.poke(level, BlockPos.of(e.getLongKey()));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void clear(ServerLevel level) {
      PENDING.remove(level.dimension());
   }

   private WaxIntegrityHooks() {
   }
}
