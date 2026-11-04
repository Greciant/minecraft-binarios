package com.awakenedmod.awakened.core.terrain;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.ChunkEvent.Load;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

final class TerrainScheduler {
   private static final Map<ResourceKey<Level>, Long2ObjectMap<Handle>> LIVE = new HashMap<>();
   private static final Map<ResourceKey<Level>, LongSet> LOADED = new HashMap<>();
   private static final List<Handle> SHOWING = new ArrayList<>();

   private TerrainScheduler() {
   }

   static void register(IEventBus gameBus) {
      gameBus.addListener(TerrainScheduler::onServerTick);
      gameBus.addListener(TerrainScheduler::onServerStopping);
      gameBus.addListener(TerrainScheduler::onServerStopped);
      gameBus.addListener(TerrainScheduler::onChunkLoad);
   }

   static Handle start(ServerLevel level, PlanResolver.Resolved plan, CarvePlan source, boolean visualOnly) {
      long now = level.getGameTime();
      if (plan.isEmpty()) {
         return new Handle(level, null, null, now, plan, source.permanent, visualOnly);
      } else if (visualOnly) {
         Handle handle = new Handle(level, null, null, now, plan, source.permanent, true);
         SHOWING.add(handle);
         return handle;
      } else {
         int minCx = SectionPos.blockToSectionCoord(plan.minX());
         int maxCx = SectionPos.blockToSectionCoord(plan.maxX());
         int minCz = SectionPos.blockToSectionCoord(plan.minZ());
         int maxCz = SectionPos.blockToSectionCoord(plan.maxZ());
         int cx = minCx + maxCx >> 1;
         int cz = minCz + maxCz >> 1;
         int chunkRadius = Math.max(Math.max(cx - minCx, maxCx - cx), Math.max(cz - minCz, maxCz - cz));
         long ticketChunk = ChunkPos.asLong(cx, cz);
         int ticketRadius = TerrainTickets.radiusFor(chunkRadius);
         TempTerrainData data = TempTerrainData.of(level);

         long id;
         do {
            id = ThreadLocalRandom.current().nextLong();
         } while (data.job(id) != null);

         JobClock clock = new JobClock(
            id, now + (long)source.lastWaveTick() + (long)source.holdTicks, source.riseTicks, plan.minY(), plan.maxY(), ticketChunk, ticketRadius
         );
         TempTerrainData.Job indexed = data.jobFor(clock);
         TerrainTickets.hold(level, ticketChunk, ticketRadius);
         Handle handle = new Handle(level, clock, indexed, now, plan, source.permanent, false);
         LIVE.computeIfAbsent(level.dimension(), k -> new Long2ObjectOpenHashMap()).put(id, handle);
         return handle;
      }
   }

   static void restoreNow(Handle handle) {
      handle.cancelWaves();
      JobClock clock = handle.clock;
      if (clock != null) {
         long now = handle.level.getGameTime();
         clock.restoreAt = Math.min(clock.restoreAt, now);
         TempTerrainData data = TempTerrainData.peek(handle.level);
         TempTerrainData.Job job = data == null ? null : data.job(clock.id);
         if (job != null) {
            for (long chunkPos : job.chunks.toLongArray()) {
               LevelChunk chunk = TerrainLedger.loaded(handle.level, chunkPos);
               ChunkLedger ledger = chunk == null ? null : TerrainLedger.existing(chunk);
               ChunkHole hole = ledger == null ? null : ledger.hole(clock.id);
               if (hole != null && hole.clock.restoreAt > now) {
                  hole.clock.restoreAt = now;
                  TerrainLedger.touch(chunk);
               }
            }
         }
      }
   }

   private static void onChunkLoad(Load event) {
      ChunkAccess access = event.getChunk();
      if (event.getLevel() instanceof ServerLevel level && access instanceof LevelChunk chunk) {
         ChunkLedger ledger = TerrainLedger.existing(chunk);
         if (ledger != null && !ledger.isEmpty()) {
            synchronized (LOADED) {
               LOADED.computeIfAbsent(level.dimension(), k -> new LongOpenHashSet()).add(chunk.getPos().toLong());
            }
         }

         return;
      }
   }

