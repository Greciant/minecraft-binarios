package com.awakenedmod.awakened.core.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

final class JobClock {
   static final MapCodec<JobClock> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
               Codec.LONG.fieldOf("id").forGetter(c -> c.id),
               Codec.LONG.fieldOf("restore_at").forGetter(c -> c.restoreAt),
               Codec.INT.fieldOf("rise").forGetter(c -> c.riseTicks),
               Codec.INT.fieldOf("base_y").forGetter(c -> c.baseY),
               Codec.INT.fieldOf("top_y").forGetter(c -> c.topY),
               Codec.LONG.fieldOf("ticket_chunk").forGetter(c -> c.ticketChunk),
               Codec.INT.fieldOf("ticket_radius").forGetter(c -> c.ticketRadius)
            )
            .apply(i, JobClock::new)
   );
   final long id;
   long restoreAt;
   final int riseTicks;
   final int baseY;
   final int topY;
   final long ticketChunk;
   final int ticketRadius;

   JobClock(long id, long restoreAt, int riseTicks, int baseY, int topY, long ticketChunk, int ticketRadius) {
      this.id = id;
      this.restoreAt = restoreAt;
      this.riseTicks = riseTicks;
      this.baseY = baseY;
      this.topY = Math.max(baseY, topY);
      this.ticketChunk = ticketChunk;
      this.ticketRadius = ticketRadius;
   }

   JobClock copy() {
      return new JobClock(this.id, this.restoreAt, this.riseTicks, this.baseY, this.topY, this.ticketChunk, this.ticketRadius);
   }

   long dueAt(int y) {
      int layers = this.topY - this.baseY + 1;
      int layer = Math.max(0, Math.min(layers - 1, y - this.baseY));
      return this.restoreAt + (long)layer * (long)this.riseTicks / (long)layers;
   }
}
