package com.awakenedmod.awakened.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Post;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;

public final class SpiderSpin {
   private static final float DEG_PER_TICK = 36.0F;
   private static boolean active;
   private static boolean pushed;

   private SpiderSpin() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(SpiderSpin::onPre);
      gameBus.addListener(SpiderSpin::onPost);
   }

   public static void start(int ticks, float perTick) {
      active = ticks > 0;
   }

   public static boolean active() {
      return active;
   }

   private static void applySpin(PoseStack poseStack, float partial) {
      LocalPlayer p = Minecraft.getInstance().player;
      if (p != null) {
         float angle = ((float)p.tickCount + partial) * 36.0F;
         poseStack.pushPose();
         poseStack.mulPose(Axis.YP.rotationDegrees(angle));
         pushed = true;
      }
   }

   private static boolean shouldSpin(RenderPlayerEvent event) {
      return event.getEntity() == Minecraft.getInstance().player && active;
   }

   private static void onPre(Pre event) {
      pushed = false;
      if (shouldSpin(event)) {
         applySpin(event.getPoseStack(), event.getPartialTick());
      }
   }

   private static void onPost(Post event) {
      if (pushed) {
         event.getPoseStack().popPose();
         pushed = false;
      }
   }
}
