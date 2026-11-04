package com.awakenedmod.awakened.client.graviton.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Post;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Pre;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class HeavyHandTidal {
   private static final float LAPSE_TICKS = 2.0F;
   private static final double MATCH_EPSILON = 0.001;
   private static final float PRESS_Y = 0.1F;
   private static final float PRESS_XZ = 0.05F;
   private static final float LIFT_Y = 0.14F;
   private static final float LIFT_XZ = 0.05F;
   private static final Map<Integer, HeavyHandTidal.Mark> MARKS = new HashMap<>();
   private static final Deque<Object> PUSHED = new ArrayDeque<>();

   private HeavyHandTidal() {
   }

   static void mark(LivingEntity e, float amount, float t) {
      if (!(Math.abs(amount) < 0.01F)) {
         if (MARKS.size() > 64) {
            MARKS.clear();
         }

         MARKS.put(e.getId(), new HeavyHandTidal.Mark(e, Mth.clamp(amount, -1.3F, 1.3F), t));
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   static void onRenderPre(Pre event) {
      Minecraft mc = Minecraft.getInstance();
      if (!event.isCanceled() && !MARKS.isEmpty() && mc.level != null) {
         float pt = event.getPartialTick();
         float t = (float)(mc.level.getGameTime() % 100000L) + pt;
         HeavyHandTidal.Mark mark = MARKS.get(event.getEntity().getId());
         Object key = event.getEntity();
         if (mark != null) {
            float age = t - mark.at();
            if (!(age > 2.0F) && !(age < -1000.0F) && mark.entity().level() == mc.level) {
               float k = mark.amount();
               float sy = k < 0.0F ? 1.0F + 0.1F * k : 1.0F + 0.14F * k;
               float sxz = k < 0.0F ? 1.0F - 0.05F * k : 1.0F - 0.05F * k;
               PoseStack pose = event.getPoseStack();
               pose.pushPose();
               pose.scale(sxz, sy, sxz);
               PUSHED.push(key);
            } else {
               MARKS.remove(mark.entity().getId());
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   static void onRenderPost(Post event) {
      if (!PUSHED.isEmpty()) {
         Object key = event.getEntity();
         if (PUSHED.peek() == key) {
            PUSHED.pop();
            event.getPoseStack().popPose();
         }
      }
   }

   private static record Mark(LivingEntity entity, float amount, float at) {
   }
}
