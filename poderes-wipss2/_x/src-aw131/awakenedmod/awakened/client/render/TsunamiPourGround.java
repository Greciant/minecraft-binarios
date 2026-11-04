package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

final class TsunamiPourGround {
   private static final Lab.Value INFLOW_RADIUS = Lab.CLIENT.def("hydro.tsunami.inflow_radius", 12.0, 2.0, 30.0, 0.5);
   private static final Lab.Value STREAM_WIDTH = Lab.CLIENT.def("hydro.tsunami.stream_width", 0.55, 0.05, 2.0, 0.01);
   private static final Lab.Value POOL_RADIUS = Lab.CLIENT.def("hydro.tsunami.pool_radius", 4.5, 0.5, 12.0, 0.1);
   private static final Lab.Value POOL_ALPHA = Lab.CLIENT.def("hydro.tsunami.pool_alpha", 1.0, 0.0, 1.5, 0.05);
   private static final Lab.Value POOL_CUBE = Lab.CLIENT.def("hydro.tsunami.pool_cube", 1.0, 0.3, 2.0, 0.05);
   private static final int STREAMS = 10;
   private static final float INFLOW_MIN = 2.5F;
   private static final float STREAM_MIN = 0.15F;
   private static final float STREAM_GAP = 1.25F;
   private static final float POOL_MIN = 0.8F;
   private static final float DEPTH_MIN = 0.08F;
   private static final float DEPTH_MAX = 0.3F;
   private static final float CHURN = 0.22F;
   private static final float POOL_SHEEN = 0.3F;

   private TsunamiPourGround() {
   }

   static void pool(
      Pose pose,
      VertexConsumer vc,
      WaterWave.Look look,
      float alpha,
      float f,
      float half,
      float backLen,
      float wallCube,
      TsunamiPourGround.Tune tune,
      TsunamiPourGround.Floor under,
      int light,
      float time,
      int seed
   ) {
      float reach = Mth.lerp(f, 0.8F, tune.pool());
      float rx = half + reach;
      float rz = reach + 0.5F;
      float cz = -0.35F * rz;
      float a0 = alpha * tune.poolAlpha() * Mth.lerp(AnimEnvelopes.smooth(f * 3.0F), 0.5F, 1.0F);
      if (!(a0 < 0.05F)) {
         float c = tune.poolCube();
         int nx = Mth.ceil(rx / c);
         int nz = Mth.ceil(rz / c);
         WaterCubeGrid g = new WaterCubeGrid(2 * nx + 1, 2 * nz + 1, c, 0.0F);

         for (int k = 0; k < g.cols(); k++) {
            g.colX(k, (float)(k - nx) * c);
         }

         for (int r = 0; r < g.rows(); r++) {
            g.rowZ(r, cz + (float)(r - nz) * c);
         }

         float depth = Mth.lerp(f, 0.08F, 0.3F);
         float wallHalf = half + wallCube * 0.5F;
         float wallBack = -backLen - wallCube * 0.5F;

         for (int k = 0; k < g.cols(); k++) {
            float x = g.x(k);

            for (int r = 0; r < g.rows(); r++) {
               float z = g.z(r);
               if (!(Math.abs(x) < wallHalf) || !(z > wallBack) || !(z < wallCube * 0.5F)) {
                  int id = k * 131 + r;
                  float ex = x / rx;
                  float ez = (z - cz) / rz;
                  float t = Mth.sqrt(ex * ex + ez * ez);
                  if (!(t > 1.0F - 0.25F * WaterMesh.noise01(seed, id, 41))) {
                     float y = under.at(x, z);
                     if (!Float.isNaN(y)) {
                        float beat = Mth.sin(time * 0.22F + (float) (Math.PI * 2) * WaterMesh.noise01(seed, id, 42));
                        g.floor(k, r, y + 0.01F);
                        g.set(k, r, depth * (1.3F - 0.7F * t) * (0.6F + 0.4F * beat));
                        if (beat > 0.8F) {
                           g.flash(k, r, (beat - 0.8F) * 5.0F * f);
                        }
                     }
                  }
               }
            }
         }

         int rgb = WaterMesh.mix(look.crestRgb(), 15004159, 0.1F);
         WaterMesh.Paint water = WaterMesh.Paint.of(rgb, a0, light);
         WaterMesh.Paint sheen = water.sheen(0.3F);
         g.drawWater(pose, vc, water, false);
         WaterFramePass.foam(pose, vc, (fp, fvc) -> g.drawFoam(fp, fvc, sheen, (k, rxx, h) -> 0.5F));
      }
   }

