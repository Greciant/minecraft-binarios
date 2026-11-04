package com.devilfruits.fruits.supa_supa.client.blade;

import com.devilfruits.client.render.SkinPixels;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

final class SkinTone {
   private static final int GRID = 64;
   private static final int FALLBACK = 13013106;
   private static final Map<ResourceLocation, Integer> TONES = new HashMap<>();

   static int of(AbstractClientPlayer player) {
      ResourceLocation skin = player.getSkin().texture();
      Integer tone = TONES.get(skin);
      if (tone != null) {
         return tone;
      } else {
         int[] px = SkinPixels.read(skin, 64, 512);
         if (px == null) {
            return 13013106;
         } else {
            int size = (int)Math.round(Math.sqrt((double)px.length));
            int scale = Math.max(1, size / 64);
            long r = 0L;
            long g = 0L;
            long b = 0L;
            long n = 0L;
            int[][] areas = new int[][]{{44, 26, 47, 31}, {36, 58, 39, 63}};

            for (int[] a : areas) {
               for (int y = a[1] * scale; y <= a[3] * scale + scale - 1; y++) {
                  for (int x = a[0] * scale; x <= a[2] * scale + scale - 1; x++) {
                     int c = px[y * size + x];
                     if (c >>> 24 >= 200) {
                        r += (long)(c & 0xFF);
                        g += (long)(c >> 8 & 0xFF);
                        b += (long)(c >> 16 & 0xFF);
                        n++;
                     }
                  }
               }
            }

            int out = n == 0L ? 13013106 : (int)(r / n) << 16 | (int)(g / n) << 8 | (int)(b / n);
            TONES.put(skin, out);
            return out;
         }
      }
   }

   static void clear() {
      TONES.clear();
   }

   private SkinTone() {
   }
}