   private static void onServerTick(Post event) {
      MinecraftServer server = event.getServer();
      showDue();
      boolean refreshTickets = server.getTickCount() % 100 == 0;
      List<TerrainScheduler.Work> work = new ArrayList<>();

      for (ServerLevel level : server.getAllLevels()) {
         indexLoaded(level);
         TempTerrainData data = TempTerrainData.peek(level);
         if (data != null && !data.jobs().isEmpty()) {
            Long2ObjectMap<Handle> live = LIVE.get(level.dimension());

            for (TempTerrainData.Job job : List.copyOf(data.jobs())) {
               if (refreshTickets) {
                  TerrainTickets.hold(level, job.ticketChunk, job.ticketRadius);
               }

               work.add(new TerrainScheduler.Work(level, data, job, live == null ? null : (Handle)live.get(job.id)));
            }
         }
      }

      if (!work.isEmpty()) {
         int n = work.size();
         int first = Math.floorMod(server.getTickCount(), n);
         int budget = 2048;

         for (int i = 0; i < n && budget > 0; i++) {
            int share = (budget + (n - i) - 1) / (n - i);
            budget -= run(work.get((first + i) % n), share);
         }

         for (TerrainScheduler.Work w : work) {
            if (w.job().chunks.isEmpty() && (w.handle() == null || !w.handle().wavesPending())) {
               w.data().finish(w.job());
               if (w.handle() != null) {
                  LIVE.get(w.level().dimension()).remove(w.job().id);
                  w.handle().finish();
               }
            }
         }
      }
   }

   private static void showDue() {
      int budget = 2048;

      for (int i = SHOWING.size() - 1; i >= 0; i--) {
         Handle handle = SHOWING.get(i);
         if (handle.wavesPending() && budget > 0) {
            int share = (budget + i) / (i + 1);
            budget -= handle.removeDue(handle.level.getGameTime(), share);
         }

         if (!handle.wavesPending()) {
            SHOWING.remove(i);
            handle.finish();
         }
      }
   }

   private static int run(TerrainScheduler.Work w, int share) {
      long now = w.level().getGameTime();
      int used = 0;
      if (w.handle() != null && w.handle().wavesPending()) {
         used += w.handle().removeDue(now, share);
      }

      if (used < share) {
         used += refill(w.level(), w.data(), w.job(), w.handle(), now, share - used, false);
      }

      return used;
   }

   private static void indexLoaded(ServerLevel level) {
      LongSet chunks;
      synchronized (LOADED) {
         chunks = LOADED.remove(level.dimension());
      }

      if (chunks != null) {
         long now = level.getGameTime();
         Long2ObjectMap<Handle> live = LIVE.get(level.dimension());

         for (long chunkPos : chunks.toLongArray()) {
            LevelChunk chunk = TerrainLedger.loaded(level, chunkPos);
            ChunkLedger ledger = chunk == null ? null : TerrainLedger.existing(chunk);
            if (ledger != null && !ledger.isEmpty()) {
               TempTerrainData data = TempTerrainData.of(level);

               for (ChunkHole hole : ledger.holes()) {
                  boolean running = live != null && live.containsKey(hole.clock.id);
                  if (!running && hole.clock.restoreAt > now + 400L) {
                     hole.clock.restoreAt = now + 400L;
                     TerrainLedger.touch(chunk);
                  }

                  data.addChunk(data.jobFor(hole.clock), chunkPos);
               }
            }
         }
      }
   }

