package com.awakenedmod.awakened.core.terrain;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;

public final class CarvePlan {
   final List<CarvePlan.Wave> waves;
   final LongSet keep;
   final int holdTicks;
   final int riseTicks;
   final boolean permanent;

   private CarvePlan(List<CarvePlan.Wave> waves, LongSet keep, int holdTicks, int riseTicks, boolean permanent) {
      this.waves = waves;
      this.keep = keep;
      this.holdTicks = holdTicks;
      this.riseTicks = riseTicks;
      this.permanent = permanent;
   }

   public static CarvePlan.Builder builder() {
      return new CarvePlan.Builder();
   }

   int lastWaveTick() {
      return this.waves.isEmpty() ? 0 : this.waves.get(this.waves.size() - 1).atTick;
   }

   public static final class Builder {
      private final List<CarvePlan.Wave> waves = new ArrayList<>();
      private final LongSet keep = new LongOpenHashSet();
      private int holdTicks;
      private int riseTicks;
      private boolean permanent;
      private int size;

      private Builder() {
      }

      public CarvePlan.Builder wave(int atTick, Iterable<BlockPos> positions) {
         if (atTick < 0) {
            throw new IllegalArgumentException("A wave cannot start before the carve: " + atTick);
         } else {
            LongArrayList packed = new LongArrayList();
            positions.forEach(pos -> packed.add(pos.asLong()));
            this.size = this.size + packed.size();
            if (this.size > 262144) {
               throw new IllegalArgumentException("Carve plan over 262144 blocks");
            } else {
               this.waves.add(new CarvePlan.Wave(atTick, packed.toLongArray()));
               return this;
            }
         }
      }

      public CarvePlan.Builder keep(Iterable<BlockPos> positions) {
         positions.forEach(pos -> this.keep.add(pos.asLong()));
         return this;
      }

      public CarvePlan.Builder hold(int ticks) {
         this.holdTicks = Math.max(0, ticks);
         return this;
      }

      public CarvePlan.Builder rise(int ticks) {
         this.riseTicks = Math.max(0, ticks);
         return this;
      }

      public CarvePlan.Builder permanent(boolean value) {
         this.permanent = value;
         return this;
      }

      public CarvePlan build() {
         List<CarvePlan.Wave> sorted = new ArrayList<>(this.waves);
         sorted.sort(Comparator.comparingInt(CarvePlan.Wave::atTick));
         return new CarvePlan(List.copyOf(sorted), new LongOpenHashSet(this.keep), this.holdTicks, this.riseTicks, this.permanent);
      }
   }

   static record Wave(int atTick, long[] positions) {
   }
}
