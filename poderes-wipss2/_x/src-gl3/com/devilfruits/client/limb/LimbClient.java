package com.devilfruits.client.limb;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.client.dev.LimbCapture;
import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.core.limb.LimbTestCommand;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Post;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;
import net.neoforged.neoforge.common.NeoForge;

public final class LimbClient {
   private static final boolean[] SAVED = new boolean[8];
   private static PlayerModel<?> hiddenOn;

   public static void init(IEventBus modBus) {
      modBus.addListener(LimbClient::addLayers);
      NeoForge.EVENT_BUS.addListener(LimbClient::onRenderPlayerPre);
      NeoForge.EVENT_BUS.addListener(LimbClient::onRenderPlayerPost);
      NeoForge.EVENT_BUS.addListener(LimbClient::onRenderHand);
      NeoForge.EVENT_BUS.addListener(LimbClient::onRenderStage);
      NeoForge.EVENT_BUS.addListener(e -> Limbs.prune());
      NeoForge.EVENT_BUS.addListener(e -> {
         Limbs.clearAll();
         LimbTest.reset();
         SkinSize.clear();
         SkinFill.clear();
      });
      ClientFxRegistry.register(LimbTestCommand.FX, LimbTest::onFx);
      AutoTest.addScript(DevilFruits.id("vfx_gomu_m1"), new LimbCapture("vfx_gomu_m1_"));
   }

   private static void addLayers(AddLayers event) {
      for (Model skin : event.getSkins()) {
         if (event.getSkin(skin) instanceof PlayerRenderer r) {
            r.addLayer(new LimbLayer(r));
         }
      }
   }

   private static void onRenderPlayerPre(Pre event) {
      if (Limbs.any()) {
         List<Limbs.Active> limbs = Limbs.of(event.getEntity());
         if (!limbs.isEmpty()) {
            PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)event.getRenderer().getModel();
            hiddenOn = model;
            ModelPart[] parts = parts(model);

            for (int i = 0; i < parts.length; i++) {
               SAVED[i] = parts[i].visible;
            }

            for (Limbs.Active a : limbs) {
               if (!a.done && a.spec.replaces) {
                  LimbRenderer.part(model, a.spec, false).visible = false;
                  LimbRenderer.part(model, a.spec, true).visible = false;
               }
            }
         }
      }
   }

   private static void onRenderPlayerPost(Post event) {
      if (hiddenOn == event.getRenderer().getModel()) {
         ModelPart[] parts = parts(hiddenOn);

         for (int i = 0; i < parts.length; i++) {
            parts[i].visible = SAVED[i];
         }

         hiddenOn = null;
      }
   }

   private static ModelPart[] parts(PlayerModel<?> m) {
      return new ModelPart[]{m.rightArm, m.rightSleeve, m.leftArm, m.leftSleeve, m.rightLeg, m.rightPants, m.leftLeg, m.leftPants};
   }

   private static void onRenderHand(RenderHandEvent event) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null && Limbs.any()) {
         HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
         if (Limbs.replacesArm(player, arm)) {
            event.setCanceled(true);
         }
      }
   }

   private static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_ENTITIES && Limbs.any()) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;
         if (player != null
            && mc.getCameraEntity() == player
            && mc.options.getCameraType().isFirstPerson()
            && !CaptureRig.active()
            && !Limbs.of(player).isEmpty()) {
            float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            Vec3 cam = event.getCamera().getPosition();
            Vec3 at = player.getPosition(partial);
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(at.x - cam.x, at.y - cam.y + (player.isCrouching() ? -0.125 : 0.0), at.z - cam.z);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - player.getViewYRot(partial)));
            float s = 0.9375F * player.getScale();
            pose.scale(-s, -s, s);
            pose.translate(0.0F, -1.501F, 0.0F);
            BufferSource buffers = mc.renderBuffers().bufferSource();
            ResourceLocation texture = mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer pr
               ? pr.getTextureLocation(player)
               : player.getSkin().texture();
            int light = mc.getEntityRenderDispatcher().getPackedLightCoords(player, partial);
            LimbRenderer.drawPlayer(player, pose, buffers, null, partial, light, texture);
            pose.popPose();
            buffers.endBatch();
         }
      }
   }

   private LimbClient() {
   }
}
