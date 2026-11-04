package com.awakenedmod.awakened.client.render.limitless.purple;

import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.core.Lab;

public final class PurpleRadiant extends PurpleBallLook {
   private static final String K = "limitless.purple.";
   private static final Lab.Value HALO = Lab.CLIENT.def("limitless.purple.halo", 0.42, 0.0, 1.0, 0.01);
   private static final Lab.Value HALO_PAINT = Lab.CLIENT.def("limitless.purple.halo_paint", 0.3, 0.0, 1.0, 0.01);
   private static final Lab.Value HALO_SIZE = Lab.CLIENT.def("limitless.purple.halo_size", 2.4, 1.0, 5.0, 0.05);
   private static final Lab.Value CORE = Lab.CLIENT.def("limitless.purple.core", 0.45, 0.0, 1.0, 0.01);
   private static final Lab.Value RIM = Lab.CLIENT.def("limitless.purple.rim", 0.55, 0.0, 1.0, 0.01);
   private static final Lab.Value ARCS = Lab.CLIENT.def("limitless.purple.arcs", 12.0, 0.0, 40.0, 1.0);
   private static final Lab.Value ARC_JAG = Lab.CLIENT.def("limitless.purple.arc_jag", 0.12, 0.0, 0.5, 0.005);
   private static final Lab.Value ARC_ALPHA = Lab.CLIENT.def("limitless.purple.arc_alpha", 0.9, 0.0, 1.0, 0.01);
   private static final int CORE_DIM = 9062632;
   private static final int CORE_HOT = 16315135;
   private static final int MID = 11036415;
   private static final int EDGE = 3804296;
   private static final int HALO_RGB = 9059583;
   private static final int RIM_RGB = 12089599;
   private final PurpleArcs arcs = new PurpleArcs();

   @Override
   protected void paintBall(LimitlessFrame f, LimitlessMesh m, PurpleBallLook.Ball ball) {
      float glow = Math.min(1.0F, ball.glow());
      float a = ball.a();
      float haloR = ball.r() * HALO_SIZE.getF() * 0.8F;
      m.disc(ball.c(), haloR, 40, LimitlessMesh.rgba(9059583, HALO_PAINT.getF() * glow * a * nearFade(ball.c(), haloR)), 0);
      int core = LimitlessMesh.mix(LimitlessMesh.rgba(9062632, 1.0F), LimitlessMesh.rgba(16315135, 1.0F), glow);
      int mid = LimitlessMesh.rgba(11036415, 1.0F);
      int edge = LimitlessMesh.rgba(3804296, 1.0F);
      m.sphere(ball.c(), ball.r(), 24, 48, (n, facing) -> {
         float e = 1.0F - Math.max(0.0F, facing);
         int col = e < 0.5F ? LimitlessMesh.mix(core, mid, e / 0.5F) : LimitlessMesh.mix(mid, edge, (e - 0.5F) / 0.5F);
         return LimitlessMesh.fade(col, a);
      });
      this.arcs
         .draw(
            m,
            ball.c(),
            ball.r(),
            ball.age(),
            ball.seed(),
            PurpleArcs.count(ARCS.getF(), ball),
            ARC_JAG.getF() * ball.calm(),
            new PurpleArcs.Style(0.026F, ARC_ALPHA.getF(), 1985791, 15732784),
            a * glow
         );
   }

   @Override
   protected void lightBall(LimitlessFrame f, LimitlessMesh m, PurpleBallLook.Ball ball) {
      float glow = ball.glow();
      float a = ball.a();
      float haloR = ball.r() * HALO_SIZE.getF();
      m.disc(ball.c(), haloR, 40, LimitlessMesh.rgba(9059583, HALO.getF() * Math.min(1.4F, glow) * a * nearFade(ball.c(), haloR)), 0);
      m.disc(ball.c(), ball.r() * 0.75F, 32, LimitlessMesh.rgba(16777215, CORE.getF() * Math.min(1.3F, glow) * a), LimitlessMesh.rgba(12620031, 0.0F));
      float rimA = RIM.getF() * a;
      m.sphere(
         ball.c(), ball.r() * 1.01F, 20, 48, (n, facing) -> LimitlessMesh.rgba(12089599, rimA * (float)Math.pow((double)(1.0F - Math.max(0.0F, facing)), 2.5))
      );
      this.arcs.glow(m, ball, ARCS.getF(), ARC_JAG.getF(), ARC_ALPHA.getF(), 0.05F);
   }
}
