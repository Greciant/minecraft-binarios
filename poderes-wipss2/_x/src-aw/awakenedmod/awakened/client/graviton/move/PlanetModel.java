package com.awakenedmod.awakened.client.graviton.move;

import com.awakenedmod.awakened.content.graviton.GravityWellPlanet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class PlanetModel {
   final GravityWellPlanet.Kind kind;
   final int seed;
   final float halfAcross;
   final List<PlanetModel.Mat> mats = new ArrayList<>();
   final List<PlanetModel.Layer> layers = new ArrayList<>();
   final List<Integer> crust = new ArrayList<>();
   final float tilt;
   final float tiltYaw;
   private final Map<PlanetModel.Mat, Integer> matIndex = new HashMap<>();
   private static final PlanetModel.Mat GRASS = PlanetModel.Mat.tinted("grass_block_top", 8174923, Blocks.GRASS_BLOCK.defaultBlockState());
   private static final PlanetModel.Mat WATER = PlanetModel.Mat.tinted("water_still", 4159204, Blocks.BLUE_CONCRETE_POWDER.defaultBlockState());
   private static final PlanetModel.Mat DEEP = PlanetModel.Mat.tinted("water_still", 2972352, Blocks.BLUE_CONCRETE_POWDER.defaultBlockState());
   private static final PlanetModel.Mat SAND = PlanetModel.Mat.of("sand", Blocks.SAND.defaultBlockState());
   private static final PlanetModel.Mat SNOW = PlanetModel.Mat.of("snow", Blocks.SNOW_BLOCK.defaultBlockState());
   private static final PlanetModel.Mat LEAVES = PlanetModel.Mat.tinted("oak_leaves", 5151274, Blocks.OAK_LEAVES.defaultBlockState());
   private static final PlanetModel.Mat CLOUD = PlanetModel.Mat.of("white_wool", Blocks.WHITE_WOOL.defaultBlockState());
   private static final PlanetModel.Mat SMOOTH_STONE = PlanetModel.Mat.of("smooth_stone", Blocks.SMOOTH_STONE.defaultBlockState());
   private static final PlanetModel.Mat SANDSTONE = PlanetModel.Mat.of("sandstone_top", Blocks.SANDSTONE.defaultBlockState());
   private static final PlanetModel.Mat RED_SAND = PlanetModel.Mat.of("red_sand", Blocks.RED_SAND.defaultBlockState());
   private static final PlanetModel.Mat ORANGE_TERRACOTTA = PlanetModel.Mat.of("orange_terracotta", Blocks.ORANGE_TERRACOTTA.defaultBlockState());
   private static final PlanetModel.Mat TERRACOTTA = PlanetModel.Mat.of("terracotta", Blocks.TERRACOTTA.defaultBlockState());
   private static final PlanetModel.Mat PACKED_ICE = PlanetModel.Mat.of("packed_ice", Blocks.PACKED_ICE.defaultBlockState());
   private static final PlanetModel.Mat BLUE_ICE = PlanetModel.Mat.of("blue_ice", Blocks.BLUE_ICE.defaultBlockState());
   private static final PlanetModel.Mat POWDER_SNOW = PlanetModel.Mat.of("powder_snow", Blocks.SNOW_BLOCK.defaultBlockState());
   private static final PlanetModel.Mat MAGMA = PlanetModel.Mat.glowing("magma", Blocks.MAGMA_BLOCK.defaultBlockState());
   private static final PlanetModel.Mat LAVA = PlanetModel.Mat.glowing("lava_still", Blocks.MAGMA_BLOCK.defaultBlockState());
   private static final PlanetModel.Mat GLOWSTONE = PlanetModel.Mat.glowing("glowstone", Blocks.GLOWSTONE.defaultBlockState());
   private static final PlanetModel.Mat SHROOMLIGHT = PlanetModel.Mat.glowing("shroomlight", Blocks.SHROOMLIGHT.defaultBlockState());
   private static final PlanetModel.Mat END_STONE = PlanetModel.Mat.of("end_stone", Blocks.END_STONE.defaultBlockState());
   private static final PlanetModel.Mat ANDESITE = PlanetModel.Mat.of("andesite", Blocks.ANDESITE.defaultBlockState());
   private static final PlanetModel.Mat TUFF = PlanetModel.Mat.of("tuff", Blocks.TUFF.defaultBlockState());
   private static final PlanetModel.Mat STONE = PlanetModel.Mat.of("stone", Blocks.STONE.defaultBlockState());
   private static final PlanetModel.Mat ORANGE_WOOL = PlanetModel.Mat.of("orange_wool", Blocks.ORANGE_WOOL.defaultBlockState());
   private static final PlanetModel.Mat BROWN_WOOL = PlanetModel.Mat.of("brown_wool", Blocks.BROWN_WOOL.defaultBlockState());
   private static final PlanetModel.Mat YELLOW_WOOL = PlanetModel.Mat.of("yellow_wool", Blocks.YELLOW_WOOL.defaultBlockState());
   private static final PlanetModel.Mat WHITE_WOOL = PlanetModel.Mat.of("white_wool", Blocks.WHITE_WOOL.defaultBlockState());
   private static final PlanetModel.Mat RED_WOOL = PlanetModel.Mat.of("red_wool", Blocks.RED_WOOL.defaultBlockState());
   private static final PlanetModel.Mat WHITE_TERRACOTTA = PlanetModel.Mat.of("white_terracotta", Blocks.WHITE_TERRACOTTA.defaultBlockState());
   private static final PlanetModel.Mat LIGHT_GRAY_CONCRETE = PlanetModel.Mat.of("light_gray_concrete", Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
   private static final PlanetModel.Mat SMOOTH_SANDSTONE = PlanetModel.Mat.of("sandstone_top", Blocks.SMOOTH_SANDSTONE.defaultBlockState());
   private static final PlanetModel.Mat AMETHYST = PlanetModel.Mat.of("amethyst_block", Blocks.AMETHYST_BLOCK.defaultBlockState());
   private static final PlanetModel.Mat BUDDING = PlanetModel.Mat.of("budding_amethyst", Blocks.BUDDING_AMETHYST.defaultBlockState());
   private static final PlanetModel.Mat CALCITE = PlanetModel.Mat.of("calcite", Blocks.CALCITE.defaultBlockState());
   private static final PlanetModel.Mat BASALT = PlanetModel.Mat.of("smooth_basalt", Blocks.SMOOTH_BASALT.defaultBlockState());
   private static final PlanetModel.Mat NETHERRACK = PlanetModel.Mat.of("netherrack", Blocks.NETHERRACK.defaultBlockState());
   private static final PlanetModel.Mat NYLIUM = PlanetModel.Mat.of("crimson_nylium", Blocks.CRIMSON_NYLIUM.defaultBlockState());
   private static final PlanetModel.Mat WART = PlanetModel.Mat.of("nether_wart_block", Blocks.NETHER_WART_BLOCK.defaultBlockState());
   private static final PlanetModel.Mat WARPED_WART = PlanetModel.Mat.of("warped_wart_block", Blocks.WARPED_WART_BLOCK.defaultBlockState());
   private static final PlanetModel.Mat BLACKSTONE = PlanetModel.Mat.of("blackstone", Blocks.BLACKSTONE.defaultBlockState());
   private static final PlanetModel.Mat PRISMARINE = PlanetModel.Mat.of("prismarine", Blocks.PRISMARINE.defaultBlockState());
   private static final PlanetModel.Mat DARK_PRISMARINE = PlanetModel.Mat.of("dark_prismarine", Blocks.DARK_PRISMARINE.defaultBlockState());
   private static final PlanetModel.Mat SEA_LANTERN = PlanetModel.Mat.glowing("sea_lantern", Blocks.SEA_LANTERN.defaultBlockState());
   static final int[] DX = new int[]{0, 0, 0, 0, -1, 1};
   static final int[] DY = new int[]{-1, 1, 0, 0, 0, 0};
   static final int[] DZ = new int[]{0, 0, -1, 1, 0, 0};

   private PlanetModel(GravityWellPlanet.Kind kind, int seed, float halfAcross) {
      this.kind = kind;
      this.seed = seed;
      this.halfAcross = halfAcross;
      this.tilt = (float)Math.toRadians(12.0 + 22.0 * hash01(seed, 91));
      this.tiltYaw = (float)((Math.PI * 2) * hash01(seed, 92));
   }

   private int mat(PlanetModel.Mat m) {
      return this.matIndex.computeIfAbsent(m, k -> {
         this.mats.add(k);
         return this.mats.size() - 1;
      });
   }

   static PlanetModel build(int planet, int across) {
      GravityWellPlanet.Kind kind = GravityWellPlanet.kind(planet);
      int seed = GravityWellPlanet.seed(planet);
      float half = (float)across / 2.0F;
      PlanetModel m = new PlanetModel(kind, seed, half);
      switch (kind) {
         case EARTH:
            m.earth();
            break;
         case DESERT:
            m.desert();
            break;
         case ICE:
            m.ice();
            break;
         case STAR:
            m.star();
            break;
         case MOON:
            m.moon();
            break;
         case GAS_GIANT:
            m.gasGiant();
            break;
         case CRYSTAL:
            m.crystal();
            break;
         case NETHER:
            m.nether();
            break;
         case OCEAN:
            m.ocean();
      }

      for (PlanetModel.Layer layer : m.layers) {
         m.seal(layer);
      }

      return m;
   }

   private void earth() {
      double h = (double)this.halfAcross;
      this.ball(h, (x, y, z) -> h + 0.35 * (this.noise(x, y, z, 2.6, 1) - 0.5) * 2.0, (x, y, z, depth) -> {
         double land = this.noise(x, y, z, 1.8, 2);
         if (Math.abs(y) > 0.8 + 0.1 * this.noise(x, y, z, 4.0, 3)) {
            return SNOW;
         } else if (land < 0.47) {
            return land < 0.36 ? DEEP : WATER;
         } else if (land < 0.51) {
            return SAND;
         } else {
            return this.noise(x, y, z, 4.5, 4) > 0.64 ? LEAVES : GRASS;
         }
      });
      this.addCrust(GRASS, SAND, LEAVES);
      PlanetModel.Layer clouds = new PlanetModel.Layer(1.35F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.layers.add(clouds);
      this.shell(clouds, h + 1.2, (x, y, z) -> this.noise(x, y, z, 2.6, 5) > 0.69 ? CLOUD : null);
      this.moon(STONE, 1.3, h * 2.1, 0.05F);
   }

   private void desert() {
      double h = (double)this.halfAcross;
      this.ball(h, (x, y, z) -> h + 0.6 * (this.noise(x, y, z, 3.2, 1) - 0.5) * 2.0, (x, y, z, depth) -> {
         double band = y * 2.6 + 0.9 * (this.noise(x, y, z, 2.0, 2) - 0.5) + 0.3 * Math.sin(Math.atan2(z, x) * 3.0 + (double)this.seed);
         int b = Mth.floor(band * 1.7) & 7;

         return switch (b) {
            case 0, 4 -> SAND;
            case 1 -> SANDSTONE;
            case 2, 6 -> RED_SAND;
            case 3 -> ORANGE_TERRACOTTA;
            case 5 -> TERRACOTTA;
            default -> SAND;
         };
      });
      this.addCrust(SAND, RED_SAND, SANDSTONE);
      this.rocks(TERRACOTTA, 3, h * 1.9);
   }

   private void ice() {
      double h = (double)this.halfAcross;
      this.ball(h, (x, y, z) -> h + 0.3 * (this.noise(x, y, z, 2.8, 1) - 0.5) * 2.0, (x, y, z, depth) -> {
         double n = this.noise(x, y, z, 3.0, 2);
         if (Math.abs(n - 0.5) < 0.045) {
            return BLUE_ICE;
         } else if (!(Math.abs(y) > 0.78) && !(this.noise(x, y, z, 2.2, 3) > 0.7)) {
            return n > 0.68 ? POWDER_SNOW : (n < 0.34 ? BLUE_ICE : PACKED_ICE);
         } else {
            return SNOW;
         }
      });
      this.addCrust(PACKED_ICE, SNOW, BLUE_ICE);
      this.moon(PACKED_ICE, 1.0, h * 1.9, -0.07F);
   }

   private void star() {
      double h = (double)this.halfAcross;
      this.ball(h, (x, y, z) -> h + 0.35 * (this.noise(x, y, z, 3.4, 1) - 0.5) * 2.0, (x, y, z, depth) -> {
         double n = this.noise(x, y, z, 3.0, 2);
         if (n > 0.66) {
            return this.noise(x, y, z, 6.0, 3) > 0.5 ? SHROOMLIGHT : GLOWSTONE;
         } else {
            return n < 0.42 ? LAVA : MAGMA;
         }
      });
      this.addCrust(MAGMA, GLOWSTONE, SHROOMLIGHT);
   }

   private void moon() {
      double h = (double)this.halfAcross;
      int craters = 6;
      double[][] c = new double[craters][4];

      for (int i = 0; i < craters; i++) {
         double[] d = direction(this.seed, 20 + i);
         c[i] = new double[]{d[0], d[1], d[2], 0.28 + 0.22 * hash01(this.seed, 40 + i)};
      }

      PlanetModel.Shape shape = (x, y, z) -> {
         double r = h + 0.25 * (this.noise(x, y, z, 3.0, 1) - 0.5) * 2.0;

         for (double[] k : c) {
            double ang = Math.acos(Mth.clamp(x * k[0] + y * k[1] + z * k[2], -1.0, 1.0));
            double u = ang / k[3];
            if (u < 1.0) {
               r -= 1.3 * (1.0 - u * u);
            } else if (u < 1.35) {
               r += 0.6 * Math.sin((u - 1.0) / 0.35 * Math.PI);
            }
         }

         return r;
      };
      this.ball(h, shape, (x, y, z, depth) -> {
         for (double[] k : c) {
            double ang = Math.acos(Mth.clamp(x * k[0] + y * k[1] + z * k[2], -1.0, 1.0));
            if (ang < k[3] * 0.85) {
               return TUFF;
            }
         }

         double n = this.noise(x, y, z, 2.0, 2);
         return n > 0.6 ? ANDESITE : (n < 0.35 ? END_STONE : SMOOTH_STONE);
      });
      this.addCrust(SMOOTH_STONE, ANDESITE, END_STONE);
   }

   private void gasGiant() {
      double h = (double)this.halfAcross;
      double spotA = (Math.PI * 2) * hash01(this.seed, 7);
      double sx = Math.cos(spotA) * 0.97;
      double sz = Math.sin(spotA) * 0.97;
      this.ball(h, (xx, y, zx) -> h, (xx, y, zx, depth) -> {
         double dx = xx - sx;
         double dz = zx - sz;
         if (Math.abs(y + 0.25) < 0.14 && dx * dx + dz * dz < 0.1) {
            return RED_WOOL;
         } else {
            double band = y * 3.4 + 0.35 * Math.sin(Math.atan2(zx, xx) * 2.0 + (double)this.seed * 0.37) + 0.5 * (this.noise(xx, y, zx, 2.2, 2) - 0.5);
            int b = Math.floorMod(Mth.floor(band * 1.25), 5);

            return switch (b) {
               case 0 -> ORANGE_WOOL;
               case 1 -> WHITE_WOOL;
               case 2 -> BROWN_WOOL;
               case 3 -> YELLOW_WOOL;
               default -> WHITE_TERRACOTTA;
            };
         }
      });
      this.addCrust(ORANGE_WOOL, WHITE_WOOL, BROWN_WOOL);
      PlanetModel.Layer ring = new PlanetModel.Layer(1.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      ring.thick = 0.35F;
      this.layers.add(ring);
      int reach = Mth.ceil(h * 2.0);

      for (int x = -reach; x <= reach; x++) {
         for (int z = -reach; z <= reach; z++) {
            double r = Math.sqrt((double)(x * x + z * z)) / h;
            if (!(r < 1.45) && !(r > 1.85) && !(hash01(this.seed, x * 131 + z * 7 + 3000) < 0.1)) {
               ring.voxels.add(new float[]{(float)x, 0.0F, (float)z, (float)this.mat(r < 1.62 ? LIGHT_GRAY_CONCRETE : SMOOTH_SANDSTONE), 0.0F});
            }
         }
      }
   }

   private void crystal() {
      double h = (double)this.halfAcross;
      int spikes = 12;
      double[][] s = new double[spikes][4];

      for (int i = 0; i < spikes; i++) {
         double[] d = direction(this.seed, 60 + i);
         s[i] = new double[]{d[0], d[1], d[2], 1.0 + 1.8 * hash01(this.seed, 70 + i)};
      }

      PlanetModel.Shape shape = (x, y, z) -> {
         double r = h + 0.3 * (this.noise(x, y, z, 3.0, 1) - 0.5) * 2.0;

         for (double[] k : s) {
            double dot = x * k[0] + y * k[1] + z * k[2];
            double lateral = Math.sqrt(Math.max(0.0, 1.0 - dot * dot)) * h;
            if (dot > 0.0 && lateral < 0.55) {
               r = Math.max(r, h + k[3]);
            }
         }

         return r;
      };
      this.ball(h + 5.0, shape, (x, y, z, depth) -> {
         double n = this.noise(x, y, z, 3.0, 2);
         if (Math.abs(y) < 0.18 && n > 0.35) {
            return BASALT;
         } else {
            return n > 0.62 ? CALCITE : (n < 0.4 ? BUDDING : AMETHYST);
         }
      });
      this.addCrust(AMETHYST, CALCITE, BUDDING);
   }

   private void nether() {
      double h = (double)this.halfAcross;
      this.ball(h, (x, y, z) -> h + 0.5 * (this.noise(x, y, z, 3.0, 1) - 0.5) * 2.0, (x, y, z, depth) -> {
         double n = this.noise(x, y, z, 2.4, 2);
         if (hash01(this.seed, (int)(x * 997.0 + y * 131.0 + z * 17.0)) > 0.95) {
            return GLOWSTONE;
         } else if (Math.abs(n - 0.5) < 0.03) {
            return MAGMA;
         } else if (n > 0.64) {
            return this.noise(x, y, z, 5.0, 3) > 0.5 ? WART : NYLIUM;
         } else {
            return n < 0.25 ? WARPED_WART : NETHERRACK;
         }
      });
      this.addCrust(NETHERRACK, WART, NYLIUM);
      this.rocks(BLACKSTONE, 4, h * 1.8);
   }

   private void ocean() {
      double h = (double)this.halfAcross;
      this.ball(h, (x, y, z) -> h + (this.noise(x, y, z, 2.0, 1) > 0.74 ? 0.6 : 0.0), (x, y, z, depth) -> {
         double n = this.noise(x, y, z, 2.0, 1);
         if (n > 0.74) {
            return SAND;
         } else {
            double reef = this.noise(x, y, z, 3.6, 2);
            if (reef > 0.66) {
               return hash01(this.seed, (int)(x * 331.0 + y * 37.0 + z * 7.0)) > 0.9 ? SEA_LANTERN : (reef > 0.72 ? DARK_PRISMARINE : PRISMARINE);
            } else {
               return n < 0.4 ? DEEP : WATER;
            }
         }
      });
      this.addCrust(PRISMARINE, DARK_PRISMARINE, SAND);
   }

   private void addCrust(PlanetModel.Mat... ms) {
      for (PlanetModel.Mat m : ms) {
         this.crust.add(this.mat(m));
      }
   }

   private void ball(double reach, PlanetModel.Shape shape, PlanetModel.Paint paint) {
      PlanetModel.Layer body = new PlanetModel.Layer(1.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.layers.add(body);
      int n = Mth.ceil(reach) + 1;

      for (int x = -n; x <= n; x++) {
         for (int y = -n; y <= n; y++) {
            for (int z = -n; z <= n; z++) {
               double r = Math.sqrt((double)(x * x + y * y + z * z));
               if (r < 1.0E-6) {
                  body.voxels.add(new float[]{0.0F, 0.0F, 0.0F, (float)this.mat(paint.at(0.0, 1.0, 0.0, (double)this.halfAcross)), 0.0F});
               } else {
                  double dx = (double)x / r;
                  double dy = (double)y / r;
                  double dz = (double)z / r;
                  double top = shape.radius(dx, dy, dz);
                  if (r <= top) {
                     body.voxels.add(new float[]{(float)x, (float)y, (float)z, (float)this.mat(paint.at(dx, dy, dz, top - r)), 0.0F});
                  }
               }
            }
         }
      }
   }

   private void shell(PlanetModel.Layer layer, double radius, PlanetModel.Shape3Mat paint) {
      int n = Mth.ceil(radius) + 1;

      for (int x = -n; x <= n; x++) {
         for (int y = -n; y <= n; y++) {
            for (int z = -n; z <= n; z++) {
               double r = Math.sqrt((double)(x * x + y * y + z * z));
               if (!(Math.abs(r - radius) > 0.5)) {
                  PlanetModel.Mat m = paint.at((double)x / r, (double)y / r, (double)z / r);
                  if (m != null) {
                     layer.voxels.add(new float[]{(float)x, (float)y, (float)z, (float)this.mat(m), 0.0F});
                  }
               }
            }
         }
      }
   }

   private void moon(PlanetModel.Mat m, double r, double orbit, float rate) {
      PlanetModel.Layer layer = new PlanetModel.Layer(
         0.6F, (float)orbit, rate, (float)((Math.PI * 2) * hash01(this.seed, 81)), (float)Math.toRadians(-15.0 + 30.0 * hash01(this.seed, 82))
      );
      this.layers.add(layer);
      int n = Mth.ceil(r);

      for (int x = -n; x <= n; x++) {
         for (int y = -n; y <= n; y++) {
            for (int z = -n; z <= n; z++) {
               if ((double)(x * x + y * y + z * z) <= r * r + 0.3) {
                  layer.voxels.add(new float[]{(float)x, (float)y, (float)z, (float)this.mat(m), 0.0F});
               }
            }
         }
      }
   }

   private void rocks(PlanetModel.Mat m, int count, double orbit) {
      for (int i = 0; i < count; i++) {
         float o = (float)(orbit * (0.9 + 0.3 * hash01(this.seed, 100 + i)));
         float rate = (float)((0.04 + 0.04 * hash01(this.seed, 110 + i)) * (double)(hash01(this.seed, 120 + i) > 0.5 ? 1 : -1));
         PlanetModel.Layer layer = new PlanetModel.Layer(
            2.5F,
            o,
            rate,
            (float)((Math.PI * 2) * (double)i / (double)count + hash01(this.seed, 130 + i)),
            (float)Math.toRadians(-25.0 + 50.0 * hash01(this.seed, 140 + i))
         );
         this.layers.add(layer);
         layer.voxels.add(new float[]{0.0F, 0.0F, 0.0F, (float)this.mat(m), 0.0F});
         if (hash01(this.seed, 150 + i) > 0.4) {
            layer.voxels.add(new float[]{1.0F, 0.0F, 0.0F, (float)this.mat(m), 0.0F});
         }

         if (hash01(this.seed, 160 + i) > 0.6) {
            layer.voxels.add(new float[]{0.0F, 1.0F, 0.0F, (float)this.mat(m), 0.0F});
         }
      }
   }

   private void seal(PlanetModel.Layer layer) {
      Set<Long> filled = new HashSet<>();

      for (float[] v : layer.voxels) {
         filled.add(key((int)v[0], (int)v[1], (int)v[2]));
      }

      List<float[]> kept = new ArrayList<>();

      for (float[] v : layer.voxels) {
         int x = (int)v[0];
         int y = (int)v[1];
         int z = (int)v[2];
         int faces = 0;

         for (int f = 0; f < 6; f++) {
            if (!filled.contains(key(x + DX[f], y + DY[f], z + DZ[f]))) {
               faces |= 1 << f;
            }
         }

         if (faces != 0) {
            v[4] = (float)faces;
            kept.add(v);
         }
      }

      layer.count = kept.size();
      layer.data = new float[kept.size() * 5];

      for (int i = 0; i < kept.size(); i++) {
         System.arraycopy(kept.get(i), 0, layer.data, i * 5, 5);
      }

      layer.voxels.clear();
   }

   private static long key(int x, int y, int z) {
      return (long)(x + 512) << 20 | (long)(y + 512) << 10 | (long)(z + 512);
   }

   static double hash01(int seed, int salt) {
      long h = (long)seed * -7046029254386353131L + (long)salt * -4417276706812531889L;
      h ^= h >>> 31;
      h *= -4658895280553007687L;
      h ^= h >>> 29;
      return (double)(h >>> 11) * 1.110223E-16F;
   }

   private static double[] direction(int seed, int salt) {
      double y = hash01(seed, salt) * 2.0 - 1.0;
      double a = hash01(seed, salt + 5000) * Math.PI * 2.0;
      double s = Math.sqrt(1.0 - y * y);
      return new double[]{Math.cos(a) * s, y, Math.sin(a) * s};
   }

   private double noise(double x, double y, double z, double freq, int salt) {
      x = x * freq + 17.3 * (double)salt;
      y = y * freq + 5.1 * (double)salt;
      z = z * freq + 11.7 * (double)salt;
      int x0 = Mth.floor(x);
      int y0 = Mth.floor(y);
      int z0 = Mth.floor(z);
      double fx = smooth(x - (double)x0);
      double fy = smooth(y - (double)y0);
      double fz = smooth(z - (double)z0);
      double v = 0.0;

      for (int i = 0; i < 8; i++) {
         int ix = x0 + (i & 1);
         int iy = y0 + (i >> 1 & 1);
         int iz = z0 + (i >> 2 & 1);
         double w = ((i & 1) == 0 ? 1.0 - fx : fx) * ((i >> 1 & 1) == 0 ? 1.0 - fy : fy) * ((i >> 2 & 1) == 0 ? 1.0 - fz : fz);
         v += w * hash01(this.seed + salt * 7919, ix * 73856093 ^ iy * 19349663 ^ iz * 83492791);
      }

      return v;
   }

   private static double smooth(double t) {
      return t * t * (3.0 - 2.0 * t);
   }

   static final class Layer {
      final float spin;
      final float orbit;
      final float orbitRate;
      final float phase;
      final float incline;
      float thick = 1.0F;
      final List<float[]> voxels = new ArrayList<>();
      float[] data;
      int count;

      Layer(float spin, float orbit, float orbitRate, float phase, float incline) {
         this.spin = spin;
         this.orbit = orbit;
         this.orbitRate = orbitRate;
         this.phase = phase;
         this.incline = incline;
      }

      float x(int i) {
         return this.data[i * 5];
      }

      float y(int i) {
         return this.data[i * 5 + 1];
      }

      float z(int i) {
         return this.data[i * 5 + 2];
      }

      int mat(int i) {
         return (int)this.data[i * 5 + 3];
      }

      int faces(int i) {
         return (int)this.data[i * 5 + 4];
      }
   }

   static record Mat(ResourceLocation sprite, int tint, boolean emissive, BlockState state) {
      static PlanetModel.Mat of(String sprite, BlockState state) {
         return new PlanetModel.Mat(ResourceLocation.withDefaultNamespace("block/" + sprite), 16777215, false, state);
      }

      static PlanetModel.Mat tinted(String sprite, int tint, BlockState state) {
         return new PlanetModel.Mat(ResourceLocation.withDefaultNamespace("block/" + sprite), tint, false, state);
      }

      static PlanetModel.Mat glowing(String sprite, BlockState state) {
         return new PlanetModel.Mat(ResourceLocation.withDefaultNamespace("block/" + sprite), 16777215, true, state);
      }
   }

   private interface Paint {
      PlanetModel.Mat at(double var1, double var3, double var5, double var7);
   }

   private interface Shape {
      double radius(double var1, double var3, double var5);
   }

   private interface Shape3Mat {
      PlanetModel.Mat at(double var1, double var3, double var5);
   }
}
