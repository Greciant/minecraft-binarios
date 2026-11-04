package com.awakenedmod.awakened.client.voidview;

import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.FxLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;

public final class VoidViewRenderer {
   private static final int STACKS = 40;
   private static final int SLICES = 80;
   private static final int STAR_COUNT = 1400;
   private static final double STAR_INSET = 0.985;
   private static final float[] STAR_DIR = new float[4200];
   private static final float[] STAR_U = new float[4200];
   private static final float[] STAR_V = new float[4200];
   private static final float[] STAR_SIZE = new float[1400];
   private static final float[] STAR_BRIGHT = new float[1400];
   private static final float[] STAR_PHASE = new float[1400];
   private static final int[] STAR_RGB = new int[1400];
   private static final float[] UNIT;

   private VoidViewRenderer() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(event -> {
         if (event.getStage() == Stage.AFTER_ENTITIES) {
            draw(event);
         }
      });
   }

   private static void draw(RenderLevelStageEvent event) {
      if (VoidView.any()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            Vec3 cam = event.getCamera().getPosition();
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            PoseStack poseStack = event.getPoseStack();
            if (poseStack != null) {
               BufferSource buffers = mc.renderBuffers().bufferSource();
               Pose pose = poseStack.last();
               VertexConsumer vc = FxLayers.begin(buffers, FxLayer.SOLID);

               for (VoidView.Sphere s : VoidView.spheres()) {
                  shell(pose, vc, s.centre().subtract(cam), (float)s.radius());
               }

               VoidView.Sphere inside = VoidView.at(cam);
               if (inside != null && inside.stars()) {
                  stars(pose, vc, cam.subtract(inside.centre()), inside.radius(), (float)mc.level.getGameTime() + partialTick);
               }

               FxLayers.end(buffers, FxLayer.SOLID);
            }
         }
      }
   }

   static void shell(Pose pose, VertexConsumer vc, Vec3 c, float r) {
      float cx = (float)c.x;
      float cy = (float)c.y;
      float cz = (float)c.z;

      for (int i = 0; i < 40; i++) {
         for (int j = 0; j < 80; j++) {
            int a = (i * 81 + j) * 3;
            int b = a + 3;
            int d = a + 243;
            int e = d + 3;
            corner(pose, vc, a, cx, cy, cz, r);
            corner(pose, vc, b, cx, cy, cz, r);
            corner(pose, vc, e, cx, cy, cz, r);
            corner(pose, vc, a, cx, cy, cz, r);
            corner(pose, vc, e, cx, cy, cz, r);
            corner(pose, vc, d, cx, cy, cz, r);
         }
      }
   }

   static void shellQuads(Pose pose, VertexConsumer vc, float r) {
      for (int i = 0; i < 40; i++) {
         for (int j = 0; j < 80; j++) {
            int a = (i * 81 + j) * 3;
            int b = a + 3;
            int d = a + 243;
            int e = d + 3;
            quadCorner(pose, vc, a, r);
            quadCorner(pose, vc, b, r);
            quadCorner(pose, vc, e, r);
            quadCorner(pose, vc, d, r);
            quadCorner(pose, vc, d, r);
            quadCorner(pose, vc, e, r);
            quadCorner(pose, vc, b, r);
            quadCorner(pose, vc, a, r);
         }
      }
   }

   private static void quadCorner(Pose pose, VertexConsumer vc, int k, float r) {
      vc.addVertex(pose, UNIT[k] * r, UNIT[k + 1] * r, UNIT[k + 2] * r)
         .setColor(-16777216)
         .setUv(0.5F, 0.5F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, UNIT[k], UNIT[k + 1], UNIT[k + 2]);
   }

   private static void corner(Pose pose, VertexConsumer vc, int k, float cx, float cy, float cz, float r) {
      vertex(pose, vc, cx + UNIT[k] * r, cy + UNIT[k + 1] * r, cz + UNIT[k + 2] * r, -16777216);
   }

   private static void stars(Pose pose, VertexConsumer vc, Vec3 oc, double r, float time) {
      double occ = oc.lengthSqr() - r * r;

      for (int i = 0; i < 1400; i++) {
         double dx = (double)STAR_DIR[i * 3];
         double dy = (double)STAR_DIR[i * 3 + 1];
         double dz = (double)STAR_DIR[i * 3 + 2];
         double b = dx * oc.x + dy * oc.y + dz * oc.z;
         double t = (-b + Math.sqrt(b * b - occ)) * 0.985;
         float px = (float)(dx * t);
         float py = (float)(dy * t);
         float pz = (float)(dz * t);
         float h = (float)((double)STAR_SIZE[i] * t);
         float ux = STAR_U[i * 3] * h;
         float uy = STAR_U[i * 3 + 1] * h;
         float uz = STAR_U[i * 3 + 2] * h;
         float vx = STAR_V[i * 3] * h;
         float vy = STAR_V[i * 3 + 1] * h;
         float vz = STAR_V[i * 3 + 2] * h;
         float twinkle = 0.75F + 0.25F * (float)Math.sin((double)(time * 0.15F + STAR_PHASE[i]));
         float k = STAR_BRIGHT[i] * twinkle;
         int rgb = STAR_RGB[i];
         int argb = 0xFF000000 | (int)((float)(rgb >> 16 & 0xFF) * k) << 16 | (int)((float)(rgb >> 8 & 0xFF) * k) << 8 | (int)((float)(rgb & 0xFF) * k);
         quad(pose, vc, px, py, pz, ux, uy, uz, vx, vy, vz, argb);
      }
   }

   private static void quad(Pose pose, VertexConsumer vc, float px, float py, float pz, float ux, float uy, float uz, float vx, float vy, float vz, int argb) {
      vertex(pose, vc, px - ux - vx, py - uy - vy, pz - uz - vz, argb);
      vertex(pose, vc, px + ux - vx, py + uy - vy, pz + uz - vz, argb);
      vertex(pose, vc, px + ux + vx, py + uy + vy, pz + uz + vz, argb);
      vertex(pose, vc, px - ux - vx, py - uy - vy, pz - uz - vz, argb);
      vertex(pose, vc, px + ux + vx, py + uy + vy, pz + uz + vz, argb);
      vertex(pose, vc, px - ux + vx, py - uy + vy, pz - uz + vz, argb);
   }

   private static void vertex(Pose pose, VertexConsumer vc, float x, float y, float z, int argb) {
      vc.addVertex(pose, x, y, z).setColor(argb);
   }

   static {
      RandomSource r = RandomSource.create(1617977069L);

      for (int i = 0; i < 1400; i++) {
         double z = r.nextDouble() * 2.0 - 1.0;
         double a = r.nextDouble() * Math.PI * 2.0;
         double s = Math.sqrt(1.0 - z * z);
         double dx = s * Math.cos(a);
         double dz = s * Math.sin(a);
         STAR_DIR[i * 3] = (float)dx;
         STAR_DIR[i * 3 + 1] = (float)z;
         STAR_DIR[i * 3 + 2] = (float)dz;
         double hx = Math.abs(z) < 0.9 ? 0.0 : 1.0;
         double hy = Math.abs(z) < 0.9 ? 1.0 : 0.0;
         double hz = 0.0;
         double ux = hy * dz - hz * z;
         double uy = hz * dx - hx * dz;
         double uz = hx * z - hy * dx;
         double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
         ux /= ul;
         uy /= ul;
         uz /= ul;
         double vx = z * uz - dz * uy;
         double vy = dz * ux - dx * uz;
         double vz = dx * uy - z * ux;
         STAR_U[i * 3] = (float)ux;
         STAR_U[i * 3 + 1] = (float)uy;
         STAR_U[i * 3 + 2] = (float)uz;
         STAR_V[i * 3] = (float)vx;
         STAR_V[i * 3 + 1] = (float)vy;
         STAR_V[i * 3 + 2] = (float)vz;
         double big = r.nextDouble();
         STAR_SIZE[i] = (float)(0.0012 + big * big * big * 0.0045);
         STAR_BRIGHT[i] = (float)(0.35 + 0.65 * r.nextDouble());
         STAR_PHASE[i] = (float)(r.nextDouble() * Math.PI * 2.0);
         double tint = r.nextDouble();
         STAR_RGB[i] = tint < 0.6 ? 16777215 : (tint < 0.9 ? 12572927 : 9417983);
      }

      UNIT = new float[9963];

      for (int i = 0; i <= 40; i++) {
         double p = Math.PI * (double)i / 40.0 - (Math.PI / 2);

         for (int j = 0; j <= 80; j++) {
            double a = (Math.PI * 2) * (double)j / 80.0;
            int k = (i * 81 + j) * 3;
            UNIT[k] = (float)(Math.cos(p) * Math.cos(a));
            UNIT[k + 1] = (float)Math.sin(p);
            UNIT[k + 2] = (float)(Math.cos(p) * Math.sin(a));
         }
      }
   }
}
