package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.ReaperAuraSettings;
import com.awakenedmod.awakened.client.ReaperAuraState;
import com.awakenedmod.awakened.content.reaper.ReaperAuraShape;
import com.awakenedmod.awakened.content.reaper.ReaperFx;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.joml.Vector3f;

public final class ReaperFlameLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final int RIBBONS = 14;
   private static final int INNER_RIBBONS = 7;
   private static final int SEGS = 9;
   private static final float LEAN = 0.82F;
   private static final float LICK_SPEED = 0.3F;
   private static final float LICK_AMP = 0.55F;
   private static final float RIBBON_W = 0.42F;
   private static final float OUTER_ALPHA = 78.0F;
   private static final float INNER_ALPHA = 96.0F;
   private static final float MIN_SHOWN = 0.01F;
   private static final float BASE_SINK = 0.12F;
   private static final float BASE_STAGGER = 0.22F;

   public ReaperFlameLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
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
      int tier;
      float shown;
      if (player == Minecraft.getInstance().player) {
         tier = ReaperAuraState.localTier();
         shown = ReaperAuraSettings.get();
      } else {
         tier = ReaperAuraState.trackedTier(player.getId());
         shown = ReaperAuraState.trackedShown(player.getId());
      }

      drawFlame(poseStack, new VfxDraw.Sink(buffers), tier, shown, player.getBbHeight(), ageInTicks, player.getId() & 0xFF);
   }

   public static int localTier() {
      return ReaperAuraState.localTier();
   }

   public static float localShown() {
      return ReaperAuraSettings.get();
   }

   private static void drawFlame(PoseStack ps, VfxDraw.Sink sink, int tier, float shown, float bbHeight, float age, int seed) {
      if (tier >= 0 && !(shown <= 0.01F)) {
         ReaperAuraShape.Band band = ReaperAuraShape.band(tier);
         if (band != ReaperAuraShape.Band.NONE) {
            float tierF = (float)tier / 8.0F;
            int body = ReaperFx.auraBody(tierF);
            int coreCol = ReaperFx.auraCore(tierF);
            float baseR;
            float height;
            float intensity;
            if (band == ReaperAuraShape.Band.CHEST) {
               float flicker = 0.9F + 0.1F * (float)Math.sin((double)age * 0.1);
               baseR = 0.5F * flicker;
               height = 0.8F;
               intensity = ReaperFx.flameIntensityChest();
            } else {
               baseR = ReaperAuraShape.bodyRadius(tier);
               height = ReaperAuraShape.bodyHeight(tier, bbHeight, shown);
               intensity = ReaperFx.flameIntensityBody(tierF);
            }

            intensity *= shown;
            if (!(baseR <= 0.001F) && !(height <= 0.01F)) {
               int hot = VfxColor.core(body, coreCol, 0.6F);
               int aOuter = VfxDraw.ia(78.0F, intensity);
               int aInner = VfxDraw.ia(96.0F, intensity);
               if (aOuter > 1 || aInner > 1) {
                  float fBaseR = baseR;
                  float fHeight = height;
                  float phase = age * 0.3F + (float)seed * 0.61F;
                  float sway = baseR * 0.55F;
                  float baseW = baseR * 0.42F;
                  ps.pushPose();
                  ps.translate(0.0F, 1.501F, 0.0F);
                  ps.scale(-1.0F, -1.0F, 1.0F);
                  if (band == ReaperAuraShape.Band.CHEST) {
                     ps.translate(0.0F, bbHeight * 0.55F, 0.0F);
                  }

                  VfxDraw.additive(ps, sink, (pose, vc) -> {
                     for (int i = 0; i < 14; i++) {
                        double a = (Math.PI * 2) * (double)i / 14.0;
                        float bx = (float)(Math.cos(a) * (double)fBaseR);
                        float bz = (float)(Math.sin(a) * (double)fBaseR);
                        float by = -0.12F - hash01(i, 7) * 0.22F;
                        float ph = phase + (float)i * 0.7F;
                        VfxGeometry.flameRibbon(pose, vc, bx, bz, by, fHeight, baseW, 9, sway, ph, 0.82F, body, hot, aOuter);
                     }

                     float innerR = fBaseR * 0.46F;
                     float innerH = fHeight * 0.62F;
                     float innerW = baseW * 0.85F;

                     for (int i = 0; i < 7; i++) {
                        double a = (Math.PI * 2) * (double)i / 7.0 + 0.4;
                        float bx = (float)(Math.cos(a) * (double)innerR);
                        float bz = (float)(Math.sin(a) * (double)innerR);
                        float by = -0.072000004F - hash01(i, 13) * 0.22F * 0.6F;
                        float ph = phase * 1.25F + (float)i * 0.9F;
                        VfxGeometry.flameRibbon(pose, vc, bx, bz, by, innerH, innerW, 9, sway * 0.7F, ph, 0.574F, hot, hot, aInner);
                     }
                  });
                  if (band == ReaperAuraShape.Band.BODY && ReaperAuraShape.hasLightningLicks(tier)) {
                     drawLightningLicks(ps, sink, tier, shown, bbHeight, baseR, age, body, coreCol);
                  }

                  ps.popPose();
               }
            }
         }
      }
   }

   private static void drawLightningLicks(PoseStack ps, VfxDraw.Sink sink, int tier, float shown, float bbHeight, float baseR, float age, int body, int coreCol) {
      float strength = (float)tier / 8.0F * shown;
      int tongues = 1 + (int)(strength * 2.0F);
      int crackle = (int)(age / 2.0F);
      int col = VfxColor.core(body, coreCol, 0.4F);
      int alphaByte = VfxDraw.ia(70.0F, 0.35F * shown);
      if (alphaByte > 1) {
         int colorArgb = alphaByte << 24 | col & 16777215;
         ps.pushPose();
         ps.translate(0.0, (double)bbHeight * 0.5, 0.0);
         VfxDraw.additive(ps, sink, (pose, vc) -> {
            for (int i = 0; i < tongues; i++) {
               double a = (double)crackle * 0.7 + (double)i * ((Math.PI * 2) / (double)Math.max(1, tongues));
               float reach = 0.6F + hash01(crackle, i) * 0.5F;
               float ex = (float)(Math.cos(a) * (double)reach);
               float ez = (float)(Math.sin(a) * (double)reach);
               float ey = 0.25F + hash01(crackle, i + 31) * 0.35F;
               Vector3f end = new Vector3f(ex, ey, ez);
               VfxGeometry.arc(pose, vc, end, 0.08F, 4, 0.25F, crackle * 131 + i * 17, colorArgb);
            }
         });
         ps.popPose();
      }
   }

   private static float hash01(int a, int b) {
      int h = a * 374761393 + b * 668265263;
      h = (h ^ h >>> 13) * 1274126177;
      return (float)((h ^ h >>> 16) & 65535) / 65535.0F;
   }
}
