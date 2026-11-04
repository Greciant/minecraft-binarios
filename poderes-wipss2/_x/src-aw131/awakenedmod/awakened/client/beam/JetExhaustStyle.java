package com.awakenedmod.awakened.client.beam;

import com.awakenedmod.awakened.client.PyroSourceAnchors;
import net.minecraft.util.Mth;

public final class JetExhaustStyle implements PyroBeamStyle {
   static final JetExhaustStyle INSTANCE = new JetExhaustStyle();
   private static final float RATE_IDLE = 1.5F;
   private static final float RATE_FULL = 4.0F;
   private static final float RATE_BLUE = 2.0F;
   private static final float FOOT_RATE = 1.5F;
   private static final float FOOT_NOZZLE = 1.3F;
   private static final int TAP_LAUNCHES_HAND = 8;
   private static final int TAP_LAUNCHES_FOOT = 16;
   private static final float TAP_JITTER = 0.3F;
   private static final float HOLD_JITTER = 0.07F;
   private static final float TAP_SPEED = 1.35F;
   private static final float BLUE_SPEED = 0.3F;
   private static final float HALF = 0.07F;
   private static final float HALF_SPAN = 0.04F;
   private static final float SIZE_NEAR = 0.8F;
   private static final float SIZE_FAR = 1.35F;
   private static final float SPLASH_CHANCE = 0.3F;
   private static final int SPLASH_CUBES = 2;
   private static final float SPLASH_SPEED = 0.16F;
   private static final float SPLASH_HALF = 0.06F;
   private static final float IMPACT_GLOW_R = 0.28F;
   private static final int IMPACT_GLOW_A = 64;
   private static final int IMPACT_GLOW_LIFE = 5;
   private static final int IMPACT_GLOW_EVERY = 3;
   private static final float NOZZLE_R = 0.14F;
   private static final int NOZZLE_A = 96;
   private static final float NOZZLE_THROTTLE_GAIN = 0.6F;
   private static final float NOZZLE_BLUE_GAIN = 0.5F;
   private static final float FLASH_GAIN = 2.2F;
   private static final float FLASH_TICKS = 4.0F;

   private JetExhaustStyle() {
   }

   static float throttle(float litAge) {
      return Mth.clamp((litAge + 1.0F) / 15.0F, 0.0F, 1.0F);
   }

   @Override
   public void emit(PyroBeamSim.Emitter e) {
      PyroBeamSim.Buffer b = e.buffer();
      float blue = b.blueFraction();
      boolean foot = e.foot();
      if (b.litAge() == 0) {
         int launches = foot ? 16 : 8;
         int k = 0;

         while (k < launches && e.launch((byte)0, 1.35F + 0.3F * blue, 0.3F, 0.6F, true) >= 0) {
            k++;
         }
      } else {
         float rate = (Mth.lerp(throttle((float)b.litAge()), 1.5F, 4.0F) + 2.0F * blue) * (foot ? 1.5F : 1.0F);
         int n = e.owed(rate * e.density());
         int k = 0;

         while (k < n && e.launch((byte)0, 1.0F + 0.3F * blue, 0.07F, 1.0F, true) >= 0) {
            k++;
         }
      }
   }

   @Override
   public void impact(PyroBeamSim.Emitter e, byte kind) {
      if (kind == 0) {
         if (e.rng().nextFloat() < 0.3F) {
            e.splash(2, 0.16F);
         }

         if (e.throttle(e.left() ? 1 : 0, 3)) {
            e.place((byte)3, 5, 0.28F);
         }
      }
   }

   @Override
   public void draw(PyroBeamStyle.Frame f, PyroBeamSim.Buffer b) {
      for (int i = 0; i < 400; i++) {
         if (b.life[i] != 0) {
            switch (b.kind[i]) {
               case 0:
                  BeamCubes.flightCube(f, b, i, 0.07F, 0.04F, 0.8F, 1.35F);
               case 1:
               default:
                  break;
               case 2:
                  BeamCubes.splashCube(f, b, i, 0.06F);
                  break;
               case 3:
                  BeamCubes.impactGlow(f, b, i, 64);
            }
         }
      }

      if (f.channeling()) {
         float lit = (float)b.litAge() + f.partialTick();
         float flash = Math.max(0.0F, 1.0F - lit / 4.0F);
         float blue = b.blueFraction();
         float r = 0.14F * (1.0F + 0.6F * throttle(lit) + 0.5F * blue) * (1.0F + 2.2F * flash) * f.breathe();
         int a = Math.min(255, (int)(96.0F * (0.6F + 0.4F * throttle(lit) + 0.5F * blue) * (1.0F + flash)));
         PyroSourceAnchors.Nozzles nozzles = f.nozzles();

         for (int slot = 0; slot < nozzles.count(); slot++) {
            boolean foot = slot >= 2;
            BeamCubes.palmGlow(f, nozzles.point(slot), foot ? r * 1.3F : r, a);
         }
      }
   }
}
