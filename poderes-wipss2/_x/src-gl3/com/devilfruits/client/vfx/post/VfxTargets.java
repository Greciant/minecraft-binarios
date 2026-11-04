package com.devilfruits.client.vfx.post;

import com.devilfruits.client.vfx.VfxSettings;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;

public final class VfxTargets {
   public static final int MIPS = 5;
   private static TextureTarget depth;
   private static TextureTarget scene;
   private static HdrTarget emissive;
   private static HdrTarget emissiveLight;
   private static HdrTarget distort;
   private static final HdrTarget[] BLOOM = new HdrTarget[5];
   private static int width = -1;
   private static int height = -1;
   private static boolean stencil;

   public static void ensure(RenderTarget main) {
      if (main.width != width || main.height != height || main.isStencilEnabled() != stencil || depth == null) {
         width = main.width;
         height = main.height;
         stencil = main.isStencilEnabled();
         free();
         depth = new TextureTarget(width, height, true, Minecraft.ON_OSX);
         if (stencil) {
            depth.enableStencil();
         }

         scene = new TextureTarget(width, height, false, Minecraft.ON_OSX);
         scene.setFilterMode(9729);
         emissive = new HdrTarget(width / 2, height / 2);
         emissiveLight = new HdrTarget(Math.max(1, width / 4), Math.max(1, height / 4));
         distort = new HdrTarget(width / 2, height / 2);

         for (int i = 0; i < 5; i++) {
            BLOOM[i] = new HdrTarget(width >> i + 2, height >> i + 2);
         }
      }
   }

   private static void free() {
      for (RenderTarget t : new RenderTarget[]{depth, scene, emissive, emissiveLight, distort}) {
         if (t != null) {
            t.destroyBuffers();
         }
      }

      for (int i = 0; i < 5; i++) {
         if (BLOOM[i] != null) {
            BLOOM[i].destroyBuffers();
            BLOOM[i] = null;
         }
      }
   }

   public static void copyScene(RenderTarget main) {
      GlStateManager._glBindFramebuffer(36008, main.frameBufferId);
      GlStateManager._glBindFramebuffer(36009, scene.frameBufferId);
      GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, scene.width, scene.height, 16384, 9728);
      GlStateManager._glBindFramebuffer(36160, 0);
   }

   public static TextureTarget depth() {
      return depth;
   }

   public static TextureTarget scene() {
      return scene;
   }

   public static HdrTarget emissive() {
      return VfxSettings.underLoad() ? emissiveLight : emissive;
   }

   public static HdrTarget distort() {
      return distort;
   }

   public static HdrTarget bloom(int mip) {
      return BLOOM[mip];
   }

   private VfxTargets() {
   }
}
