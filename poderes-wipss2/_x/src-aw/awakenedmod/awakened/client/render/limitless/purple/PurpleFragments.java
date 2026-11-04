package com.awakenedmod.awakened.client.render.limitless.purple;

import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import java.util.List;
import java.util.function.DoubleFunction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class PurpleFragments {
   public static final int FLY = 10;
   private static final int VIO_EDGE = 5906100;
   private static final int VIO_GLOW = 9715967;

   private PurpleFragments() {
   }

   public static void draw(LimitlessFrame f, List<PurpleFragments.Shard> shards, Vec3 fwd, DoubleFunction<Vec3> centreAt) {
      if (!shards.isEmpty()) {
         double now = (double)((float)f.gameTime + f.partialTick);
         Vec3 centre = centreAt.apply(now);
         Vec3 before = centreAt.apply(now - 1.2);
         LimitlessMesh paint = f.begin(FxLayer.PAINT);

         for (PurpleFragments.Shard s : shards) {
            float t = (float)(now - (double)s.born());
            float u = t / 10.0F;
            if (!(u < 0.0F) && !(u >= 1.0F)) {
               Vec3 p = at(s, u, centre, fwd);
               float dark = Ease.smooth(u);
               int body = LimitlessMesh.mix(LimitlessMesh.rgba(s.rgb(), 1.0F), LimitlessMesh.rgba(5906100, 1.0F), dark);
               int shadow = LimitlessMesh.mix(body, LimitlessMesh.rgba(1181215, 1.0F), 0.5F);
               float a = Ease.ramp(t, 0.0F, 1.5F);
               Quaternionf rot = new Quaternionf()
                  .rotationXYZ((float)s.seed() * 0.37F + t * 0.31F, (float)s.seed() * 0.61F + t * 0.23F, (float)s.seed() * 0.13F);
               paint.shard(f.rel(p), s.size() * (1.0F - 0.6F * u), rot, LimitlessMesh.fade(body, a), LimitlessMesh.fade(shadow, a));
            }
         }

         f.end(FxLayer.PAINT);
         LimitlessMesh light = f.begin(FxLayer.LIGHT);

         for (PurpleFragments.Shard sx : shards) {
            float t = (float)(now - (double)sx.born());
            float u = t / 10.0F;
            float u0 = (t - 1.2F) / 10.0F;
            if (!(u0 < 0.0F) && !(u >= 1.0F)) {
               float glow = 0.45F * Ease.smooth(u) * (1.0F - Ease.ramp(u, 0.85F, 0.15F));
               float w = sx.size() * 0.3F * (1.0F - 0.5F * u);
               light.streak(
                  f.rel(at(sx, u0, before, fwd)), f.rel(at(sx, u, centre, fwd)), 0.0F, w, LimitlessMesh.rgba(9715967, 0.0F), LimitlessMesh.rgba(9715967, glow)
               );
            }
         }

         f.end(FxLayer.LIGHT);
      }
   }

   private static Vec3 at(PurpleFragments.Shard s, float u, Vec3 c, Vec3 fwd) {
      Vec3 rel = s.from().subtract(c);
      double ang = (double)(s.spin() * u);
      double cos = Math.cos(ang);
      double sin = Math.sin(ang);
      Vec3 turned = rel.scale(cos).add(fwd.cross(rel).scale(sin)).add(fwd.scale(fwd.dot(rel) * (1.0 - cos)));
      double k = (double)((1.0F - u) * (1.0F - u));
      return c.add(turned.scale(k));
   }

   public static double age(double t, int releaseTick) {
      return 50.0 + (t - (double)releaseTick);
   }

   public static record Shard(Vec3 from, int born, int rgb, float size, float spin, int seed) {
   }
}
