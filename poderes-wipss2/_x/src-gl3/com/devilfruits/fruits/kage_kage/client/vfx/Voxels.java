package com.devilfruits.fruits.kage_kage.client.vfx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class Voxels {
   public static final int WEST = 1;
   public static final int EAST = 2;
   public static final int DOWN = 4;
   public static final int UP = 8;
   public static final int NORTH = 16;
   public static final int SOUTH = 32;
   public static final int ALL = 63;
   private static final Vector3f P = new Vector3f();
   private static final Vector3f N = new Vector3f();

   public static void box(VertexConsumer vc, Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int faces, int argb, int light) {
      Matrix4f m = pose.pose();
      Matrix3f nm = pose.normal();
      if ((faces & 1) != 0) {
         quad(vc, m, nm, argb, light, -1.0F, 0.0F, 0.0F, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
      }

      if ((faces & 2) != 0) {
         quad(vc, m, nm, argb, light, 1.0F, 0.0F, 0.0F, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
      }

      if ((faces & 4) != 0) {
         quad(vc, m, nm, argb, light, 0.0F, -1.0F, 0.0F, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
      }

      if ((faces & 8) != 0) {
         quad(vc, m, nm, argb, light, 0.0F, 1.0F, 0.0F, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
      }

      if ((faces & 16) != 0) {
         quad(vc, m, nm, argb, light, 0.0F, 0.0F, -1.0F, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
      }

      if ((faces & 32) != 0) {
         quad(vc, m, nm, argb, light, 0.0F, 0.0F, 1.0F, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
      }
   }

   public static void rim(RimConsumer rim, Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int faces, float t) {
      box(rim, pose, x0 - t, y0 - t, z0 - t, x1 + t, y1 + t, z1 + t, faces, -1, 15728880);
   }

   private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f nm, int argb, int light, float nx, float ny, float nz, float... c) {
      nm.transform(nx, ny, nz, N);
      float len = N.length();
      if (len > 1.0E-6F) {
         N.div(len);
      }

      float[] uv = new float[]{0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F};

      for (int i = 0; i < 4; i++) {
         m.transformPosition(c[i * 3], c[i * 3 + 1], c[i * 3 + 2], P);
         vc.addVertex(P.x, P.y, P.z, argb, uv[i * 2], uv[i * 2 + 1], OverlayTexture.NO_OVERLAY, light, N.x, N.y, N.z);
      }
   }

   private Voxels() {
   }
}
