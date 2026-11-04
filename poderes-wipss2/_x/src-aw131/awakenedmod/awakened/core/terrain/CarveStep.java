package com.awakenedmod.awakened.core.terrain;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

final class CarveStep {
   private CarveStep() {
   }

   static int take(Handle handle, long packed, List<RemovedBlock> out) {
      ServerLevel level = handle.level;
      BlockPos pos = BlockPos.of(packed);
      if (pos.getY() < handle.floors().get(TerrainColumns.key(packed))) {
         return 0;
      } else if (!TerrainWriter.live(level, pos)) {
         PlanResolver.raise(handle.floors(), packed);
         return 0;
      } else {
         BlockState state = level.getBlockState(pos);
         if (state.isAir()) {
            return 0;
         } else if (!TerrainSafety.canTake(level, pos, state)) {
            PlanResolver.raise(handle.floors(), packed);
            return 0;
         } else if (!TerrainSafety.isDouble(state)) {
            TerrainLedger.Claim prior = remove(handle, pos, state);
            if (orphans(handle, pos, null)) {
               undo(handle, pos, state);
               PlanResolver.raise(handle.floors(), packed);
               return 1;
            } else {
               release(prior);
               out.add(new RemovedBlock(pos.immutable(), state));
               return 1;
            }
         } else {
            BlockPos partner = TerrainSafety.partner(pos, state);
            BlockState partnerState = level.getBlockState(partner);
            if (TerrainSafety.isPartner(state, partnerState)
               && TerrainSafety.canTake(level, partner, partnerState)
               && partner.getY() >= handle.floors().get(TerrainColumns.key(packed))) {
               TerrainLedger.Claim prior = remove(handle, pos, state);
               TerrainLedger.Claim partnerPrior = remove(handle, partner, partnerState);
               if (!orphans(handle, pos, partner) && !orphans(handle, partner, pos)) {
                  release(prior);
                  release(partnerPrior);
                  out.add(new RemovedBlock(pos.immutable(), state));
                  out.add(new RemovedBlock(partner.immutable(), partnerState));
                  return 2;
               } else {
                  undo(handle, pos, state);
                  undo(handle, partner, partnerState);
                  PlanResolver.raise(handle.floors(), packed);
                  PlanResolver.raise(handle.floors(), partner.asLong());
                  return 2;
               }
            } else {
               PlanResolver.raise(handle.floors(), packed);
               PlanResolver.raise(handle.floors(), partner.asLong());
               return 0;
            }
         }
      }
   }

   static void show(Handle handle, long packed, List<RemovedBlock> out) {
      ServerLevel level = handle.level;
      BlockPos pos = BlockPos.of(packed);
      if (pos.getY() >= handle.floors().get(TerrainColumns.key(packed)) && TerrainWriter.live(level, pos)) {
         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && TerrainSafety.canTake(level, pos, state)) {
            out.add(new RemovedBlock(pos.immutable(), state));
         }
      }
   }

   @Nullable
   private static TerrainLedger.Claim remove(Handle handle, BlockPos pos, BlockState state) {
      TerrainLedger.Claim prior = TerrainLedger.claimAt(handle.level, handle.clock.id, pos);
      handle.record(pos, prior == null ? state : prior.original(state));
      TerrainWriter.take(handle.level, pos);
      return prior;
   }

   private static void release(@Nullable TerrainLedger.Claim prior) {
      if (prior != null) {
         prior.release();
      }
   }

   private static void undo(Handle handle, BlockPos pos, BlockState state) {
      TerrainWriter.put(handle.level, pos, state);
      handle.forget(pos);
   }

   private static boolean orphans(Handle handle, BlockPos pos, @Nullable BlockPos ignore) {
      ServerLevel level = handle.level;

      for (Direction side : Direction.values()) {
         BlockPos next = pos.relative(side);
         if (!next.equals(ignore) && !handle.isPlanned(next.asLong())) {
            if (!level.isLoaded(next)) {
               return true;
            }

            BlockState neighbour = level.getBlockState(next);
            if (!neighbour.isAir()
               && (
                  side == Direction.UP && neighbour.getBlock() instanceof FallingBlock || neighbour.is(BlockTags.PORTALS) || !neighbour.canSurvive(level, next)
               )) {
               return true;
            }
         }
      }

      return false;
   }
}
