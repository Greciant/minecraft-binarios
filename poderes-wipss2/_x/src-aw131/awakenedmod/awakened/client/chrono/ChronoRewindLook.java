package com.awakenedmod.awakened.client.chrono;

import com.awakenedmod.awakened.client.render.ChronoDial;
import com.awakenedmod.awakened.client.render.ChronoThread;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.net.ChronoFxPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class ChronoRewindLook {
   static final Lab.Value THREAD_HOLD = Lab.CLIENT.def("chrono.rewind.thread_hold", 4.0, 0.0, 20.0, 1.0);
   static final Lab.Value WIND_UP = Lab.CLIENT.def("chrono.rewind.wind_up", 10.0, 1.0, 40.0, 1.0);
   static final Lab.Value LIFT = Lab.CLIENT.def("chrono.rewind.thread_lift", 0.5, 0.0, 1.2, 0.01);
   static final Lab.Value DIAL_TICKS = Lab.CLIENT.def("chrono.rewind.dial_ticks", 12.0, 1.0, 40.0, 1.0);
   static final Lab.Value DIAL_TURNS = Lab.CLIENT.def("chrono.rewind.dial_turns", 1.5, 0.0, 6.0, 0.05);
   static final Lab.Value DEVELOP = Lab.CLIENT.def("chrono.rewind.develop", 12.0, 1.0, 40.0, 1.0);
   static final Lab.Value DIAL_BEHIND = Lab.CLIENT.def("chrono.rewind.dial_behind", 0.1, 0.0, 2.0, 0.01);
   private static final float MINUTE_HAND = 0.75F;
   private static final float HOUR_HAND = 0.45F;

   private ChronoRewindLook() {
   }

   static void preloadLabKeys() {
      ChronoThread.preloadLabKeys();
      ChronoDial.preloadLabKeys();
   }

   static int tailTicks() {
      return Math.max(THREAD_HOLD.getI() + WIND_UP.getI(), DIAL_TICKS.getI()) + 1;
   }

   static int developTicks() {
      return DEVELOP.getI();
   }

   static float holdTicks(int glide) {
      float hold = Math.max(0.0F, THREAD_HOLD.getF());
      return glide > 0 ? Math.min(hold, (float)glide * 0.5F) : hold;
   }

   static float windUpTicks(int glide) {
      return glide > 0 ? Math.max(1.0F, (float)glide - holdTicks(glide)) : Math.max(1.0F, WIND_UP.getF());
   }

   static void draw(PoseStack world, MultiBufferSource buffers, Vec3 cam, float partialTick) {
      Minecraft mc = Minecraft.getInstance();

      for (ChronoFxClient.Effect e : ChronoFxClient.effects(ChronoFxPayload.Kind.REWIND)) {
         float age = e.age(partialTick);
         float wind = Mth.clamp((age - holdTicks(e.duration)) / windUpTicks(e.duration), 0.0F, 1.0F);
         if (wind < 1.0F) {
            float eased = wind * wind * (3.0F - 2.0F * wind);
            ChronoThread.draw(world.last(), buffers, e.points, cam, e.radius * LIFT.getF(), eased, 1.0F);
         }

         Entity body = mc.level == null ? null : mc.level.getEntity(e.entityId);
         ChronoDial.Face face = flash(e, age);
         if (face != null && !ChronoDials.ownFirstPerson(mc, body)) {
            Vec3 chest = e.pos.add(0.0, (double)(e.radius * 0.65F), 0.0);
            Entity at = age >= (float)e.duration ? body : null;
            ChronoDials.drawBehind(world, buffers, cam, at, chest, e.radius, face, DIAL_BEHIND.getF(), partialTick);
         }
      }
   }

   static ChronoDial.Face flash(ChronoFxClient.Effect e, float age) {
      float t = (age - (float)e.duration) / Math.max(1.0F, DIAL_TICKS.getF());
      if (!(t < 0.0F) && !(t >= 1.0F)) {
         float spin = 1.0F - (1.0F - t) * (1.0F - t);
         float minute = -spin * DIAL_TURNS.getF() * (float) (Math.PI * 2);
         ChronoDial.Hand[] hands = new ChronoDial.Hand[]{new ChronoDial.Hand(minute, 0.75F), new ChronoDial.Hand(minute / 12.0F, 0.45F)};
         float alpha = (1.0F - t) * Mth.clamp(t * 6.0F, 0.0F, 1.0F);
         return new ChronoDial.Face(ChronoDials.radius(e.radius), 4095, hands, alpha, 0.0F);
      } else {
         return null;
      }
   }
}
