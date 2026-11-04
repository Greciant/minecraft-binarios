package com.devilfruits.client.limb;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class SkinSize {
   private static final int[] DEFAULT = new int[]{64, 64};
   private static final Map<ResourceLocation, int[]> SIZES = new HashMap<>();

   public static int[] of(ResourceLocation texture) {
      int[] size = SIZES.get(texture);
      if (size != null) {
         return size;
      } else {
         RenderSystem.assertOnRenderThread();
         Minecraft.getInstance().getTextureManager().getTexture(texture).bind();
         int w = GlStateManager._getTexLevelParameter(3553, 0, 4096);
         int h = GlStateManager._getTexLevelParameter(3553, 0, 4097);
         if (w >= 64 && h >= 32 && (w == h || w == h * 2)) {
            size = new int[]{w, h};
            SIZES.put(texture, size);
            return size;
         } else {
            return DEFAULT;
         }
      }
   }

   static boolean loading(int[] size) {
      return size == DEFAULT;
   }

   static void clear() {
      SIZES.clear();
   }

   private SkinSize() {
   }
}
