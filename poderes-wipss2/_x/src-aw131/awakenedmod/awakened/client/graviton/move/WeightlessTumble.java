package com.awakenedmod.awakened.client.graviton.move;

import com.awakenedmod.awakened.content.graviton.WeightlessField;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Pre;
import org.joml.Quaternionf;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class WeightlessTumble {
   static final float SWAY_DEG = 25.0F;
   private static final float SWAY_RATE = 0.12F;
   private static final float SHUDDER_DEG = 1.5F;
   private static final float SETTLE_SPEED = 8.0F;
   private static final double MATCH_EPSILON = 0.001;
   private static final Map<Integer, WeightlessTumble.Tumble> TUMBLES = new HashMap<>();
   private static final Deque<Object> PUSHED = new ArrayDeque<>();

   private WeightlessTumble() {
   }

   private static boolean isFloating(LivingEntity e) {
      return WeightlessField.zeroG(e) != 0;
   }

   @SubscribeEvent
   static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         TUMBLES.clear();
         PUSHED.clear();
      } else {
         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof LivingEntity) {
               LivingEntity living = (LivingEntity)e;
               if (isFloating(living)) {
                  TUMBLES.computeIfAbsent(living.getId(), k -> new WeightlessTumble.Tumble(living));
               }
            }
         }

         Iterator<WeightlessTumble.Tumble> it = TUMBLES.values().iterator();

         while (it.hasNext()) {
            WeightlessTumble.Tumble t = it.next();
            t.prevAngle = t.angle;
            if (t.entity.isRemoved()) {
               it.remove();
            } else {
               int state = WeightlessField.zeroG(t.entity);
               t.floating = state != 0;
               if (t.floating) {
                  t.ticks++;
                  float in = Math.min(1.0F, (float)t.ticks / 20.0F);
                  t.angle = 25.0F * in * Mth.sin((float)t.ticks * 0.12F + t.phase);
                  if (state == 2) {
                     t.angle = t.angle + (t.ticks / 2 % 2 == 0 ? 1.5F : -1.5F);
                  }
               } else {
                  t.angle = t.angle + Mth.clamp(-t.angle, -8.0F, 8.0F);
                  if (Math.abs(t.angle) < 0.5F) {
                     it.remove();
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   static void onRenderPre(Pre event) {
      if (!event.isCanceled() && !TUMBLES.isEmpty()) {
         WeightlessTumble.Tumble tumble = TUMBLES.get(event.getEntity().getId());
         float height = event.getEntity().getBbHeight();
         Object key = event.getEntity();
         if (tumble != null) {
            float angle = tumble.angleAt(event.getPartialTick());
            if (!(Math.abs(angle) < 0.01F)) {
               PoseStack pose = event.getPoseStack();
               pose.pushPose();
               pose.translate(0.0, (double)height * 0.5, 0.0);
               pose.mulPose(new Quaternionf().rotationAxis(angle * (float) (Math.PI / 180.0), tumble.axisX, 0.0F, tumble.axisZ));
               pose.translate(0.0, (double)(-height) * 0.5, 0.0);
               PUSHED.push(key);
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   static void onRenderPost(net.neoforged.neoforge.client.event.RenderLivingEvent.Post event) {
      if (!PUSHED.isEmpty()) {
         Object key = event.getEntity();
         if (PUSHED.peek() == key) {
            PUSHED.pop();
            event.getPoseStack().popPose();
         }
      }
   }

   private static final class Tumble {
      final LivingEntity entity;
      final float axisX;
      final float axisZ;
      final float phase;
      float angle;
      float prevAngle;
      int ticks;
      boolean floating = true;

      Tumble(LivingEntity entity) {
         this.entity = entity;
         float theta = (float)((long)entity.getId() * 2654435761L % 360L) * (float) (Math.PI / 180.0);
         this.axisX = Mth.cos(theta);
         this.axisZ = Mth.sin(theta);
         this.phase = (float)(entity.getId() % 7) * 0.4F;
      }

      float angleAt(float partialTick) {
         return Mth.lerp(partialTick, this.prevAngle, this.angle);
      }
   }
}
