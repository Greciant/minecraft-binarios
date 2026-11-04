package com.devilfruits.core.world;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class TechniqueHeat {
   private static final List<TechniqueHeat.Listener> LISTENERS = new CopyOnWriteArrayList<>();

   private TechniqueHeat() {
   }

   public static void register(TechniqueHeat.Listener listener) {
      LISTENERS.add(listener);
   }

   public static void emit(ServerLevel level, Vec3 at, double radius, @Nullable Entity source) {
      for (TechniqueHeat.Listener l : LISTENERS) {
         l.heat(level, at, radius, source);
      }
   }

   @FunctionalInterface
   public interface Listener {
      void heat(ServerLevel var1, Vec3 var2, double var3, @Nullable Entity var5);
   }
}
