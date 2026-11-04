package com.awakenedmod.awakened.core.terrain;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

final class ChunkLedger {
   static final MapCodec<ChunkLedger> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Lenient.list(ChunkHole.CODEC, "temporary terrain hole").optionalFieldOf("holes", List.of()).forGetter(l -> new ArrayList(l.holes.values())))
            .apply(i, ChunkLedger::new)
   );
   private final Long2ObjectLinkedOpenHashMap<ChunkHole> holes = new Long2ObjectLinkedOpenHashMap();

   ChunkLedger() {
   }

   private ChunkLedger(List<ChunkHole> read) {
      read.forEach(hole -> {
         if (!hole.isEmpty()) {
            this.holes.put(hole.clock.id, hole);
         }
      });
   }

   boolean isEmpty() {
      return this.holes.isEmpty();
   }

   @Nullable
   ChunkHole hole(long jobId) {
      return (ChunkHole)this.holes.get(jobId);
   }

   ChunkHole holeFor(JobClock clock) {
      return (ChunkHole)this.holes.computeIfAbsent(clock.id, id -> new ChunkHole(clock.copy()));
   }

   Collection<ChunkHole> holes() {
      return this.holes.values();
   }

   void dropIfEmpty(long jobId) {
      ChunkHole hole = (ChunkHole)this.holes.get(jobId);
      if (hole != null && hole.isEmpty()) {
         this.holes.remove(jobId);
      }
   }
}
