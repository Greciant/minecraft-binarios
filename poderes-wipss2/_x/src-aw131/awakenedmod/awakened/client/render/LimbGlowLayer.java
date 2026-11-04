package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.core.glow.GlowState;
import com.awakenedmod.awakened.core.glow.Limb;
import com.awakenedmod.awakened.core.glow.LimbGlow;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;

public final class LimbGlowLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> implements FirstPersonPassLayer {
   private static final float FLASH_TICKS = 5.0F;
   static final float MIN_VISIBLE = 0.01F;
   private static final Map<Integer, float[]> FLASHES = new HashMap<>();
   private static final int FLASHES_CAP = 256;

   public static void register(IEventBus gameBus) {
      gameBus.addListener(LimbGlowLayer::onLoggingOut);
   }

   private static void onLoggingOut(LoggingOut event) {
      FLASHES.clear();
      LimbGlowShells.forgetAll();
   }

   public LimbGlowLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
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
      if (!player.isInvisible()) {
         GlowState glow = LimbGlow.of(player);
         boolean firstPerson = player == Minecraft.getInstance().player && FirstPersonMode.isFirstPersonPass();
         this.draw(poseStack, new VfxDraw.Sink(buffers), glow, (float)player.tickCount + partialTick, player.getId(), firstPerson);
      }
   }

   @Override
   public boolean showsInFirstPerson(AbstractClientPlayer player) {
      GlowState glow = LimbGlow.of(player);
      return glow.anyArm() || armFlashing(player.getId(), glow);
   }

   private void draw(PoseStack ps, VfxDraw.Sink sink, GlowState glow, float age, int id, boolean firstPerson) {
      int flashMask = noteFlash(id, glow, age);
      if (glow.any() || flashMask != 0) {
         for (Limb limb : Limb.REAL) {
            if (!firstPerson || limb.isArm()) {
               float intensity = glow.intensity(limb);
               float flash = (flashMask & limb.mask()) != 0 ? flashProgress(id, age) : 0.0F;
               if (!(intensity < 0.01F) || !(flash <= 0.0F)) {
                  ModelPart part = this.limbPart(limb);
                  if (part.visible) {
                     ps.pushPose();
                     part.translateAndRotate(ps);
                     LimbGlowShells.draw(ps, sink, part, intensity, flash, age, id * 7 + limb.ordinal(), glow.color());
                     ps.popPose();
                  }
               }
            }
         }
      }
   }

   private ModelPart limbPart(Limb limb) {
      PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)this.getParentModel();

      return switch (limb) {
         case HEAD -> model.head;
         case BODY -> model.body;
         case LEFT_ARM -> model.leftArm;
         case RIGHT_ARM -> model.rightArm;
         case LEFT_LEG -> model.leftLeg;
         case RIGHT_LEG, ALL -> model.rightLeg;
      };
   }

   static int noteFlash(int id, GlowState glow, float age) {
      if (FLASHES.size() > 256) {
         ClientLevel level = Minecraft.getInstance().level;
         FLASHES.keySet().removeIf(tracked -> level == null || level.getEntity(tracked) == null);
         if (FLASHES.size() > 256) {
            FLASHES.clear();
         }
      }

      float[] f = FLASHES.get(id);
      if (f == null) {
         FLASHES.put(id, new float[]{(float)glow.flashSeq(), -5.0F, 0.0F});
         return 0;
      } else {
         if ((int)f[0] != glow.flashSeq()) {
            f[0] = (float)glow.flashSeq();
            f[1] = age;
            f[2] = (float)glow.flashMask();
         }

         return age - f[1] < 5.0F ? (int)f[2] : 0;
      }
   }

   static float flashProgress(int id, float age) {
      float[] f = FLASHES.get(id);
      if (f == null) {
         return 0.0F;
      } else {
         float t = (age - f[1]) / 5.0F;
         return !(t < 0.0F) && !(t >= 1.0F) ? 1.0F - Mth.clamp(t, 0.0F, 1.0F) * (2.0F - Mth.clamp(t, 0.0F, 1.0F)) : 0.0F;
      }
   }

   static boolean armFlashing(int id, GlowState glow) {
      float[] f = FLASHES.get(id);
      if (f == null) {
         return false;
      } else {
         int arms = Limb.LEFT_ARM.mask() | Limb.RIGHT_ARM.mask();
         int mask = (int)f[0] != glow.flashSeq() ? glow.flashMask() : (int)f[2];
         return (mask & arms) != 0;
      }
   }
}
