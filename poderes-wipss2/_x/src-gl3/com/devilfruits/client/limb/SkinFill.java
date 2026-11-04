package com.devilfruits.client.limb;

import com.devilfruits.client.render.SkinPixels;
import com.devilfruits.core.limb.LimbFill;
import com.devilfruits.core.limb.LimbUv;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

final class SkinFill {
   private static final Map<ResourceLocation, int[]> PIXELS = new HashMap<>();
   private static final Map<ResourceLocation, Int2IntOpenHashMap> ROWS = new HashMap<>();
   private static final int[] NONE = new int[0];

   static int of(ResourceLocation texture, LimbUv.Layout layout) {
      Int2IntOpenHashMap rows = ROWS.computeIfAbsent(texture, k -> new Int2IntOpenHashMap());
      int key = layout.u() << 8 | layout.v() | layout.width() << 16;
      if (rows.containsKey(key)) {
         return rows.get(key);
      } else {
         int[] size = SkinSize.of(texture);
         if (SkinSize.loading(size)) {
            return 4;
         } else {
            int[] px = PIXELS.get(texture);
            if (px == null) {
               int[] read = LimbUv.legacy(size[0], size[1]) ? null : SkinPixels.read(texture, 64, 1024);
               px = read == null ? NONE : read;
               PIXELS.put(texture, px);
            }

            int row = px == NONE ? 4 : LimbFill.firstRow(px, size[0], size[1], layout);
            rows.put(key, row);
            return row;
         }
      }
   }

   static void clear() {
      PIXELS.clear();
      ROWS.clear();
   }

   private SkinFill() {
   }
}
