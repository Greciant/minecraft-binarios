package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.core.glow.GlowState;
import com.awakenedmod.awakened.core.glow.Limb;
import com.awakenedmod.awakened.core.glow.LimbGlow;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import org.joml.Quaternionf;

public final class LimbGlowFirstPerson {
   private static final float HAND_ROLL = 0.1F;

   private LimbGlowFirstPerson() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(LimbGlowFirstPerson::onRenderArm);
   }

   private static void onRenderArm(RenderArmEvent event) {
      Minecraft mc = Minecraft.getInstance();
      AbstractClientPlayer player = mc.player;
      if (player != null && event.getPlayer() == player && !AwakenedAnimations.libraryOwnsFirstPerson() && !player.isInvisible()) {
         GlowState glow = LimbGlow.of(player);
         Limb limb = event.getArm() == HumanoidArm.LEFT ? Limb.LEFT_ARM : Limb.RIGHT_ARM;
         float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
         float age = (float)player.tickCount + partial;
         int flashMask = LimbGlowLayer.noteFlash(player.getId(), glow, age);
         float intensity = glow.intensity(limb);
         float flash = (flashMask & limb.mask()) != 0 ? LimbGlowLayer.flashProgress(player.getId(), age) : 0.0F;
         if (!(intensity < 0.01F) || !(flash <= 0.0F)) {
            ModelPart arm = enterHand(event, player);
            if (arm != null) {
               LimbGlowShells.draw(event.getPoseStack(), sinkOf(event), arm, intensity, flash, age, player.getId() * 7 + limb.ordinal(), glow.color());
               event.getPoseStack().popPose();
            }
         }
      }
   }

   static ModelPart enterHand(RenderArmEvent event, AbstractClientPlayer player) {
      ModelPart arm = armPart(player, event.getArm());
      if (arm == null) {
         return null;
      } else {
         resetLikeVanilla(player, arm);
         PoseStack ps = event.getPoseStack();
         ps.pushPose();
         ps.translate(arm.x / 16.0F, arm.y / 16.0F, arm.z / 16.0F);
         ps.mulPose(new Quaternionf().rotationZYX(event.getArm() == HumanoidArm.LEFT ? -0.1F : 0.1F, 0.0F, 0.0F));
         return arm;
      }
   }

   static VfxDraw.Sink sinkOf(RenderArmEvent event) {
      return new VfxDraw.Sink(event.getMultiBufferSource());
   }

   private static void resetLikeVanilla(AbstractClientPlayer player, ModelPart arm) {
      PlayerModel<AbstractClientPlayer> model = playerModel(player);
      if (model != null) {
         model.attackTime = 0.0F;
         model.crouching = false;
         model.swimAmount = 0.0F;
         model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      }
   }

   private static ModelPart armPart(AbstractClientPlayer player, HumanoidArm arm) {
      PlayerModel<AbstractClientPlayer> model = playerModel(player);
      if (model == null) {
         return null;
      } else {
         return arm == HumanoidArm.LEFT ? model.leftArm : model.rightArm;
      }
   }

   private static PlayerModel<AbstractClientPlayer> playerModel(AbstractClientPlayer player) {
      return Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer
         ? (PlayerModel)renderer.getModel()
         : null;
   }
}
