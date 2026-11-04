package com.awakenedmod.awakened.client.chrono.ending;

import com.awakenedmod.awakened.client.chrono.ChronoFxClient;
import com.awakenedmod.awakened.client.chrono.ChronoTimeStop;
import com.awakenedmod.awakened.client.render.ChronoGlass;
import com.awakenedmod.awakened.core.Lab;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class HeldImpactLook {
   public static final Lab.Value SIZE = Lab.CLIENT.def("chrono.impact.size", 0.45, 0.05, 2.0, 0.01);
   public static final Lab.Value GROW = Lab.CLIENT.def("chrono.impact.grow", 2.0, 0.5, 20.0, 0.5);
   public static final Lab.Value SLAM = Lab.CLIENT.def("chrono.impact.slam", 0.4, 0.0, 2.0, 0.05);
   public static final Lab.Value SLAM_TICKS = Lab.CLIENT.def("chrono.impact.slam_ticks", 2.0, 0.5, 10.0, 0.5);

   private HeldImpactLook() {
   }

   public static int tailTicks() {
      return Mth.ceil(SLAM_TICKS.getF()) + ChronoTimeStop.impactTailTicks();
   }

   public static void draw(StopDraw d, ChronoFxClient.Effect impact) {
      Vec3 c = d.rel(impact.pos);
      Vec3 facing = impact.points.isEmpty() ? new Vec3(0.0, 0.0, 1.0) : impact.points.get(0).subtract(impact.pos);
      float size = SIZE.getF();
      if (!impact.ended()) {
         float growth = Mth.clamp(impact.age(d.partialTick()) / Math.max(0.5F, GROW.getF()), 0.0F, 1.0F);
         ChronoGlass.planarCrack(d.pose(), d.vc(), d.layer(), c, facing, size, impact.seed, growth, 1.0F);
      } else {
         float t = impact.sinceEnd(d.partialTick());
         float slamTicks = Math.max(0.5F, SLAM_TICKS.getF());
         Vec3 into = facing.lengthSqr() < 1.0E-6 ? Vec3.ZERO : facing.normalize().scale(-1.0);
         float s = Mth.clamp(t / slamTicks, 0.0F, 1.0F);
         Vec3 at = c.add(into.scale((double)(SLAM.getF() * s * s)));
         if (t < slamTicks) {
            ChronoGlass.planarCrack(d.pose(), d.vc(), d.layer(), at, facing, size, impact.seed, 1.0F, 1.0F);
         } else {
            float bt = t - slamTicks;
            float life = (float)ChronoTimeStop.impactTailTicks();
            float fade = 1.0F - Mth.clamp(bt / Math.max(1.0F, life * 0.4F), 0.0F, 1.0F);
            ChronoGlass.planarCrack(d.pose(), d.vc(), d.layer(), at, facing, size, impact.seed, 1.0F, fade);
            ChronoGlass.burst(d.pose(), d.vc(), d.layer(), at, into, size, impact.seed, bt, life);
         }
      }
   }
}
