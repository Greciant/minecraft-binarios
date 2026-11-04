package com.devilfruits.client.render;

import com.devilfruits.DevilFruits;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class SkinPixels {
   @Nullable
   public static int[] read(ResourceLocation skin, int grid, int maxSize) {
      RenderSystem.assertOnRenderThread();
      Minecraft.getInstance().getTextureManager().getTexture(skin).bind();
      int w = GlStateManager._getTexLevelParameter(3553, 0, 4096);
      int h = GlStateManager._getTexLevelParameter(3553, 0, 4097);
      if (w >= grid && w == h && w % grid == 0 && w <= maxSize) {
         int[] pixels = new int[w * h];

         try {
            NativeImage image = new NativeImage(w, h, false);

            try {
               image.downloadTexture(0, false);

               for (int y = 0; y < h; y++) {
                  for (int x = 0; x < w; x++) {
                     pixels[y * w + x] = image.getPixelRGBA(x, y);
                  }
               }
            } catch (Throwable var10) {
               try {
                  image.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }

               throw var10;
            }

            image.close();
            return pixels;
         } catch (RuntimeException var11) {
            DevilFruits.LOGGER.debug("Could not read skin {}", skin, var11);
            return null;
         }
      } else {
         return null;
      }
   }

   private SkinPixels() {
   }
}
