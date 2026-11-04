package com.awakenedmod.awakened.core.terrain;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.phys.AABB;

final class HangingSupports {
   private static final double REACH = 1.0;
   private static final double EDGE = 1.0E-7;

   private HangingSupports() {
   }

   static LongSet in(ServerLevel level, AABB area) {
      LongSet supports = new LongOpenHashSet();

      for (BlockAttachedEntity attached : level.getEntitiesOfClass(BlockAttachedEntity.class, area.inflate(1.0))) {
         supports.add(attached.getPos().asLong());
         if (attached instanceof HangingEntity hanging) {
            Direction facing = hanging.getDirection();
            supports.add(attached.getPos().relative(facing.getOpposite()).asLong());
            AABB wall = hanging.getBoundingBox()
               .move((double)facing.getStepX() * -0.5, (double)facing.getStepY() * -0.5, (double)facing.getStepZ() * -0.5)
               .deflate(1.0E-7);
            BlockPos.betweenClosedStream(wall).forEach(pos -> supports.add(pos.asLong()));
         }
      }

      return supports;
   }
}
