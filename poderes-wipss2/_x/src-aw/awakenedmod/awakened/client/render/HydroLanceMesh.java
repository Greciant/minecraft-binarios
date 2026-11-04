package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public final class HydroLanceMesh {
   private static final Lab.Value CUBE = Lab.CLIENT.def("hydro.lance.cube", 1.0, 0.5, 2.0, 0.05);
   private static final Lab.Value CHURN = Lab.CLIENT.def("hydro.lance.churn", 1.0, 0.0, 3.0, 0.05);
   private static final Lab.Value GAP = Lab.CLIENT.def("hydro.lance.gap", 1.6, 0.8, 3.0, 0.05);
   public static final float LEN_TAP = 1.7F;
   public static final float LEN_FULL = 2.4F;
   private static final float SHAFT_TAP = 0.055F;
   private static final float SHAFT_FULL = 0.08F;
   private static final float HEAD_MUL = 1.55F;
   private static final float HEAD_LEN = 0.55F;
   private static final float DRY_THIN = 0.8F;
   public static final float GRIP_BACK = 0.32F;
   private static final float[] HEAD_STEPS = new float[]{0.75F, 1.0F, 0.72F, 0.48F, 0.3F};
   private static final float HEAD_PACK = 0.85F;
   private static final float TIP_FOAM = 0.8F;
   private static final float FLECK_FOAM = 0.35F;
   private static final float FLECK_SHARE = 0.18F;
   private static final float WET_OPACITY = 0.85F;
   private static final int DRY_RGB = 10474741;
   private static final float DRY_BOOST = 1.25F;
   private static final float FLOW_SPEED = 0.06F;

   private HydroLanceMesh() {
   }

   public static float crumbled(float drained) {
      float d = Mth.clamp(drained, 0.0F, 1.0F);
      return 1.0F - (1.0F - d) * (1.0F - d);
   }

   public static void spear(Pose pose, VertexConsumer vc, HydroLanceMesh.Paints paints, HydroLanceMesh.Dims d, int seed, final float time) {
      float len = d.length();
      if (!(len <= 0.02F)) {
         final float phase = WaterMesh.noise01(seed, 0, 11) * (float) (Math.PI * 2);
         float z = 0.0F;
         float prev = 0.0F;
         float neck = 0.0F;
         int last = HEAD_STEPS.length - 1;

         for (int i = last; i >= 0; i--) {
            float hs = d.head() * HEAD_STEPS[i] * (1.0F + 0.05F * Mth.sin(time * 0.5F + (float)i * 1.7F + phase));
            float cz = i == last ? -hs : z - (hs + prev) * 0.85F;
            if (-cz + hs * 0.5F > len) {
               break;
            }

            WaterBlocks.cube(pose, vc, paints.water(), i >= last - 1 ? paints.foam() : null, 0.0F, 0.0F, cz, hs);
            z = cz;
            prev = hs;
            neck = -cz;
         }

         final float shaftLen = len - neck;
         if (!(shaftLen <= 0.02F)) {
            final float hs = d.shaft();
            final float churn = CHURN.getF();
            final float from = neck;
            WaterMesh.Spine spine = new WaterMesh.Spine() {
               @Override
               public Vector3f center(float t) {
                  float zz = -from - shaftLen * t;
                  float sway = 0.01F * churn * Mth.sin(zz * 4.5F + time * 0.45F + phase) * t;
                  return new Vector3f(sway, 0.6F * sway, zz);
               }

               @Override
               public float radius(float t) {
                  float s = shaftLen * t;
                  float r = hs * (1.0F + 0.12F * Mth.sin(s * 7.5F - time * 0.8F + phase));
                  float fromButt = shaftLen - s;
                  if (fromButt < 0.25F) {
                     r *= 0.55F + 0.45F * (fromButt / 0.25F);
                  }

                  return r;
               }
            };
            WaterBlocks.chain(
               pose,
               vc,
               paints.water(),
               paints.fleck(),
               WaterBlocks.Chain.along(spine, shaftLen, hs * GAP.getF()).flowing(time * 0.06F).rough(0.012F * churn, 16.0F * churn).foamed(0.18F).seeded(seed)
            );
         }
      }
   }

   public static void trail(Pose pose, VertexConsumer vc, HydroLanceMesh.Paints paints, HydroLanceMesh.Dims d, int seed, float time, float speed) {
      float butt = -d.length();
      float hs = d.shaft();
      float wake = 0.45F + 0.35F * Math.min(3.0F, speed);
      WaterMesh.Spine peel = WaterMesh.Spine.line(
         new Vector3f(0.0F, 0.0F, butt - hs), new Vector3f(0.0F, 0.0F, butt - wake), t -> hs * 0.85F * (1.0F - 0.65F * t)
      );
      HydroLanceMesh.Paints faint = paints.faded(0.7F);
      WaterBlocks.chain(
         pose,
         vc,
         faint.water(),
         faint.fleck(),
         WaterBlocks.Chain.along(peel, wake - hs, hs * 2.6F).flowing(time * (0.1F + 0.2F * speed)).rough(0.035F, 40.0F).foamed(0.3F).seeded(seed + 101)
      );
   }

   public static void gather(Pose pose, VertexConsumer vc, HydroLanceMesh.Paints paints, HydroLanceMesh.Dims d, int seed, float time, float form, float reach) {
      int drops = 12;
      float cube = CUBE.getF();

      for (int k = 0; k < drops; k++) {
         float period = 5.0F + 3.0F * WaterMesh.noise01(seed, k, 30);
         float u = time / period + WaterMesh.noise01(seed, k, 31);
         int pass = Mth.floor(u);
         float q = (u - (float)pass) / 0.85F;
         if (!(q >= 1.0F)) {
            int s = seed + pass * 7919;
            if (!(WaterMesh.noise01(s, k, 37) < form * 0.6F)) {
               float theta = WaterMesh.noise01(s, k, 32) * (float) (Math.PI * 2);
               float phi = (WaterMesh.noise01(s, k, 33) - 0.5F) * 2.0F;
               float far = reach * (0.6F + 0.4F * WaterMesh.noise01(s, k, 34));
               float onZ = -d.length() * (0.15F + 0.75F * WaterMesh.noise01(s, k, 35));
               Vector3f from = new Vector3f(Mth.cos(theta) * Mth.cos(phi) * far, Mth.sin(phi) * far, onZ + Mth.sin(theta) * Mth.cos(phi) * far);
               Vector3f onto = new Vector3f(0.0F, 0.0F, onZ);
               float alpha = paints.water().opacity() * Math.min(1.0F, q * 6.0F);
               float size = (0.03F + 0.015F * WaterMesh.noise01(s, k, 38)) * (1.0F - 0.3F * q) * cube;

               for (int j = 0; j < 3; j++) {
                  float qj = q - 0.14F * (float)j;
                  if (qj <= 0.0F) {
                     break;
                  }

                  Vector3f at = new Vector3f(from).lerp(onto, qj * qj * qj);
                  WaterBlocks.cube(pose, vc, paints.water().alpha(alpha * (1.0F - 0.3F * (float)j)), null, at.x, at.y, at.z, size * (1.0F - 0.28F * (float)j));
               }
            }
         }
      }
   }

   public static record Dims(float length, float shaft, float head, float headLen) {
      public static HydroLanceMesh.Dims of(float charge, boolean wet, float form) {
         return of(charge, wet, form, 0.35F);
      }

      public static HydroLanceMesh.Dims of(float charge, boolean wet, float form, float startLength) {
         float grow = Mth.lerp(form, startLength, 1.0F);
         float thick = Mth.lerp(form, 0.55F, 1.0F) * (wet ? 1.0F : 0.8F) * HydroLanceMesh.CUBE.getF();
         float shaft = Mth.lerp(charge, 0.055F, 0.08F) * thick;
         return new HydroLanceMesh.Dims(Mth.lerp(charge, 1.7F, 2.4F) * grow, shaft, shaft * 1.55F, 0.55F * Mth.lerp(form, 0.6F, 1.0F));
      }

      public HydroLanceMesh.Dims scaled(float k) {
         return new HydroLanceMesh.Dims(this.length * k, this.shaft * k, this.head * k, this.headLen * k);
      }

      public HydroLanceMesh.Dims drain(float drained) {
         return new HydroLanceMesh.Dims(
            this.length * (1.0F - HydroLanceMesh.crumbled(drained)), this.shaft * (1.0F - 0.3F * drained), this.head * (1.0F - 0.3F * drained), this.headLen
         );
      }
   }

   public static record Paints(WaterMesh.Paint water, WaterMesh.Paint foam, WaterMesh.Paint fleck) {
      public static HydroLanceMesh.Paints of(boolean wet, float fade, int light) {
         WaterMesh.Paint base = HydroLook.paint(wet, fade, light);
         WaterMesh.Paint water = wet ? base.alpha(base.opacity() * 0.85F) : base.tint(10474741).alpha(Math.min(1.0F, base.opacity() * 1.25F));
         float alpha = water.opacity();
         return new HydroLanceMesh.Paints(water, water.foam(Math.min(1.0F, alpha * 0.8F)), water.foam(alpha * 0.35F));
      }

      public HydroLanceMesh.Paints faded(float k) {
         return new HydroLanceMesh.Paints(
            this.water.alpha(this.water.opacity() * k), this.foam.alpha(this.foam.opacity() * k), this.fleck.alpha(this.fleck.opacity() * k)
         );
      }
   }
}
