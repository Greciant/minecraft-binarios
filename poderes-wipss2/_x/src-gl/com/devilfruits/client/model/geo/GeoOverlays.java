package com.devilfruits.client.model.geo;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;

public final class GeoOverlays {
   private static final List<GeoOverlays.Overlay> OVERLAYS = new ArrayList<>();

   public static void register(GeoOverlays.Overlay overlay) {
      OVERLAYS.add(overlay);
   }

   public static void render(LivingEntity entity, GeoRenderer geo, GeoPose pose, MultiBufferSource buffers, int light, int overlay) {
      for (int i = 0; i < OVERLAYS.size(); i++) {
         OVERLAYS.get(i).render(entity, geo, pose, buffers, light, overlay);
      }
   }

   private GeoOverlays() {
   }

   @FunctionalInterface
   public interface Overlay {
      void render(LivingEntity var1, GeoRenderer var2, GeoPose var3, MultiBufferSource var4, int var5, int var6);
   }
}
