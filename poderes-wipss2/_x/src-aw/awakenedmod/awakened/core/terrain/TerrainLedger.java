package com.awakenedmod.awakened.core.terrain;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

final class TerrainLedger {
   private static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "awakened");
   static final Supplier<AttachmentType<ChunkLedger>> LEDGER = TYPES.register(
      "temp_terrain", () -> AttachmentType.builder(ChunkLedger::new).serialize(ChunkLedger.MAP_CODEC.codec(), ledger -> !ledger.isEmpty()).build()
   );

   private TerrainLedger() {
   }

   static void register(IEventBus modBus) {
      TYPES.register(modBus);
   }

   @Nullable
   static ChunkLedger existing(LevelChunk chunk) {
      return (ChunkLedger)chunk.getExistingDataOrNull(LEDGER.get());
   }

   static long record(ServerLevel level, JobClock clock, BlockPos pos, BlockState state) {
      LevelChunk chunk = level.getChunkAt(pos);
      ((ChunkLedger)chunk.getData(LEDGER.get())).holeFor(clock).add(pos.asLong(), state);
      touch(chunk);
      return chunk.getPos().toLong();
   }

   static void forget(ServerLevel level, JobClock clock, BlockPos pos) {
      LevelChunk chunk = level.getChunkAt(pos);
      ChunkLedger ledger = existing(chunk);
      ChunkHole hole = ledger == null ? null : ledger.hole(clock.id);
      if (hole != null) {
         hole.forget(pos.asLong());
         ledger.dropIfEmpty(clock.id);
         touch(chunk);
      }
   }

   @Nullable
   static TerrainLedger.Claim claimAt(ServerLevel level, long jobId, BlockPos pos) {
      return find(level, pos, jobId, false);
   }

   static boolean pendingAt(ServerLevel level, BlockPos pos) {
      return find(level, pos, 0L, true) != null;
   }

   @Nullable
   private static TerrainLedger.Claim find(ServerLevel level, BlockPos pos, long jobId, boolean anyJob) {
      LevelChunk chunk = loaded(level, chunkOf(pos));
      ChunkLedger ledger = chunk == null ? null : existing(chunk);
      if (ledger == null) {
         return null;
      } else {
         long packed = pos.asLong();

         for (ChunkHole hole : ledger.holes()) {
            if ((anyJob || hole.clock.id != jobId) && hole.contains(packed)) {
               return new TerrainLedger.Claim(chunk, ledger, hole, packed);
            }
         }

         return null;
      }
   }

   @Nullable
   static LevelChunk loaded(ServerLevel level, long chunkPos) {
      return level.getChunkSource().getChunkNow(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
   }

   static long chunkOf(BlockPos pos) {
      return ChunkPos.asLong(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
   }

   static void touch(LevelChunk chunk) {
      chunk.setUnsaved(true);
   }

   static record Claim(LevelChunk chunk, ChunkLedger ledger, ChunkHole hole, long pos) {
      BlockState original(BlockState standing) {
         return this.hole.isEdited(this.pos) ? standing : this.hole.state(this.hole.indexOf(this.pos));
      }

      void release() {
         this.hole.forget(this.pos);
         this.ledger.dropIfEmpty(this.hole.clock.id);
         if (this.ledger.isEmpty()) {
            this.chunk.removeData(TerrainLedger.LEDGER.get());
         }

         TerrainLedger.touch(this.chunk);
      }
   }
}
