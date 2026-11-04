package com.awakenedmod.awakened.content.ruin;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

final class RuinCracks {
   private static final int FIRST_ID = -268435456;
   private static final int ID_SPAN = 268435455;
   private static final float[] OPEN = new float[]{0.25F, 0.55F, 0.8F, 1.0F};
   private static final float[] CLOSE = new float[]{0.4F};
   private static final float[] SNAP = new float[]{1.0F};
   private static final float[] NONE = new float[0];
   private static final int FLOOR_SCAN = 3;
   private static int next;

   private RuinCracks() {
   }

   static int newId() {
      next = (next + 1) % 268435455;
      return -268435456 - next;
   }

   static int sweep(ServerLevel level, BlockPos pos, int peak, int delay, int open, int hold, int close) {
      return run(level, pos, peak, delay, open, hold, close, OPEN, CLOSE);
   }

   static int snap(ServerLevel level, BlockPos pos, int peak, int delay, int hold) {
      return run(level, pos, peak, delay, 1, hold, 1, SNAP, NONE);
   }

   private static int run(ServerLevel level, BlockPos pos, int peak, int delay, int open, int hold, int close, float[] opening, float[] closing) {
      int id = newId();
      BlockPos at = pos.immutable();
      int top = Mth.clamp(peak, 0, 9);
      int up = Math.max(1, open);
      int down = Math.max(1, close);
      int end = delay + up + Math.max(0, hold) + down;
      int[] shown = new int[]{-1};
      RuinFxClock.every(level, end + 1, t -> {
         int s = t - 1 - delay;
         if (s >= 0) {
            if (s >= end - delay) {
               level.destroyBlockProgress(id, at, -1);
            } else {
               int stage;
               if (s < up) {
                  stage = Math.round((float)top * opening[Math.min(opening.length - 1, s * opening.length / up)]);
               } else if (s >= up + hold && closing.length != 0) {
                  int k = (s - up - hold) * closing.length / down;
                  stage = Math.round((float)top * closing[Math.min(closing.length - 1, k)]);
               } else {
                  stage = top;
               }

               if (stage != shown[0]) {
                  shown[0] = stage;
                  level.destroyBlockProgress(id, at, stage);
               }
            }
         }
      });
      return id;
   }

   static void stamp(ServerLevel level, Vec3 center, double radius, int peak, int hold, int maxBlocks) {
      int r = Mth.ceil(radius);
      int cx = Mth.floor(center.x);
      int cz = Mth.floor(center.z);
      int placed = 0;

      for (int ring = 0; ring <= r && placed < maxBlocks; ring++) {
         for (int dx = -ring; dx <= ring && placed < maxBlocks; dx++) {
            for (int dz = -ring; dz <= ring && placed < maxBlocks; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                  double x = (double)(cx + dx) + 0.5;
                  double z = (double)(cz + dz) + 0.5;
                  double d = Math.sqrt((x - center.x) * (x - center.x) + (z - center.z) * (z - center.z));
                  if (!(d > radius + 0.35)) {
                     BlockPos pos = solidUnder(level, BlockPos.containing(x, center.y + 0.5, z));
                     if (pos != null) {
                        int stage = Math.round((float)peak * (1.0F - 0.55F * (float)Math.min(1.0, d / Math.max(0.5, radius))));
                        sweep(level, pos, stage, 0, 2, hold, 8);
                        placed++;
                     }
                  }
               }
            }
         }
      }
   }

   static void wall(ServerLevel level, BlockPos hit, Direction face, int radius, int peak, int hold) {
      Axis normal = face.getAxis();

      for (int a = -radius; a <= radius; a++) {
         for (int b = -radius; b <= radius; b++) {
            BlockPos pos = switch (normal) {
               case X -> hit.offset(0, a, b);
               case Y -> hit.offset(a, 0, b);
               case Z -> hit.offset(a, b, 0);
               default -> throw new MatchException(null, null);
            };
            double d = Math.sqrt((double)(a * a + b * b));
            if (!(d > (double)radius + 0.25) && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
               int stage = Math.round((float)peak * (1.0F - 0.5F * (float)Math.min(1.0, d / (double)Math.max(1, radius))));
               sweep(level, pos, stage, 0, 1, hold, 10);
            }
         }
      }
   }

   @Nullable
   static BlockPos solidUnder(ServerLevel level, BlockPos from) {
      MutableBlockPos p = from.mutable();

      for (int i = 0; i <= 3; i++) {
         BlockState st = level.getBlockState(p);
         if (!st.isAir() && st.getFluidState().isEmpty() && !st.getCollisionShape(level, p).isEmpty()) {
            return p.immutable();
         }

         p.move(0, -1, 0);
      }

      return null;
   }
}
