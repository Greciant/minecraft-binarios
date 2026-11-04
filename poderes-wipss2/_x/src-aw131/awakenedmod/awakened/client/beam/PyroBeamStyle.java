package com.awakenedmod.awakened.client.beam;

import com.awakenedmod.awakened.client.PyroSourceAnchors;
import com.awakenedmod.awakened.client.render.VoxelFlameDraw;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public interface PyroBeamStyle {
   void emit(PyroBeamSim.Emitter var1);

   void impact(PyroBeamSim.Emitter var1, byte var2);

   void draw(PyroBeamStyle.Frame var1, PyroBeamSim.Buffer var2);

   public static record Frame(
      Pose pose,
      VertexConsumer vc,
      float partialTick,
      boolean channeling,
      PyroSourceAnchors.Nozzles nozzles,
      VoxelFlameDraw.Palette palette,
      float time,
      int seed,
      Vector3f[] camAxes,
      Vec3 cam,
      boolean nearFade
   ) {
      private static final float FADE_NEAR = 0.5F;
      private static final float FADE_FAR = 1.2F;
      private static final float BREATHE = 0.22F;
      private static final float BREATHE_AMT = 0.18F;

      public float nearFade(float x, float y, float z) {
         if (!this.nearFade) {
            return 1.0F;
         } else {
            float dx = (float)((double)x - this.cam.x);
            float dy = (float)((double)y - this.cam.y);
            float dz = (float)((double)z - this.cam.z);
            float d2 = dx * dx + dy * dy + dz * dz;
            return d2 >= 1.44F ? 1.0F : Mth.clamp((Mth.sqrt(d2) - 0.5F) / 0.70000005F, 0.0F, 1.0F);
         }
      }

      public Vec3 palmLeft() {
         return this.nozzles == null ? null : this.nozzles.left();
      }

      public Vec3 palmRight() {
         return this.nozzles == null ? null : this.nozzles.right();
      }

      public int[] tiers() {
         return this.palette.base();
      }

      public int[] tiers(int particleSeed) {
         return this.palette.pick(particleSeed);
      }

      public float breathe() {
         return 1.0F + 0.18F * Mth.sin(this.time * 0.22F + (float)(this.seed & 0xFF) * 0.19F);
      }

      public Vec3 palm(boolean left) {
         return left ? this.palmLeft() : this.palmRight();
      }
   }
}
