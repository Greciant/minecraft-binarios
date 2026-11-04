package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.exosavant.ExoPiece;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Post;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;

public final class ExoSuitHide {
   public static final float COVERED_MIN_ASSEMBLY = 0.97F;
   private static PlayerModel<?> hiddenModel;
   private static boolean vHead;
   private static boolean vHat;
   private static boolean vBody;
   private static boolean vJacket;
   private static boolean vLeftArm;
   private static boolean vRightArm;
   private static boolean vLeftSleeve;
   private static boolean vRightSleeve;
   private static boolean vLeftLeg;
   private static boolean vRightLeg;
   private static boolean vLeftPants;
   private static boolean vRightPants;

   private ExoSuitHide() {
   }

   public static void hideCovered(
      ModelPart head, ModelPart hat, ModelPart body, ModelPart leftArm, ModelPart rightArm, ModelPart leftLeg, ModelPart rightLeg, boolean suited, int mask
   ) {
      if (suited) {
         if (headCovered(mask)) {
            head.visible = false;
         }

         if (hatCovered(mask)) {
            hat.visible = false;
         }

         if (bodyCovered(mask)) {
            body.visible = false;
         }

         if (armsCovered(mask)) {
            leftArm.visible = false;
            rightArm.visible = false;
         }

         if (legsCovered(mask)) {
            leftLeg.visible = false;
            rightLeg.visible = false;
         }
      }
   }

   public static boolean headCovered(int mask) {
      return false;
   }

   public static boolean hatCovered(int mask) {
      return (mask & ExoPiece.HELMET.bit()) != 0;
   }

   public static boolean bodyCovered(int mask) {
      return (mask & ExoPiece.CHESTPLATE.bit()) != 0;
   }

   public static boolean armsCovered(int mask) {
      return (mask & ExoPiece.GAUNTLETS.bit()) != 0;
   }

   public static boolean legsCovered(int mask) {
      int legMask = ExoPiece.LEGGINGS.bit() | ExoPiece.BOOTS.bit();
      return (mask & legMask) == legMask;
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ExoSuitHide::onPre);
      gameBus.addListener(ExoSuitHide::onPost);
   }

   private static void onPre(Pre event) {
      Player player = event.getEntity();
      ExoVisual vis = (ExoVisual)player.getData(AwakenedAttachments.EXO_VISUAL);
      if (vis.suited() && !(ExoSuitAssembly.progress(player.getId(), true) < 0.97F)) {
         PlayerModel<?> model = (PlayerModel<?>)event.getRenderer().getModel();
         vHead = model.head.visible;
         vHat = model.hat.visible;
         vBody = model.body.visible;
         vJacket = model.jacket.visible;
         vLeftArm = model.leftArm.visible;
         vRightArm = model.rightArm.visible;
         vLeftSleeve = model.leftSleeve.visible;
         vRightSleeve = model.rightSleeve.visible;
         vLeftLeg = model.leftLeg.visible;
         vRightLeg = model.rightLeg.visible;
         vLeftPants = model.leftPants.visible;
         vRightPants = model.rightPants.visible;
         hiddenModel = model;
         int mask = ExoSuitVisibility.visibleMask(player, vis.builtMask());
         hideCovered(model.head, model.hat, model.body, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg, true, mask);
         if (bodyCovered(mask)) {
            model.jacket.visible = false;
         }

         if (armsCovered(mask)) {
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
         }

         if (legsCovered(mask)) {
            model.leftPants.visible = false;
            model.rightPants.visible = false;
         }
      } else {
         hiddenModel = null;
      }
   }

   private static void onPost(Post event) {
      if (hiddenModel != null) {
         hiddenModel.head.visible = vHead;
         hiddenModel.hat.visible = vHat;
         hiddenModel.body.visible = vBody;
         hiddenModel.jacket.visible = vJacket;
         hiddenModel.leftArm.visible = vLeftArm;
         hiddenModel.rightArm.visible = vRightArm;
         hiddenModel.leftSleeve.visible = vLeftSleeve;
         hiddenModel.rightSleeve.visible = vRightSleeve;
         hiddenModel.leftLeg.visible = vLeftLeg;
         hiddenModel.rightLeg.visible = vRightLeg;
         hiddenModel.leftPants.visible = vLeftPants;
         hiddenModel.rightPants.visible = vRightPants;
         hiddenModel = null;
      }
   }
}
