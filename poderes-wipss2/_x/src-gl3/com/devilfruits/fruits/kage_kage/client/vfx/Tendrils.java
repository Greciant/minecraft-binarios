package com.devilfruits.fruits.kage_kage.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Tendrils {
   private static final int LINKS = 16;

   public static void draw(
      boolean glow,
      PoseStack ps,
      MultiBufferSource buffers,
      Vec3 cam,
      Vec3 root,
      Vec3 bend,
      Vec3 tip,
      float reach,
      float time,
      float thick,
      int light,
      long seed
   ) {
      if (!(reach <= 0.01F)) {
         Vec3 along = tip.subtract(root);
         Vec3 side = along.cross(new Vec3(0.0, 1.0, 0.0));
         side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
         float phase = (float)(seed & 255L) / 40.0F;
         VertexConsumer vc = glow ? buffers.getBuffer(RenderType.eyes(KageLook.white())) : buffers.getBuffer(RenderType.entityCutout(KageLook.white()));
         int links = Math.max(1, Mth.ceil(16.0F * reach));

         for (int i = 0; i <= links; i++) {
            float u = (float)i / 16.0F;
            if (u > reach) {
               break;
            }

            Vec3 p = quad(root, bend, tip, u);
            float wave = Mth.sin(time * 0.45F - u * 9.0F + phase) * 0.22F * u * (1.0F - u) * 4.0F;
            p = p.add(side.scale((double)wave)).add(0.0, (double)(Mth.cos(time * 0.37F - u * 7.0F + phase) * 0.08F * u), 0.0);
            float size = Mth.lerp(u, thick, thick * 0.3F);
            boolean isTip = i == links;
            if (!glow || isTip) {
               float h = size / 2.0F;
               ps.pushPose();
               ps.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
               if (glow) {
                  float g = 0.06F * (0.7F + 0.3F * Mth.sin(time * 0.8F + phase));
                  Voxels.box(vc, ps.last(), -g, -g, -g, g, g, g, 63, -3908640, 15728880);
               } else {
                  int rgb = KageLook.mix(459788, 2495552, (float)(i * 37 % 11) / 10.0F);
                  Voxels.box(vc, ps.last(), -h, -h, -h, h, h, h, 63, 0xFF000000 | rgb, light);
               }

               ps.popPose();
            }
         }
      }
   }

   private static Vec3 quad(Vec3 a, Vec3 b, Vec3 c, float t) {
      float u = 1.0F - t;
      return a.scale((double)(u * u)).add(b.scale((double)(2.0F * u * t))).add(c.scale((double)(t * t)));
   }

   private Tendrils() {
   }
}
