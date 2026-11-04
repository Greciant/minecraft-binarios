package com.awakenedmod.awakened.core.terrain;

import com.awakenedmod.awakened.Awakened;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class Handle {
   final ServerLevel level;
   @Nullable
   final JobClock clock;
   @Nullable
   private final TempTerrainData.Job indexed;
   private final long startTick;
   private final boolean permanent;
   private final boolean visualOnly;
   private final int[] waveTicks;
   private final long[][] waves;
   private Long2IntOpenHashMap floors;
   private LongSet plannedPositions;
   private final int planned;
   private int wave;
   private int cursor;
   private boolean done;
   private Handle.WaveListener onWave = (w, blocks) -> {
   };
   private Handle.LayerListener onLayer = (y, blocks) -> {
   };
   private Runnable onFinish = () -> {
   };

   Handle(
      ServerLevel level,
      @Nullable JobClock clock,
      @Nullable TempTerrainData.Job indexed,
      long startTick,
      PlanResolver.Resolved plan,
      boolean permanent,
      boolean visualOnly
   ) {
      this.level = level;
      this.permanent = permanent && !visualOnly;
      this.visualOnly = visualOnly;
      this.clock = clock;
      this.indexed = indexed;
      this.startTick = startTick;
      this.waveTicks = plan.waveTicks();
      this.waves = plan.waves();
      this.floors = plan.floors();
      this.plannedPositions = plan.positions();
      this.planned = plan.size();
      this.done = plan.isEmpty();
   }

   public Handle onWaveRemoved(Handle.WaveListener listener) {
      this.onWave = listener;
      return this;
   }

   public Handle onLayerRestored(Handle.LayerListener listener) {
      this.onLayer = listener;
      return this;
   }

   public Handle onFinished(Runnable listener) {
      this.onFinish = listener;
      if (this.done) {
         safely(listener);
      }

      return this;
   }

   public int planned() {
      return this.planned;
   }

   public boolean visualOnly() {
      return this.visualOnly;
   }

   public boolean permanent() {
      return this.permanent;
   }

   public boolean isDone() {
      return this.done;
   }

   public boolean carving() {
      return !this.done && this.wavesPending();
   }

   public void restoreNow() {
      TerrainScheduler.restoreNow(this);
   }

   public void stopWaves() {
      this.cancelWaves();
   }

   boolean wavesPending() {
      return this.wave < this.waves.length;
   }

   void cancelWaves() {
      this.wave = this.waves.length;
      this.release();
   }

   private void release() {
      Arrays.fill(this.waves, null);
      this.floors = new Long2IntOpenHashMap();
      this.floors.defaultReturnValue(Integer.MIN_VALUE);
      this.plannedPositions = LongSets.EMPTY_SET;
   }

   int removeDue(long now, int budget) {
      int used = 0;

      while (this.wavesPending() && this.startTick + (long)this.waveTicks[this.wave] <= now && used < budget) {
         long[] positions = this.waves[this.wave];
         List<RemovedBlock> batch = new ArrayList<>();

         while (this.cursor < positions.length && used < budget) {
            long packed = positions[this.cursor++];
            if (this.visualOnly) {
               CarveStep.show(this, packed, batch);
            } else {
               used += CarveStep.take(this, packed, batch);
            }
         }

         if (!batch.isEmpty()) {
            int index = this.wave;
            safely(() -> this.onWave.removed(index, batch));
         }

         if (this.cursor < positions.length) {
            break;
         }

         this.waves[this.wave++] = null;
         this.cursor = 0;
         if (!this.wavesPending()) {
            this.release();
         }
      }

      return used;
   }

   Long2IntOpenHashMap floors() {
      return this.floors;
   }

   boolean isPlanned(long packed) {
      return this.plannedPositions.contains(packed);
   }

   void record(BlockPos pos, BlockState state) {
      if (!this.permanent) {
         long chunk = TerrainLedger.record(this.level, this.clock, pos, state);
         TempTerrainData.of(this.level).addChunk(this.indexed, chunk);
      }
   }

   void forget(BlockPos pos) {
      if (!this.permanent) {
         TerrainLedger.forget(this.level, this.clock, pos);
      }
   }

   void layerRestored(int y, List<RemovedBlock> blocks) {
      safely(() -> this.onLayer.restored(y, blocks));
   }

   void finish() {
      if (!this.done) {
         this.done = true;
         safely(this.onFinish);
      }
   }

   private static void safely(Runnable hook) {
      try {
         hook.run();
      } catch (RuntimeException var2) {
         Awakened.LOGGER.error("Awakened: a temporary terrain hook threw", var2);
      }
   }

   @FunctionalInterface
   public interface LayerListener {
      void restored(int var1, List<RemovedBlock> var2);
   }

   @FunctionalInterface
   public interface WaveListener {
      void removed(int var1, List<RemovedBlock> var2);
   }
}