   static void inflow(
      Pose pose,
      VertexConsumer vc,
      WaterWave.Look look,
      float alpha,
      float f,
      float half,
      TsunamiPourGround.Tune tune,
      final TsunamiPourGround.Floor under,
      int light,
      float time,
      int seed
   ) {
      float out = Mth.lerp(f, 2.5F, tune.inflow());
      float cube = Mth.lerp(f, 0.15F, tune.streamCube());
      int rgb = WaterMesh.mix(look.crestRgb(), 15004159, 0.1F);
      WaterMesh.Paint water = WaterMesh.Paint.of(rgb, Math.min(1.0F, alpha), light);
      WaterMesh.Paint foam = water.foam(water.opacity() * 0.4F);
      float gap = cube * 1.25F;
      float speed = 0.12F + 0.18F * f;

      for (int i = 0; i < 10; i++) {
         float joins = i < 4 ? 0.0F : ((float)i - 4.0F) / 6.0F * 0.8F;
         float shown = AnimEnvelopes.smooth((f - joins) / 0.12F);
         if (!(shown < 0.05F)) {
            float ang = (float) Math.PI + (Mth.frac((float)i * 0.618034F + 0.11F) - 0.5F) * 2.0F * 2.75F;
            float reach = out * (0.75F + 0.35F * WaterMesh.noise01(seed, i, 2));
            final float sx = Mth.sin(ang) * (reach + half * 0.7F * Math.abs(Mth.sin(ang)));
            final float sz = Mth.cos(ang) * reach;
            float tx = Mth.clamp(sx * 0.12F, -half * 0.8F, half * 0.8F);
            float tz = Mth.cos(ang) > 0.0F ? 0.4F : -0.4F;
            final float bend = (WaterMesh.noise01(seed, i, 5) - 0.5F) * reach * 0.35F;
            final float dx = tx - sx;
            final float dz = tz - sz;
            float len = Math.max(0.001F, Mth.sqrt(dx * dx + dz * dz));
            final float px = -dz / len;
            final float pz = dx / len;
            final float hs = cube * 0.5F * (0.8F + 0.4F * WaterMesh.noise01(seed, i, 4)) * shown;
            WaterMesh.Spine spine = new WaterMesh.Spine() {
               @Override
               public Vector3f center(float t) {
                  float sb = Mth.sin((float) Math.PI * t);
                  float x = sx + dx * t + px * bend * sb;
                  float z = sz + dz * t + pz * bend * sb;
                  float y = under.at(x, z);
                  return new Vector3f(x, (Float.isNaN(y) ? 0.0F : y) + this.radius(t) + 0.01F, z);
               }

               @Override
               public float radius(float t) {
                  float sb = Mth.sin((float) Math.PI * t);
                  float x = sx + dx * t + px * bend * sb;
                  float z = sz + dz * t + pz * bend * sb;
                  return Float.isNaN(under.at(x, z)) ? 0.0F : hs * (0.45F + 0.55F * t);
               }
            };
            WaterBlocks.chain(
               pose,
               vc,
               water,
               foam,
               WaterBlocks.Chain.along(spine, len, gap)
                  .flowing(time * speed + WaterMesh.noise01(seed, i, 3) * gap)
                  .rough(cube * 0.12F, 0.0F)
                  .foamed(0.2F)
                  .aligned(false)
                  .seeded(seed + i * 17)
            );
         }
      }
   }

   @FunctionalInterface
   interface Floor {
      float at(float var1, float var2);
   }

   static record Tune(float inflow, float streamCube, float pool, float poolAlpha, float poolCube) {
      static TsunamiPourGround.Tune now() {
         return new TsunamiPourGround.Tune(
            TsunamiPourGround.INFLOW_RADIUS.getF(),
            TsunamiPourGround.STREAM_WIDTH.getF(),
            TsunamiPourGround.POOL_RADIUS.getF(),
            TsunamiPourGround.POOL_ALPHA.getF(),
            TsunamiPourGround.POOL_CUBE.getF()
         );
      }
   }
}
