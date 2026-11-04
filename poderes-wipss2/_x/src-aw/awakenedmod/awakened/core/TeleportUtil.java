package com.awakenedmod.awakened.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TeleportUtil {
   private static final double EJECT_MAX_UP = 6.0;
   private static final double EJECT_STEP = 0.5;
   private static final double UNWEDGE_RADIUS = 5.0;
   private static final int UNWEDGE_DROP = 6;
   private static final int CEILING_DIMENSION_TOP = 120;

   private TeleportUtil() {
   }

   public static void place(ServerPlayer player, ServerLevel level, Vec3 pos, float yaw, float pitch) {
      player.teleportTo(level, pos.x, pos.y, pos.z, yaw, pitch);
      player.setYHeadRot(yaw);
   }

   public static boolean fits(Entity entity, Vec3 feet) {
      AABB box = entity.getBoundingBox().move(feet.subtract(entity.position()));
      return entity.level().noCollision(entity, box);
   }

   public static boolean safeToStand(Entity entity, Vec3 feet) {
      return fits(entity, feet) && !standingHazard(entity.level(), feet);
   }

   public static void ejectFromWall(ServerPlayer player) {
      if (!fits(player, player.position())) {
         for (double up = 0.5; up <= 6.0; up += 0.5) {
            Vec3 candidate = player.position().add(0.0, up, 0.0);
            if (fits(player, candidate)) {
               player.teleportTo(candidate.x, candidate.y, candidate.z);
               player.resetFallDistance();
               return;
            }
         }
      }
   }

   public static void unwedge(ServerPlayer player) {
      if (!fits(player, player.position())) {
         Vec3 spot = nearestSafeSpot(player, player.position(), 5.0, 6);
         if (spot == null) {
            ejectFromWall(player);
         } else {
            player.teleportTo(spot.x, spot.y, spot.z);
            player.resetFallDistance();
         }
      }
   }

   public static void moveBody(Entity body, Vec3 feet) {
      body.teleportTo(feet.x, feet.y, feet.z);
      body.resetFallDistance();
      if (body.getControllingPassenger() instanceof ServerPlayer driver) {
         driver.connection.send(new ClientboundMoveVehiclePacket(body));
      }
   }

   public static Vec3 findLanding(ServerPlayer player, Vec3 origin, Vec3 dir, double maxRange, double step, double minDist) {
      double d = maxRange;

      while (d >= minDist) {
         Vec3 candidate = origin.add(dir.scale(d));
         if (fits(player, candidate) && !TeleportGuard.crosses(player, origin, candidate)) {
            return candidate;
         }

         d -= step;
      }

      return null;
   }

   public static Vec3 findSafeLanding(ServerPlayer player, Vec3 origin, Vec3 dir, double maxRange, double step, double minDist, int maxDrop) {
      Level level = player.serverLevel();
      double d = maxRange;

      while (d >= minDist) {
         Vec3 candidate = origin.add(dir.scale(d));
         if (fits(player, candidate)) {
            Vec3 grounded = dropToGround(level, candidate, maxDrop);
            if (grounded != null && fits(player, grounded) && !standingHazard(level, grounded) && !TeleportGuard.crosses(player, origin, grounded)) {
               return grounded;
            }
         }

         d -= step;
      }

      return null;
   }

   public static Vec3 findLandingToward(ServerPlayer player, Vec3 eye, Vec3 aimPoint, double minDist, double step, int maxDrop) {
      Level level = player.serverLevel();
      Vec3 from = player.position();
      Vec3 ray = aimPoint.subtract(eye);
      double total = ray.length();
      if (total < 1.0E-4) {
         return null;
      } else {
         Vec3 dir = ray.scale(1.0 / total);
         double d = total;

         while (d >= 0.0) {
            Vec3 air = eye.add(dir.scale(d));
            if (fits(player, air) && !standingHazard(level, air)) {
               Vec3 grounded = dropToGround(level, air, maxDrop);
               Vec3 dest = grounded != null && fits(player, grounded) && !standingHazard(level, grounded) ? grounded : air;
               if (dest.distanceTo(from) >= minDist && !TeleportGuard.crosses(player, from, dest)) {
                  return dest;
               }
            }

            d -= step;
         }

         return null;
      }
   }

   public static Vec3 nearestSafeSpot(Entity entity, Vec3 origin, double radius, int maxDrop) {
      Level world = entity.level();
      return nearest(origin, radius, candidate -> safeFooting(entity, world, origin, candidate, maxDrop));
   }

   public static Vec3 nearestFittingSpot(Entity entity, Vec3 origin, double radius) {
      return nearest(origin, radius, candidate -> fits(entity, candidate) ? candidate : null);
   }

   private static Vec3 nearest(Vec3 origin, double radius, UnaryOperator<Vec3> accept) {
      Vec3 here = accept.apply(origin);
      if (here != null) {
         return here;
      } else {
         int r = (int)Math.ceil(radius);
         double maxSq = radius * radius;
         double baseX = Math.floor(origin.x) + 0.5;
         double baseY = Math.floor(origin.y);
         double baseZ = Math.floor(origin.z) + 0.5;
         List<Vec3> candidates = new ArrayList<>();

         for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
               for (int dz = -r; dz <= r; dz++) {
                  if (!((double)(dx * dx + dy * dy + dz * dz) > maxSq)) {
                     candidates.add(new Vec3(baseX + (double)dx, baseY + (double)dy, baseZ + (double)dz));
                  }
               }
            }
         }

         candidates.sort(Comparator.comparingDouble(c -> c.distanceToSqr(origin)));

         for (Vec3 candidate : candidates) {
            Vec3 found = accept.apply(candidate);
            if (found != null) {
               return found;
            }
         }

         return null;
      }
   }

   private static Vec3 safeFooting(Entity entity, Level world, Vec3 origin, Vec3 candidate, int maxDrop) {
      if (!fits(entity, candidate)) {
         return null;
      } else {
         Vec3 grounded = dropToGround(world, candidate, maxDrop);
         if (grounded != null && fits(entity, grounded) && !standingHazard(world, grounded)) {
            if (entity instanceof ServerPlayer traveller && TeleportGuard.crosses(traveller, origin, grounded)) {
               return null;
            }

            return grounded;
         } else {
            return null;
         }
      }
   }

   public static Vec3 surfaceSpot(Entity entity, double x, double z) {
      Level world = entity.level();
      int bx = (int)Math.floor(x);
      int bz = (int)Math.floor(z);
      int top = world.dimension() == Level.NETHER ? 120 : world.getHeight(Types.MOTION_BLOCKING, bx, bz);
      int bottom = minY(world) + 1;
      Vec3 submerged = null;

      for (int y = top; y >= bottom; y--) {
         Vec3 feet = new Vec3((double)bx + 0.5, (double)y, (double)bz + 0.5);
         if (fits(entity, feet) && !standingHazard(world, feet) && dropToGround(world, feet, 1) != null) {
            BlockPos feetPos = new BlockPos(bx, y, bz);
            if (world.getFluidState(feetPos).isEmpty() && world.getFluidState(feetPos.above()).isEmpty()) {
               return feet;
            }

            if (submerged == null) {
               submerged = feet;
            }
         }
      }

      return submerged;
   }

   public static void streamIn(ServerLevel level, Vec3 at) {
      BlockPos centre = BlockPos.containing(at);
      int cx = centre.getX() >> 4;
      int cz = centre.getZ() >> 4;

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            level.getChunk(cx + dx, cz + dz);
         }
      }
   }

   public static int minY(Level world) {
      return world.getMinBuildHeight();
   }

   public static int maxY(Level world) {
      return minY(world) + world.getHeight();
   }

   private static Vec3 dropToGround(Level level, Vec3 feet, int maxDrop) {
      int x = (int)Math.floor(feet.x);
      int z = (int)Math.floor(feet.z);
      int startY = (int)Math.floor(feet.y);

      for (int i = 1; i <= maxDrop; i++) {
         BlockPos below = new BlockPos(x, startY - i, z);
         BlockState state = level.getBlockState(below);
         if (!state.getCollisionShape(level, below).isEmpty()) {
            if (isHazardBlock(state)) {
               return null;
            }

            return new Vec3(feet.x, (double)(below.getY() + 1), feet.z);
         }
      }

      return null;
   }

   private static boolean standingHazard(Level level, Vec3 feet) {
      BlockPos pos = BlockPos.containing(feet.x, feet.y, feet.z);
      return isHazardBlock(level.getBlockState(pos)) || isHazardBlock(level.getBlockState(pos.above()));
   }

   private static boolean isHazardBlock(BlockState state) {
      return state.getFluidState().is(FluidTags.LAVA) || state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK);
   }
}
