package com.awakenedmod.awakened.core.npc;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class NpcNav {
   private static final int FLEE_REACH = 20;
   private static final int FLEE_VERTICAL = 7;
   private static final double FLEE_MIN_DISTANCE = 12.0;
   private static final int FLEE_TRIES = 8;
   private static final double EYE = 1.5;

   private NpcNav() {
   }

   public static boolean canStand(Level level, BlockPos feet) {
      if (level.isOutsideBuildHeight(feet)) {
         return false;
      } else {
         BlockPos below = feet.below();
         BlockState ground = level.getBlockState(below);
         if (!ground.getCollisionShape(level, below).isEmpty() && !hot(ground) && level.getFluidState(below).isEmpty()) {
            for (BlockPos p : new BlockPos[]{feet, feet.above()}) {
               BlockState s = level.getBlockState(p);
               if (!s.getCollisionShape(level, p).isEmpty() || hot(s) || !level.getFluidState(p).isEmpty()) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   @Nullable
   public static Vec3 standInColumn(Level level, Vec3 point, int up, int down) {
      BlockPos base = BlockPos.containing(point);

      for (int dy = up; dy >= -down; dy--) {
         BlockPos feet = base.offset(0, dy, 0);
         if (canStand(level, feet)) {
            return Vec3.atBottomCenterOf(feet);
         }
      }

      return null;
   }

   public static boolean safeBelow(Level level, Vec3 pos, int depth) {
      BlockPos start = BlockPos.containing(pos);

      for (int dy = 0; dy <= depth; dy++) {
         BlockPos p = start.below(dy);
         if (level.isOutsideBuildHeight(p)) {
            return false;
         }

         if (!level.getFluidState(p).isEmpty()) {
            return false;
         }

         BlockState s = level.getBlockState(p);
         if (!s.getCollisionShape(level, p).isEmpty()) {
            return !hot(s);
         }

         if (hot(s)) {
            return false;
         }
      }

      return false;
   }

   public static boolean clearLine(Level level, Entity self, Vec3 a, Vec3 b) {
      return level.clip(new ClipContext(a, b, Block.COLLIDER, Fluid.NONE, self)).getType() == Type.MISS;
   }

   public static boolean crossesFluid(Level level, Vec3 a, Vec3 b) {
      double length = a.distanceTo(b);
      int steps = Math.max(1, Mth.ceil(length));

      for (int i = 0; i <= steps; i++) {
         Vec3 p = a.lerp(b, (double)i / (double)steps);
         if (!level.getFluidState(BlockPos.containing(p)).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   @Nullable
   public static Vec3 fleePoint(PathfinderMob mob, LivingEntity threat) {
      return fleePoint(mob, threat, false);
   }

   @Nullable
   public static Vec3 fleePoint(PathfinderMob mob, LivingEntity threat, boolean hiddenOnly) {
      Level level = mob.level();
      Vec3 threatEye = threat.getEyePosition();
      Vec3 fallback = null;

      for (int i = 0; i < 8; i++) {
         Vec3 c = LandRandomPos.getPosAway(mob, 20, 7, threat.position());
         if (c != null && !(c.distanceTo(threat.position()) < 12.0)) {
            Vec3 stand = standInColumn(level, c, 1, 2);
            if (stand != null) {
               if (!clearLine(level, mob, stand.add(0.0, 1.5, 0.0), threatEye)) {
                  return stand;
               }

               if (fallback == null) {
                  fallback = stand;
               }
            }
         }
      }

      return hiddenOnly ? null : fallback;
   }

   @Nullable
   public static Vec3 dryLand(PathfinderMob mob, int radius) {
      Level level = mob.level();
      Vec3 pos = mob.position();
      Vec3 best = null;
      double bestDist = Double.MAX_VALUE;

      for (int r = 1; r <= radius; r++) {
         for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) == r) {
                  Vec3 stand = standInColumn(level, pos.add((double)dx, 0.0, (double)dz), 2, 2);
                  if (stand != null) {
                     double d = stand.distanceToSqr(pos);
                     if (d < bestDist) {
                        bestDist = d;
                        best = stand;
                     }
                  }
               }
            }
         }

         if (best != null) {
            return best;
         }
      }

      return null;
   }

   public static boolean wet(Entity mob) {
      return mob.isInWater() || mob.level().getFluidState(mob.blockPosition()).is(FluidTags.WATER);
   }

   @Nullable
   public static Vec3 around(Entity self, LivingEntity target, double angleDeg, double radius) {
      return around(self, target.position(), target.getEyePosition(), angleDeg, radius);
   }

   @Nullable
   public static Vec3 around(Entity self, Vec3 centre, Vec3 eye, double angleDeg, double radius) {
      Level level = self.level();
      Vec3 out = new Vec3(self.getX() - centre.x, 0.0, self.getZ() - centre.z);
      out = out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize();
      Vec3 p = centre.add(out.yRot((float)Math.toRadians(angleDeg)).scale(radius));
      Vec3 stand = standInColumn(level, p, 2, 3);
      return stand != null
            && !crossesFluid(level, self.position().add(0.0, 0.5, 0.0), stand.add(0.0, 0.5, 0.0))
            && clearLine(level, self, stand.add(0.0, 1.5, 0.0), eye)
         ? stand
         : null;
   }

   @Nullable
   public static Vec3 sidestep(Entity self, LivingEntity threat, double reach, double back, boolean preferRight) {
      Level level = self.level();
      Vec3 away = new Vec3(self.getX() - threat.getX(), 0.0, self.getZ() - threat.getZ());
      away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
      Vec3 side = new Vec3(-away.z, 0.0, away.x);
      Vec3 from = self.position().add(0.0, 0.5, 0.0);

      for (int i = 0; i < 2; i++) {
         Vec3 dir = side.scale(preferRight == (i == 0) ? 1.0 : -1.0).add(away.scale(back)).normalize();
         Vec3 stand = standInColumn(level, self.position().add(dir.scale(reach)), 1, 3);
         if (stand != null
            && clearLine(level, self, from, stand.add(0.0, 0.5, 0.0))
            && clearLine(level, self, from.add(0.0, 1.2, 0.0), stand.add(0.0, 1.7, 0.0))
            && !crossesFluid(level, from, stand.add(0.0, 0.5, 0.0))) {
            return stand;
         }
      }

      return null;
   }

   public static double flat(Vec3 a, Vec3 b) {
      double dx = b.x - a.x;
      double dz = b.z - a.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static boolean hot(BlockState s) {
      return s.is(BlockTags.FIRE) || s.is(BlockTags.CAMPFIRES) || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.LAVA);
   }
}
