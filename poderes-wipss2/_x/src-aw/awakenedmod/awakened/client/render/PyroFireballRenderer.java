package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.PyroSourceAnchors;
import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.content.pyrokinetic.FlameTone;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class PyroFireballRenderer extends EntityRenderer<KineticProjectile> {
   private static final float SCALE = 0.35F;
   private static final float CUBE_ALPHA = 140.0F;
   private static final float DISC_ALPHA = 45.0F;
   private static final float BLINK = 0.035F;
   private static final float POP_AT = 0.95F;
   private static final int DISCS_MIN = 2;
   private static final float BRIDGE_TICKS = 3.0F;

   public PyroFireballRenderer(Context ctx) {
      super(ctx);
   }

   public ResourceLocation getTextureLocation(KineticProjectile entity) {
      return null;
   }

   public boolean shouldRender(KineticProjectile entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }

   public void render(KineticProjectile entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
      Vec3 bridge = bridgeOffset(entity, partialTick);
      draw(
         poseStack,
         new VfxDraw.Sink(buffers),
         entity.visualStyle(),
         entity.getId(),
         (float)entity.tickCount + partialTick,
         (float)bridge.x,
         (float)bridge.y,
         (float)bridge.z
      );
   }

   private static Vec3 bridgeOffset(KineticProjectile entity, float partialTick) {
      float age = (float)entity.tickCount + partialTick;
      if (age >= 3.0F) {
         return Vec3.ZERO;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && entity.getOwner() == mc.player && mc.options.getCameraType().isFirstPerson()) {
            boolean leftArm = (entity.visualStyle() & 16) != 0;
            Vec3 anchor = PyroSourceAnchors.fireballFpAnchor(mc.player, partialTick, leftArm);
            Vec3 toAnchor = anchor.subtract(entity.getPosition(partialTick));
            return toAnchor.scale((double)(1.0F - age / 3.0F));
         } else {
            return Vec3.ZERO;
         }
      }
   }

   private static void draw(PoseStack ps, VfxDraw.Sink sink, int styleByte, int seed, float age, float bridgeX, float bridgeY, float bridgeZ) {
      VoxelFlameDraw.Palette ladders = VoxelFlameDraw.blend(FlameTone.blueFractionFromSyncByte(styleByte));
      int[] tiers = ladders.base();
      Vector3f[] cam = VoxelFlameDraw.cameraAxes();
      Vector3f right = cam[0];
      Vector3f up = cam[1];
      float breathe = VoxelFlameDraw.breathe(age, seed & 0xFF);
      int cubes = 5 + (int)(VoxelFlameDraw.noise01(seed, 0, 90) * 3.999F);
      int discs = 2 + (VoxelFlameDraw.noise01(seed, 0, 91) > 0.5F ? 1 : 0);
      ps.pushPose();
      ps.translate(bridgeX, 0.125F + bridgeY, bridgeZ);
      VfxDraw.flame(ps, sink, (pose, vc) -> {
         int aDisc = VfxDraw.ia(45.0F * breathe, 1.0F);
         if (aDisc > 1) {
            for (int m = 0; m < discs; m++) {
               float ox = (VoxelFlameDraw.noise01(seed, m, 60) - 0.5F) * 0.35F * 0.4F;
               float oy = (VoxelFlameDraw.noise01(seed, m, 61) - 0.5F) * 0.35F * 0.4F;
               float oz = (VoxelFlameDraw.noise01(seed, m, 62) - 0.5F) * 0.35F * 0.4F;
               float r = 0.35F * (0.85F + 0.35F * VoxelFlameDraw.noise01(seed, m, 63)) * breathe;
               int rgb = VoxelFlameDraw.gradient(tiers, 0.15F + 0.45F * VoxelFlameDraw.noise01(seed, m, 64));
               VoxelFlameDraw.disc(pose, vc, ox, oy, oz, right, up, r, aDisc, rgb);
            }
         }

         for (int c = 0; c < cubes; c++) {
            float u = VoxelFlameDraw.noise01(seed, c, 70) + age * 0.035F;
            u -= (float)Mth.floor(u);
            if (!(u > 0.95F)) {
               float flicker = (float)Math.round((0.72F + 0.28F * Mth.sin(u * 6.2832F)) * 4.0F) / 4.0F;
               float dist = 0.35F * (0.12F + 0.55F * VoxelFlameDraw.noise01(seed, c, 71));
               float yawA = VoxelFlameDraw.noise01(seed, c, 72) * 6.2832F;
               float pitchA = (VoxelFlameDraw.noise01(seed, c, 73) - 0.5F) * 3.1416F;
               float cx = Mth.cos(yawA) * Mth.cos(pitchA) * dist;
               float cy = Mth.sin(pitchA) * dist;
               float cz = Mth.sin(yawA) * Mth.cos(pitchA) * dist;
               int tier = 3 - Math.min(3, (int)(dist / 0.2345F * 4.0F));
               int a = (int)(140.0F * flicker * (1.0F - 0.08F * (float)tier));
               if (a > 1) {
                  float half = 0.35F * (0.16F + 0.14F * VoxelFlameDraw.noise01(seed, c, 74)) * (1.0F - 0.08F * (float)tier);
                  int axis = (int)(VoxelFlameDraw.noise01(seed, c, 75) * 3.0F) % 3;
                  float spin = (0.12F + 0.18F * VoxelFlameDraw.noise01(seed, c, 76)) * (VoxelFlameDraw.noise01(seed, c, 77) > 0.5F ? 1.0F : -1.0F);
                  float ang = VoxelFlameDraw.noise01(seed, c, 78) * 6.2832F + age * spin;
                  VoxelFlameDraw.cube(pose, vc, cx, cy, cz, half, axis, ang, a, ladders.pick(seed + c)[tier]);
               }
            }
         }
      });
      ps.popPose();
   }
}
