package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import org.joml.Vector3f;

final class CrystalMesh {
   private static final int SIDES = 6;
   private static final float SHOULDER = 0.72F;
   private static final float SHOULDER_RADIUS = 0.82F;

   private CrystalMesh() {
   }

   static void emit(Pose pose, VertexConsumer vc, TextureAtlasSprite body, TextureAtlasSprite tip, float length, float radius, long seed, int light) {
      emit(pose, vc, body, tip, length, radius, 0.72F, seed, light, 255);
   }

   static void emit(
      Pose pose,
      VertexConsumer vc,
      TextureAtlasSprite body,
      TextureAtlasSprite tip,
      float length,
      float radius,
      float shoulderAt,
      long seed,
      int light,
      int alpha
   ) {
      if (!(length <= 0.01F) && !(radius <= 0.01F)) {
         Vector3f[] base = new Vector3f[6];
         Vector3f[] shoulder = new Vector3f[6];
         float h = length * shoulderAt;

         for (int i = 0; i < 6; i++) {
            float a = (float)i * (float) (Math.PI * 2) / 6.0F;
            float jitter = 0.85F + 0.3F * unit(seed, i);
            float r = radius * jitter;
            base[i] = new Vector3f(Mth.cos(a) * r, 0.0F, Mth.sin(a) * r);
            shoulder[i] = new Vector3f(Mth.cos(a) * r * 0.82F, h, Mth.sin(a) * r * 0.82F);
         }

         Vector3f apex = new Vector3f((unit(seed, 7) - 0.5F) * radius * 0.4F, length, (unit(seed, 8) - 0.5F) * radius * 0.4F);
         int tiles = Math.max(1, Math.round(h / Math.max(1.0F, radius * 2.0F)));

         for (int i = 0; i < 6; i++) {
            int j = (i + 1) % 6;

            for (int t = 0; t < tiles; t++) {
               float t0 = (float)t / (float)tiles;
               float t1 = (float)(t + 1) / (float)tiles;
               Vector3f p0 = lerp(base[i], shoulder[i], t0);
               Vector3f p1 = lerp(base[i], shoulder[i], t1);
               Vector3f p2 = lerp(base[j], shoulder[j], t1);
               Vector3f p3 = lerp(base[j], shoulder[j], t0);
               quad(pose, vc, body, p0, p1, p2, p3, light, alpha);
            }
         }

         for (int i = 0; i < 6; i++) {
            int j = (i + 1) % 6;
            quad(pose, vc, tip, shoulder[i], apex, apex, shoulder[j], light, alpha);
         }

         Vector3f centre = new Vector3f();

         for (int i = 0; i < 6; i++) {
            int j = (i + 1) % 6;
            quad(pose, vc, body, centre, base[i], base[j], base[j], light, alpha);
         }
      }
   }

   static TextureAtlasSprite sprite(Block block) {
      BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(block.defaultBlockState());
      return model.getParticleIcon();
   }

   private static void quad(Pose pose, VertexConsumer vc, TextureAtlasSprite sprite, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, int light, int alpha) {
      Vector3f n = new Vector3f(p1).sub(p0).cross(new Vector3f(p3).sub(p0));
      if (n.lengthSquared() < 1.0E-8F) {
         n.set(0.0F, 1.0F, 0.0F);
      }

      n.normalize();
      Vector3f w = pose.transformNormal(new Vector3f(n), new Vector3f());
      float shade = Mth.clamp(0.64F + 0.36F * w.y + 0.12F * w.x, 0.38F, 1.0F);
      int c = (int)(255.0F * shade);
      vertex(pose, vc, p0, sprite.getU0(), sprite.getV1(), c, alpha, light, n);
      vertex(pose, vc, p1, sprite.getU0(), sprite.getV0(), c, alpha, light, n);
      vertex(pose, vc, p2, sprite.getU1(), sprite.getV0(), c, alpha, light, n);
      vertex(pose, vc, p3, sprite.getU1(), sprite.getV1(), c, alpha, light, n);
   }

   private static void vertex(Pose pose, VertexConsumer vc, Vector3f p, float u, float v, int c, int alpha, int light, Vector3f n) {
      vc.addVertex(pose, p.x, p.y, p.z).setColor(c, c, c, alpha).setUv(u, v).setLight(light).setNormal(pose, n.x, n.y, n.z);
   }

   private static Vector3f lerp(Vector3f a, Vector3f b, float t) {
      return new Vector3f(a).lerp(b, t);
   }

   private static float unit(long seed, int k) {
      long x = seed * -7046029254386353131L + (long)k * -4658895280553007687L;
      x ^= x >>> 31;
      x *= -7723592293110705685L;
      x ^= x >>> 29;
      return (float)(x >>> 40) / 1.6777216E7F;
   }
}
