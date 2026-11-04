package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.ruin.RuinHand;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

final class RuinHandCrust {
   private static final Lab.Value REACH = Lab.CLIENT.def("ruin.hand.reach", 0.62, 0.2, 1.0, 0.02);
   private static final Lab.Value REACH_MIN = Lab.CLIENT.def("ruin.hand.reach_min", 0.3, 0.1, 1.0, 0.02);
   private static final Lab.Value TINT = Lab.CLIENT.def("ruin.hand.tint", 170.0, 0.0, 255.0, 1.0);
   private static final Lab.Value CRACK_LIGHT = Lab.CLIENT.def("ruin.hand.crack_light", 90.0, 0.0, 255.0, 1.0);
   private static final int ASH = 4604479;
   private static final float INFLATE_PX = 0.3F;
   private static final float CRACK_LIFT_PX = 0.04F;
   private static final float MIN_VISIBLE = 0.01F;

   private RuinHandCrust() {
   }

   static void preloadLabKeys() {
   }

   static void draw(PoseStack ps, VfxDraw.Sink sink, ModelPart part, RuinHand hand, int light) {
      float amount = hand.amount();
      if (!(amount < 0.01F)) {
         float[][] boxes = LimbGlowShells.boxes(part);
         if (boxes.length != 0) {
            float reach = Mth.lerp(amount, Math.min(REACH_MIN.getF(), REACH.getF()), REACH.getF());
            int tintA = Mth.clamp(Math.round(TINT.getF() * (0.35F + 0.65F * amount)), 0, 255);
            int crackRgb = grey(CRACK_LIGHT.getI());
            RenderType cracks = crackType(hand.crackStage());
            float d = 0.01875F;
            float dc = 0.02125F;
            VfxDraw.translucent(ps, sink, (pose, vc) -> {
               for (float[] b : boxes) {
                  tint(pose, vc, b, reach, d, tintA);
               }
            });
            VfxDraw.vanillaLayer(ps, sink, cracks, (pose, vc) -> {
               for (float[] b : boxes) {
                  cracks(pose, vc, b, reach, dc, crackRgb, light);
               }
            });
         }
      }
   }

   private static RenderType crackType(int stage) {
      ResourceLocation texture = (ResourceLocation)ModelBakery.BREAKING_LOCATIONS.get(Mth.clamp(stage, 0, ModelBakery.BREAKING_LOCATIONS.size() - 1));
      return RenderType.entityTranslucent(texture);
   }

   private static int grey(int v) {
      int c = Mth.clamp(v, 0, 255);
      return c << 16 | c << 8 | c;
   }

   private static void tint(Pose pose, VertexConsumer vc, float[] b, float reach, float d, int alpha) {
      float x0 = b[0] / 16.0F - d;
      float z0 = b[2] / 16.0F - d;
      float x1 = b[3] / 16.0F + d;
      float z1 = b[5] / 16.0F + d;
      float y1 = b[4] / 16.0F + d;
      float yt = Mth.lerp(1.0F - reach, b[1] / 16.0F, b[4] / 16.0F);
      int top = VfxColor.argb(0, 4604479);
      int low = VfxColor.argb(alpha, 4604479);
      side(pose, vc, x0, z0, x1, z0, yt, y1, top, low);
      side(pose, vc, x1, z0, x1, z1, yt, y1, top, low);
      side(pose, vc, x1, z1, x0, z1, yt, y1, top, low);
      side(pose, vc, x0, z1, x0, z0, yt, y1, top, low);
      tri(pose, vc, x0, y1, z0, low, x1, y1, z0, low, x1, y1, z1, low);
      tri(pose, vc, x0, y1, z0, low, x1, y1, z1, low, x0, y1, z1, low);
   }

   private static void side(Pose pose, VertexConsumer vc, float ax, float az, float bx, float bz, float yt, float yb, int top, int low) {
      tri(pose, vc, ax, yt, az, top, bx, yt, bz, top, bx, yb, bz, low);
      tri(pose, vc, ax, yt, az, top, bx, yb, bz, low, ax, yb, az, low);
   }

   private static void tri(
      Pose pose, VertexConsumer vc, float ax, float ay, float az, int ca, float bx, float by, float bz, int cb, float cx, float cy, float cz, int cc
   ) {
      vc.addVertex(pose, ax, ay, az).setColor(ca);
      vc.addVertex(pose, bx, by, bz).setColor(cb);
      vc.addVertex(pose, cx, cy, cz).setColor(cc);
   }

   private static void cracks(Pose pose, VertexConsumer vc, float[] b, float reach, float d, int rgb, int light) {
      float x0 = b[0] / 16.0F - d;
      float z0 = b[2] / 16.0F - d;
      float x1 = b[3] / 16.0F + d;
      float z1 = b[5] / 16.0F + d;
      float y1 = b[4] / 16.0F + d;
      float yt = Mth.lerp(1.0F - reach, b[1] / 16.0F, b[4] / 16.0F);
      float h = y1 - yt;
      float wx = x1 - x0;
      float wz = z1 - z0;
      int top = VfxColor.argb(40, rgb);
      int low = VfxColor.argb(255, rgb);
      crackFace(pose, vc, x0, z0, x1, z0, yt, y1, 0.0F, 0.0F, wx, h, 0.0F, 0.0F, -1.0F, top, low, light);
      crackFace(pose, vc, x1, z0, x1, z1, yt, y1, 0.25F, 0.1F, wz, h, 1.0F, 0.0F, 0.0F, top, low, light);
      crackFace(pose, vc, x1, z1, x0, z1, yt, y1, 0.5F, 0.2F, wx, h, 0.0F, 0.0F, 1.0F, top, low, light);
      crackFace(pose, vc, x0, z1, x0, z0, yt, y1, 0.7F, 0.05F, wz, h, -1.0F, 0.0F, 0.0F, top, low, light);
      float u0 = 0.3F;
      float v0 = 0.6F;
      vertex(pose, vc, x0, y1, z0, u0, v0, low, light, 0.0F, 1.0F, 0.0F);
      vertex(pose, vc, x1, y1, z0, u0 + wx, v0, low, light, 0.0F, 1.0F, 0.0F);
      vertex(pose, vc, x1, y1, z1, u0 + wx, v0 + wz, low, light, 0.0F, 1.0F, 0.0F);
      vertex(pose, vc, x0, y1, z1, u0, v0 + wz, low, light, 0.0F, 1.0F, 0.0F);
   }

   private static void crackFace(
      Pose pose,
      VertexConsumer vc,
      float ax,
      float az,
      float bx,
      float bz,
      float yt,
      float yb,
      float u0,
      float v0,
      float w,
      float h,
      float nx,
      float ny,
      float nz,
      int top,
      int low,
      int light
   ) {
      float u1 = Math.min(1.0F, u0 + w);
      float v1 = Math.min(1.0F, v0 + h);
      vertex(pose, vc, ax, yt, az, u0, v0, top, light, nx, ny, nz);
      vertex(pose, vc, bx, yt, bz, u1, v0, top, light, nx, ny, nz);
      vertex(pose, vc, bx, yb, bz, u1, v1, low, light, nx, ny, nz);
      vertex(pose, vc, ax, yb, az, u0, v1, low, light, nx, ny, nz);
   }

   private static void vertex(Pose pose, VertexConsumer vc, float x, float y, float z, float u, float v, int argb, int light, float nx, float ny, float nz) {
      vc.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
   }
}
