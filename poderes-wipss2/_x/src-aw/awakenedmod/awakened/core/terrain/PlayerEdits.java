package com.awakenedmod.awakened.core.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent.EntityMultiPlaceEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;

final class PlayerEdits {
   private PlayerEdits() {
   }

   static void register(IEventBus gameBus) {
      gameBus.addListener(EventPriority.LOWEST, PlayerEdits::onPlace);
   }

   private static void onPlace(EntityPlaceEvent event) {
      if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Player) {
         if (event instanceof EntityMultiPlaceEvent multi) {
            for (BlockSnapshot snapshot : multi.getReplacedBlockSnapshots()) {
               mark(level, snapshot.getPos());
            }
         } else {
            mark(level, event.getPos());
         }

         return;
      }
   }

   private static void mark(ServerLevel level, BlockPos pos) {
      LevelChunk chunk = TerrainLedger.loaded(level, TerrainLedger.chunkOf(pos));
      ChunkLedger ledger = chunk == null ? null : TerrainLedger.existing(chunk);
      if (ledger != null) {
         boolean marked = false;

         for (ChunkHole hole : ledger.holes()) {
            marked |= hole.markEdited(pos.asLong());
         }

         if (marked) {
            TerrainLedger.touch(chunk);
         }
      }
   }
}
