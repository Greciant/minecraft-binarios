package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.cryokinetic.GlacierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class GlacierRenderer extends EntityRenderer<GlacierEntity> {
   private final BlockRenderDispatcher blockRenderer;
   private static final float GROW_TICKS = 3.0F;
   private static final float SHARD_GROW_TICKS = 4.0F;

   public GlacierRenderer(Context ctx) {
      super(ctx);
      this.blockRenderer = ctx.getBlockRenderDispatcher();
   }

   public ResourceLocation getTextureLocation(GlacierEntity entity) {
      return null;
   }

   public boolean shouldRender(GlacierEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }

   public void render(GlacierEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
      draw(
         entity,
         poseStack,
         (float)entity.tickCount + partialTick,
         entity.lifetime(),
         lights(entity),
         shardLights(entity),
         (pose, block, light) -> this.blockRenderer.renderSingleBlock(block.defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY),
         (pose, writer) -> writer.write(pose.last(), buffers.getBuffer(RenderType.translucentMovingBlock()))
      );
   }

   private static int[] lights(GlacierEntity g) {
      int[] visible = g.visible();
      int[] out = new int[visible.length];
      MutableBlockPos pos = new MutableBlockPos();
      BlockPos base = g.blockPosition();

      for (int j = 0; j < visible.length; j++) {
         int i = visible[j];
         pos.set(base.getX() + g.bx(i), base.getY() + g.by(i), base.getZ() + g.bz(i));
         out[j] = LevelRenderer.getLightColor(g.level(), pos);
      }

      return out;
   }

   private static int[] shardLights(GlacierEntity g) {
      int[] out = new int[g.shardCount()];
      MutableBlockPos pos = new MutableBlockPos();
      BlockPos base = g.blockPosition();

      for (int i = 0; i < out.length; i++) {
         float[] s = g.shard(i);
         float half = s[6] * 0.5F;
         pos.set(base.getX() + Mth.floor(s[0] + s[3] * half), base.getY() + Mth.floor(s[1] + s[4] * half), base.getZ() + Mth.floor(s[2] + s[5] * half));
         out[i] = LevelRenderer.getLightColor(g.level(), pos);
      }

      return out;
   }

   private static void draw(
      GlacierEntity g,
      PoseStack poseStack,
      float age,
      int lifetime,
      int[] light,
      int[] shardLight,
      GlacierRenderer.BlockSubmit submit,
      GlacierRenderer.GeometrySubmit geometry
   ) {
      float melt = Mth.clamp(((float)lifetime - age) / 12.0F, 0.0F, 1.0F);
      if (!(melt <= 0.0F)) {
         int[] visible = g.visible();

         for (int j = 0; j < visible.length; j++) {
            int i = visible[j];
            float grow = Mth.clamp((age - (float)g.delay(i)) / 3.0F, 0.0F, 1.0F);
            if (!(grow <= 0.0F)) {
               float k = (1.0F - (1.0F - grow) * (1.0F - grow)) * melt;
               poseStack.pushPose();
               poseStack.translate((double)g.bx(i) + 0.5, (double)g.by(i), (double)g.bz(i) + 0.5);
               poseStack.scale(k, k * (g.half(i) ? 0.5F : 1.0F), k);
               poseStack.translate(-0.5, 0.0, -0.5);
               submit.submit(poseStack, g.block(i), light[j]);
               poseStack.popPose();
            }
         }

         for (int i = 0; i < g.shardCount(); i++) {
            float grow = Mth.clamp((age - (float)g.shardDelay(i)) / 4.0F, 0.0F, 1.0F);
            if (grow > 0.0F) {
               drawShard(poseStack, g.shard(i), g.shardClear(i), i, g.getId(), (1.0F - (1.0F - grow) * (1.0F - grow)) * melt, geometry, shardLight[i]);
            }
         }
      }
   }

   private static void drawShard(
      PoseStack poseStack, float[] s, boolean clear, int index, int entityId, float k, GlacierRenderer.GeometrySubmit geometry, int light
   ) {
      float length = s[6] * k;
      float radius = s[7] * Math.min(1.0F, k * 1.6F);
      TextureAtlasSprite body = CrystalMesh.sprite(clear ? Blocks.ICE : (index % 3 == 0 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE));
      TextureAtlasSprite tip = CrystalMesh.sprite(Blocks.ICE);
      long seed = (long)entityId * 31L + (long)index;
      poseStack.pushPose();
      poseStack.translate(s[0], s[1], s[2]);
      poseStack.mulPose(new Quaternionf().rotateTo(new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f(s[3], s[4], s[5])));
      poseStack.mulPose(new Quaternionf().rotateY(s[8] * (float) (Math.PI / 180.0)));
      geometry.submit(poseStack, (pose, vc) -> CrystalMesh.emit(pose, vc, body, tip, length, radius, seed, light));
      poseStack.popPose();
   }

   @FunctionalInterface
   private interface BlockSubmit {
      void submit(PoseStack var1, Block var2, int var3);
   }

   @FunctionalInterface
   private interface GeometrySubmit {
      void submit(PoseStack var1, GlacierRenderer.GeometryWriter var2);
   }

   @FunctionalInterface
   private interface GeometryWriter {
      void write(Pose var1, VertexConsumer var2);
   }
}
