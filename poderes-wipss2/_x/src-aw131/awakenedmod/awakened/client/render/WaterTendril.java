package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

final class WaterTendril {
   static final Lab.Value CUBE = Lab.CLIENT.def("hydro.hook.cube", 0.7, 0.2, 1.5, 0.05);
   static final Lab.Value GAP = Lab.CLIENT.def("hydro.hook.gap", 0.2, 0.08, 0.6, 0.01);
   static final Lab.Value FLOW = Lab.CLIENT.def("hydro.hook.flow", 0.3, 0.0, 1.5, 0.01);
   private static final float DRIP_PERIOD = 9.0F;
   private static final int DRIPS = 4;
   private static final float TREMBLE_RATE = (float) (Math.PI * 3.0 / 10.0);
   private static final float HAND_END = 0.25F;
   private static final float FOAM_SHARE = 0.2F;
   private static final float FOAM_ALPHA = 0.5F;

   private WaterTendril() {
   }

   static void draw(Pose pose, VertexConsumer vc, WaterMesh.Paint water, Vector3f from, Vector3f to, float radius, float reach, float time, int seed) {
      draw(pose, vc, water, from, to, radius, reach, time, seed, WaterTendril.Strain.SLACK);
   }

   static void draw(
      Pose pose,
      VertexConsumer vc,
      WaterMesh.Paint water,
      Vector3f from,
      Vector3f to,
      float radius,
      float reach,
      final float time,
      int seed,
      final WaterTendril.Strain strain
   ) {
      final float len = from.distance(to);
      if (!(len < 0.15F) && !(reach <= 0.01F)) {
         float sag = Math.min(1.1F, len * strain.sag()) * (0.6F + 0.4F * reach);
         Vector3f bow = new Vector3f(strain.bow()).add(0.0F, -sag, 0.0F);
         final WaterMesh.Spine arc = WaterMesh.Spine.arc(from, to, bow, t -> 0.0F);
         final float phase = WaterMesh.noise01(seed, 0, 1) * (float) (Math.PI * 2);
         final float drawn = Math.min(1.0F, reach);
         final float hs0 = radius * CUBE.getF();
         float dx = to.x - from.x;
         float dz = to.z - from.z;
         float flat = Mth.sqrt(dx * dx + dz * dz);
         final float sx = flat < 0.001F ? 1.0F : -dz / flat;
         final float sz = flat < 0.001F ? 0.0F : dx / flat;
         WaterMesh.Spine body = new WaterMesh.Spine() {
            @Override
            public Vector3f center(float u) {
               float t = u * drawn;
               Vector3f c = arc.center(t);
               float env = Mth.sin((float) Math.PI * t);
               c.y = c.y + strain.wave() * env * Mth.sin(t * len * 1.3F - time * 0.55F + phase);
               if (strain.tremble() > 0.0F) {
                  float shake = strain.tremble()
                     * env
                     * (Mth.sin(time * (float) (Math.PI * 3.0 / 10.0) + phase) + 0.4F * Mth.sin(time * (float) (Math.PI * 3.0 / 10.0) * 2.7F + t * 5.0F));
                  c.add(sx * shake, 0.5F * shake, sz * shake);
               }

               return c;
            }

            @Override
            public float radius(float u) {
               float t = u * drawn;
               float shape = 0.8F + 0.35F * t;
               if (strain.neck()) {
                  shape *= 0.8F + 0.3F * Math.abs(2.0F * t - 1.0F);
               }

               if (strain.handSwell() != 1.0F) {
                  shape *= 1.0F + (strain.handSwell() - 1.0F) * Math.max(0.0F, 1.0F - t / 0.25F);
               }

               return hs0 * shape;
            }
         };
         WaterMesh.Paint foam = water.foam(water.opacity() * 0.5F);
         WaterBlocks.chain(
            pose,
            vc,
            water,
            foam,
            WaterBlocks.Chain.along(body, len * drawn, GAP.getF()).flowing(time * FLOW.getF()).rough(0.015F, 25.0F).foamed(0.2F).seeded(seed)
         );
         if (reach < 1.0F) {
            Vector3f tip = body.center(1.0F);
            WaterBlocks.cube(pose, vc, water, foam, tip.x, tip.y, tip.z, hs0 * 1.6F);
         } else {
            for (int i = 0; i < 4; i++) {
               float at = 0.2F + 0.6F * WaterMesh.noise01(seed, i, 2);
               float cyc = (time / 9.0F + WaterMesh.noise01(seed, i, 3)) % 1.0F;
               Vector3f c = body.center(at);
               float fall = 1.6F * cyc * cyc;
               float hs = hs0 * 0.5F * (1.0F - 0.5F * cyc);
               WaterBlocks.cube(pose, vc, water, null, c.x, c.y - hs0 - fall, c.z, hs);
            }
         }
      }
   }

   static record Strain(float sag, float wave, float tremble, boolean neck, Vector3f bow, float handSwell) {
      static final WaterTendril.Strain SLACK = new WaterTendril.Strain(0.07F, 0.07F, 0.0F, false, new Vector3f(), 1.0F);

      static WaterTendril.Strain taut(Vector3f bow, float handSwell) {
         return new WaterTendril.Strain(0.015F, 0.015F, 0.02F, true, bow, handSwell);
      }
   }
}
