package com.awakenedmod.awakened.core.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

final class TerrainTickets {
   private TerrainTickets() {
   }

   static int radiusFor(int chunkRadius) {
      return chunkRadius + 1;
   }

   static void hold(ServerLevel level, long center, int radius) {
      ChunkPos chunk = new ChunkPos(center);
      level.getChunkSource().addRegionTicket(TicketType.PORTAL, chunk, radius, new BlockPos(chunk.getMinBlockX(), 0, chunk.getMinBlockZ()));
   }
}
