package com.devilfruits.fruits.doru_doru.atelier;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class AtelierGen {
   public static final int BRUSH_ADD = 0;
   public static final int BRUSH_CARVE = 1;
   public static final int BRUSH_SMOOTH = 2;
   public static final int BRUSH_DYE = 3;
   public static final float[] SCALES = new float[]{0.25F, 0.5F, 1.0F, 2.0F};
   public static final int SLUMP_STEPS = 6;

   @Nullable
   public static AtelierGen.Result generate(SculptRequest r, AtelierGen.Context ctx) {
      return switch (r.tool()) {
         case BRUSH -> {
            VoxelSet set = stroke(r);
            if (set != null && r.kind() == 0 && r.viscosity() > 0.0F) {
               slump(set, ctx, r.viscosity(), seed(r));
            }

            yield set == null ? null : new AtelierGen.Result(set, null);
         }
         case DISSOLVE -> {
            if (r.points().isEmpty()) {
               yield null;
            } else {
               VoxelSet set = new VoxelSet();
               Vec3 c = r.points().get(0);
               double radius = Math.max(0.5, (double)Math.min(8.0F, r.param(0)));
               yield ShapeGen.rasterize(ShapeGen.sphere(c, radius, false), 0.0F, set) ? new AtelierGen.Result(set, null) : null;
            }
         }
         case CAST -> cast(r, ctx);
         default -> null;
      };
   }

   static long seed(SculptRequest r) {
      long h = -7046029254386353131L;

      for (Vec3 p : r.points()) {
         h = h * 31L + Double.doubleToLongBits(p.x) * 7L + Double.doubleToLongBits(p.y) * 13L + Double.doubleToLongBits(p.z);
      }

      return h;
   }

   @Nullable
   static VoxelSet stroke(SculptRequest r) {
      if (r.points().isEmpty()) {
         return null;
      } else {
         double radius = Math.max(0.25, (double)Math.min(4.0F, r.param(0)));
         VoxelSet out = new VoxelSet();

         for (List<Vec3> pts : ShapeGen.symmetric(r.points(), r)) {
            ShapeGen.Field f = tube(pts, radius);
            if (!ShapeGen.rasterize(f, 0.0F, out)) {
               return null;
            }
         }

         return out;
      }
   }

   static ShapeGen.Field tube(final List<Vec3> pts, final double radius) {
      double minX = Double.MAX_VALUE;
      double minY = minX;
      double minZ = minX;
      double maxX = -minX;
      double maxY = maxX;
      double maxZ = maxX;

      for (Vec3 p : pts) {
         minX = Math.min(minX, p.x);
         minY = Math.min(minY, p.y);
         minZ = Math.min(minZ, p.z);
         maxX = Math.max(maxX, p.x);
         maxY = Math.max(maxY, p.y);
         maxZ = Math.max(maxZ, p.z);
      }

      final double[] bb = new double[]{
         minX - radius - 0.5, minY - radius - 0.5, minZ - radius - 0.5, maxX + radius + 0.5, maxY + radius + 0.5, maxZ + radius + 0.5
      };
      return new ShapeGen.Field() {
         @Override
         public double sdf(double x, double y, double z) {
            double best = Double.MAX_VALUE;
            if (pts.size() == 1) {
               Vec3 p = pts.get(0);
               best = (x - p.x) * (x - p.x) + (y - p.y) * (y - p.y) + (z - p.z) * (z - p.z);
            }

            for (int i = 0; i + 1 < pts.size(); i++) {
               Vec3 a = pts.get(i);
               Vec3 b = pts.get(i + 1);
               double sx = b.x - a.x;
               double sy = b.y - a.y;
               double sz = b.z - a.z;
               double px = x - a.x;
               double py = y - a.y;
               double pz = z - a.z;
               double ss = sx * sx + sy * sy + sz * sz;
               double t = ss < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, (px * sx + py * sy + pz * sz) / ss));
               double ex = px - sx * t;
               double ey = py - sy * t;
               double ez = pz - sz * t;
               best = Math.min(best, ex * ex + ey * ey + ez * ez);
            }

            return Math.sqrt(best) - radius;
         }

         @Override
         public double[] bounds() {
            return bb;
         }
      };
   }

   static void slump(VoxelSet set, AtelierGen.Context ctx, float viscosity, long seed) {
      Long2IntOpenHashMap bottom = new Long2IntOpenHashMap();
      bottom.defaultReturnValue(Integer.MAX_VALUE);
      ObjectIterator cols = set.blocks().long2LongEntrySet().iterator();

      while (cols.hasNext()) {
         Entry e = (Entry)cols.next();
         long key = e.getLongKey();
         int bx = BlockPos.getX(key);
         int by = BlockPos.getY(key);
         int bz = BlockPos.getZ(key);
         long m = e.getLongValue();

         while (m != 0L) {
            int c = Long.numberOfTrailingZeros(m);
            m &= m - 1L;
            int x = bx * 4 + (c & 3);
            int y = by * 4 + (c >> 4);
            int z = bz * 4 + (c >> 2 & 3);
            long col = BlockPos.asLong(x, 0, z);
            if (y < bottom.get(col)) {
               bottom.put(col, y);
            }
         }
      }

      LongArrayList colsx = new LongArrayList(bottom.keySet());
      colsx.sort(Long::compare);
      LongListIterator var24 = colsx.iterator();

      while (var24.hasNext()) {
         long col = (Long)var24.next();
         int x = BlockPos.getX(col);
         int z = BlockPos.getZ(col);
         int y = bottom.get(col);
         if (!ctx.solid(x, y - 1, z)) {
            long h = col * -7046029254386353131L ^ seed;
            h ^= h >>> 29;
            h *= -4658895280553007687L;
            h ^= h >>> 32;
            double r1 = (double)(h >>> 11 & 1048575L) / 1048575.0;
            double r2 = (double)(h >>> 33 & 1048575L) / 1048575.0;
            int sag = r1 < (double)viscosity * 0.8 ? 1 + (r2 < (double)viscosity * 0.5 ? 1 : 0) : 0;
            int drip = r2 < (double)viscosity * 0.45 ? (int)Math.round((double)(6.0F * viscosity) * (0.4 + 0.6 * r1)) : 0;
            int length = Math.min(6, sag + drip);

            for (int k = 1; k <= length && !ctx.solid(x, y - k, z) && !set.has(x, y - k, z) && (k <= sag || (h >>> k * 3 & 3L) != 0L || k <= 2); k++) {
               set.add(x, y - k, z);
            }
         }
      }
   }

   @Nullable
   static AtelierGen.Result cast(SculptRequest r, AtelierGen.Context ctx) {
      int slot = r.kind();
      CastData data = ctx.cast(slot);
      if (data != null && !r.points().isEmpty()) {
         CastData.Unpacked u = data.unpack();
         if (u == null) {
            return null;
         } else {
            int[] maskIndex = u.maskIndex();
            int turns = Math.floorMod(Math.round(r.param(0)), 4);
            boolean mirror = r.param(1) >= 0.5F;
            float scale = SCALES[Math.floorMod(Math.round(r.param(2)), SCALES.length)];
            int sw = u.sx() * 4;
            int sh = u.sy() * 4;
            int sd = u.sz() * 4;
            int tw0 = Math.max(1, Math.round((float)sw * scale));
            int th = Math.max(1, Math.round((float)sh * scale));
            int td0 = Math.max(1, Math.round((float)sd * scale));
            int tw = turns % 2 == 0 ? tw0 : td0;
            int td = turns % 2 == 0 ? td0 : tw0;
            Vec3 p = r.points().get(0);
            int ox = (int)Math.floor(p.x * 4.0) - tw / 2;
            int oy = (int)Math.floor(p.y * 4.0);
            int oz = (int)Math.floor(p.z * 4.0) - td / 2;
            VoxelSet out = new VoxelSet();
            Long2IntOpenHashMap colors = new Long2IntOpenHashMap();

            for (int ty = 0; ty < th; ty++) {
               for (int tz = 0; tz < td; tz++) {
                  for (int tx = 0; tx < tw; tx++) {
                     int ux;
                     int uz;
                     switch (turns) {
                        case 1:
                           ux = tz;
                           uz = tw - 1 - tx;
                           break;
                        case 2:
                           ux = tw - 1 - tx;
                           uz = td - 1 - tz;
                           break;
                        case 3:
                           ux = td - 1 - tz;
                           uz = tx;
                           break;
                        default:
                           ux = tx;
                           uz = tz;
                     }

                     if (mirror) {
                        ux = tw0 - 1 - ux;
                     }

                     int sx = (int)Math.floor(((double)ux + 0.5) / (double)scale);
                     int sy = (int)Math.floor(((double)ty + 0.5) / (double)scale);
                     int sz = (int)Math.floor(((double)uz + 0.5) / (double)scale);
                     if (sx >= 0 && sy >= 0 && sz >= 0 && sx < sw && sy < sh && sz < sd) {
                        long m = u.mask(sx >> 2, sy >> 2, sz >> 2, maskIndex);
                        if (m != 0L && (m >>> (sx & 3 | (sz & 3) << 2 | (sy & 3) << 4) & 1L) != 0L) {
                           int wx = ox + tx;
                           int wy = oy + ty;
                           int wz = oz + tz;
                           out.add(wx, wy, wz);
                           long key = BlockPos.asLong(wx >> 2, wy >> 2, wz >> 2);
                           if (!colors.containsKey(key)) {
                              colors.put(key, Math.max(0, u.color(sx >> 2, sy >> 2, sz >> 2)));
                           }
                        }
                     }
                  }
               }
            }

            return out.isEmpty() ? null : new AtelierGen.Result(out, colors);
         }
      } else {
         return null;
      }
   }

   private AtelierGen() {
   }

   public interface Context {
      boolean solid(int var1, int var2, int var3);

      @Nullable
      CastData cast(int var1);
   }

   public static record Result(VoxelSet set, @Nullable Long2IntOpenHashMap colors) {
   }
}
