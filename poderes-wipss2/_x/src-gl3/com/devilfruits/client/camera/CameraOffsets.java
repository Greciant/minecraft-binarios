package com.devilfruits.client.camera;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.Entity;

public final class CameraOffsets {
   private static final List<CameraOffsets.Source> SOURCES = new CopyOnWriteArrayList<>();

   public static void register(CameraOffsets.Source source) {
      SOURCES.add(source);
   }

   public static double firstPerson(Entity entity, float partialTick) {
      double dy = 0.0;

      for (CameraOffsets.Source s : SOURCES) {
         dy += s.offset(entity, partialTick);
      }

      return Math.max(-0.5, Math.min(0.5, dy));
   }

   private CameraOffsets() {
   }

   @FunctionalInterface
   public interface Source {
      double offset(Entity var1, float var2);
   }
}
