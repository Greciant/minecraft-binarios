package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumMap;
import java.util.Map;

public final class VfxComposites {
   private static final Map<VfxEntity.Kind, VfxComposites.Drawer> REGISTRY = new EnumMap<>(VfxEntity.Kind.class);

   private VfxComposites() {
   }

   public static void register(VfxEntity.Kind kind, VfxComposites.Drawer drawer) {
      REGISTRY.put(kind, drawer);
   }

   static VfxComposites.Drawer get(VfxEntity.Kind kind) {
      return REGISTRY.get(kind);
   }

   @FunctionalInterface
   public interface Drawer {
      void draw(PoseStack var1, VfxDraw.Sink var2, VfxRenderState var3, float var4);
   }
}
