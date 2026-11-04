package com.devilfruits.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class CaptureRig {
   private static boolean active;
   private static int targetId = -1;
   private static double distance;
   private static double lift;
   private static float yaw;
   private static float pitch;
   private static double sx;
   private static double sy;
   private static double sz;
   private static double ox;
   private static double oy;
   private static double oz;
   private static boolean primed;
   private static Vec3 position = Vec3.ZERO;

   public static void follow(Entity target, double dist, float yawDeg, float pitchDeg, double liftBlocks) {
      active = true;
      targetId = target.getId();
      distance = dist;
      yaw = yawDeg;
      pitch = pitchDeg;
      lift = liftBlocks;
      primed = false;
   }

   public static void off() {
      active = false;
      targetId = -1;
   }

   public static boolean active() {
      return active;
   }

   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (active && mc.level != null) {
         Entity e = mc.level.getEntity(targetId);
         if (e != null) {
            double tx = e.getX();
            double ty = e.getY() + (double)e.getBbHeight() * 0.5 + lift;
            double tz = e.getZ();
            ox = sx;
            oy = sy;
            oz = sz;
            if (!primed) {
               ox = tx;
               sx = tx;
               oy = ty;
               sy = ty;
               oz = tz;
               sz = tz;
               primed = true;
            } else {
               sx = sx + (tx - sx) * 0.3;
               sy = sy + (ty - sy) * 0.3;
               sz = sz + (tz - sz) * 0.3;
            }
         }
      }
   }

   public static Vec3 position(float partialTick) {
      double cx = Mth.lerp((double)partialTick, ox, sx);
      double cy = Mth.lerp((double)partialTick, oy, sy);
      double cz = Mth.lerp((double)partialTick, oz, sz);
      float yr = yaw * (float) (Math.PI / 180.0);
      float xr = pitch * (float) (Math.PI / 180.0);
      double fx = (double)(-Mth.sin(yr) * Mth.cos(xr));
      double fy = (double)(-Mth.sin(xr));
      double fz = (double)(Mth.cos(yr) * Mth.cos(xr));
      position = new Vec3(cx - fx * distance, cy - fy * distance, cz - fz * distance);
      return position;
   }

   public static float yaw() {
      return yaw;
   }

   public static float pitch() {
      return pitch;
   }

   private CaptureRig() {
   }
}
