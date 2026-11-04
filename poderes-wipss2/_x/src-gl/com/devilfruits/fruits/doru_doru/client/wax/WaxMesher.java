package com.devilfruits.fruits.doru_doru.client.wax;

import com.devilfruits.fruits.doru_doru.wax.WaxBlockEntity;
import com.devilfruits.fruits.doru_doru.wax.WaxPalette;
import com.devilfruits.fruits.doru_doru.wax.WaxSurface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

public final class WaxMesher {
   public static final ResourceLocation CURED = ResourceLocation.fromNamespaceAndPath("devilfruits", "block/doru_doru/wax");
   public static final ResourceLocation FRESH = ResourceLocation.fromNamespaceAndPath("devilfruits", "block/doru_doru/wax_fresh");
   private static final int CACHE = 4096;
   private static final Direction[] DIRS = Direction.values();
   private static final Map<WaxMesher.Key, WaxMesher.Mesh> MESHES = new LinkedHashMap<WaxMesher.Key, WaxMesher.Mesh>(512, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<WaxMesher.Key, WaxMesher.Mesh> eldest) {
         return this.size() > 4096;
      }
   };
   private static volatile TextureAtlasSprite cured;
   private static volatile TextureAtlasSprite fresh;
   private static final int[][][] CORNERS = new int[][][]{
      {{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}},
      {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}},
      {{1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}},
      {{0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}},
      {{0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}},
      {{1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}}
   };

   public static void clear() {
      synchronized (MESHES) {
         MESHES.clear();
      }

      cured = null;
      fresh = null;
   }

   public static WaxMesher.Key key(BlockAndTintGetter level, BlockPos pos, WaxBlockEntity be) {
      long[] masks = new long[27];
      MutableBlockPos m = new MutableBlockPos();

      for (int dy = -1; dy <= 1; dy++) {
         for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
               int i = dx + 1 + 3 * (dz + 1 + 3 * (dy + 1));
               if (dx == 0 && dy == 0 && dz == 0) {
                  masks[i] = be.mask();
               } else {
                  m.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                  if (level.getBlockEntity(m) instanceof WaxBlockEntity w) {
                     masks[i] = w.mask();
                  } else {
                     BlockState state = level.getBlockState(m);
                     masks[i] = !state.isAir() && state.isSolidRender(level, m) ? -1L : 0L;
                  }
               }
            }
         }
      }

      int look = be.color() | be.melt() << 4 | (be.cured() ? 1 : 0) << 6;
      return new WaxMesher.Key(WaxSurface.grid(masks), look);
   }

   public static List<BakedQuad> quads(WaxMesher.Key key, @Nullable Direction side) {
      WaxMesher.Mesh mesh;
      synchronized (MESHES) {
         mesh = MESHES.get(key);
      }

      if (mesh == null) {
         mesh = build(key);
         synchronized (MESHES) {
            MESHES.put(key, mesh);
         }
      }

      return mesh.sides[side == null ? 6 : side.get3DDataValue()];
   }

   private static TextureAtlasSprite sprite(boolean isCured) {
      TextureAtlasSprite s = isCured ? cured : fresh;
      if (s == null) {
         s = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(isCured ? CURED : FRESH);
         if (isCured) {
            cured = s;
         } else {
            fresh = s;
         }
      }

      return s;
   }

   private static WaxMesher.Mesh build(WaxMesher.Key key) {
      long[] grid = key.grid;
      int color = key.look & 15;
      int melt = key.look >> 4 & 3;
      boolean isCured = (key.look >> 6 & 1) != 0;
      TextureAtlasSprite sprite = sprite(isCured);
      int tint = WaxPalette.tint(color, !isCured);
      float[] lattice = new float[750];
      boolean[] solved = new boolean[125];
      float[] tmp = new float[6];
      WaxMesher.Mesh mesh = new WaxMesher.Mesh();
      boolean merge = melt == 0;

      for (Direction dir : DIRS) {
         int axis = dir.getAxis().ordinal();
         int sx = dir.getStepX();
         int sy = dir.getStepY();
         int sz = dir.getStepZ();
         int ua = axis == 0 ? 2 : 0;
         int va = axis == 1 ? 2 : 1;

         for (int s = 0; s < 4; s++) {
            boolean[] exposed = new boolean[16];
            boolean[] flat = new boolean[16];
            int count = 0;

            for (int v = 0; v < 4; v++) {
               for (int u = 0; u < 4; u++) {
                  int[] c = cell(axis, s, ua, u, va, v);
                  if (WaxSurface.filled(grid, c[0] + 2, c[1] + 2, c[2] + 2) && !WaxSurface.filled(grid, c[0] + 2 + sx, c[1] + 2 + sy, c[2] + 2 + sz)) {
                     exposed[u + 4 * v] = true;
                     count++;
                     if (merge) {
                        boolean still = true;

                        for (int[] k : CORNERS[dir.get3DDataValue()]) {
                           int li = solve(grid, c[0] + k[0], c[1] + k[1], c[2] + k[2], lattice, solved, tmp);
                           float dot = lattice[li * 6 + 3] * (float)sx + lattice[li * 6 + 4] * (float)sy + lattice[li * 6 + 5] * (float)sz;
                           if (lattice[li * 6] != 0.0F || lattice[li * 6 + 1] != 0.0F || lattice[li * 6 + 2] != 0.0F || dot < 0.9995F) {
                              still = false;
                              break;
                           }
                        }

                        flat[u + 4 * v] = still;
                     }
                  }
               }
            }

            if (count != 0) {
               boolean boundary = dir.getAxisDirection() == AxisDirection.POSITIVE ? s == 3 : s == 0;
               List<BakedQuad> out = mesh.sides[boundary ? dir.get3DDataValue() : 6];
               boolean[] done = new boolean[16];

               for (int v = 0; v < 4; v++) {
                  for (int ux = 0; ux < 4; ux++) {
                     int i = ux + 4 * v;
                     if (exposed[i] && !done[i]) {
                        int w = 1;
                        int h = 1;
                        if (flat[i]) {
                           while (ux + w < 4 && flat[i + w] && !done[i + w]) {
                              w++;
                           }

                           label174:
                           while (v + h < 4) {
                              for (int kx = 0; kx < w; kx++) {
                                 int j = ux + kx + 4 * (v + h);
                                 if (!flat[j] || done[j]) {
                                    break label174;
                                 }
                              }

                              h++;
                           }
                        }

                        for (int b = 0; b < h; b++) {
                           for (int a = 0; a < w; a++) {
                              done[ux + a + 4 * (v + b)] = true;
                           }
                        }

                        int[] c0 = cell(axis, s, ua, ux, va, v);
                        int[] size = new int[]{1, 1, 1};
                        size[ua] = w;
                        size[va] = h;
                        out.add(quad(dir, c0, size, flat[i], grid, lattice, solved, tmp, sprite, tint, melt));
                     }
                  }
               }
            }
         }
      }

      return mesh;
   }

   private static int[] cell(int axis, int s, int ua, int u, int va, int v) {
      int[] c = new int[3];
      c[axis] = s;
      c[ua] = u;
      c[va] = v;
      return c;
   }

   private static int solve(long[] grid, int x, int y, int z, float[] lattice, boolean[] solved, float[] tmp) {
      int li = x + 5 * (z + 5 * y);
      if (!solved[li]) {
         WaxSurface.solve(grid, x, y, z, tmp);
         System.arraycopy(tmp, 0, lattice, li * 6, 6);
         solved[li] = true;
      }

      return li;
   }

   private static BakedQuad quad(
      Direction dir,
      int[] c0,
      int[] size,
      boolean flat,
      long[] grid,
      float[] lattice,
      boolean[] solved,
      float[] tmp,
      TextureAtlasSprite sprite,
      int tint,
      int melt
   ) {
      QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
      baker.setSprite(sprite);
      baker.setDirection(dir);
      baker.setShade(false);
      baker.setHasAmbientOcclusion(false);
      baker.setTintIndex(-1);
      int d = dir.get3DDataValue();
      float droop = melt == 2 ? 0.9F : (melt == 1 ? 0.45F : 0.0F);

      for (int[] k : CORNERS[d]) {
         int lx = c0[0] + k[0] * size[0];
         int ly = c0[1] + k[1] * size[1];
         int lz = c0[2] + k[2] * size[2];
         float px = (float)lx;
         float py = (float)ly;
         float pz = (float)lz;
         float nx = (float)dir.getStepX();
         float ny = (float)dir.getStepY();
         float nz = (float)dir.getStepZ();
         if (!flat) {
            int li = solve(grid, lx, ly, lz, lattice, solved, tmp);
            px += lattice[li * 6];
            py += lattice[li * 6 + 1];
            pz += lattice[li * 6 + 2];
            if (lattice[li * 6 + 3] * (float)dir.getStepX() + lattice[li * 6 + 4] * (float)dir.getStepY() + lattice[li * 6 + 5] * (float)dir.getStepZ() > -0.2F
               )
             {
               nx = lattice[li * 6 + 3];
               ny = lattice[li * 6 + 4];
               nz = lattice[li * 6 + 5];
            }

            if (droop > 0.0F && lx > 0 && lx < 4 && lz > 0 && lz < 4) {
               float t = (float)ly / 4.0F;
               py -= droop * t * t * 1.2F;
            }
         }

         float u;
         float v;
         switch (dir.getAxis()) {
            case Y:
               u = (float)lx / 4.0F;
               v = (float)lz / 4.0F;
               break;
            case Z:
               u = (float)lx / 4.0F;
               v = 1.0F - (float)ly / 4.0F;
               break;
            default:
               u = (float)lz / 4.0F;
               v = 1.0F - (float)ly / 4.0F;
         }

         baker.addVertex(px / 4.0F, py / 4.0F, pz / 4.0F);
         baker.setColor(shade(tint, nx, ny, nz));
         baker.setUv(sprite.getU(u), sprite.getV(v));
         baker.setNormal(nx, ny, nz);
      }

      return baker.bakeQuad();
   }

   static int shade(int tint, float nx, float ny, float nz) {
      float s = 0.6F * nx * nx + 0.8F * nz * nz + (ny > 0.0F ? 1.0F : 0.5F) * ny * ny;
      float lift = (1.0F - s) * 0.38F;
      float sheen = ny > 0.0F ? 0.05F * (float)Math.pow((double)ny, 6.0) : 0.0F;
      float r = (float)(tint >> 16 & 0xFF) / 255.0F * Math.min(1.08F, s + lift * 1.0F + sheen);
      float g = (float)(tint >> 8 & 0xFF) / 255.0F * Math.min(1.08F, s + lift * 0.94F + sheen);
      float b = (float)(tint & 0xFF) / 255.0F * Math.min(1.08F, s + lift * 0.84F + sheen);
      return 0xFF000000 | to8(r) << 16 | to8(g) << 8 | to8(b);
   }

   private static int to8(float v) {
      return Math.max(0, Math.min(255, Math.round(v * 255.0F)));
   }

   private WaxMesher() {
   }

   public static final class Key {
      final long[] grid;
      final int look;
      private final int hash;

      Key(long[] grid, int look) {
         this.grid = grid;
         this.look = look;
         this.hash = 31 * Arrays.hashCode(grid) + look;
      }

      @Override
      public boolean equals(Object o) {
         if (o instanceof WaxMesher.Key k && k.look == this.look && Arrays.equals(k.grid, this.grid)) {
            return true;
         }

         return false;
      }

      @Override
      public int hashCode() {
         return this.hash;
      }
   }

   private static final class Mesh {
      final List<BakedQuad>[] sides = new List[7];

      Mesh() {
         for (int i = 0; i < 7; i++) {
            this.sides[i] = new ArrayList<>();
         }
      }
   }
}
