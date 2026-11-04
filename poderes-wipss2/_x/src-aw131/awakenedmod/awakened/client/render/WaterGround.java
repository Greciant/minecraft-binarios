package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.core.Terrain;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

final class WaterGround {
   private static final int KEEP_TICKS = 10;
   private static final Map<Object, WaterGround> KEPT = new HashMap<>();
   private final Level level;
   private final int up;
   private final int down;
   private final long born;
   private final Long2FloatOpenHashMap cache = new Long2FloatOpenHashMap();

   private WaterGround(Level level, int up, int down, long born) {
      this.level = level;
      this.up = up;
      this.down = down;
      this.born = born;
   }

   static WaterGround kept(Object key, Level level, int up, int down) {
      long now = level.getGameTime();
      WaterGround g = KEPT.get(key);
      if (g == null || g.level != level || g.up != up || g.down != down || now - g.born >= 10L || now < g.born) {
         KEPT.values().removeIf(v -> v.level != level || now - v.born >= 20L || now < v.born);
         g = new WaterGround(level, up, down, now);
         KEPT.put(key, g);
      }

      return g;
   }

   float floor(double x, double z, double refY) {
      int base = Mth.floor(refY - 0.001);
      long key = (long)(Mth.floor(x) & 67108863) << 38 | (long)(Mth.floor(z) & 67108863) << 12 | (long)(base & 4095);
      if (this.cache.containsKey(key)) {
         return this.cache.get(key);
      } else {
         float y = (float)Terrain.floorNear(this.level, x, z, refY, this.up, this.down, true);
         this.cache.put(key, y);
         return y;
      }
   }
}
