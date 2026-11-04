package com.devilfruits.fruits.uo_uo_seiryu.client.render;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class HybridGhosts {
   static final int SAMPLES = 5;
   private static final Int2ObjectMap<HybridGhosts.Trail> TRAILS = new Int2ObjectOpenHashMap();

   public static void start(Entity entity, int ticks, int argb) {
      HybridGhosts.Trail t = (HybridGhosts.Trail)TRAILS.computeIfAbsent(entity.getId(), k -> new HybridGhosts.Trail());
      if (t.left <= 0) {
         t.count = 0;
      }

      t.left = Math.max(t.left, ticks);
      t.tint = argb;
   }

   public static void tick(ClientLevel level) {
      if (!TRAILS.isEmpty()) {
         TRAILS.int2ObjectEntrySet().removeIf(en -> {
            HybridGhosts.Trail t = (HybridGhosts.Trail)en.getValue();
            Entity e = level.getEntity(en.getIntKey());
            if (e != null && --t.left >= -5) {
               if (t.left >= 0) {
                  t.head = (t.head + 1) % 5;
                  t.x[t.head] = e.getX();
                  t.y[t.head] = e.getY();
                  t.z[t.head] = e.getZ();
                  t.yaw[t.head] = e instanceof LivingEntity l ? l.yBodyRot : e.getYRot();
                  t.count = Math.min(5, t.count + 1);
               } else {
                  t.count = Math.max(0, t.count - 1);
               }

               return false;
            } else {
               return true;
            }
         });
      }
   }

   static HybridGhosts.Trail get(Entity entity) {
      return (HybridGhosts.Trail)TRAILS.get(entity.getId());
   }

   static void clear() {
      TRAILS.clear();
   }

   private HybridGhosts() {
   }

   static final class Trail {
      final double[] x = new double[5];
      final double[] y = new double[5];
      final double[] z = new double[5];
      final float[] yaw = new float[5];
      int count;
      int head;
      int left;
      int tint;
   }
}
