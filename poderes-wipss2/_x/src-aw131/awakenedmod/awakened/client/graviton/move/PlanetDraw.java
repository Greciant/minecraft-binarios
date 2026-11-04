package com.awakenedmod.awakened.client.graviton.move;

import com.awakenedmod.awakened.client.render.BlockAtlas;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class PlanetDraw {
   private static final Vector3f SUN = new Vector3f(0.35F, 0.9F, 0.25F).normalize();
   static final int FULL_BRIGHT = 15728880;

   private PlanetDraw() {
   }

   static RenderType layer() {
      return BlockAtlas.layer();
   }

   static TextureAtlasSprite sprite(ResourceLocation id) {
      return BlockAtlas.sprite(id);
   }

   static TextureAtlasSprite sprite(BlockState state) {
      return Minecraft.getInstance().getBlockRenderer().getBlockModel(state).getParticleIcon();
   }

   static TextureAtlasSprite sprite(PlanetModel.Mat mat) {
      return mat.sprite() != null ? sprite(mat.sprite()) : sprite(mat.state());
   }

   static void cube(
      Pose pose, VertexConsumer vc, Quaternionf rot, float cx, float cy, float cz, float half, int faces, TextureAtlasSprite sp, int tint, int light
   ) {
      float u0 = sp.getU0();
      float u1 = sp.getU1();
      float v0 = sp.getV0();
      float v1 = sp.getV1();
      Vector3f n = new Vector3f();

      for (int f = 0; f < 6; f++) {
         if ((faces & 1 << f) != 0) {
            int dx = PlanetModel.DX[f];
            int dy = PlanetModel.DY[f];
            int dz = PlanetModel.DZ[f];
            rot.transform(n.set((float)dx, (float)dy, (float)dz));
            float lit = Mth.clamp(0.55F + 0.6F * n.dot(SUN), 0.0F, 1.0F);
            float shade = light == 15728880 ? 0.8F + 0.2F * lit : 0.5F + 0.5F * lit;
            int color = 0xFF000000 | scale(tint, shade);
            float ax;
            float ay;
            float az;
            float bx;
            float by;
            float bz;
            if (dy != 0) {
               ax = half;
               ay = 0.0F;
               az = 0.0F;
               bx = 0.0F;
               by = 0.0F;
               bz = -half * (float)dy;
            } else if (dz != 0) {
               ax = half * (float)dz;
               ay = 0.0F;
               az = 0.0F;
               bx = 0.0F;
               by = half;
               bz = 0.0F;
            } else {
               ax = 0.0F;
               ay = 0.0F;
               az = -half * (float)dx;
               bx = 0.0F;
               by = half;
               bz = 0.0F;
            }

            float fx = cx + (float)dx * half;
            float fy = cy + (float)dy * half;
            float fz = cz + (float)dz * half;
            vertex(pose, vc, fx - ax - bx, fy - ay - by, fz - az - bz, u0, v1, color, light, dx, dy, dz);
            vertex(pose, vc, fx + ax - bx, fy + ay - by, fz + az - bz, u1, v1, color, light, dx, dy, dz);
            vertex(pose, vc, fx + ax + bx, fy + ay + by, fz + az + bz, u1, v0, color, light, dx, dy, dz);
            vertex(pose, vc, fx - ax + bx, fy - ay + by, fz - az + bz, u0, v0, color, light, dx, dy, dz);
         }
      }
   }

   private static void vertex(Pose pose, VertexConsumer vc, float x, float y, float z, float u, float v, int color, int light, int nx, int ny, int nz) {
      vc.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setLight(light).setNormal(pose, (float)nx, (float)ny, (float)nz);
   }

   private static int scale(int rgb, float k) {
      int r = Math.min(255, Math.round((float)(rgb >> 16 & 0xFF) * k));
      int g = Math.min(255, Math.round((float)(rgb >> 8 & 0xFF) * k));
      int b = Math.min(255, Math.round((float)(rgb & 0xFF) * k));
      return r << 16 | g << 8 | b;
   }
}
