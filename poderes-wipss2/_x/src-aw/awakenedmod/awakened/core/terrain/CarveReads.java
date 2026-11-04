package com.awakenedmod.awakened.core.terrain;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public final class CarveReads {
   final ServerLevel level;
   @Nullable
   final ServerPlayer actor;
   private final Long2ObjectOpenHashMap<BlockState> states = new Long2ObjectOpenHashMap();
   private final TerrainProtection protection;

   CarveReads(ServerLevel level, @Nullable ServerPlayer actor) {
      this.level = level;
      this.actor = actor;
      this.protection = new TerrainProtection(level, actor);
   }

   boolean isFor(ServerLevel level, @Nullable ServerPlayer actor) {
      return this.level == level && this.actor == actor;
   }

   public void note(BlockPos pos, BlockState state) {
      this.states.put(pos.asLong(), state);
   }

   public BlockState state(BlockPos pos) {
      long packed = pos.asLong();
      BlockState state = (BlockState)this.states.get(packed);
      if (state == null) {
         state = this.level.getBlockState(pos);
         this.states.put(packed, state);
      }

      return state;
   }

   public boolean allows(BlockPos top) {
      return this.protection.allows(top, this.state(top));
   }
}
