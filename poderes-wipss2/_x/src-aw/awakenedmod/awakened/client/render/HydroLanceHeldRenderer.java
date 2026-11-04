package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;

public final class HydroLanceHeldRenderer {
   private static final float GATHER_REACH = 0.8F;

   private HydroLanceHeldRenderer() {
   }

   public static void register(IEventBus gameBus) {
      HydroLanceFirstPerson.register(gameBus);
      WaterFramePass.register(gameBus, HydroLanceHeldRenderer::draw);
   }

   private static void draw(ClientLevel level, Player p, float pt, PoseStack poseStack, MultiBufferSource buffers, Vec3 cam) {
      long age = HydroLanceAnchors.chargeAge(p);
      if (age >= 0L && !HydroHands.ownFirstPerson(p)) {
         float a = (float)age + pt;
         float f = Math.min(1.0F, a / 40.0F);
         boolean wet = HydroLanceAnchors.wet(p);
         HydroLanceMesh.Dims dims = HydroLanceMesh.Dims.of(f, wet, f);
         Vec3 tip = HydroLanceAnchors.tip(p, pt, a, HydroHands.mainArmLeft(p), dims.length());
         int light = LevelRenderer.getLightColor(level, BlockPos.containing(tip));
         HydroLanceMesh.Paints paints = HydroLanceMesh.Paints.of(wet, Mth.lerp(Math.min(1.0F, a / 6.0F), 0.3F, 1.0F), light);
         int seed = p.getId() * 31 + 7;
         float time = (float)p.tickCount + pt;
         poseStack.pushPose();
         poseStack.translate(tip.x - cam.x, tip.y - cam.y, tip.z - cam.z);
         poseStack.mulPose(Axis.YP.rotationDegrees(-p.getViewYRot(pt)));
         poseStack.mulPose(Axis.XP.rotationDegrees(p.getViewXRot(pt)));
         WaterMesh.submit(poseStack, buffers, (pose, vc) -> {
            HydroLanceMesh.spear(pose, vc, paints, dims, seed, time);
            HydroLanceMesh.gather(pose, vc, paints, dims, seed, time, f, 0.8F);
         });
         poseStack.popPose();
      }
   }
}
