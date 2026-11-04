package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.terrain.CarveReads;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;

record RuinCrater(double radius, int depth, int groundY, int topY, Map<Integer, List<BlockPos>> waves, List<BlockPos> pillar) {
   static RuinCrater.Planner plan(ServerLevel level, BlockPos feet, double radius, int depth, long seed, CarveReads reads) {
      return new RuinCrater.Planner(level, feet, radius, depth, seed, reads);
   }

   Map<Long, Integer> restingFloors() {
      Map<Long, Integer> floors = new HashMap<>();
      this.waves.values().forEach(ring -> ring.forEach(p -> floors.merge(RuinGroundFx.columnKey(p), p.getY(), Math::min)));
      return floors;
   }

   private static RuinCrater fit(double radius, int depth, int ground, List<RuinCrater.Column> columns, List<BlockPos> pillar) {
      int budget = Math.min(150000, 262144);
      int cap = 32;
      double depthScale = 1.0;

      while (cap > 0 && count(columns, ground, ground + cap, depthScale) > budget) {
         cap = Math.max(0, cap - 4);
      }

      while (depthScale > 0.25 && count(columns, ground, ground + cap, depthScale) > budget) {
         depthScale -= 0.125;
      }

      List<RuinCrater.Column> byRing = new ArrayList<>(columns);
      byRing.sort(Comparator.comparingInt(RuinCrater.Column::ring));
      Map<Integer, List<BlockPos>> waves = new TreeMap<>();
      int total = 0;
      int topY = ground;
      int every = Math.max(1, 2);
      IntArrayList taken = new IntArrayList();

      for (RuinCrater.Column c : byRing) {
         taken.clear();
         c.take(ground, ground + cap, depthScale, taken);
         if (!taken.isEmpty()) {
            if (total + taken.size() > budget) {
               break;
            }

            total += taken.size();
            int top = taken.getInt(0);
            topY = Math.max(topY, top);

            for (int i = 0; i < taken.size(); i++) {
               int y = taken.getInt(i);
               int above = top - Math.max(y, c.groundTop());
               int tick = c.ring() + (int)Math.round((double)Math.max(0, above) * 0.25);
               waves.computeIfAbsent(tick / every * every, t -> new ArrayList<>()).add(new BlockPos(c.x(), y, c.z()));
            }
         }
      }

      return new RuinCrater(radius, depth, ground, topY, waves, pillar);
   }

   private static int count(List<RuinCrater.Column> columns, int ground, int capY, double depthScale) {
      IntArrayList scratch = new IntArrayList();
      int total = 0;

      for (RuinCrater.Column c : columns) {
         scratch.clear();
         c.take(ground, capY, depthScale, scratch);
         total += scratch.size();
      }

      return total;
   }

   private static record Column(
      int x, int z, int ring, int groundTop, int worldTop, int depth, boolean held, boolean underCaster, boolean rimTree, IntArrayList solids
   ) {
      void take(int ground, int capY, double depthScale, IntArrayList out) {
         int lowest = this.underCaster ? this.groundTop + 1 : ground - Math.max(1, (int)Math.round((double)this.depth * depthScale)) + 1;
         int highest;
         if (this.worldTop <= capY && !this.rimTree) {
            highest = this.worldTop;
         } else {
            if (this.held) {
               return;
            }

            highest = this.groundTop;
         }

         for (int i = 0; i < this.solids.size(); i++) {
            int y = this.solids.getInt(i);
            if (y <= highest && y >= lowest) {
               out.add(y);
            }
         }
      }
   }

   static final class Planner {
      private static final int PROBE_POINTS = 8;
      private final ServerLevel level;
      private final BlockPos feet;
      private final double radius;
      private final int depth;
      private final int ground;
      private final int reach;
      private final int side;
      private final double farthest;
      private final RuinCrater.Rim rim;
      private final CarveReads reads;
      private final List<RuinCrater.Column> columns = new ArrayList<>();
      private final List<BlockPos> pillar = new ArrayList<>();
      private final MutableBlockPos probe = new MutableBlockPos();
      private int cursor;

      private Planner(ServerLevel level, BlockPos feet, double radius, int depth, long seed, CarveReads reads) {
         this.level = level;
         this.feet = feet.immutable();
         this.radius = radius;
         this.depth = depth;
         this.ground = feet.getY() - 1;
         this.reach = (int)Math.ceil(radius * 1.12) + 1;
         this.side = 2 * this.reach + 1;
         this.farthest = radius * 1.12 + 0.5;
         this.rim = new RuinCrater.Rim(RandomSource.create(seed));
         this.reads = reads;
      }

