package com.devilfruits.core.logia;

import com.devilfruits.core.physics.PhysicsWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class LogiaReform {
   private static final int[][] SEARCH = buildSearch(3);

   public static void place(ServerPlayer player, Vec3 at) {
      player.teleportTo(at.x, at.y, at.z);
      if (player.position().distanceToSqr(at) > 1.0E-4) {
         player.moveTo(at.x, at.y, at.z, player.getYRot(), player.getXRot());
      }

      player.fallDistance = 0.0F;
   }

   public static boolean fits(ServerPlayer player, Vec3 at, boolean allowFluid) {
      ServerLevel level = player.serverLevel();
      if (level.getWorldBorder().isWithinBounds(at.x, at.z)
         && !(at.y < (double)(level.getMinBuildHeight() + 1))
         && !(at.y > (double)(level.getMaxBuildHeight() + 64))) {
         AABB box = player.getDimensions(Pose.STANDING).makeBoundingBox(at).deflate(1.0E-4);
         return level.noCollision(player, box) && !PhysicsWorld.collidesWithStructure(level, box) ? allowFluid || !level.containsAnyLiquid(box) : false;
      } else {
         return false;
      }
   }

   public static Optional<Vec3> find(ServerPlayer player, Vec3 from, Vec3 wanted, int search, boolean allowFluid) {
      for (int[] o : SEARCH) {
         if (Math.abs(o[0]) <= search && Math.abs(o[1]) <= search && Math.abs(o[2]) <= search) {
            Vec3 at = new Vec3(Math.floor(wanted.x) + 0.5 + (double)o[0], Math.floor(wanted.y) + (double)o[1], Math.floor(wanted.z) + 0.5 + (double)o[2]);
            if (fits(player, at, allowFluid) && reachable(player, from, at)) {
               return Optional.of(at);
            }
         }
      }

      return Optional.empty();
   }

   public static boolean reachable(ServerPlayer player, Vec3 from, Vec3 to) {
      double h = (double)player.getBbHeight() * 0.5;
      BlockHitResult hit = player.level().clip(new ClipContext(from.add(0.0, h, 0.0), to.add(0.0, h, 0.0), Block.COLLIDER, Fluid.NONE, player));
      return hit.getType() == Type.MISS;
   }

   public static void settleSafely(ServerPlayer player) {
      Vec3 here = player.position();
      if (!fits(player, here, true)) {
         for (int[] o : SEARCH) {
            Vec3 at = new Vec3(Math.floor(here.x) + 0.5 + (double)o[0], Math.floor(here.y) + (double)o[1], Math.floor(here.z) + 0.5 + (double)o[2]);
            if (fits(player, at, true)) {
               place(player, at);
               return;
            }
         }

         BlockPos top = player.serverLevel().getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, player.blockPosition());
         place(player, new Vec3((double)top.getX() + 0.5, (double)top.getY(), (double)top.getZ() + 0.5));
      }
   }

   private static int[][] buildSearch(int r) {
      List<int[]> list = new ArrayList<>();

      for (int dy = -r; dy <= r; dy++) {
         for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
               list.add(new int[]{dx, dy, dz});
            }
         }
      }

      list.sort((a, b) -> {
         int da = a[0] * a[0] + a[1] * a[1] + a[2] * a[2];
         int db = b[0] * b[0] + b[1] * b[1] + b[2] * b[2];
         return da != db ? Integer.compare(da, db) : Integer.compare(-a[1], -b[1]);
      });
      return list.toArray(new int[0][]);
   }

   private LogiaReform() {
   }
}
