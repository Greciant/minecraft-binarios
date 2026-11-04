package com.awakenedmod.awakened.content.graviton;

import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class GravityWellPlanet {
   public static final int UNKNOWN = -1;
   public static final double BASE_RADIUS = 1.5;
   private static final double FEET_GAP = 0.08;
   private static final double HEAD_GAP = 2.25;
   private static final int SEED_BITS = 20;

   private GravityWellPlanet() {
   }

   public static int roll(RandomSource random, int previous) {
      int n = GravityWellPlanet.Kind.ALL.length;
      int prev = previous == -1 ? -1 : previous & 15;
      int kind = random.nextInt(prev < 0 ? n : n - 1);
      if (prev >= 0 && kind >= prev) {
         kind++;
      }

      return pack(kind, random.nextInt(1048576));
   }

   public static int pack(int kind, int seed) {
      return (seed & 1048575) << 4 | kind & 15;
   }

   public static GravityWellPlanet.Kind kind(int planet) {
      return GravityWellPlanet.Kind.ALL[Math.floorMod(planet & 15, GravityWellPlanet.Kind.ALL.length)];
   }

   public static int seed(int planet) {
      return planet >>> 4;
   }

   public static boolean known(int planet) {
      return planet != -1;
   }

   public static int kinds() {
      return GravityWellPlanet.Kind.ALL.length;
   }

   public static Vec3 center(Level level, Vec3 point, Direction face, boolean surface, double radius) {
      Vec3 under = point.subtract(0.0, radius + 0.08, 0.0);
      Vec3 over = point.add(0.0, 2.25 + radius, 0.0);
      if (face.getAxis().isHorizontal()) {
         Vec3 out = GravityWellVisual.axis(face).scale(radius + 0.1 - 0.45);
         under = under.add(out);
         over = over.add(out);
      }

      boolean floor = surface && face == Direction.UP;
      Vec3 first = floor ? over : under;
      Vec3 second = floor ? under : over;
      return !clear(level, first, radius) && clear(level, second, radius) ? second : first;
   }

   private static boolean clear(Level level, Vec3 c, double radius) {
      double r = radius * 0.8;
      MutableBlockPos pos = new MutableBlockPos();
      int x0 = (int)Math.floor(c.x - r);
      int x1 = (int)Math.floor(c.x + r);
      int y0 = (int)Math.floor(c.y - r);
      int y1 = (int)Math.floor(c.y + r);
      int z0 = (int)Math.floor(c.z - r);
      int z1 = (int)Math.floor(c.z + r);

      for (int x = x0; x <= x1; x++) {
         for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
               double dx = (double)x + 0.5 - c.x;
               double dy = (double)y + 0.5 - c.y;
               double dz = (double)z + 0.5 - c.z;
               if (!(dx * dx + dy * dy + dz * dz > (r + 0.5) * (r + 0.5))) {
                  pos.set(x, y, z);
                  if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                     return false;
                  }
               }
            }
         }
      }

      return true;
   }

   public static enum Kind {
      EARTH(1.0, () -> Blocks.GRASS_BLOCK.defaultBlockState(), 0),
      DESERT(0.95, () -> Blocks.SAND.defaultBlockState(), 0),
      ICE(0.9, () -> Blocks.PACKED_ICE.defaultBlockState(), 0),
      STAR(1.05, () -> Blocks.MAGMA_BLOCK.defaultBlockState(), 12731932),
      MOON(0.85, () -> Blocks.STONE.defaultBlockState(), 0),
      GAS_GIANT(1.1, () -> Blocks.ORANGE_WOOL.defaultBlockState(), 0),
      CRYSTAL(0.9, () -> Blocks.CALCITE.defaultBlockState(), 0),
      NETHER(0.95, () -> Blocks.NETHERRACK.defaultBlockState(), 0),
      OCEAN(1.0, () -> Blocks.PRISMARINE.defaultBlockState(), 0);

      private static final GravityWellPlanet.Kind[] ALL = values();
      public final double scale;
      private final Supplier<BlockState> sound;
      public final int glow;

      private Kind(double scale, Supplier<BlockState> sound, int glow) {
         this.scale = scale;
         this.sound = sound;
         this.glow = glow;
      }

      public BlockState material() {
         return this.sound.get();
      }

      public double radius(double base) {
         return base * this.scale;
      }
   }
}
