package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.terrain.CarvePlan;
import com.awakenedmod.awakened.core.terrain.CarveReads;
import com.awakenedmod.awakened.core.terrain.Handle;
import com.awakenedmod.awakened.core.terrain.RemovedBlock;
import com.awakenedmod.awakened.core.terrain.TempTerrain;
import com.awakenedmod.awakened.core.terrain.TerrainDrops;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

final class RuinCarve {
   private final TreeMap<Integer, List<BlockPos>> waves = new TreeMap<>();
   private final LongSet keep = new LongOpenHashSet();
   private int hold;
   private int rise;
   private boolean drops;
   @Nullable
   private CarveReads reads;

   RuinCarve wave(int atTick, Collection<BlockPos> positions) {
      this.waves.computeIfAbsent(atTick, t -> new ArrayList<>()).addAll(positions);
      return this;
   }

   RuinCarve keep(Collection<BlockPos> positions) {
      positions.forEach(p -> this.keep.add(p.asLong()));
      return this;
   }

   RuinCarve hold(int ticks) {
      this.hold = ticks;
      return this;
   }

   RuinCarve rise(int ticks) {
      this.rise = ticks;
      return this;
   }

   RuinCarve drops() {
      this.drops = true;
      return this;
   }

   RuinCarve reads(CarveReads reads) {
      this.reads = reads;
      return this;
   }

   RuinCarve.Run start(ServerLevel level, ServerPlayer actor, RuinCarve.Listener listener) {
      CarvePlan.Builder plan = CarvePlan.builder().hold(this.hold).rise(this.rise).permanent(TempTerrain.permanent());
      List<BlockPos> kept = new ArrayList<>(this.keep.size());
      this.keep.forEach(l -> kept.add(BlockPos.of(l)));
      plan.keep(kept);
      this.waves.forEach(plan::wave);
      Handle handle = TempTerrain.carve(level, actor, plan.build(), this.reads);
      boolean permanent = handle.permanent();
      handle.onWaveRemoved((w, blocks) -> {
         if (permanent && this.drops) {
            TerrainDrops.asMined(level, actor, blocks);
         }

         listener.wave(blocks);
      }).onLayerRestored(listener::layer);
      if (handle.planned() > 0 && !permanent && !handle.visualOnly()) {
         handle.onFinished(listener::done);
      }

      return new RuinCarve.Run(handle);
   }

   interface Listener {
      void wave(List<RemovedBlock> var1);

      default void layer(int y, List<RemovedBlock> blocks) {
      }

      default void done() {
      }
   }

   static record Run(Handle handle) {
      int planned() {
         return this.handle.planned();
      }

      boolean carving() {
         return this.handle.carving();
      }

      void cancel() {
         this.handle.restoreNow();
      }

      void stop() {
         this.handle.stopWaves();
      }
   }
}
