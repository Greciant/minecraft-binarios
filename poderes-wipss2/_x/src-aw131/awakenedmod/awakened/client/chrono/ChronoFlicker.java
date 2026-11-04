package com.awakenedmod.awakened.client.chrono;

import com.awakenedmod.awakened.client.render.ChronoGlass;
import com.awakenedmod.awakened.client.render.CrackWeb;
import com.awakenedmod.awakened.content.chronokinetic.ChronoFx;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.net.ChronoFxPayload;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class ChronoFlicker {
   static final Lab.Value SIZE = Lab.CLIENT.def("chrono.flicker.size", 0.55, 0.05, 2.0, 0.01);
   static final Lab.Value GROW = Lab.CLIENT.def("chrono.flicker.grow", 2.0, 0.5, 20.0, 0.5);
   static final Lab.Value HOLD = Lab.CLIENT.def("chrono.flicker.hold", 3.0, 0.0, 20.0, 0.5);
   static final Lab.Value FADE = Lab.CLIENT.def("chrono.flicker.fade", 3.0, 0.5, 20.0, 0.5);
   static final Lab.Value CUT_FADE = Lab.CLIENT.def("chrono.flicker.cut_fade", 6.0, 0.5, 30.0, 0.5);
   static final Lab.Value CUT_WIDTH = Lab.CLIENT.def("chrono.flicker.cut_width", 0.025, 0.002, 0.1, 0.001);

   private ChronoFlicker() {
   }

   static void preloadLabKeys() {
   }

   static int tailTicks(ChronoFxClient.Effect e) {
      float crack = GROW.getF() + HOLD.getF() + FADE.getF() - (float)e.duration;
      return Mth.ceil(Math.max(crack, CUT_FADE.getF()));
   }

   static void tick(ClientLevel level) {
      for (ChronoFxClient.Effect e : ChronoFxClient.effects(ChronoFxPayload.Kind.FLICKER)) {
         if (e.arrival == null && e.ended()) {
            Entity body = level.getEntity(e.entityId);
            e.arrival = body != null ? ChronoFx.chest(body, body.getPosition(0.0F)) : e.pos;
         }
      }
   }

   static void draw(Pose pose, VertexConsumer vc, ChronoGlass.Layer layer, ChronoFxClient.Effect e, ClientLevel level, Vec3 cam, float partialTick) {
      float age = e.age(partialTick);
      Vec3 from = e.pos.subtract(cam);
      float grow = Math.max(0.5F, GROW.getF());
      float fadeFrom = grow + HOLD.getF();
      float growth = Mth.clamp(age / grow, 0.0F, 1.0F);
      float alpha = 1.0F - Mth.clamp((age - fadeFrom) / Math.max(0.5F, FADE.getF()), 0.0F, 1.0F);
      Vec3 dir = e.points.isEmpty() ? new Vec3(0.0, 0.0, 1.0) : e.points.get(0);
      int rays = 5 + (int)(CrackWeb.hash01(e.seed ^ 24348) * 2.999F);
      ChronoGlass.planarCrack(pose, vc, layer, from, dir, SIZE.getF(), rays, e.seed, growth, alpha);
      Vec3 to = e.arrival;
      float cutAlpha = 1.0F;
      if (to == null) {
         Entity body = level.getEntity(e.entityId);
         if (body == null) {
            return;
         }

         to = ChronoFx.chest(body, body.getPosition(partialTick));
      } else {
         cutAlpha = 1.0F - Mth.clamp(e.sinceEnd(partialTick) / Math.max(0.5F, CUT_FADE.getF()), 0.0F, 1.0F);
      }

      ChronoGlass.cut(pose, vc, layer, from, to.subtract(cam), CUT_WIDTH.getF(), cutAlpha);
   }
}
