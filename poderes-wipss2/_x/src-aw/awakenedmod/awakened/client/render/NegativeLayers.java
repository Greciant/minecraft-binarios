package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.compat.ShaderPacks;
import net.minecraft.client.renderer.RenderType;

public final class NegativeLayers {
   private NegativeLayers() {
   }

   public static boolean available() {
      return !ShaderPacks.active();
   }

   public static RenderType triangles() {
      return ModRenderTypes.INVERT_TRIANGLES;
   }

   public static RenderType trianglesNoDepth() {
      return ModRenderTypes.INVERT_TRIANGLES_NO_DEPTH;
   }

   public static RenderType model() {
      return ModRenderTypes.INVERT_MODEL;
   }
}
