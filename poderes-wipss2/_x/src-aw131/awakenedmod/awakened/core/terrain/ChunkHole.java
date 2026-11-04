package com.awakenedmod.awakened.core.terrain;

import com.awakenedmod.awakened.Awakened;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

final class ChunkHole {
   static final Codec<ChunkHole> CODEC = RecordCodecBuilder.create(
      i -> i.group(
               JobClock.MAP_CODEC.forGetter(h -> h.clock),
               Lenient.optional(BlockState.CODEC, "block state").listOf().fieldOf("palette").forGetter(h -> h.palette.stream().map(Optional::of).toList()),
               Codec.LONG_STREAM.fieldOf("pos").forGetter(h -> LongStream.of(h.pos.toLongArray())),
               Codec.INT_STREAM.fieldOf("state").forGetter(h -> IntStream.of(h.state.toIntArray())),
               Codec.LONG_STREAM
                  .optionalFieldOf("edited")
                  .forGetter(h -> h.edited.isEmpty() ? Optional.empty() : Optional.of(LongStream.of(h.edited.toLongArray())))
            )
            .apply(i, ChunkHole::decoded)
   );
   final JobClock clock;
   private final List<BlockState> palette = new ArrayList<>();
   private final Object2IntOpenHashMap<BlockState> paletteIndex = new Object2IntOpenHashMap();
   private final LongArrayList pos = new LongArrayList();
   private final IntArrayList state = new IntArrayList();
   private final LongOpenHashSet edited = new LongOpenHashSet();
   private final LongOpenHashSet members = new LongOpenHashSet();
   private boolean sorted;

   ChunkHole(JobClock clock) {
      this.clock = clock;
   }

   private static ChunkHole decoded(
      JobClock clock, List<Optional<BlockState>> palette, LongStream posStream, IntStream stateStream, Optional<LongStream> editedStream
   ) {
      ChunkHole hole = new ChunkHole(clock);
      long[] positions = posStream.toArray();
      int[] states = stateStream.toArray();
      int n = Math.min(positions.length, states.length);
      int skipped = Math.max(positions.length, states.length) - n;

      for (int e = 0; e < n; e++) {
         int index = states[e];
         if (index >= 0 && index < palette.size() && !palette.get(index).isEmpty()) {
            hole.add(positions[e], palette.get(index).get());
         } else {
            skipped++;
         }
      }

      editedStream.ifPresent(s -> s.forEach(hole.edited::add));
      if (skipped > 0) {
         Awakened.LOGGER.warn("Awakened: {} saved temporary terrain block(s) of carve {} could not be read and will not come back", skipped, clock.id);
      }

      return hole;
   }

   int size() {
      return this.pos.size();
   }

   boolean isEmpty() {
      return this.pos.isEmpty();
   }

   long pos(int e) {
      return this.pos.getLong(e);
   }

   BlockState state(int e) {
      return this.palette.get(this.state.getInt(e));
   }

   void add(long at, BlockState removed) {
      int index = this.paletteIndex.computeIfAbsent(removed, s -> {
         this.palette.add(removed);
         return this.palette.size() - 1;
      });
      this.pos.add(at);
      this.state.add(index);
      this.members.add(at);
      this.sorted = false;
   }

   boolean contains(long at) {
      return this.members.contains(at);
   }

   int indexOf(long at) {
      if (!this.members.contains(at)) {
         return -1;
      } else {
         for (int e = 0; e < this.pos.size(); e++) {
            if (this.pos.getLong(e) == at) {
               return e;
            }
         }

         return -1;
      }
   }

   void forget(long at) {
      int e = this.indexOf(at);
      if (e >= 0) {
         BitSet one = new BitSet();
         one.set(e);
         this.removeAll(one);
      }
   }

   boolean markEdited(long at) {
      return this.indexOf(at) >= 0 && this.edited.add(at);
   }

   boolean isEdited(long at) {
      return this.edited.contains(at);
   }

   void sortBottomUp() {
      if (!this.sorted) {
         int n = this.pos.size();
         Integer[] order = new Integer[n];

         for (int e = 0; e < n; e++) {
            order[e] = e;
         }

         Arrays.sort(order, (a, b) -> Integer.compare(BlockPos.getY(this.pos.getLong(a)), BlockPos.getY(this.pos.getLong(b))));
         long[] sortedPos = new long[n];
         int[] sortedState = new int[n];

         for (int e = 0; e < n; e++) {
            sortedPos[e] = this.pos.getLong(order[e]);
            sortedState[e] = this.state.getInt(order[e]);
         }

         this.pos.clear();
         this.pos.addElements(0, sortedPos);
         this.state.clear();
         this.state.addElements(0, sortedState);
         this.sorted = true;
      }
   }

   void removeAll(BitSet done) {
      if (!done.isEmpty()) {
         int write = 0;

         for (int read = 0; read < this.pos.size(); read++) {
            if (done.get(read)) {
               long at = this.pos.getLong(read);
               this.edited.remove(at);
               this.members.remove(at);
            } else {
               this.pos.set(write, this.pos.getLong(read));
               this.state.set(write, this.state.getInt(read));
               write++;
            }
         }

         this.pos.size(write);
         this.state.size(write);
      }
   }
}
