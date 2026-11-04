package com.awakenedmod.awakened.client.ruin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Post;

public final class RuinCounter {
   private static final float TAG_SCALE = 0.025F;
   private static final float ABOVE_HEAD = 0.5F;
   private static final float ABOVE_NAME_TAG = 0.28F;
   private static final int FULL_BRIGHT = 15728880;
   private static final int COLOR = 15592164;

   private RuinCounter() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(RuinCounter::onRenderLiving);
   }

   private static void onRenderLiving(Post<?, ?> event) {
      LivingEntity entity = event.getEntity();
      if (!entity.isInvisible()) {
         RuinLook.Look look = RuinDecayBodies.of(entity);
         if (look != null && look.mine() && !look.crumbling()) {
            Minecraft mc = Minecraft.getInstance();
            double dist = mc.gameRenderer.getMainCamera().getPosition().distanceTo(entity.position());
            float alpha = alpha(look, dist);
            if (!(alpha <= 0.02F)) {
               String text = text(look);
               float scale = scale(look);
               boolean tag = entity instanceof Player || entity.hasCustomName() && entity.isCustomNameVisible();
               PoseStack ps = event.getPoseStack();
               ps.pushPose();
               ps.translate(0.0F, entity.getBbHeight() + 0.5F + (tag ? 0.28F : 0.0F), 0.0F);
               ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
               ps.scale(scale, -scale, scale);
               float x = (float)(-mc.font.width(text)) / 2.0F;
               mc.font.drawInBatch(text, x, 0.0F, argb(alpha), true, ps.last().pose(), event.getMultiBufferSource(), DisplayMode.NORMAL, 0, 15728880);
               ps.popPose();
            }
         }
      }
   }

   private static String text(RuinLook.Look look) {
      return Integer.toString(look.stacks() > 0 ? look.stacks() : Math.max(1, Math.round(look.level())));
   }

   private static float scale(RuinLook.Look look) {
      return 0.025F * RuinLook.COUNTER_SCALE.getF() * (1.0F + 0.35F * RuinLook.smooth(look.pop()));
   }

   private static float alpha(RuinLook.Look look, double distance) {
      float near = RuinLook.COUNTER_NEAR.getF();
      float far = Math.max(near + 1.0F, RuinLook.COUNTER_FAR.getF());
      float byDistance = 1.0F - Mth.clamp(((float)distance - near) / (far - near), 0.0F, 1.0F);
      float byLevel = look.stacks() > 0 ? expiring(look.left()) : Mth.clamp(look.level() / 1.5F, 0.0F, 1.0F);
      return byDistance * byLevel;
   }

   private static float expiring(float left) {
      int window = RuinLook.COUNTER_DIM_TICKS.getI();
      if (window > 0 && !(left >= (float)window)) {
         float t = Mth.clamp(left / (float)window, 0.0F, 1.0F);
         return Mth.lerp(t, RuinLook.COUNTER_DIM.getF(), 1.0F);
      } else {
         return 1.0F;
      }
   }

   private static int argb(float alpha) {
      return Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24 | 15592164;
   }
}
