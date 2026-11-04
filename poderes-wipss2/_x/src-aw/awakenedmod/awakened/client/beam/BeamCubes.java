package com.awakenedmod.awakened.client.beam;

import com.awakenedmod.awakened.client.render.VoxelFlameDraw;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class BeamCubes {
   static final float CUBE_ALPHA = 150.0F;
   static final int ENV_STEPS = 4;
   private static final float IN_TICKS = 0.6F;
   private static final float FADE_FROM = 0.6F;
   private static final float[] AT = new float[3];

   private BeamCubes() {
   }

   static float quantize(float v) {
      return (float)Math.round(Mth.clamp(v, 0.0F, 1.0F) * 4.0F) / 4.0F;
   }

   static float u(PyroBeamSim.Buffer b, int i, float partial) {
      return Mth.clamp(((float)b.age[i] + partial) / (float)b.life[i], 0.0F, 1.0F);
   }

   static float envelope(float u, float t) {
      float in = Math.min(1.0F, t / 0.6F);
      float out = u < 0.6F ? 1.0F : (1.0F - u) / 0.39999998F;
      return in * out;
   }

   static int tier(PyroBeamSim.Buffer b, int i, float u) {
      int t = 3 - Math.min(2, (int)(u * 3.0F));
      return Mth.clamp(t + b.tierBias[i], 0, 3);
   }

   static float[] at(PyroBeamSim.Buffer b, int i, float partial) {
      float t = (float)b.age[i] + partial;
      float amp = b.wAmp[i];
      float sway = amp == 0.0F ? 0.0F : amp * (Mth.sin(t * b.wFreq[i] + b.wPhase[i]) - Mth.sin(b.wPhase[i]));
      AT[0] = (float)(b.px[i] + (double)((b.vx[i] + b.cx[i]) * partial) + (double)(b.wx[i] * sway));
      AT[1] = (float)(b.py[i] + (double)((b.vy[i] + b.cy[i]) * partial) + (double)(b.wy[i] * sway));
      AT[2] = (float)(b.pz[i] + (double)((b.vz[i] + b.cz[i]) * partial) + (double)(b.wz[i] * sway));
      return AT;
   }

   static float angle(PyroBeamSim.Buffer b, int i, float t) {
      return b.ang0[i] + t * b.spin[i];
   }

   static void flightCube(PyroBeamStyle.Frame f, PyroBeamSim.Buffer b, int i, float half, float halfSpan, float sizeNear, float sizeFar) {
      float partial = f.partialTick();
      float u = u(b, i, partial);
      float[] at = at(b, i, partial);
      int a = (int)(150.0F * quantize(envelope(u, (float)b.age[i] + partial)) * f.nearFade(at[0], at[1], at[2]));
      if (a > 1) {
         float sz = b.size[i];
         float h = (half + halfSpan * (sz - 0.7F) / 0.7F) * sz * Mth.lerp(u, sizeNear, sizeFar);
         VoxelFlameDraw.cube(f.pose(), f.vc(), at[0], at[1], at[2], h, b.axis[i], angle(b, i, (float)b.age[i] + partial), a, f.tiers(b.seed[i])[tier(b, i, u)]);
      }
   }

   static void splashCube(PyroBeamStyle.Frame f, PyroBeamSim.Buffer b, int i, float half) {
      float partial = f.partialTick();
      float u = u(b, i, partial);
      float[] at = at(b, i, partial);
      int a = (int)(150.0F * quantize(1.0F - u) * f.nearFade(at[0], at[1], at[2]));
      if (a > 1) {
         float h = half * b.size[i] * (1.0F - 0.4F * u);
         int tier = 3 - Math.min(3, (int)(u * 4.0F));
         VoxelFlameDraw.cube(f.pose(), f.vc(), at[0], at[1], at[2], h, b.axis[i], angle(b, i, (float)b.age[i] + partial), a, f.tiers(b.seed[i])[tier]);
      }
   }

   static void impactGlow(PyroBeamStyle.Frame f, PyroBeamSim.Buffer b, int i, int alpha) {
      float u = u(b, i, f.partialTick());
      float x = (float)b.px[i];
      float y = (float)b.py[i];
      float z = (float)b.pz[i];
      int a = (int)((float)alpha * quantize(1.0F - u * 0.8F) * f.nearFade(x, y, z));
      if (a > 1) {
         VoxelFlameDraw.disc(f.pose(), f.vc(), x, y, z, f.camAxes()[0], f.camAxes()[1], b.size[i] * f.breathe(), a, f.tiers()[2]);
      }
   }

   static void palmGlow(PyroBeamStyle.Frame f, Vec3 palm, float r, int alpha) {
      float x = (float)palm.x;
      float y = (float)palm.y;
      float z = (float)palm.z;
      int a = (int)((float)alpha * f.nearFade(x, y, z));
      if (a > 1) {
         VoxelFlameDraw.disc(f.pose(), f.vc(), x, y, z, f.camAxes()[0], f.camAxes()[1], r, a, f.tiers()[2]);
      }
   }
}
