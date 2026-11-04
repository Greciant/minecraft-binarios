package com.devilfruits.fruits.gura_gura.client.render;

import com.devilfruits.client.render.VfxRenderTypes;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.fruits.gura_gura.client.vfx.WaveCinematic;
import com.devilfruits.fruits.gura_gura.entity.QuakeWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

public class QuakeWaveRenderer extends EntityRenderer<QuakeWaveEntity> {
   private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
   private static final int COLUMNS = 20;
   private static final int FRONT_ROWS = 10;
   private static final int BACK_ROWS = 6;
   private static final float BACK_SLOPE = 1.6F;

   public QuakeWaveRenderer(Context context) {
      super(context);
   }

   public boolean shouldRender(QuakeWaveEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return !VfxSettings.legacy()
         ? entity.isWater() && entity.distanceToSqr(camX, camY, camZ) < 25600.0
         : entity.isWater() && frustum.isVisible(entity.getBoundingBoxForCulling());
   }

   public void render(QuakeWaveEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      if (!VfxSettings.legacy()) {
         WaveCinematic.ensure(entity);
      } else {
         float envelope = entity.envelope(partialTick);
         if (entity.isWater() && !(envelope <= 0.01F)) {
            TextureAtlasSprite sprite = (TextureAtlasSprite)Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WATER_STILL);
            float time = (float)entity.tickCount + partialTick;
            float width = entity.width();
            float height = entity.height() * envelope;
            int light = Math.max(packedLight, 11534512);
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
            Matrix4f m = poseStack.last().pose();
            VertexConsumer vc = buffer.getBuffer(VfxRenderTypes.WATER);

            for (int c = 0; c < 20; c++) {
               float s0 = (float)c / 20.0F;
               float s1 = (float)(c + 1) / 20.0F;

               for (int r = 0; r < 10; r++) {
                  float h0 = (float)r / 10.0F;
                  float h1 = (float)(r + 1) / 10.0F;
                  front(vc, m, sprite, s0, h0, 0.0F, 1.0F, width, height, time, light);
                  front(vc, m, sprite, s1, h0, 1.0F, 1.0F, width, height, time, light);
                  front(vc, m, sprite, s1, h1, 1.0F, 0.0F, width, height, time, light);
                  front(vc, m, sprite, s0, h1, 0.0F, 0.0F, width, height, time, light);
               }

               for (int r = 0; r < 6; r++) {
                  float b0 = (float)r / 6.0F;
                  float b1 = (float)(r + 1) / 6.0F;
                  back(vc, m, sprite, s0, b0, 0.0F, 0.0F, width, height, time, light);
                  back(vc, m, sprite, s1, b0, 1.0F, 0.0F, width, height, time, light);
                  back(vc, m, sprite, s1, b1, 1.0F, 1.0F, width, height, time, light);
                  back(vc, m, sprite, s0, b1, 0.0F, 1.0F, width, height, time, light);
               }

               foam(vc, m, sprite, s0, s1, width, height, time, light);
            }

            poseStack.popPose();
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
         }
      }
   }

   private static float columnHeight(float s, float height, float time) {
      float across = s * 2.0F - 1.0F;
      float a2 = across * across;
      float profile = 1.0F - 0.85F * a2 * a2 * a2;
      float ripple = 1.0F + 0.06F * Mth.sin(time * 0.31F + s * 11.0F) + 0.03F * Mth.sin(time * 0.57F + s * 23.0F);
      return height * profile * ripple;
   }

   private static float frontZ(float h, float columnHeight) {
      float lean = (float)Math.pow((double)h, 2.4) * columnHeight * 0.45F;
      float curl = h > 0.75F ? (float)Math.pow((double)(h - 0.75F), 1.5) * columnHeight * 1.6F : 0.0F;
      return lean + curl;
   }

   private static void front(
      VertexConsumer vc, Matrix4f m, TextureAtlasSprite sprite, float s, float h, float su, float sv, float width, float height, float time, int light
   ) {
      float hc = columnHeight(s, height, time);
      float x = (s * 2.0F - 1.0F) * width * 0.5F;
      float y = h * hc - (h > 0.9F ? (h - 0.9F) * hc * 0.6F : 0.0F);
      float z = frontZ(h, hc);
      float foam = Mth.clamp((h - 0.86F) / 0.14F, 0.0F, 1.0F);
      int red = (int)Mth.lerp(foam, Mth.lerp(h, 22.0F, 64.0F), 236.0F);
      int green = (int)Mth.lerp(foam, Mth.lerp(h, 74.0F, 150.0F), 246.0F);
      int blue = (int)Mth.lerp(foam, Mth.lerp(h, 176.0F, 238.0F), 255.0F);
      int alpha = (int)Mth.lerp(h, 222.0F, 242.0F);
      vc.addVertex(m, x, y, z).setColor(red, green, blue, alpha).setUv(sprite.getU(su), sprite.getV(sv)).setLight(light).setNormal(0.0F, 0.6F, 0.8F);
   }

   private static void back(
      VertexConsumer vc, Matrix4f m, TextureAtlasSprite sprite, float s, float b, float su, float sv, float width, float height, float time, int light
   ) {
      float hc = columnHeight(s, height, time);
      float x = (s * 2.0F - 1.0F) * width * 0.5F;
      float crestZ = frontZ(0.8F, hc);
      float y = hc * 0.82F * (1.0F - b * b * (3.0F - 2.0F * b));
      float z = crestZ - b * hc * 1.6F;
      int red = (int)Mth.lerp(b, 52.0F, 20.0F);
      int green = (int)Mth.lerp(b, 120.0F, 66.0F);
      int blue = (int)Mth.lerp(b, 222.0F, 168.0F);
      vc.addVertex(m, x, y, z).setColor(red, green, blue, 232).setUv(sprite.getU(su), sprite.getV(sv)).setLight(light).setNormal(0.0F, 0.8F, -0.6F);
   }

   private static void foam(VertexConsumer vc, Matrix4f m, TextureAtlasSprite sprite, float s0, float s1, float width, float height, float time, int light) {
      float h0 = columnHeight(s0, height, time);
      float h1 = columnHeight(s1, height, time);
      float x0 = (s0 * 2.0F - 1.0F) * width * 0.5F;
      float x1 = (s1 * 2.0F - 1.0F) * width * 0.5F;
      float yTop0 = h0 * 0.86F + 0.25F + 0.1F * Mth.sin(time * 0.9F + s0 * 31.0F);
      float yTop1 = h1 * 0.86F + 0.25F + 0.1F * Mth.sin(time * 0.9F + s1 * 31.0F);
      float z0 = frontZ(0.82F, h0);
      float z1 = frontZ(0.82F, h1);
      vc.addVertex(m, x0, h0 * 0.8F, z0 - 0.4F)
         .setColor(240, 248, 255, 235)
         .setUv(sprite.getU(0.0F), sprite.getV(1.0F))
         .setLight(light)
         .setNormal(0.0F, 1.0F, 0.0F);
      vc.addVertex(m, x1, h1 * 0.8F, z1 - 0.4F)
         .setColor(240, 248, 255, 235)
         .setUv(sprite.getU(1.0F), sprite.getV(1.0F))
         .setLight(light)
         .setNormal(0.0F, 1.0F, 0.0F);
      vc.addVertex(m, x1, yTop1, z1 + 0.2F)
         .setColor(255, 255, 255, 200)
         .setUv(sprite.getU(1.0F), sprite.getV(0.0F))
         .setLight(light)
         .setNormal(0.0F, 1.0F, 0.0F);
      vc.addVertex(m, x0, yTop0, z0 + 0.2F)
         .setColor(255, 255, 255, 200)
         .setUv(sprite.getU(0.0F), sprite.getV(0.0F))
         .setLight(light)
         .setNormal(0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(QuakeWaveEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}
