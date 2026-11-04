package com.awakenedmod.awakened.client.beam;

public final class CubeTorrentStyle implements PyroBeamStyle {
   static final CubeTorrentStyle INSTANCE = new CubeTorrentStyle();
   private static final float RATE = 4.0F;
   private static final float JITTER = 0.025F;
   private static final float HALF = 0.075F;
   private static final float HALF_SPAN = 0.045F;
   private static final float SIZE_NEAR = 0.85F;
   private static final float SIZE_FAR = 1.15F;
   private static final float SPLASH_CHANCE = 0.35F;
   private static final int SPLASH_CUBES = 2;
   private static final float SPLASH_SPEED = 0.22F;
   private static final float SPLASH_HALF = 0.07F;
   private static final float IMPACT_GLOW_R = 0.3F;
   private static final int IMPACT_GLOW_A = 70;
   private static final int IMPACT_GLOW_LIFE = 5;
   private static final int IMPACT_GLOW_EVERY = 3;
   private static final float PALM_GLOW_R = 0.16F;
   private static final int PALM_GLOW_A = 52;

   @Override
   public void emit(PyroBeamSim.Emitter e) {
      int n = e.owed(4.0F * e.density());
      int k = 0;

      while (k < n && e.launch((byte)0, 1.0F, 0.025F, 1.0F, true) >= 0) {
         k++;
      }
   }

   @Override
   public void impact(PyroBeamSim.Emitter e, byte kind) {
      if (kind == 0) {
         if (e.rng().nextFloat() < 0.35F) {
            e.splash(2, 0.22F);
         }

         if (e.throttle(e.left() ? 1 : 0, 3)) {
            e.place((byte)3, 5, 0.3F);
         }
      }
   }

   @Override
   public void draw(PyroBeamStyle.Frame f, PyroBeamSim.Buffer b) {
      for (int i = 0; i < 400; i++) {
         if (b.life[i] != 0) {
            switch (b.kind[i]) {
               case 0:
                  BeamCubes.flightCube(f, b, i, 0.075F, 0.045F, 0.85F, 1.15F);
               case 1:
               default:
                  break;
               case 2:
                  BeamCubes.splashCube(f, b, i, 0.07F);
                  break;
               case 3:
                  BeamCubes.impactGlow(f, b, i, 70);
            }
         }
      }

      if (f.channeling()) {
         BeamCubes.palmGlow(f, f.palmLeft(), 0.16F, 52);
         BeamCubes.palmGlow(f, f.palmRight(), 0.16F, 52);
      }
   }
}
