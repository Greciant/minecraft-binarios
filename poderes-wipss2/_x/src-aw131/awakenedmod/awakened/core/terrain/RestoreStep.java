package com.awakenedmod.awakened.core.terrain;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

final class RestoreStep {
   private RestoreStep() {
   }

   static RestoreStep.Result put(
      ServerLevel level, ChunkHole hole, BlockPos pos, BlockState state, @Nullable BlockPos partnerPos, @Nullable BlockState partnerState
   ) {
      if (!mayOverwrite(level, hole, pos)) {
         return RestoreStep.Result.GIVEN_UP;
      } else {
         boolean pair = TerrainSafety.isDouble(state);
         if (!pair || partnerPos != null && partnerState != null && mayOverwrite(level, hole, partnerPos)) {
            if (!wouldFall(level, pos, state) && (!pair || !wouldFall(level, partnerPos, partnerState))) {
               List<Unbury.Move> moves = new ArrayList<>();
               if (Unbury.plan(level, pos, state, moves) && (!pair || Unbury.plan(level, partnerPos, partnerState, moves))) {
                  write(level, pos, state);
                  if (pair) {
                     write(level, partnerPos, partnerState);
                  }

                  moves.forEach(Unbury.Move::run);
                  return RestoreStep.Result.PLACED;
               } else {
                  return RestoreStep.Result.RETRY;
               }
            } else {
               return RestoreStep.Result.RETRY;
            }
         } else {
            return RestoreStep.Result.GIVEN_UP;
         }
      }
   }

   private static boolean wouldFall(ServerLevel level, BlockPos pos, BlockState state) {
      if (!(state.getBlock() instanceof FallingBlock)) {
         return false;
      } else {
         BlockPos below = pos.below();
         return FallingBlock.isFree(level.getBlockState(below)) && TerrainLedger.pendingAt(level, below);
      }
   }

   private static boolean mayOverwrite(ServerLevel level, ChunkHole hole, BlockPos pos) {
      BlockState current = level.getBlockState(pos);
      return current.isAir() ? true : !current.hasBlockEntity() && !hole.isEdited(pos.asLong());
   }

   private static void write(ServerLevel level, BlockPos pos, BlockState state) {
      boolean overFluid = !level.getBlockState(pos).getFluidState().isEmpty();
      TerrainWriter.put(level, pos, state);
      if (overFluid) {
         settleFluids(level, pos);
      }
   }

   private static void settleFluids(ServerLevel level, BlockPos pos) {
      for (Direction side : Direction.values()) {
         BlockPos next = pos.relative(side);
         if (level.isLoaded(next)) {
            FluidState fluid = level.getFluidState(next);
            if (!fluid.isEmpty()) {
               level.scheduleTick(next, fluid.getType(), fluid.getType().getTickDelay(level));
            }
         }
      }
   }

   static enum Result {
      PLACED,
      GIVEN_UP,
      RETRY;
   }
}
