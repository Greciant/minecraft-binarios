package com.awakenedmod.awakened.core.terrain;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;

final class TerrainProtection {
   @Nullable
   private final ServerPlayer actor;
   private final ServerLevel level;
   private final Long2BooleanOpenHashMap columns = new Long2BooleanOpenHashMap();

   TerrainProtection(ServerLevel level, @Nullable ServerPlayer actor) {
      this.level = level;
      this.actor = actor;
   }

   boolean allows(BlockPos top, BlockState state) {
      if (this.actor == null) {
         return true;
      } else {
         long key = TerrainColumns.key(top);
         if (this.columns.containsKey(key)) {
            return this.columns.get(key);
         } else if (!this.level.mayInteract(this.actor, top) || this.actor.blockActionRestricted(this.level, top, this.actor.gameMode.getGameModeForPlayer())) {
            this.columns.put(key, false);
            return false;
         } else if (state.isAir()) {
            return true;
         } else {
            TerrainProtection.Probe probe = new TerrainProtection.Probe(this.level, top.immutable(), state, this.actor);
            NeoForge.EVENT_BUS.post(probe);
            boolean allowed = !probe.isCanceled();
            this.columns.put(key, allowed);
            return allowed;
         }
      }
   }

   static final class Probe extends BreakEvent {
      Probe(Level level, BlockPos pos, BlockState state, Player breaker) {
         super(level, pos, state, breaker);
      }
   }
}
