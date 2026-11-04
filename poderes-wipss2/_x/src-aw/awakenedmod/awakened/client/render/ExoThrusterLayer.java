package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.ExoSuitVisibility;
import com.awakenedmod.awakened.content.exosavant.ExoPiece;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public final class ExoThrusterLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final int PLUME = 4169983;
   private static final int PLUME_MID = 7978495;
   private static final int PLUME_CORE = 15398655;
   private static final int CRACKLE = 8373503;
   private static final float CRUISE_TOP_MS = 34.0F;
   private static final ExoThrusterLayer.Nozzle[] NOZZLES = new ExoThrusterLayer.Nozzle[]{
      new ExoThrusterLayer.Nozzle(ExoThrusterLayer.Limb.LLEG, ExoPiece.BOOTS, 0.0F, 0.7F, -0.03F, 0.0F, 0.13F, 1.0F, 0.05F),
      new ExoThrusterLayer.Nozzle(ExoThrusterLayer.Limb.RLEG, ExoPiece.BOOTS, 0.0F, 0.7F, -0.03F, 0.0F, 0.13F, 1.0F, 0.05F),
      new ExoThrusterLayer.Nozzle(ExoThrusterLayer.Limb.BODY, ExoPiece.CHESTPLATE, 0.19F, 0.4F, 0.23F, 10.0F, 0.068F, 0.75F, 0.0F),
      new ExoThrusterLayer.Nozzle(ExoThrusterLayer.Limb.BODY, ExoPiece.CHESTPLATE, -0.19F, 0.4F, 0.23F, 10.0F, 0.068F, 0.75F, 0.0F),
      new ExoThrusterLayer.Nozzle(ExoThrusterLayer.Limb.LARM, ExoPiece.GAUNTLETS, 0.094F, 0.656F, 0.0F, 0.0F, 0.11F, 0.45F, 0.0F),
      new ExoThrusterLayer.Nozzle(ExoThrusterLayer.Limb.RARM, ExoPiece.GAUNTLETS, -0.094F, 0.656F, 0.0F, 0.0F, 0.11F, 0.45F, 0.0F)
   };

   public ExoThrusterLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
      super(parent);
   }

   public void render(
      PoseStack poseStack,
      MultiBufferSource buffers,
      int packedLight,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      ExoVisual vis = (ExoVisual)player.getData(AwakenedAttachments.EXO_VISUAL);
      if (vis.flying() && !player.isInvisible()) {
         int mask = ExoSuitVisibility.visibleMask(player, vis.builtMask());
         this.draw(
            poseStack, new VfxDraw.Sink(buffers), vis.jet(), vis.afterburner(), vis.sputtering(), (float)vis.flightSpeed(), ageInTicks, player.getId(), mask
         );
      }
   }

   private void draw(PoseStack ps, VfxDraw.Sink sink, boolean jet, boolean afterburner, boolean sputtering, float speedMs, float age, int entityId, int mask) {
      float throttle = jet ? Mth.clamp(speedMs / 34.0F, 0.0F, 1.0F) : 0.0F;
      float len = 0.7F + throttle * 0.5F + (afterburner ? 0.6F : 0.0F);
      float rGain = 1.0F + 0.28F * throttle;
      float intensity = 1.3F + throttle * 0.7F + (afterburner ? 0.2F : 0.0F);
      float roughGain = 1.0F + 0.9F * throttle;
      float coreRadF = 0.3F + 0.15F * throttle;
      float coreLenF = 0.5F + 0.25F * throttle;
      float midRadF = 0.62F + 0.06F * throttle;
      float midLenF = 0.78F + 0.1F * throttle;
      float breathe = 0.92F + 0.08F * Mth.sin(age * 0.35F + (float)(entityId & 0xFF));
      if (sputtering) {
         len *= 0.45F;
         intensity *= 0.4F;
         int gutter = (int)(age / 2.0F) + entityId;
         if ((gutter * 374761393 >>> 28 & 3) == 0) {
            return;
         }
      }

      int bodyRgb = VfxColor.lighten(4169983, throttle * 0.35F);
      int body = VfxColor.argb(VfxDraw.ia(88.0F * breathe, intensity), bodyRgb);
      int mid = VfxColor.argb(VfxDraw.ia(131.0F * breathe, intensity), VfxColor.lighten(7978495, throttle * 0.35F));
      int hot = VfxColor.argb(VfxDraw.ia(200.0F * breathe, intensity), 15398655);
      int ribbonAlpha = VfxDraw.ia(90.0F * breathe, intensity);
      int seed = entityId * 31 + (int)age;
      int crackle = (int)(age / (afterburner ? 1.5F : 3.0F));
      int arcCol = VfxColor.argb(VfxDraw.ia(165.0F * breathe, intensity), 8373503);
      float arcJitter = 0.22F + 0.15F * throttle;
      boolean diamondsOn = throttle > 0.55F && !sputtering;
      int diamondCount = afterburner ? 3 : 2;

      for (int i = 0; i < NOZZLES.length; i++) {
         ExoThrusterLayer.Nozzle n = NOZZLES[i];
         if ((mask & n.piece().bit()) != 0) {
            float r = n.mouth() * rGain;
            float l = len * n.size();
            ps.pushPose();
            this.limbPart(n.limb()).translateAndRotate(ps);
            ps.translate(n.x(), n.y(), n.z());
            if (n.tiltDeg() != 0.0F) {
               ps.mulPose(Axis.XP.rotationDegrees(n.tiltDeg()));
            }

            int idx = i;
            int nozzleSeed = seed + i * 7;
            boolean arcNow = throttle > 0.7F || (crackle + i * 5 & 1) == 0;
            boolean premium = n.size() >= 0.75F;
            boolean diamonds = diamondsOn && premium;
            float inset = n.inset();
            VfxDraw.additive(
               ps,
               sink,
               (pose, vc) -> {
                  if (premium) {
                     VfxGeometry.disc(pose, vc, inset, r * 0.92F, 12, hot);
                     VfxGeometry.sphere(pose, vc, inset - r * 0.35F, r * 0.75F * (1.0F + 0.4F * throttle), 3, 8, hot);
                     bellLayer(pose, vc, r, l, r * 0.06F, 12, 0.16F * roughGain, nozzleSeed, body);
                     bellLayer(pose, vc, r * midRadF, l * midLenF, r * 0.05F, 10, 0.12F * roughGain, nozzleSeed + 7, mid);
                     int licks = afterburner ? 6 : 4;
                     float lickPhase = age * (0.3F + 0.5F * throttle) + (float)nozzleSeed * 0.61F;

                     for (int k = 0; k < licks; k++) {
                        double a = (Math.PI * 2) * (double)k / (double)licks + (double)idx * 0.9;
                        float bx = (float)(Math.cos(a) * (double)r * 0.95F);
                        float bz = (float)(Math.sin(a) * (double)r * 0.95F);
                        VfxGeometry.flameRibbon(
                           pose, vc, bx, bz, inset, l * 0.55F, r * 0.5F, 6, r * 0.55F, lickPhase + (float)k * 0.7F, 0.7F, bodyRgb, 15398655, ribbonAlpha
                        );
                     }

                     if (afterburner) {
                        VfxGeometry.beam(pose, vc, new Vector3f(0.0F, l * 1.1F, 0.0F), r * 0.22F, hot);
                     }
                  } else {
                     VfxGeometry.cylinder(pose, vc, 0.0F, r, l, r * 0.06F, 12, 0.16F * roughGain, nozzleSeed, body);
                     VfxGeometry.cylinder(pose, vc, 0.0F, r * midRadF, l * midLenF, r * 0.05F, 10, 0.12F * roughGain, nozzleSeed + 7, mid);
                  }

                  VfxGeometry.cylinder(pose, vc, 0.0F, r * coreRadF, l * coreLenF, r * 0.03F, 8, 0.08F * roughGain, nozzleSeed + 13, hot);
                  if (diamonds) {
                     float coreLen = l * coreLenF;

                     for (int k = 1; k <= diamondCount; k++) {
                        VfxGeometry.sphere(pose, vc, coreLen * 0.25F * (float)k, r * 0.42F * (1.0F - 0.12F * (float)k), 3, 6, hot);
                     }
                  }

                  if (arcNow) {
                     VfxGeometry.arc(pose, vc, new Vector3f(0.0F, l * 0.9F, 0.0F), r * 0.4F, 3, arcJitter, crackle * 131 + idx * 17, arcCol);
                  }
               }
            );
            ps.popPose();
         }
      }
   }

   private static void bellLayer(Pose pose, VertexConsumer vc, float R, float L, float tip, int segs, float rough, int seed, int color) {
      float belly = 0.3F * L;
      VfxGeometry.cylinder(pose, vc, 0.0F, R, belly, R * 1.25F, segs, rough, seed, color);
      VfxGeometry.cylinder(pose, vc, belly, R * 1.25F, L, tip, segs, rough, seed, color);
   }

   private ModelPart limbPart(ExoThrusterLayer.Limb limb) {
      return switch (limb) {
         case LLEG -> ((PlayerModel)this.getParentModel()).leftLeg;
         case RLEG -> ((PlayerModel)this.getParentModel()).rightLeg;
         case LARM -> ((PlayerModel)this.getParentModel()).leftArm;
         case RARM -> ((PlayerModel)this.getParentModel()).rightArm;
         case BODY -> ((PlayerModel)this.getParentModel()).body;
      };
   }

   private static enum Limb {
      LLEG,
      RLEG,
      LARM,
      RARM,
      BODY;
   }

   private static record Nozzle(ExoThrusterLayer.Limb limb, ExoPiece piece, float x, float y, float z, float tiltDeg, float mouth, float size, float inset) {
   }
}
