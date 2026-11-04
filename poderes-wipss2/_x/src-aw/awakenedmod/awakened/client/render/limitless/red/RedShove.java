package com.awakenedmod.awakened.client.render.limitless.red;

import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessComfort;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessLook;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.client.render.limitless.LimitlessShapes;
import com.awakenedmod.awakened.core.Lab;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Vector3f;

public final class RedShove implements LimitlessLook<RedCue> {
   private static final String K = "limitless.red.";
   private static final Lab.Value DOME_R = Lab.CLIENT.def("limitless.red.dome_reach", 9.5, 1.0, 14.0, 0.1);
   private static final Lab.Value DOME_T = Lab.CLIENT.def("limitless.red.dome_time", 8.5, 2.0, 20.0, 0.5);
   private static final Lab.Value DOME_A = Lab.CLIENT.def("limitless.red.dome_alpha", 0.32, 0.0, 1.0, 0.01);
   private static final Lab.Value DOME_DARK = Lab.CLIENT.def("limitless.red.dome_dark", 0.45, 0.0, 1.0, 0.01);
   private static final Lab.Value DUST = Lab.CLIENT.def("limitless.red.dust", 96.0, 0.0, 200.0, 1.0);
   private static final Lab.Value DUST_WALL = Lab.CLIENT.def("limitless.red.dust_wall", 0.85, 0.1, 2.0, 0.05);
   private static final Lab.Value CHUNKS = Lab.CLIENT.def("limitless.red.chunks", 26.0, 0.0, 100.0, 1.0);
   private static final Lab.Value CHUNK_SIZE = Lab.CLIENT.def("limitless.red.chunk_size", 0.42, 0.05, 1.0, 0.01);
   private static final Lab.Value GLOW_R = Lab.CLIENT.def("limitless.red.glow", 0.9, 0.0, 4.0, 0.05);
   private static final float TWO_PI = (float) (Math.PI * 2);
   private static final int AIR_LIGHT = 15921386;
   private static final int AIR_DARK = 3948616;
   private static final int SCAR = 3023898;
   private static final float GLOW_RISE = 2.0F;
   private final RedBlast.Painter painter = RedBlast.painter();

   public void draw(LimitlessFrame f, RedCue cue, float age) {
      RedBlast.Shot sh = new RedBlast.Shot(f, cue, age);
      float air = 0.35F + 0.65F * LimitlessComfort.distortion(f.mc);
      this.painter.clear();
      if (sh.grounded()) {
         RedBlast.dustRing(this.painter, sh, DUST.getI(), DOME_R.getF() * 0.9F, DOME_T.getF() + 1.0F, DUST_WALL.getF(), 0.85F, 0L);
         RedBlast.rubble(this.painter, sh, CHUNKS.getI(), 0.7F, CHUNK_SIZE.getF(), 0.25F, 0L);
         RedBlast.rubble(this.painter, sh, CHUNKS.getI() * 3 / 2, 0.95F, CHUNK_SIZE.getF() * 0.35F, 0.7F, 500L);
      }

      LimitlessMesh paint = f.begin(FxLayer.PAINT);
      if (sh.grounded()) {
         scar(paint, sh);
      }

      RedBlast.streak(paint, true, sh);
      sh.orb(paint, true, sh.orbVis);
      RedBlast.glow(paint, true, sh.burst, sh.tb, GLOW_R.getF(), 2.0F, 5.0F, 1.0F, sh.seed, age);
      dome(paint, true, sh, air);
      this.painter.draw(paint, age);
      f.end(FxLayer.PAINT);
      LimitlessMesh light = f.begin(FxLayer.LIGHT);
      RedBlast.streak(light, false, sh);
      sh.orb(light, false, sh.orbVis);
      RedBlast.glow(light, false, sh.burst, sh.tb, GLOW_R.getF(), 2.0F, 5.0F, 1.0F, sh.seed, age);
      dome(light, false, sh, air);
      f.end(FxLayer.LIGHT);
   }

   private static void dome(LimitlessMesh m, boolean body, RedBlast.Shot sh, float air) {
      float reach = DOME_R.getF();
      float time = DOME_T.getF();

      for (int k = 0; k < 2; k++) {
         float t = sh.tb - 1.6F * (float)k;
         if (!(t <= 0.0F)) {
            float u = t / time;
            float r = reach * (k == 0 ? 1.0F : 0.72F) * Ease.out(u, 3.0F) + 0.2F;
            float a = air * DOME_A.getF() * (k == 0 ? 1.0F : 0.5F) * Ease.ramp(t, 0.0F, 1.2F) * (1.0F - Ease.ramp(u, 0.2F, 0.45F));
            if (!(a <= 0.01F)) {
               float dark = DOME_DARK.getF() * a;
               int light = sh.litRgb(15921386);
               float ph = (float) (Math.PI * 2) * RedOrb.h(sh.seed, 30 + k);
               float age = sh.age;
               LimitlessShapes.blob(
                  m,
                  sh.burst,
                  new Vector3f(0.0F, 1.0F, 0.0F),
                  (float) Math.PI,
                  14,
                  36,
                  true,
                  n -> r * (1.0F + 0.035F * (float)Math.sin((double)(n.x * 6.0F + ph + age * 0.8F)) * (float)Math.cos((double)(n.z * 5.0F - ph))),
                  (n, facing) -> {
                     float e = 1.0F - Math.max(0.0F, facing);
                     float arcs = 0.45F + 0.55F * LimitlessShapes.patches(n, 3.2F, ph);
                     if (body) {
                        float band = (float)Math.exp(-Math.pow((double)((e - 0.8F) / 0.07F), 2.0));
                        return LimitlessMesh.rgba(3948616, dark * band * arcs);
                     } else {
                        float rim = e * e * e * e * e * e * arcs;
                        return LimitlessMesh.rgba(light, a * (0.04F + 0.9F * rim));
                     }
                  }
               );
               float hgt = sh.height();
               if (!body && r > hgt + 0.05F) {
                  float gr = (float)Math.sqrt((double)(r * r - hgt * hgt));
                  Vector3f g = sh.ground().add(0.0F, 0.04F, 0.0F);
                  m.ringFlat(
                     g, new Vector3f(0.0F, 1.0F, 0.0F), gr, 0.06F + 0.02F * gr, 48, LimitlessMesh.rgba(light, a * 0.7F), LimitlessMesh.rgba(light, 0.0F), 0.0F
                  );
               }
            }
         }
      }
   }

   private static void scar(LimitlessMesh m, RedBlast.Shot sh) {
      float a = 0.6F * Ease.ramp(sh.tb, 0.0F, 2.0F) * (1.0F - Ease.ramp(sh.tb, 36.0F, 8.0F)) / (1.0F + 0.3F * sh.height());
      if (!(a <= 0.01F)) {
         float r = 1.1F + 0.6F * Ease.out(sh.tb / 3.0F, 2.0F);
         int col = sh.litRgb(3023898);
         RedBlast.flatDisc(m, sh.ground().add(0.0F, 0.02F, 0.0F), r, 18, LimitlessMesh.rgba(col, a), LimitlessMesh.rgba(col, 0.0F), sh.seed);
      }
   }

   public void tick(ClientLevel level, RedCue cue, int age) {
      RedBlast.groundBits(level, cue, age, 1.2F);
   }
}
