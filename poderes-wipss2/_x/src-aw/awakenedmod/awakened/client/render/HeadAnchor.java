package com.awakenedmod.awakened.client.render;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class HeadAnchor {
   public static final float EYE_UP = 3.5F;
   public static final float EYE_SIDE = 2.0F;
   public static final float FACE = 4.0F;
   private static final float PLAYER_SCALE = 0.9375F;
   private static final double SANITY = 4.0;
   private static final Int2ObjectMap<HeadAnchor.Sample> SAMPLES = new Int2ObjectOpenHashMap();
   private static ClientLevel level;

   private HeadAnchor() {
   }

   public static HeadAnchor.Head of(LivingEntity e, float partialTick) {
      HeadAnchor.Sample s = (HeadAnchor.Sample)SAMPLES.get(e.getId());
      if (s != null && s.gameTime == gameTime() && s.framePartial == framePartial()) {
         HeadAnchor.Head h = observed(s);
         double range = 4.0 * Math.max(0.25, (double)e.getScale());
         if (h.pivot().distanceToSqr(e.getPosition(partialTick)) < range * range) {
            return h;
         }
      }

      return analytic(e, partialTick);
   }

   static void sample(int id, Matrix4f headPose) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != level) {
         SAMPLES.clear();
         level = mc.level;
      }

      if (mc.level != null) {
         HeadAnchor.Sample s = (HeadAnchor.Sample)SAMPLES.get(id);
         if (s == null) {
            s = new HeadAnchor.Sample();
            SAMPLES.put(id, s);
         }

         s.pose.set(headPose);
         Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
         s.camX = cam.x;
         s.camY = cam.y;
         s.camZ = cam.z;
         s.gameTime = gameTime();
         s.framePartial = framePartial();
      }
   }

   private static HeadAnchor.Head observed(HeadAnchor.Sample s) {
      Vector3f o = s.pose.transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());
      Vector3f x = s.pose.transformDirection(1.0F, 0.0F, 0.0F, new Vector3f());
      Vector3f y = s.pose.transformDirection(0.0F, -1.0F, 0.0F, new Vector3f());
      Vector3f z = s.pose.transformDirection(0.0F, 0.0F, -1.0F, new Vector3f());
      float px = x.length() / 16.0F;
      return new HeadAnchor.Head(
         new Vec3(s.camX + (double)o.x, s.camY + (double)o.y, s.camZ + (double)o.z), x.negate().normalize(), y.normalize(), z.normalize(), px
      );
   }

   private static HeadAnchor.Head analytic(LivingEntity e, float pt) {
      float px = e instanceof Player ? 0.9375F * e.getScale() / 16.0F : Mth.clamp(e.getBbWidth() * 0.42F, 0.12F, 0.5F) / 4.0F;
      float yaw = Mth.rotLerp(pt, e.yHeadRotO, e.yHeadRot) * (float) (Math.PI / 180.0);
      float pitch = Mth.lerp(pt, e.xRotO, e.getXRot()) * (float) (Math.PI / 180.0);
      float cp = Mth.cos(pitch);
      Vector3f forward = new Vector3f(-Mth.sin(yaw) * cp, -Mth.sin(pitch), Mth.cos(yaw) * cp);
      Vector3f right = new Vector3f(-Mth.cos(yaw), 0.0F, -Mth.sin(yaw));
      Vector3f up = new Vector3f(right).cross(forward).normalize();
      Vec3 pivot = e.getPosition(pt).add(0.0, (double)(e.getEyeHeight() - 3.5F * px), 0.0);
      return new HeadAnchor.Head(pivot, right, up, forward, px);
   }

   private static long gameTime() {
      Minecraft mc = Minecraft.getInstance();
      return mc.level == null ? 0L : mc.level.getGameTime();
   }

   private static float framePartial() {
      return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
   }

   public static record Head(Vec3 pivot, Vector3f right, Vector3f up, Vector3f forward, float px) {
      public Vec3 at(float x, float y, float z) {
         float sx = x * this.px;
         float sy = y * this.px;
         float sz = z * this.px;
         return this.pivot
            .add(
               (double)(this.right.x * sx + this.up.x * sy + this.forward.x * sz),
               (double)(this.right.y * sx + this.up.y * sy + this.forward.y * sz),
               (double)(this.right.z * sx + this.up.z * sy + this.forward.z * sz)
            );
      }

      public Vec3 eye(int side, float front) {
         return this.at((float)side * 2.0F, 3.5F, front);
      }

      public static HeadAnchor.Head level(Vec3 eyes, Vec3 facing) {
         float px = 0.05859375F;
         Vector3f f = new Vector3f((float)facing.x, 0.0F, (float)facing.z);
         if (f.lengthSquared() < 1.0E-6F) {
            f.set(0.0F, 0.0F, 1.0F);
         }

         f.normalize();
         Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
         return new HeadAnchor.Head(eyes.add(0.0, (double)(-3.5F * px), 0.0), new Vector3f(f).cross(up), up, f, px);
      }
   }

   private static final class Sample {
      final Matrix4f pose = new Matrix4f();
      double camX;
      double camY;
      double camZ;
      long gameTime;
      float framePartial;
   }
}
