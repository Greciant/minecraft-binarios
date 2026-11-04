package com.awakenedmod.awakened.core.terrain;

import com.awakenedmod.awakened.core.TeleportUtil;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

final class Unbury {
   private static final double TOUCH = 1.0E-4;

   private Unbury() {
   }

   static boolean plan(ServerLevel level, BlockPos pos, BlockState placed, List<Unbury.Move> moves) {
      VoxelShape shape = placed.getCollisionShape(level, pos);
      if (shape.isEmpty()) {
         return true;
      } else {
         AABB solid = shape.bounds().move(pos);

         for (Entity entity : level.getEntities((Entity)null, solid, Unbury::liftable)) {
            if (entity.getY() < solid.maxY - 1.0E-4) {
               Vec3 to = destination(entity, new Vec3(entity.getX(), solid.maxY, entity.getZ()));
               if (to == null) {
                  return false;
               }

               moves.add(new Unbury.Move(entity, to));
            }
         }

         return true;
      }
   }

   private static boolean liftable(Entity entity) {
      return entity.isAlive() && !entity.isSpectator() && !entity.noPhysics && !entity.isPassenger() && !(entity instanceof BlockAttachedEntity);
   }

   @Nullable
   private static Vec3 destination(Entity entity, Vec3 onTop) {
      if (TeleportUtil.fits(entity, onTop)) {
         return onTop;
      } else {
         Vec3 to = TeleportUtil.nearestSafeSpot(entity, onTop, 4.0, 3);
         return to != null ? to : TeleportUtil.surfaceSpot(entity, entity.getX(), entity.getZ());
      }
   }

   static record Move(Entity entity, Vec3 to) {
      void run() {
         if (this.entity.isAlive()) {
            if (this.entity instanceof ServerPlayer player) {
               player.teleportTo(this.to.x, this.to.y, this.to.z);
            } else {
               this.entity.teleportTo(this.to.x, this.to.y, this.to.z);
            }

            Vec3 motion = this.entity.getDeltaMovement();
            this.entity.setDeltaMovement(motion.x, Math.max(0.0, motion.y), motion.z);
            this.entity.resetFallDistance();
         }
      }
   }
}
