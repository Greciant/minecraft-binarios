package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

final class VelocityBoltDrawer {
   private static final float REVEAL_TICKS = 2.0F;
   private static final float HOLD_TICKS = 2.0F;
   private static final float DECAY_TICKS = 3.0F;
   private static final float JITTER_WINDOW_TICKS = 2.0F;
   private static final int JOINTS_MIN = 8;
   private static final int JOINTS_MAX = 14;
   private static final float JOINTS_PER_BLOCK = 0.45F;
   private static final float JITTER = 0.25F;
   private static final float CORE_HALF = 0.025F;
   private static final float HALO_HALF = 0.07F;
   private static final float CORE_ALPHA = 200.0F;
   private static final float HALO_ALPHA = 100.0F;
   private static final float PAINT_HALF = 0.08F;
   private static final float PAINT_ALPHA = 40.0F;
   private static final float HALO_WARM_AT_TOP = 0.12F;
   private static final int TOP_GEAR = 5;
   private static final int FORKS_BASE = 1;
   private static final int CHARGE_STEPS = 2;
   private static final float FORK_LEN_MIN = 1.0F;
   private static final float FORK_LEN_MAX = 3.0F;
   private static final int FORK_SEGS_MIN = 3;
   private static final int FORK_SEGS_MAX = 5;
   private static final float FORK_HALF = 0.018F;
   private static final float FORK_JITTER = 0.22F;
   private static final float FORK_FORWARD_LEAN = 0.8F;
   private static final float FORK_ALPHA = 150.0F;
   private static final float FORK_TIP_ALPHA_FRAC = 0.45F;
   private static final float FORK_ROOT_LIGHTEN = 0.25F;
   private static final Vector3f ORIGIN = new Vector3f();

   private VelocityBoltDrawer() {
   }

   static void draw(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float age = state.ageInTicks - (float)state.delay;
      float length = (float)state.secondary.length();
      if (!(length < 0.05F)) {
         float reveal = VelocityStreaks.easeOut(age / 2.0F);
         float decayT = (age - 2.0F - 2.0F) / 3.0F;
         float alpha = decayT <= 0.0F ? 1.0F : 1.0F - VelocityStreaks.easeOut(decayT);
         if (!(alpha <= 0.01F) && !(reveal <= 0.01F)) {
            int steps = Math.max(0, Math.min(7, Math.round(state.scale)));
            int gear = Math.min(5, steps);
            int window = state.seed * 31 + (int)(age / 2.0F);
            int joints = Math.max(8, Math.min(14, Math.round(length * 0.45F)));
            float in = state.intensity;
            int hot = VfxColor.core(state.color, state.color2, 0.8F);
            int haloRgb = VelocityStreaks.lerpRgb(state.color, hot, 0.12F * (float)gear / 5.0F);
            int core = VfxColor.argb(VfxDraw.ia(200.0F * alpha, in), hot);
            int halo = VfxColor.argb(VfxDraw.ia(100.0F * alpha, in), haloRgb);
            int forkRoot = VfxColor.argb(VfxDraw.ia(150.0F * alpha, in), VfxColor.lighten(state.color, 0.25F));
            int forkTip = VfxColor.argb(VfxDraw.ia(67.5F * alpha, in), state.color);
            int forks = 1 + steps;
            Vector3f[] pts = VelocityStreaks.boltJoints(ORIGIN, new Vector3f(0.0F, 0.0F, length), joints, 0.25F, window);
            if (pts != null) {
               int paint = VfxColor.argb(VfxDraw.ia(40.0F * alpha, in), state.color);
               VfxDraw.custom(ps, sink, FxLayer.PAINT, (pose, vc) -> VelocityStreaks.boltPath(pose, vc, pts, reveal, 0.08F, paint, 0.0F, 0));
               VfxDraw.additive(ps, sink, (pose, vc) -> {
                  VelocityStreaks.boltPath(pose, vc, pts, reveal, 0.025F, core, 0.07F, halo);
                  float shownJoints = reveal * (float)joints;

                  for (int f = 0; f < forks; f++) {
                     int j = 1 + (int)(VelocityStreaks.noise(window, f, 1) * (float)(joints - 1));
                     if (!((float)j > shownJoints)) {
                        double phi = (double)VelocityStreaks.noise(window, f, 2) * Math.PI * 2.0;
                        float reach = Mth.lerp(VelocityStreaks.noise(window, f, 3), 1.0F, 3.0F);
                        float lean = 0.8F * VelocityStreaks.noise(window, f, 4);
                        Vector3f dir = new Vector3f((float)Math.cos(phi), (float)Math.sin(phi), lean).normalize();
                        Vector3f tip = new Vector3f(dir).mul(reach).add(pts[j]);
                        int segs = 3 + (int)(VelocityStreaks.noise(window, f, 5) * 3.0F);
                        VelocityStreaks.lightning(pose, vc, pts[j], tip, 0.018F, Math.min(5, segs), 0.22F, window * 7919 + f, forkRoot, forkTip);
                     }
                  }
               });
            }
         }
      }
   }
}
