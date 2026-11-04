package com.devilfruits.client.gacha;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.registry.DFItems;
import com.devilfruits.registry.DFTags;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public final class CasketItemRenderer extends BlockEntityWithoutLevelRenderer {
   public static final ModelResourceLocation BASE = ModelResourceLocation.standalone(DevilFruits.id("item/gacha/casket_base"));
   public static final ModelResourceLocation LID = ModelResourceLocation.standalone(DevilFruits.id("item/gacha/casket_lid"));
   public static final ModelResourceLocation KEY = ModelResourceLocation.standalone(DevilFruits.id("item/gacha/gold_key_3d"));
   public static final float HINGE_Y = 0.4375F;
   public static final float HINGE_Z = 0.1875F;
   private static final float PX = 0.0625F;
   static float progress;
   static int seed;
   static ItemStack offhand = ItemStack.EMPTY;
   private static CasketItemRenderer instance;

   static CasketItemRenderer get() {
      if (instance == null) {
         Minecraft mc = Minecraft.getInstance();
         instance = new CasketItemRenderer(mc);
      }

      return instance;
   }

   private CasketItemRenderer(Minecraft mc) {
      super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
   }

   public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
      float p = Mth.clamp(progress, 0.0F, 1.0F);
      float time = VfxClock.time() + (float)seed * 7.13F;
      pose.pushPose();
      float amp = (0.08F + 0.6F * p * p) * 0.0625F;
      pose.translate(amp * Mth.sin(time * 2.9F), amp * 0.6F * Mth.sin(time * 3.7F + 1.3F), amp * 0.5F * Mth.sin(time * 2.3F + 2.1F));
      pose.rotateAround(Axis.ZP.rotationDegrees(p * p * 1.6F * Mth.sin(time * 3.3F)), 0.5F, 0.4F, 0.5F);
      drawModel(BASE, stack, pose, buffers, light, overlay);
      pose.pushPose();
      float strain = Math.max(0.0F, (p - 0.66F) / 0.34F);
      pose.rotateAround(Axis.XP.rotationDegrees(-strain * strain * (2.5F + 1.5F * Mth.sin(time * 5.1F))), 0.0F, 0.4375F, 0.1875F);
      drawModel(LID, stack, pose, buffers, light, overlay);
      pose.popPose();
      glow(pose, buffers, p, time);
      if (context.firstPerson()) {
         key(stack, pose, buffers, light, overlay, p, time);
      }

      pose.popPose();
   }

   static void drawModel(ModelResourceLocation id, ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
      Minecraft mc = Minecraft.getInstance();
      BakedModel model = mc.getModelManager().getModel(id);
      VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buffers, Sheets.cutoutBlockSheet(), true, false);
      mc.getItemRenderer().renderModelLists(model, stack, light, overlay, pose, vc);
   }

   private static void key(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay, float p, float time) {
      float in = smooth(p / 0.12F);
      float turn = 90.0F * smooth((p - 0.16F) / 0.36F) + (p > 0.8F ? 4.0F * Mth.sin(time * 6.0F) : 0.0F);
      pose.pushPose();
      pose.translate(0.5F, 0.234375F, (12.5F + 3.0F * (1.0F - in)) * 0.0625F);
      pose.mulPose(Axis.ZP.rotationDegrees(turn));
      pose.mulPose(Axis.YP.rotationDegrees(90.0F));
      pose.mulPose(Axis.ZP.rotationDegrees(-45.0F));
      pose.scale(0.55F, 0.55F, 0.55F);
      pose.translate(-0.7875F, -0.7875F, -0.5F);
      if (!offhand.isEmpty() && offhand.is(DFTags.CASKET_KEYS) && !offhand.is((Item)DFItems.GOLD_KEY.get())) {
         pose.translate(0.5F, 0.5F, 0.5F);
         Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(offhand, ItemDisplayContext.NONE, light, overlay, pose, buffers, Minecraft.getInstance().level, seed);
      } else {
         drawModel(KEY, stack, pose, buffers, light, overlay);
      }

      pose.popPose();
   }

   static void glow(PoseStack pose, MultiBufferSource buffers, float p, float time) {
      if (!(p <= 0.02F)) {
         VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
         Matrix4f m = pose.last().pose();
         float seam = smooth((p - 0.1F) / 0.6F) * (0.75F + 0.25F * Mth.sin(time * 1.7F));
         int c = 16770976;
         float y0 = 0.425F;
         float y1 = 0.45F;
         quad(vc, m, -0.003125F, y0, 0.815625F, 1.003125F, y1, 0.815625F, c, 0.85F * seam, 0.85F * seam);
         quad(vc, m, -0.003125F, y0, 0.184375F, 1.003125F, y1, 0.184375F, c, 0.85F * seam, 0.85F * seam);
         quadX(vc, m, -0.003125F, y0, y1, 0.1875F, 0.8125F, c, 0.85F * seam);
         quadX(vc, m, 1.003125F, y0, y1, 0.1875F, 0.8125F, c, 0.85F * seam);
         int rays = 3 + (int)(9.0F * p);

         for (int i = 0; i < rays; i++) {
            float f = ((float)i + 0.5F) / (float)rays;
            float x = (1.0F + 14.0F * f) * 0.0625F;
            float flick = 0.5F + 0.5F * Mth.sin(time * (1.3F + 0.17F * (float)i) + (float)i * 2.4F);
            float len = (1.5F + 6.0F * p * flick) * 0.0625F;
            float spread = (f - 0.5F) * 0.9F;
            float a = seam * (0.35F + 0.4F * flick);
            ray(vc, m, x, 0.4375F, 0.815625F, spread, 0.55F, 0.85F, len, 0.015625F, c, a);
            ray(vc, m, x, 0.4375F, 0.184375F, spread, 0.55F, -0.85F, len * 0.7F, 0.015625F, c, a * 0.7F);
         }

         float k = smooth((p - 0.05F) / 0.4F) * (0.85F + 0.15F * Mth.sin(time * 2.2F));
         float z = 0.8475F;
         quad(vc, m, 0.46875F, 0.125F, z, 0.53125F, 0.3125F, z, 16774876, k, k);
         quad(vc, m, 0.4375F, 0.109375F, z + 6.25E-4F, 0.5625F, 0.328125F, z + 6.25E-4F, c, 0.45F * k, 0.45F * k);
         quad(vc, m, 0.390625F, 0.09375F, z + 0.00125F, 0.609375F, 0.34375F, z + 0.00125F, 16763210, 0.2F * k, 0.2F * k);
      }
   }

   private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, int rgb, float aBottom, float aTop) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      int ab = (int)(Mth.clamp(aBottom, 0.0F, 1.0F) * 255.0F);
      int at = (int)(Mth.clamp(aTop, 0.0F, 1.0F) * 255.0F);
      vc.addVertex(m, x0, y0, z0).setColor(r, g, b, ab);
      vc.addVertex(m, x1, y0, z1).setColor(r, g, b, ab);
      vc.addVertex(m, x1, y1, z1).setColor(r, g, b, at);
      vc.addVertex(m, x0, y1, z0).setColor(r, g, b, at);
      vc.addVertex(m, x0, y1, z0).setColor(r, g, b, at);
      vc.addVertex(m, x1, y1, z1).setColor(r, g, b, at);
      vc.addVertex(m, x1, y0, z1).setColor(r, g, b, ab);
      vc.addVertex(m, x0, y0, z0).setColor(r, g, b, ab);
   }

   private static void quadX(VertexConsumer vc, Matrix4f m, float x, float y0, float y1, float z0, float z1, int rgb, float a) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      int al = (int)(Mth.clamp(a, 0.0F, 1.0F) * 255.0F);
      vc.addVertex(m, x, y0, z0).setColor(r, g, b, al);
      vc.addVertex(m, x, y0, z1).setColor(r, g, b, al);
      vc.addVertex(m, x, y1, z1).setColor(r, g, b, al);
      vc.addVertex(m, x, y1, z0).setColor(r, g, b, al);
      vc.addVertex(m, x, y1, z0).setColor(r, g, b, al);
      vc.addVertex(m, x, y1, z1).setColor(r, g, b, al);
      vc.addVertex(m, x, y0, z1).setColor(r, g, b, al);
      vc.addVertex(m, x, y0, z0).setColor(r, g, b, al);
   }

   private static void ray(VertexConsumer vc, Matrix4f m, float x, float y, float z, float dx, float dy, float dz, float len, float half, int rgb, float a) {
      float l = Mth.sqrt(dx * dx + dy * dy + dz * dz);
      dx /= l;
      dy /= l;
      dz /= l;
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      int a0 = (int)(Mth.clamp(a, 0.0F, 1.0F) * 255.0F);
      float ex = x + dx * len;
      float ey = y + dy * len;
      float ez = z + dz * len;

      for (int k = 0; k < 2; k++) {
         float sx = k == 0 ? half : 0.0F;
         float sy = k == 0 ? 0.0F : half;
         vc.addVertex(m, x - sx, y - sy, z).setColor(r, g, b, a0);
         vc.addVertex(m, x + sx, y + sy, z).setColor(r, g, b, a0);
         vc.addVertex(m, ex + sx * 0.3F, ey + sy * 0.3F, ez).setColor(r, g, b, 0);
         vc.addVertex(m, ex - sx * 0.3F, ey - sy * 0.3F, ez).setColor(r, g, b, 0);
         vc.addVertex(m, ex - sx * 0.3F, ey - sy * 0.3F, ez).setColor(r, g, b, 0);
         vc.addVertex(m, ex + sx * 0.3F, ey + sy * 0.3F, ez).setColor(r, g, b, 0);
         vc.addVertex(m, x + sx, y + sy, z).setColor(r, g, b, a0);
         vc.addVertex(m, x - sx, y - sy, z).setColor(r, g, b, a0);
      }
   }

   static float smooth(float x) {
      float c = Mth.clamp(x, 0.0F, 1.0F);
      return c * c * (3.0F - 2.0F * c);
   }
}
