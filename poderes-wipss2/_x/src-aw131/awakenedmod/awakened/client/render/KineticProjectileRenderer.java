package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public final class KineticProjectileRenderer extends ThrownItemRenderer<KineticProjectile> {
   private static final float DISC_R_OUTER = 0.34F;
   private static final float DISC_R_INNER = 0.22F;
   private static final float DISC_ALPHA = 48.0F;
   private static final int CUBES = 4;
   private static final float ORBIT_R = 0.27F;
   private static final float CUBE_HALF = 0.045F;
   private static final float SPIN = 0.38F;
   private static final float CUBE_ALPHA = 150.0F;
   private static final float CENTRE_Y = 0.125F;

   public KineticProjectileRenderer(Context context) {
      super(context, 1.0F, true);
   }

   public void render(KineticProjectile entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
      if (entity.visualStyle() == 1) {
         drawCharge(poseStack, new VfxDraw.Sink(buffers), entity.getId(), (float)entity.tickCount + partialTick);
      }
   }

   private static void drawCharge(PoseStack ps, VfxDraw.Sink sink, int seed, float age) {
      VoxelFlameDraw.Palette ladders = KineticComposites.ladders(8072383, 13073919);
      int[] tiers = ladders.base();
      Vector3f[] cam = VoxelFlameDraw.cameraAxes();
      Vector3f right = cam[0];
      Vector3f up = cam[1];
      float breathe = VoxelFlameDraw.breathe(age, seed & 0xFF, 1.6F);
      int aDisc = VfxDraw.ia(48.0F * breathe, 1.0F);
      int aCube = VfxDraw.ia(150.0F, 1.0F);
      ps.pushPose();
      ps.translate(0.0F, 0.125F, 0.0F);
      VfxDraw.flame(ps, sink, (pose, vc) -> {
         if (aDisc > 1) {
            VoxelFlameDraw.disc(pose, vc, 0.0F, 0.0F, 0.0F, right, up, 0.34F * breathe, aDisc, VoxelFlameDraw.gradient(tiers, 0.35F));
            VoxelFlameDraw.disc(pose, vc, 0.0F, 0.0F, 0.0F, right, up, 0.22F * breathe, aDisc, VoxelFlameDraw.gradient(tiers, 0.8F));
         }

         for (int c = 0; c < 4; c++) {
            float phase = VoxelFlameDraw.noise01(seed, c, 1) * 6.2832F + age * 0.38F * (c % 2 == 0 ? 1.0F : -1.0F);
            float tilt = (VoxelFlameDraw.noise01(seed, c, 2) - 0.5F) * 1.6F;
            float x = Mth.cos(phase) * 0.27F;
            float y = Mth.sin(phase) * 0.27F * Mth.sin(tilt);
            float z = Mth.sin(phase) * 0.27F * Mth.cos(tilt);
            float ang = VoxelFlameDraw.noise01(seed, c, 3) * 6.2832F + age * 0.3F;
            VoxelFlameDraw.cube(pose, vc, x, y, z, 0.045F, c % 3, ang, aCube, ladders.pick(seed + c)[3 - (c & 1)]);
         }
      });
      ps.popPose();
   }
}
