package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.hydro.HydroLance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class HydroLanceRenderer extends EntityRenderer<HydroLance> {
   private static final float BRIDGE_TICKS = 4.0F;
   private static final float TRICKLE_REACH = 0.4F;
   private static final float TRICKLE_CUBE = 0.055F;

   public HydroLanceRenderer(Context ctx) {
      super(ctx);
   }

   public ResourceLocation getTextureLocation(HydroLance entity) {
      return null;
   }

   public boolean shouldRender(HydroLance entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }

   public void render(HydroLance entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
      HydroLanceSpray.shed(entity, partialTick, flightDims(entity, partialTick));
      Vec3 bridge = bridge(entity, partialTick);
      draw(
         poseStack,
         entity.style(),
         entity.getId(),
         (float)entity.tickCount + partialTick,
         Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()),
         Mth.lerp(partialTick, entity.xRotO, entity.getXRot()),
         entity.isStuck(),
         entity.drained(partialTick),
         packedLight,
         (float)bridge.x,
         (float)bridge.y,
         (float)bridge.z,
         bridgeScale(entity, partialTick),
         (float)entity.getDeltaMovement().length(),
         entity.drainLength(),
         entity.wallFace()
      );
   }

   private static HydroLanceMesh.Dims flightDims(HydroLance entity, float partialTick) {
      int style = entity.style();
      return HydroLanceMesh.Dims.of(HydroLance.charge(style), (style & 16) != 0, 1.0F).drain(entity.drained(partialTick));
   }

   private static Vec3 bridge(HydroLance entity, float partialTick) {
      float age = entity.flightAge(partialTick);
      if (!(age >= 4.0F) && !entity.isStuck() && entity.getOwner() instanceof Player owner) {
         int style = entity.style();
         float charge = HydroLance.charge(style);
         HydroLanceMesh.Dims dims = HydroLanceMesh.Dims.of(charge, (style & 16) != 0, 1.0F);
         Vec3 held = HydroLanceAnchors.tip(owner, partialTick, charge * 40.0F, (style & 32) != 0, dims.length());
         return held.subtract(entity.getPosition(partialTick)).scale((double)(1.0F - age / 4.0F));
      } else {
         return Vec3.ZERO;
      }
   }

   private static float bridgeScale(HydroLance entity, float partialTick) {
      float age = entity.flightAge(partialTick);
      return !(age >= 4.0F) && !entity.isStuck() && entity.getOwner() instanceof Player owner && HydroHands.ownFirstPerson(owner)
         ? Mth.lerp(age / 4.0F, HydroLanceFirstPerson.scale(), 1.0F)
         : 1.0F;
   }

   private static void draw(
      PoseStack ps,
      int style,
      int seed,
      float age,
      float yaw,
      float pitch,
      boolean stuck,
      float drained,
      int light,
      float bx,
      float by,
      float bz,
      float bridgeScale,
      float speed,
      float drainLength,
      Direction wallFace
   ) {
      boolean wet = (style & 16) != 0;
      HydroLanceMesh.Dims dims = HydroLanceMesh.Dims.of(HydroLance.charge(style), wet, 1.0F).drain(drained).scaled(bridgeScale);
      HydroLanceMesh.Paints paints = HydroLanceMesh.Paints.of(wet, 1.0F - 0.4F * drained, light);
      boolean flying = !stuck && bridgeScale >= 0.999F;
      if (stuck && wallFace != null && drainLength > 0.05F) {
         trickle(ps, yaw, pitch, drained, drainLength, wallFace, HydroLanceMesh.Paints.of(wet, 1.0F, light), seed, age);
      }

      ps.pushPose();
      ps.translate(bx, by, bz);
      ps.mulPose(Axis.YP.rotationDegrees(-yaw));
      ps.mulPose(Axis.XP.rotationDegrees(pitch));
      if (stuck) {
         ps.translate(0.0F, 0.0F, 0.3F);
      }

      WaterFramePass.queue(ps, (pose, vc) -> {
         HydroLanceMesh.spear(pose, vc, paints, dims, seed, age);
         if (flying) {
            HydroLanceMesh.trail(pose, vc, paints, dims, seed, age, speed);
         }
      });
      ps.popPose();
   }

   private static void trickle(
      PoseStack ps, float yaw, float pitch, float drained, float length, Direction face, HydroLanceMesh.Paints paints, int seed, float time
   ) {
      float grown = Math.min(1.0F, drained / 0.4F);
      float reach = length * (1.0F - (1.0F - grown) * (1.0F - grown));
      float fade = drained < 0.6F ? 1.0F : Math.max(0.0F, (1.0F - drained) / 0.4F);
      if (!(reach < 0.02F) && !(fade <= 0.01F)) {
         Vec3 tip = Vec3.directionFromRotation(pitch, yaw).scale(0.3F);
         float nx = (float)face.getStepX();
         float nz = (float)face.getStepZ();
         HydroLanceMesh.Paints p = paints.faded(fade);
         float hs = 0.055F * (1.0F - 0.4F * drained);
         float phase = WaterMesh.noise01(seed, 0, 51) * (float) (Math.PI * 2);
         ps.pushPose();
         ps.translate(tip.x + (double)nx * ((double)hs + 0.03), tip.y, tip.z + (double)nz * ((double)hs + 0.03));
         WaterFramePass.queue(
            ps,
            (pose, vc) -> {
               WaterMesh.Spine run = new WaterMesh.Spine() {
                  @Override
                  public Vector3f center(float t) {
                     float wander = 0.04F * Mth.sin(t * 7.0F + phase) * t;
                     return new Vector3f(-nz * wander, -reach * t, nx * wander);
                  }

                  @Override
                  public float radius(float t) {
                     return hs * (1.0F - 0.3F * t);
                  }
               };
               WaterBlocks.chain(
                  pose,
                  vc,
                  p.water(),
                  p.fleck(),
                  WaterBlocks.Chain.along(run, reach, hs * 1.8F).flowing(time * 0.12F).rough(0.01F, 0.0F).aligned(false).foamed(0.2F).seeded(seed + 7)
               );
               Vector3f end = run.center(1.0F);
               WaterBlocks.cube(pose, vc, p.water(), null, end.x, end.y, end.z, hs * 1.3F);
            }
         );
         ps.popPose();
      }
   }
}
