package com.devilfruits.fruits.doru_doru.wax;

import com.devilfruits.fruits.doru_doru.DoruBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class WaxWorld {
   public static final int CURE_TICKS = 600;

   public static boolean replaceable(BlockState state) {
      return state.isAir() || state.canBeReplaced() && !(state.getBlock() instanceof WaxBlock);
   }

   public static int pour(ServerLevel level, BlockPos pos, long cells, int color, int owner, int kind, long expires) {
      if (cells == 0L) {
         return 0;
      } else if (level.getBlockEntity(pos) instanceof WaxBlockEntity be) {
         long merged = be.mask() | cells;
         int added = WaxMask.count(merged) - WaxMask.count(be.mask());
         be.setColor(color);
         if (expires != 0L && (be.expires() == 0L || expires < be.expires())) {
            be.setExpires(expires);
         }

         setMask(level, pos, be, merged);
         return added;
      } else {
         BlockState state = level.getBlockState(pos);
         if (!replaceable(state)) {
            return -1;
         } else {
            BlockState wax = (BlockState)((WaxBlock)DoruBlocks.WAX.get()).defaultBlockState().setValue(WaxBlock.FULL, cells == -1L);
            if (!level.setBlock(pos, wax, 3)) {
               return -1;
            } else if (!(level.getBlockEntity(pos) instanceof WaxBlockEntity be)) {
               return -1;
            } else {
               long var19 = level.getGameTime();
               be.init(cells, color, owner, kind, var19, expires);
               if (owner != -1) {
                  WaxOwners.get(level.getServer()).add(owner, 1);
               }

               long first = expires == 0L ? 600L : Math.min(600L, Math.max(1L, expires - var19));
               level.scheduleTick(pos, wax.getBlock(), (int)first);
               if (WaxHeatService.tooHot(level) || WaxHeatService.nearHeat(level, pos)) {
                  WaxHeatService.poke(level, pos);
               }

               sync(level, pos);
               return WaxMask.count(cells);
            }
         }
      }
   }

   public static void setMask(ServerLevel level, BlockPos pos, WaxBlockEntity be, long mask) {
      if (mask == 0L) {
         remove(level, pos);
      } else {
         be.setMask(mask);
         BlockState state = level.getBlockState(pos);
         boolean full = mask == -1L;
         if (state.getBlock() instanceof WaxBlock && (Boolean)state.getValue(WaxBlock.FULL) != full) {
            level.setBlock(pos, (BlockState)state.setValue(WaxBlock.FULL, full), 3);
         }

         sync(level, pos);
      }
   }

   public static void remove(ServerLevel level, BlockPos pos) {
      if (level.getBlockState(pos).getBlock() instanceof WaxBlock) {
         level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      }
   }

   public static void sync(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      level.sendBlockUpdated(pos, state, state, 2);
   }

   private WaxWorld() {
   }
}
