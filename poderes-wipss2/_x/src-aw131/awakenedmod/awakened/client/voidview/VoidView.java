package com.awakenedmod.awakened.client.voidview;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public final class VoidView {
   private static final Map<Integer, VoidView.Sphere> SPHERES = new HashMap<>();
   private static final double WALL_GAP = 0.4;

   private VoidView() {
   }

   public static void open(int key, Vec3 centre, double radius, boolean stars) {
      SPHERES.put(key, new VoidView.Sphere(centre, radius, stars));
   }

   public static void open(int key, Vec3 centre, double radius, boolean stars, double wall) {
      SPHERES.put(key, new VoidView.Sphere(centre, radius, stars, Math.max(radius, wall)));
   }

   public static void close(int key) {
      SPHERES.remove(key);
   }

   public static void clear() {
      SPHERES.clear();
   }

   public static boolean any() {
      return !SPHERES.isEmpty();
   }

   public static Collection<VoidView.Sphere> spheres() {
      return Collections.unmodifiableCollection(SPHERES.values());
   }

   public static VoidView.Sphere at(Vec3 p) {
      if (SPHERES.isEmpty()) {
         return null;
      } else {
         for (VoidView.Sphere s : SPHERES.values()) {
            if (s.contains(p)) {
               return s;
            }
         }

         return null;
      }
   }

   public static boolean contains(double x, double y, double z) {
      if (SPHERES.isEmpty()) {
         return false;
      } else {
         for (VoidView.Sphere s : SPHERES.values()) {
            if (s.contains(x, y, z)) {
               return true;
            }
         }

         return false;
      }
   }

   public static VoidView.Sphere cameraSphere() {
      if (SPHERES.isEmpty()) {
         return null;
      } else {
         return Minecraft.getInstance().level == null ? null : at(cameraPos());
      }
   }

   public static Vec3 cameraPos() {
      return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
   }

   public static float reachInside(VoidView.Sphere s, Vec3 from, Vector3fc forward, float max) {
      double dx = (double)(-forward.x());
      double dy = (double)(-forward.y());
      double dz = (double)(-forward.z());
      double ox = from.x - s.centre().x;
      double oy = from.y - s.centre().y;
      double oz = from.z - s.centre().z;
      double b = ox * dx + oy * dy + oz * dz;
      double c = ox * ox + oy * oy + oz * oz - s.wall() * s.wall();
      double disc = b * b - c;
      if (disc < 0.0) {
         return Math.min(max, 0.1F);
      } else {
         double t = -b + Math.sqrt(disc) - 0.4;
         return (float)Math.max(0.1, Math.min((double)max, t));
      }
   }

   public static boolean separated(Vec3 cam, double x, double y, double z) {
      if (SPHERES.isEmpty()) {
         return false;
      } else {
         VoidView.Sphere a = at(cam);

         for (VoidView.Sphere s : SPHERES.values()) {
            if (s.contains(x, y, z)) {
               return s != a;
            }
         }

         return a != null;
      }
   }

   public static boolean hidesWorld() {
      return cameraSphere() != null;
   }

   public static record Sphere(Vec3 centre, double radius, boolean stars, double wall) {
      public Sphere(Vec3 centre, double radius, boolean stars) {
         this(centre, radius, stars, radius);
      }

      public boolean contains(Vec3 p) {
         return p.distanceToSqr(this.centre) < this.radius * this.radius;
      }

      public boolean contains(double x, double y, double z) {
         double dx = x - this.centre.x;
         double dy = y - this.centre.y;
         double dz = z - this.centre.z;
         return dx * dx + dy * dy + dz * dz < this.radius * this.radius;
      }
   }
}