   private static int refill(ServerLevel level, TempTerrainData data, TempTerrainData.Job job, @Nullable Handle handle, long now, int budget, boolean stopping) {
      List<TerrainScheduler.Due> due = new ArrayList<>();
      List<TerrainScheduler.Site> sites = new ArrayList<>();

      for (long chunkPos : job.chunks.toLongArray()) {
         LevelChunk chunk = TerrainLedger.loaded(level, chunkPos);
         if (chunk != null && (stopping || level.shouldTickBlocksAt(chunkPos))) {
            ChunkLedger ledger = TerrainLedger.existing(chunk);
            ChunkHole hole = ledger == null ? null : ledger.hole(job.id);
            if (hole != null && !hole.isEmpty()) {
               hole.sortBottomUp();
               TerrainScheduler.Site site = new TerrainScheduler.Site(chunkPos, chunk, ledger, hole, new BitSet());
               sites.add(site);

               for (int e = 0; e < hole.size(); e++) {
                  int y = BlockPos.getY(hole.pos(e));
                  if (!stopping && hole.clock.dueAt(y) > now) {
                     break;
                  }

                  due.add(new TerrainScheduler.Due(site, e, y));
               }
            } else {
               if (ledger != null) {
                  ledger.dropIfEmpty(job.id);
                  tidy(chunk, ledger);
               }

               job.chunks.remove(chunkPos);
               data.setDirty();
            }
         }
      }

      due.sort(Comparator.comparingInt(TerrainScheduler.Due::y));
      TreeMap<Integer, List<RemovedBlock>> layers = new TreeMap<>();
      int used = 0;

      for (TerrainScheduler.Due d : due) {
         if (used >= budget) {
            break;
         }

         used += restore(level, d, layers);
      }

      for (TerrainScheduler.Site site : sites) {
         if (!site.settled().isEmpty()) {
            site.hole().removeAll(site.settled());
            site.ledger().dropIfEmpty(job.id);
            tidy(site.chunk(), site.ledger());
            if (site.hole().isEmpty()) {
               job.chunks.remove(site.chunkPos());
               data.setDirty();
            }
         }
      }

      if (handle != null) {
         layers.forEach(handle::layerRestored);
      }

      return used;
   }

   private static int restore(ServerLevel level, TerrainScheduler.Due d, Map<Integer, List<RemovedBlock>> layers) {
      TerrainScheduler.Site site = d.site();
      ChunkHole hole = site.hole();
      BitSet settled = site.settled();
      if (settled.get(d.entry())) {
         return 0;
      } else {
         BlockPos pos = BlockPos.of(hole.pos(d.entry()));
         BlockState state = hole.state(d.entry());
         BlockPos partnerPos = null;
         BlockState partnerState = null;
         int partner = -1;
         if (TerrainSafety.isDouble(state)) {
            partnerPos = TerrainSafety.partner(pos, state);
            partner = hole.indexOf(partnerPos.asLong());
            if (partner >= 0 && !settled.get(partner) && TerrainSafety.isPartner(state, hole.state(partner))) {
               partnerState = hole.state(partner);
            } else {
               partner = -1;
            }
         }

         switch (RestoreStep.put(level, hole, pos, state, partnerPos, partnerState)) {
            case PLACED:
               settled.set(d.entry());
               layers.computeIfAbsent(pos.getY(), y -> new ArrayList<>()).add(new RemovedBlock(pos, state));
               if (partner >= 0) {
                  settled.set(partner);
                  layers.computeIfAbsent(partnerPos.getY(), y -> new ArrayList<>()).add(new RemovedBlock(partnerPos, partnerState));
                  return 2;
               }

               return 1;
            case GIVEN_UP:
               settled.set(d.entry());
               if (partner >= 0) {
                  settled.set(partner);
               }

               return 0;
            default:
               return 0;
         }
      }
   }

   private static void tidy(LevelChunk chunk, ChunkLedger ledger) {
      if (ledger.isEmpty()) {
         chunk.removeData(TerrainLedger.LEDGER.get());
      }

      TerrainLedger.touch(chunk);
   }

   private static void onServerStopping(ServerStoppingEvent event) {
      for (ServerLevel level : event.getServer().getAllLevels()) {
         TempTerrainData data = TempTerrainData.peek(level);
         if (data != null) {
            for (TempTerrainData.Job job : List.copyOf(data.jobs())) {
               refill(level, data, job, null, level.getGameTime(), Integer.MAX_VALUE, true);
               if (job.chunks.isEmpty()) {
                  data.finish(job);
               }
            }
         }
      }

      LIVE.clear();
      SHOWING.clear();
   }

   private static void onServerStopped(ServerStoppedEvent event) {
      LIVE.clear();
      SHOWING.clear();
      synchronized (LOADED) {
         LOADED.clear();
      }
   }

   private static record Due(TerrainScheduler.Site site, int entry, int y) {
   }

   private static record Site(long chunkPos, LevelChunk chunk, ChunkLedger ledger, ChunkHole hole, BitSet settled) {
   }

   private static record Work(ServerLevel level, TempTerrainData data, TempTerrainData.Job job, @Nullable Handle handle) {
   }
}
