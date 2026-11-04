package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class WaterCubePool {
   public static final int DEFAULT_CAP = 120;
   public static final float GRAVITY = 0.045F;
   private static final float DRAG = 0.94F;
   private static final float DRAIN = 0.012F;
   private static final double LIGHT_LIFT = 0.6;
   private static final int MAX_FALL = 120;
   private final int cap;
   private final List<WaterCubePool.Cube> cubes = new ArrayList<>();
   private int landed;
   private int landAt;
   private Consumer<Vec3> onLand;

   public WaterCubePool() {
      this(120);
   }

   public WaterCubePool(int cap) {
      this.cap = Math.max(1, cap);
   }

   public boolean isEmpty() {
      return this.cubes.isEmpty();
   }

   public int size() {
      return this.cubes.size();
   }

   public void clear() {
      this.cubes.clear();
      this.onLand = null;
   }

   public void add(ClientLevel level, Vec3 pos, Vec3 vel, float yaw, float tip, float spin, float size, int life, boolean foam, double ground) {
      if (this.cubes.size() >= this.cap) {
         this.cubes.remove(0);
      }

      WaterCubePool.Cube c = new WaterCubePool.Cube();
      c.x = c.px = pos.x;
      c.y = c.py = pos.y;
      c.z = c.pz = pos.z;
      c.vx = (float)vel.x;
      c.vy = (float)vel.y;
      c.vz = (float)vel.z;
      c.yaw = yaw;
      c.tip = c.tipO = tip;
      c.spin = spin;
      c.size = size;
      c.life = Math.max(1, life);
      c.foam = foam;
      c.ground = Double.isNaN(ground) ? pos.y - 40.0 : ground;
      c.light = WaterBlocks.lightAt(level, pos.x, Math.max(pos.y, c.ground + 0.6), pos.z);
      this.cubes.add(c);
   }

   public void addFalling(ClientLevel level, Vec3 pos, Vec3 vel, float yaw, float tip, float spin, float size, boolean foam, double ground, int settle) {
      double floor = Double.isNaN(ground) ? pos.y - 40.0 : ground;
      this.add(level, pos, vel, yaw, tip, spin, size, fallTicks(pos.y - (double)(size * 0.5F), (float)vel.y, floor) + settle, foam, ground);
   }

   public void letLand(int settle) {
      for (WaterCubePool.Cube c : this.cubes) {
         if (!c.settled) {
            int need = fallTicks(c.y - (double)(c.size * 0.5F), c.vy, c.ground) + settle;
            if (c.age + need > c.life) {
               float t = (float)c.age / (float)c.life;
               c.life = Math.round((float)need / Math.max(0.05F, 1.0F - t));
               c.age = c.life - need;
            }
         }
      }
   }

   public void onLanded(int count, Consumer<Vec3> then) {
      this.landed = 0;
      this.landAt = Math.max(1, count);
      this.onLand = then;
   }

   public static int fallTicks(double y, float vy, double floor) {
      for (int t = 1; t < 120; t++) {
         vy -= 0.045F;
         y += (double)vy;
         if (y <= floor) {
            return t;
         }
      }

      return 120;
   }

   public void tick() {
      Iterator<WaterCubePool.Cube> it = this.cubes.iterator();

      while (it.hasNext()) {
         WaterCubePool.Cube c = it.next();
         c.px = c.x;
         c.py = c.y;
         c.pz = c.z;
         c.tipO = c.tip;
         if (++c.age >= c.life) {
            it.remove();
         } else if (c.settled) {
            c.y -= 0.012F;
         } else {
            c.vy -= 0.045F;
            c.vx *= 0.94F;
            c.vz *= 0.94F;
            c.x = c.x + (double)c.vx;
            c.y = c.y + (double)c.vy;
            c.z = c.z + (double)c.vz;
            c.tip = c.tip + c.spin;
            float rest = c.size * 0.5F + 0.01F;
            if (c.y - (double)rest <= c.ground) {
               c.y = c.ground + (double)rest;
               c.settled = true;
               c.tip = (float)Math.round(c.tip / 90.0F) * 90.0F;
               if (this.onLand != null && ++this.landed >= this.landAt) {
                  Consumer<Vec3> then = this.onLand;
                  this.onLand = null;
                  then.accept(new Vec3(c.x, c.ground, c.z));
               }
            }
         }
      }
   }

   public void draw(PoseStack ps, MultiBufferSource buffers, Vec3 cam, float pt, WaterMesh.Paint water, WaterMesh.Paint foam) {
      for (WaterCubePool.Cube c : this.cubes) {
         float t = ((float)c.age + pt) / (float)c.life;
         float shrink = 1.0F - t * t;
         float size = c.size * Math.max(0.05F, shrink);
         float hs = size * 0.5F;
         if (!(hs < 0.02F)) {
            float fade = Math.min(1.0F, (1.0F - t) * 3.0F);
            ps.pushPose();
            ps.translate(Mth.lerp((double)pt, c.px, c.x) - cam.x, Mth.lerp((double)pt, c.py, c.y) - cam.y, Mth.lerp((double)pt, c.pz, c.z) - cam.z);
            ps.mulPose(Axis.YP.rotationDegrees(-c.yaw));
            ps.mulPose(Axis.XP.rotationDegrees(c.settled ? c.tip : Mth.lerp(pt, c.tipO, c.tip)));
            WaterMesh.Paint paint = water.light(c.light).alpha(water.opacity() * fade);
            WaterMesh.Paint white = c.foam ? foam.light(WaterMesh.liftLight(c.light, 10485920)).alpha(foam.opacity() * fade) : null;
            WaterMesh.submit(ps, buffers, (pose, vc) -> WaterBlocks.cube(pose, vc, paint, white, 0.0F, 0.0F, 0.0F, hs));
            ps.popPose();
         }
      }
   }

   private static final class Cube {
      double x;
      double y;
      double z;
      double px;
      double py;
      double pz;
      float vx;
      float vy;
      float vz;
      float yaw;
      float tip;
      float tipO;
      float spin;
      float size;
      int age;
      int life;
      int light;
      boolean foam;
      boolean settled;
      double ground;
   }
}
