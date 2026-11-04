package com.devilfruits.fruits.gura_gura.client.render;

import com.devilfruits.client.render.CrackGeometry;
import com.devilfruits.client.render.VfxRenderTypes;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.fruits.gura_gura.client.vfx.QuakeDraw;
import com.devilfruits.fruits.gura_gura.client.vfx.SlashCinematic;
import com.devilfruits.fruits.gura_gura.entity.QuakeSlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Vector3f;

public class QuakeSlashRenderer extends EntityRenderer<QuakeSlashEntity> {
   private static final Int2ObjectOpenHashMap<float[]> SHAPES = new Int2ObjectOpenHashMap();
   private static final VfxContext CTX = new VfxContext();
   private static final Vector3f AX = new Vector3f();
   private static final Vector3f AY = new Vector3f();
   private static final Vector3f AZ = new Vector3f();

   public QuakeSlashRenderer(Context context) {
      super(context);
   }

   public boolean shouldRender(QuakeSlashEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return entity.distanceToSqr(camX, camY, camZ) < 16384.0;
   }

   public void render(QuakeSlashEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      if (!VfxSettings.legacy()) {
         if (SlashCinematic.of(entity) == null && Minecraft.getInstance().level != null) {
            VfxDirector.spawn(new SlashCinematic(Minecraft.getInstance().level, entity));
         }
      } else {
         if (SHAPES.size() > 64) {
            SHAPES.clear();
         }

         float[] shape = (float[])SHAPES.computeIfAbsent(entity.getId(), id -> CrackGeometry.crescent((long)id * 7919L, 2.5F, 18));
         float age = (float)entity.tickCount + partialTick;
         float life = age / (float)Math.max(1, entity.lifetime());
         float alpha = Mth.clamp(age / 1.5F, 0.0F, 1.0F) * (life < 0.7F ? 1.0F : Math.max(0.0F, 1.0F - (life - 0.7F) / 0.3F));
         if (!(alpha <= 0.01F)) {
            float radius = entity.width() * 0.55F;
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot())));
            poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
            poseStack.mulPose(Axis.ZP.rotation(entity.roll()));
            CTX.matrix = poseStack.last().pose();
            CTX.buffer = buffer.getBuffer(VfxRenderTypes.GLOW);
            CTX.time = age;
            float oz = -radius * 0.85F;
            AX.set(radius, 0.0F, 0.0F);
            AY.set(0.0F, 0.0F, radius);
            AZ.set(0.0F, radius, 0.0F);
            QuakeDraw.cracks(CTX, shape, 0.0F, 0.0F, oz, AX, AY, AZ, QuakeDraw.Orientation.PLANE, 1.0F, 0.09F, 0.42F, alpha);
            AZ.set(0.0F, 0.0F, radius);
            QuakeDraw.cracks(CTX, shape, 0.0F, 0.0F, oz, AX, AY, AZ, QuakeDraw.Orientation.PLANE, 1.0F, 0.07F, 0.3F, alpha * 0.85F);
            poseStack.popPose();
            CTX.buffer = null;
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
         }
      }
   }

   public ResourceLocation getTextureLocation(QuakeSlashEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}
