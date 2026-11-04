package com.devilfruits.fruits.uo_uo_seiryu.client.render;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3d;

public final class HybridAnchors {
   public static final int RIGHT_WRIST = 0;
   public static final int LEFT_WRIST = 1;
   public static final int TAIL_TIP = 2;
   public static final int CROWN = 3;
   public static final int CHEST = 4;
   static final int COUNT = 5;
   private static final int FRESH_TICKS = 3;
   private static final Int2ObjectMap<HybridAnchors.Entry> ENTRIES = new Int2ObjectOpenHashMap();

   static void record(Entity entity, int anchor, double dx, double dy, double dz) {
      HybridAnchors.Entry e = (HybridAnchors.Entry)ENTRIES.computeIfAbsent(entity.getId(), k -> new HybridAnchors.Entry());
      e.offset[anchor * 3] = dx;
      e.offset[anchor * 3 + 1] = dy;
      e.offset[anchor * 3 + 2] = dz;
      e.tick = entity.tickCount;
   }

   public static Vector3d get(Entity entity, int anchor, float partialTick, Vector3d out) {
      double x = Mth.lerp((double)partialTick, entity.xOld, entity.getX());
      double y = Mth.lerp((double)partialTick, entity.yOld, entity.getY());
      double z = Mth.lerp((double)partialTick, entity.zOld, entity.getZ());
      HybridAnchors.Entry e = (HybridAnchors.Entry)ENTRIES.get(entity.getId());
      return e != null && Math.abs(entity.tickCount - e.tick) <= 3
         ? out.set(x + e.offset[anchor * 3], y + e.offset[anchor * 3 + 1], z + e.offset[anchor * 3 + 2])
         : estimate(entity, anchor, x, y, z, out);
   }

   public static boolean fresh(Entity entity) {
      HybridAnchors.Entry e = (HybridAnchors.Entry)ENTRIES.get(entity.getId());
      return e != null && Math.abs(entity.tickCount - e.tick) <= 3;
   }

   private static Vector3d estimate(Entity entity, int anchor, double x, double y, double z, Vector3d out) {
      float h = entity.getBbHeight();
      float yaw = (entity instanceof LivingEntity living ? living.yBodyRot : entity.getYRot()) * (float) (Math.PI / 180.0);
      double fx = (double)(-Mth.sin(yaw));
      double fz = (double)Mth.cos(yaw);
      double rx = (double)(-Mth.cos(yaw));
      double rz = (double)(-Mth.sin(yaw));
      double side;
      double up;
      double fwd;
      switch (anchor) {
         case 0:
            side = 0.27;
            up = 0.4;
            fwd = 0.02;
            break;
         case 1:
            side = -0.27;
            up = 0.4;
            fwd = 0.02;
            break;
         case 2:
            side = 0.0;
            up = 0.3;
            fwd = -0.75;
            break;
         case 3:
            side = 0.0;
            up = 1.0;
            fwd = 0.05;
            break;
         default:
            side = 0.0;
            up = 0.66;
            fwd = 0.08;
      }

      return out.set(x + (rx * side + fx * fwd) * (double)h, y + up * (double)h, z + (rz * side + fz * fwd) * (double)h);
   }

   static void prune(ClientLevel level) {
      ENTRIES.int2ObjectEntrySet().removeIf(en -> level.getEntity(en.getIntKey()) == null);
   }

   static void clear() {
      ENTRIES.clear();
   }

   private HybridAnchors() {
   }

   private static final class Entry {
      final double[] offset = new double[15];
      int tick = Integer.MIN_VALUE;
   }
}
