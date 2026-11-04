package com.awakenedmod.awakened.client.render.limitless;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class CutEdge {
   public static final double LIFT = 0.02;

   private CutEdge() {
   }

   public static boolean solid(BlockGetter level, MutableBlockPos m, double x, double y, double z) {
      m.set(Math.floor(x), Math.floor(y), Math.floor(z));
      BlockState state = level.getBlockState(m);
      return !state.isAir() && state.canOcclude();
   }

   public static List<List<CutEdge.Hit>> arcs(BlockGetter level, Vec3 start, Vec3 dir, double from, double to, double radius, int angles, double step) {
      Vec3 ref = Math.abs(dir.y) < 0.95 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
      Vec3 u = ref.cross(dir).normalize();
      Vec3 v = dir.cross(u).normalize();
      MutableBlockPos m = new MutableBlockPos();
      List<List<CutEdge.Hit>> perAngle = new ArrayList<>(angles);
      int empty = -1;

      for (int i = 0; i < angles; i++) {
         double a = (Math.PI * 2) * (double)i / (double)angles;
         Vec3 off = u.scale(Math.cos(a) * radius).add(v.scale(Math.sin(a) * radius));
         Vec3 base = start.add(off);
         List<CutEdge.Hit> hits = new ArrayList<>(2);
         boolean prev = solidAt(level, m, base, dir, from);

         for (double s = from + step; s <= to + 1.0E-6; s += step) {
            boolean now = solidAt(level, m, base, dir, s);
            if (now != prev) {
               double lo = s - step;
               double hi = s;

               for (int k = 0; k < 7; k++) {
                  double mid = (lo + hi) * 0.5;
                  if (solidAt(level, m, base, dir, mid) == prev) {
                     lo = mid;
                  } else {
                     hi = mid;
                  }
               }

               double air = now ? lo - 0.02 : hi + 0.02;
               hits.add(new CutEdge.Hit(base.add(dir.scale(air)), air, now));
            }

            prev = now;
         }

         if (hits.isEmpty() && empty < 0) {
            empty = i;
         }

         perAngle.add(hits);
      }

      return join(perAngle, Math.max(0, empty), empty < 0, step * 2.5);
   }

   private static boolean solidAt(BlockGetter level, MutableBlockPos m, Vec3 base, Vec3 dir, double s) {
      return solid(level, m, base.x + dir.x * s, base.y + dir.y * s, base.z + dir.z * s);
   }

   private static List<List<CutEdge.Hit>> join(List<List<CutEdge.Hit>> perAngle, int first, boolean closed, double tol) {
      int n = perAngle.size();
      List<List<CutEdge.Hit>> done = new ArrayList<>();
      List<List<CutEdge.Hit>> open = new ArrayList<>();
      int steps = closed ? n + 1 : n;

      for (int k = 0; k < steps; k++) {
         List<CutEdge.Hit> hits = perAngle.get((first + k) % n);
         List<List<CutEdge.Hit>> next = new ArrayList<>();

         for (CutEdge.Hit h : hits) {
            List<CutEdge.Hit> best = null;
            double bestD = tol;

            for (List<CutEdge.Hit> arc : open) {
               CutEdge.Hit last = arc.get(arc.size() - 1);
               double d = Math.abs(last.along() - h.along());
               if (last.entering() == h.entering() && d <= bestD) {
                  best = arc;
                  bestD = d;
               }
            }

            if (best != null) {
               open.remove(best);
               best.add(h);
               next.add(best);
            } else {
               List<CutEdge.Hit> arcx = new ArrayList<>();
               arcx.add(h);
               next.add(arcx);
            }
         }

         done.addAll(open);
         open = next;
      }

      done.addAll(open);
      done.removeIf(a -> a.size() < 2);
      return done;
   }

   public static record Hit(Vec3 pos, double along, boolean entering) {
   }
}
