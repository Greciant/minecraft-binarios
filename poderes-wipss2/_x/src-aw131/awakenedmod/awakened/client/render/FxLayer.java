package com.awakenedmod.awakened.client.render;

import net.minecraft.client.renderer.RenderType;

public enum FxLayer {
   LIGHT(true),
   LIGHT_THROUGH_WALLS(true),
   PAINT(false),
   PAINT_THROUGH_WALLS(false),
   SOLID(false);

   final boolean light;

   private FxLayer(boolean light) {
      this.light = light;
   }

   RenderType vanilla() {
      return switch (this) {
         case LIGHT -> dragonRays();
         case LIGHT_THROUGH_WALLS -> ModRenderTypes.ADDITIVE_NO_DEPTH;
         case PAINT -> ModRenderTypes.DARK_TRIANGLES;
         case PAINT_THROUGH_WALLS -> ModRenderTypes.DARK_TRIANGLES_NO_DEPTH;
         case SOLID -> ModRenderTypes.DARK_TRIANGLES_DEPTH;
      };
   }

   RenderType underPack() {
      return switch (this) {
         case LIGHT -> ModRenderTypes.PACK_LIGHT;
         case LIGHT_THROUGH_WALLS -> ModRenderTypes.PACK_LIGHT_NO_DEPTH;
         case PAINT -> ModRenderTypes.PACK_PAINT;
         case PAINT_THROUGH_WALLS -> ModRenderTypes.PACK_PAINT_NO_DEPTH;
         case SOLID -> ModRenderTypes.PACK_SOLID;
      };
   }

   private static RenderType dragonRays() {
      return RenderType.dragonRays();
   }
}