      int left() {
         return this.side * this.side - this.cursor;
      }

      void scan(int cells) {
         int end = Math.min(this.side * this.side, this.cursor + Math.max(0, cells));

         while (this.cursor < end) {
            int cell = this.cursor++;
            this.scanColumn(cell / this.side - this.reach, cell % this.side - this.reach);
         }
      }

      boolean mayRot() {
         boolean found = false;

         for (int ring = 0; ring <= 2; ring++) {
            int points = ring == 0 ? 1 : 8;
            double r = ring == 0 ? 0.0 : this.radius * (ring == 1 ? 0.5 : 0.9);

            for (int i = 0; i < points; i++) {
               double a = ((double)i + 0.5 * (double)(ring - 1)) * Math.PI * 2.0 / (double)points;
               int x = this.feet.getX() + (int)Math.round(Math.cos(a) * r);
               int z = this.feet.getZ() + (int)Math.round(Math.sin(a) * r);
               if (this.level.isLoaded(this.probe.set(x, this.ground, z))) {
                  int y = this.surface(x, z);
                  if (!this.reads.state(this.probe.set(x, y, z)).isAir()) {
                     found = true;
                     if (this.reads.allows(this.probe)) {
                        return true;
                     }
                  }
               }
            }
         }

         return !found;
      }

      RuinCrater finish() {
         this.scan(this.left());
         return RuinCrater.fit(this.radius, this.depth, this.ground, this.columns, this.pillar);
      }

      private void scanColumn(int dx, int dz) {
         double d = Math.sqrt((double)(dx * dx + dz * dz));
         double edge = this.radius * this.rim.scale(Math.atan2((double)dz, (double)dx)) + 0.5;
         if (!(d > edge)) {
            int x = this.feet.getX() + dx;
            int z = this.feet.getZ() + dz;
            if (this.level.isLoaded(this.probe.set(x, this.ground, z))) {
               int half = 1;
               boolean underCaster = Math.abs(dx) <= half && Math.abs(dz) <= half;
               int worldTop = this.level.getHeight(Types.WORLD_SURFACE, x, z) - 1;
               double e = Math.min(1.0, d / edge);
               int columnDepth = underCaster ? this.depth : Math.max(1, (int)Math.round((double)this.depth * Math.sqrt(1.0 - e * e)));
               int ring = underCaster ? 0 : (int)Math.round(d / this.farthest * 50.0);
               int groundTop = this.surface(x, z);
               this.probe.set(x, groundTop + 1, z);
               boolean held = !this.reads.state(this.probe).getCollisionShape(this.level, this.probe).isEmpty();
               IntArrayList solids = new IntArrayList();
               boolean tree = false;
               int scanTop = Math.min(worldTop, this.ground + 32);

               for (int y = scanTop; y > this.ground - columnDepth; y--) {
                  BlockState state = this.reads.state(this.probe.set(x, y, z));
                  if (!state.isAir()) {
                     solids.add(y);
                     tree |= y > groundTop && (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES));
                  }
               }

               if (!solids.isEmpty()) {
                  this.reads.allows(this.probe.set(x, solids.getInt(0), z));
               }

               if (underCaster) {
                  for (int yx = groundTop; yx > this.ground - this.depth; yx--) {
                     this.pillar.add(new BlockPos(x, yx, z));
                  }
               }

               boolean rimTree = tree && !underCaster && edge - d < 6.0;
               this.columns.add(new RuinCrater.Column(x, z, ring, groundTop, worldTop, columnDepth, held, underCaster, rimTree, solids));
            }
         }
      }

      private int surface(int x, int z) {
         MutableBlockPos pos = new MutableBlockPos();

         for (int y = this.ground + 2; y > this.ground - this.depth; y--) {
            if (!this.reads.state(pos.set(x, y, z)).isAir()) {
               return y;
            }
         }

         return this.ground;
      }
   }

   private static final class Rim {
      private final double phaseA;
      private final double phaseB;
      private final double phaseC;

      Rim(RandomSource random) {
         this.phaseA = random.nextDouble() * Math.PI * 2.0;
         this.phaseB = random.nextDouble() * Math.PI * 2.0;
         this.phaseC = random.nextDouble() * Math.PI * 2.0;
      }

      double scale(double angle) {
         double wobble = 0.5 * Math.sin(3.0 * angle + this.phaseA) + 0.3 * Math.sin(5.0 * angle + this.phaseB) + 0.2 * Math.sin(9.0 * angle + this.phaseC);
         return 1.0 + 0.12 * wobble;
      }
   }
}
