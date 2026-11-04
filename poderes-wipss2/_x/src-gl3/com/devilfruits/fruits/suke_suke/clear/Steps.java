package com.devilfruits.fruits.suke_suke.clear;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;

public final class Steps {
   private static final ThreadLocal<Entity> CURRENT = new ThreadLocal<>();

   public static void begin(Entity e) {
      CURRENT.set(e);
   }

   public static void end() {
      CURRENT.remove();
   }

   @Nullable
   public static Entity current() {
      return CURRENT.get();
   }

   private Steps() {
   }
}
