package com.devilfruits.fruits.supa_supa.client.blade;

import com.devilfruits.client.render.ArmOverlays;
import com.devilfruits.client.render.SkinOverrides;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.core.config.CoreClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class BladeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final Map<Integer, float[]> WHERE = new HashMap<>();
   private static final Vector3f V = new Vector3f();
   public static final SkinOverrides.Provider SKIN = BladeSkinMask::texture;
   public static final ArmOverlays.Overlay FIRST_PERSON = new ArmOverlays.Overlay() {
      @Override
      public void render(AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, boolean right, PoseStack pose, MultiBufferSource buffers, int light) {
         BladeLimbsClient.Limbs limbs = BladeLimbsClient.of(player);
         if (limbs != null) {
            float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            boolean slim = player.getSkin().model() == Model.SLIM;
            BladeLayer.drawArm(
               pose, buffers, light, player, arm, right, slim, limbs.arm(right), partial, (float)player.tickCount + partial, SkinTone.of(player), false
            );
         }
      }

      @Override
      public boolean hideSleeve(AbstractClientPlayer player, boolean right) {
         return BladeLimbsClient.armOut(player, right);
      }
   };

   public BladeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
      super(parent);
   }

   public void render(
      PoseStack ps,
      MultiBufferSource buffers,
      int light,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      BladeLimbsClient.Limbs limbs = BladeLimbsClient.of(player);
      if (limbs != null && !player.isInvisible()) {
         PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)this.getParentModel();
         boolean slim = player.getSkin().model() == Model.SLIM;
         int skin = SkinTone.of(player);
         drawArm(ps, buffers, light, player, model.rightArm, true, slim, limbs.right, partialTick, ageInTicks, skin, true);
         drawArm(ps, buffers, light, player, model.leftArm, false, slim, limbs.left, partialTick, ageInTicks, skin, true);
         if (limbs.steel.growth(partialTick) >= 1.0F
            && VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW
            && VfxSettings.tier() != CoreClientConfig.CinematicQuality.MEDIUM) {
            model.renderToBuffer(ps, buffers.getBuffer(BladeRenderTypes.sheen()), light, OverlayTexture.NO_OVERLAY);
         }

         if (limbs.legs.visible()) {
            for (boolean right : new boolean[]{true, false}) {
               ps.pushPose();
               (right ? model.rightLeg : model.leftLeg).translateAndRotate(ps);
               BladeModel.leg(ps, buffers, light, right, limbs.legs, partialTick);
               ps.popPose();
            }
         }
      }
   }

   static void drawArm(
      PoseStack ps,
      MultiBufferSource buffers,
      int light,
      AbstractClientPlayer player,
      ModelPart arm,
      boolean right,
      boolean slim,
      BladeLimbsClient.Limb limb,
      float partial,
      float time,
      int skin,
      boolean world
   ) {
      if (limb.visible()) {
         ps.pushPose();
         arm.translateAndRotate(ps);
         if (world) {
            remember(ps, player, right, slim);
         }

         BladeModel.arm(ps, buffers, light, right, slim, limb, partial, time, skin);
         ps.popPose();
      }
   }

   private static void remember(PoseStack ps, AbstractClientPlayer player, boolean right, boolean slim) {
      Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      float sx = right ? 1.0F : -1.0F;
      float shift = slim ? sx : 0.0F;
      float[] w = WHERE.computeIfAbsent(player.getId() * 2 + (right ? 0 : 1), k -> new float[6]);
      ps.last().pose().transformPosition((-3.0F * sx + shift) / 16.0F, 0.3125F, 0.0F, V);
      w[0] = (float)((double)V.x + cam.x);
      w[1] = (float)((double)V.y + cam.y);
      w[2] = (float)((double)V.z + cam.z);
      ps.last().pose().transformPosition((-4.0F * sx + shift) / 16.0F, 1.0625F, 0.0F, V);
      w[3] = (float)((double)V.x + cam.x);
      w[4] = (float)((double)V.y + cam.y);
      w[5] = (float)((double)V.z + cam.z);
   }

   static float[] where(AbstractClientPlayer player, boolean right) {
      return WHERE.get(player.getId() * 2 + (right ? 0 : 1));
   }

   static void forget() {
      WHERE.clear();
   }

   static float clamp01(float x) {
      return Mth.clamp(x, 0.0F, 1.0F);
   }
}
