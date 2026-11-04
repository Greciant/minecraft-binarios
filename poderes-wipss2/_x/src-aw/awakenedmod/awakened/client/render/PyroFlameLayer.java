package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.client.PyroBackdraftClock;
import com.awakenedmod.awakened.client.PyroClientHeat;
import com.awakenedmod.awakened.client.PyroNozzleTracker;
import com.awakenedmod.awakened.core.AwId;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public final class PyroFlameLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
   public static final ResourceLocation BACKDRAFT = AwId.of("backdraft");
   private static final int BD_CUBES_GAIN = 6;
   private static final float BD_HEIGHT_GAIN = 0.25F;
   private static final float BD_PALM_GAIN = 2.5F;
   private static final float BD_RING_GAIN = 1.0F;
   private static final float BD_CUBE_GAIN = 0.6F;
   private static final float SMOLDER_MIN = 0.25F;
   private static final float HAND_Y = 0.6F;
   private static final float HAND_OUT = 0.094F;
   private static final float ARM_LEN = 0.75F;
   private static final float HAND_TIP = 0.025F;
   private static final float COVER = 0.86F;
   private static final float ALONG_BIAS = 2.2F;
   private static final float FAR_SCALE = 0.3F;
   private static final float FAR_ALPHA_FALL = 0.45F;
   private static final float SURF = 0.145F;
   private static final float LICK_LIFT = 0.03F;
   private static final float GOLDEN = 2.39996F;
   private static final int CUBES_MIN = 7;
   private static final int CUBES_GAIN = 10;
   private static final int CUBES_BLUE_MIN = 11;
   private static final float LICK_MIN = 0.12F;
   private static final float LICK_GAIN = 0.12F;
   private static final float CUBE_HALF_MIN = 0.026F;
   private static final float CUBE_HALF_SPAN = 0.017F;
   private static final float SCROLL = 0.045F;
   private static final float POP_AT = 0.95F;
   private static final float ALPHA_BASE = 70.0F;
   private static final float ALPHA_GAIN = 85.0F;
   private static final int DISCS_MIN = 3;
   private static final int DISCS_GAIN = 3;
   private static final float DISC_R = 0.098F;
   private static final float DISC_FRAC = 0.45F;
   private static final float DISC_SCROLL = 0.012F;
   private static final float DISC_DRIFT = 0.6F;
   private static final float DISC_FORWARD = 0.14F;
   private static final int DISC_SALT = 20;
   private static final float PALM_R = 0.06F;
   private static final float PALM_FRAC = 0.6F;
   private static final float JET_NOZZLE_R = 0.085F;
   private static final float JET_NOZZLE_ALPHA = 150.0F;
   private static final float JET_FOOT_NOZZLE = 1.3F;
   private static final float FOOT_Y = 0.75F;

   public PyroFlameLayer(RenderLayerParent<T, M> parent) {
      super(parent);
   }

   public void render(
      PoseStack poseStack,
      MultiBufferSource buffers,
      int packedLight,
      T player,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      if (!player.isInvisible()) {
         float heat = PyroClientHeat.heat(player);
         boolean jetting = AwakenedAnimations.isJetting(player.getId());
         float backdraft = PyroBackdraftClock.progress(player);
         this.draw(poseStack, new VfxDraw.Sink(buffers), heat, ageInTicks, player.getId() & 0xFF, jetting, backdraft, player.getId());
      }
   }

   private void draw(PoseStack ps, VfxDraw.Sink sink, float heat, float age, int seed, boolean jetting, float backdraft, int entityId) {
      boolean smolder = heat >= 0.25F;
      boolean drawing = backdraft >= 0.0F;
      if (smolder || jetting || drawing) {
         VoxelFlameDraw.Palette ladders = VoxelFlameDraw.laddersFor(heat);
         Vector3f[] cam = VoxelFlameDraw.cameraAxes();
         if (smolder || drawing) {
            float swell = drawing ? Math.min(1.0F, backdraft) : 0.0F;
            float t = smolder ? Math.min(1.0F, (heat - 0.25F) / 0.75F) : 0.0F;
            t = Math.max(t, swell);
            int alpha = VfxDraw.ia(70.0F + 85.0F * t, 1.0F);
            if (alpha > 1) {
               int cubes = 7 + Math.round(t * 10.0F);
               if (heat >= 0.7F) {
                  cubes = Math.max(cubes, 11);
               }

               cubes += Math.round(swell * 6.0F);
               int discs = 3 + Math.round(t * 3.0F);
               float lick = 0.12F + 0.12F * t + 0.25F * swell;
               drawArm(ps, sink, ((HumanoidModel)this.getParentModel()).leftArm, 0.094F, cubes, discs, lick, age, seed, ladders, alpha, t, swell, cam);
               drawArm(ps, sink, ((HumanoidModel)this.getParentModel()).rightArm, -0.094F, cubes, discs, lick, age, seed + 77, ladders, alpha, t, swell, cam);
            }
         }

         if (jetting) {
            M model = (M)this.getParentModel();
            drawNozzle(ps, sink, model.leftArm, 0.094F, 0.6F, 0.085F, age, seed + 131, ladders, cam, entityId, 0);
            drawNozzle(ps, sink, model.rightArm, -0.094F, 0.6F, 0.085F, age, seed + 208, ladders, cam, entityId, 1);
            drawNozzle(ps, sink, model.leftLeg, 0.0F, 0.75F, 0.1105F, age, seed + 59, ladders, cam, entityId, 2);
            drawNozzle(ps, sink, model.rightLeg, 0.0F, 0.75F, 0.1105F, age, seed + 167, ladders, cam, entityId, 3);
         }
      }
   }

   private static void drawNozzle(
      PoseStack ps,
      VfxDraw.Sink sink,
      ModelPart limb,
      float out,
      float along,
      float radius,
      float age,
      int seed,
      VoxelFlameDraw.Palette ladders,
      Vector3f[] cam,
      int entityId,
      int slot
   ) {
      ps.pushPose();
      limb.translateAndRotate(ps);
      ps.translate(out, along, 0.0F);
      if (entityId >= 0) {
         PyroNozzleTracker.sample(entityId, slot, ps.last());
      }

      Matrix3f inv = new Matrix3f(ps.last().pose()).invert();
      Vector3f right = inv.transform(new Vector3f(cam[0])).normalize();
      Vector3f up = inv.transform(new Vector3f(cam[1])).normalize();
      float breathe = VoxelFlameDraw.breathe(age, seed, 2.2F);
      VfxDraw.flame(
         ps, sink, (pose, vc) -> VoxelFlameDraw.disc(pose, vc, 0.0F, 0.0F, 0.0F, right, up, radius * breathe, (int)(150.0F * breathe), ladders.base()[3])
      );
      ps.popPose();
   }

   private static void drawArm(
      PoseStack ps,
      VfxDraw.Sink sink,
      ModelPart arm,
      float out,
      int cubes,
      int discs,
      float lick,
      float age,
      int seed,
      VoxelFlameDraw.Palette ladders,
      int alpha,
      float t,
      float swell,
      Vector3f[] cam
   ) {
      ps.pushPose();
      arm.translateAndRotate(ps);
      ps.translate(out, 0.6F, 0.0F);
      ps.mulPose(Axis.XP.rotationDegrees(180.0F));
      Matrix3f inv = new Matrix3f(ps.last().pose()).invert();
      Vector3f right = inv.transform(new Vector3f(cam[0])).normalize();
      Vector3f up = inv.transform(new Vector3f(cam[1])).normalize();
      Vector3f toCam = new Vector3f(right).cross(up).normalize();
      float breathe = VoxelFlameDraw.breathe(age, seed);
      int aPalm = (int)((float)alpha * 0.6F * breathe);
      int palmRgb = ladders.base()[3];
      float reach = Math.min(0.75F, 0.645F + 0.25F * swell);
      VfxDraw.flame(
         ps,
         sink,
         (pose, vc) -> {
            VoxelFlameDraw.disc(
               pose,
               vc,
               toCam.x * 0.14F,
               toCam.y * 0.14F,
               toCam.z * 0.14F,
               right,
               up,
               0.06F * (0.9F + 0.5F * t) * (1.0F + 2.5F * swell) * breathe,
               aPalm,
               palmRgb
            );

            for (int j = 0; j < discs; j++) {
               armDisc(pose, vc, j, reach, lick, age, seed, ladders.base(), alpha, t, swell, right, up, toCam);
            }

            for (int i = 0; i < cubes; i++) {
               armCube(pose, vc, i, reach, lick, age, seed, ladders.pick(seed + i), alpha, t, swell);
            }
         }
      );
      ps.popPose();
   }

   private static float seat(int seed, int i, int salt) {
      return (float)Math.pow((double)VoxelFlameDraw.noise01(seed, i, salt), 2.2F);
   }

   private static float farScale(float s) {
      return 1.0F - 0.7F * s;
   }

   private static void armDisc(
      Pose pose,
      VertexConsumer vc,
      int j,
      float reach,
      float lick,
      float age,
      int seed,
      int[] tiers,
      int alpha,
      float t,
      float swell,
      Vector3f right,
      Vector3f up,
      Vector3f toCam
   ) {
      int lane = j + 20;
      float v = VoxelFlameDraw.noise01(seed, lane, 0) + age * 0.012F;
      v -= (float)Mth.floor(v);
      float fade = Mth.sin(v * (float) Math.PI);
      float s = seat(seed, lane, 1);
      float scale = farScale(s);
      float breathe = VoxelFlameDraw.breathe(age, seed + lane);
      int a = (int)((float)alpha * 0.45F * fade * breathe * (1.0F - 0.45F * s));
      if (a > 1) {
         float climb = lick * scale;
         float cy = -0.025F + s * (reach - climb) + v * 0.6F * climb;
         float r = 0.098F * scale * (0.9F + 0.5F * t) * (1.0F + 1.0F * swell) * breathe;
         VoxelFlameDraw.disc(
            pose, vc, toCam.x * 0.14F, cy + toCam.y * 0.14F, toCam.z * 0.14F, right, up, r, a, VoxelFlameDraw.gradient(tiers, 0.75F - 0.5F * s)
         );
      }
   }

   private static void armCube(Pose pose, VertexConsumer vc, int i, float reach, float lick, float age, int seed, int[] tiers, int alpha, float t, float swell) {
      float u = VoxelFlameDraw.noise01(seed, i, 0) + age * 0.045F;
      u -= (float)Mth.floor(u);
      if (!(u > 0.95F)) {
         int tier = Math.min(3, (int)(u * 4.0F));
         float s = seat(seed, i, 7);
         float scale = farScale(s);
         int a = (int)((float)alpha * (1.0F - 0.2F * (float)tier) * (1.0F - 0.45F * s));
         if (a > 1) {
            float ai = (float)i * 2.39996F + VoxelFlameDraw.noise01(seed, i, 1) * 0.8F;
            float c = Mth.cos(ai);
            float sn = Mth.sin(ai);
            float m = Math.max(Math.abs(c), Math.abs(sn));
            float r = (0.145F + 0.03F * u) * (1.0F + 1.0F * swell);
            float sway = 0.02F * Mth.sin(u * 6.8F + (float)seed * 0.19F + age * 0.12F);
            float cx = c / m * r + sway;
            float cz = sn / m * r;
            float climb = lick * scale;
            float cy = -0.025F + s * (reach - climb) + u * climb;
            float half = (0.026F + 0.017F * VoxelFlameDraw.noise01(seed, i, 2))
               * (1.0F + 0.5F * t)
               * (1.0F + 0.6F * swell)
               * (1.0F - 0.16F * (float)tier)
               * scale;
            int axis = (int)(VoxelFlameDraw.noise01(seed, i, 3) * 3.0F) % 3;
            float spin = (0.06F + 0.12F * VoxelFlameDraw.noise01(seed, i, 4)) * (VoxelFlameDraw.noise01(seed, i, 5) > 0.5F ? 1.0F : -1.0F);
            float ang = VoxelFlameDraw.noise01(seed, i, 6) * 6.2832F + age * spin;
            VoxelFlameDraw.cube(pose, vc, cx, cy, cz, half, axis, ang, a, tiers[tier]);
         }
      }
   }
}
