package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.util.Mth;

final class SurfBlockWave {
   static final float PLATEAU = 0.45F;
   static final float SHELF = 0.6F;
   private static final float CALM_HALF_WIDTH = 0.6F;
   static final float LIP_TIP_DEG = 100.0F;
   private static final float LIP_GROW = 0.3F;

   private SurfBlockWave() {
   }

   static WaterCubeGrid build(SurfBlockWave.Shape s) {
      int rows = s.rows();
      int cols = s.cols();
      float c = s.cube();
      int gMax = Mth.floor(s.scroll() / c);
      float frac = s.scroll() - (float)gMax * c;
      WaterCubeGrid g = new WaterCubeGrid(cols, rows, c, -s.base());

      for (int k = 0; k < cols; k++) {
         g.colX(k, s.colX(k));
      }

      for (int r = 0; r < rows; r++) {
         float z = s.top() - frac - (float)r * c;
         g.rowZ(r, z);
         int id = gMax - r;
         boolean shelf = s.onShelf(z);
         boolean flat = shelf || s.onCrest(z);

         for (int k = 0; k < cols; k++) {
            float x = s.colX(k);
            float target = s.heightAt(x, z);
            if (!(target <= 0.01F)) {
               float wave = Mth.sin(s.time() * s.boilRate() + (float) (Math.PI * 2) * WaterMesh.noise01(s.seed(), id, k));
               if (!flat) {
                  target += c * s.boil() * Math.min(1.0F, target / Math.max(0.1F, s.height()) * 3.0F) * wave;
               } else {
                  boolean calm = shelf && Math.abs(x) < 0.6F;
                  target += c * s.boil() * (calm ? 0.15F : 1.0F) * 0.5F * (wave - 1.0F);
               }

               g.setStepped(k, r, target, s.stepsFrom(x, z), s.snap());
            }
         }
      }

      return g;
   }

   static void drawFoam(Pose pose, VertexConsumer vc, SurfBlockWave.Shape s, WaterCubeGrid pile, WaterMesh.Paint foam) {
      float capFrom = s.crestZ() - 0.225F;
      float capTo = s.crestZ() + 0.45F + s.cube();
      pile.drawFoam(pose, vc, foam, (k, r, h) -> {
         float z = pile.z(r);
         return z > capFrom && z < capTo && h > 0.7F * s.crestAt(pile.x(k)) ? 0.6F : 0.0F;
      });
   }

   static float lipBeat(SurfBlockWave.Shape s, int k) {
      return WaterBlocks.lipBeat(s.breakPhase(), s.seed(), k);
   }

   static boolean hasLip(SurfBlockWave.Shape s, int k) {
      return s.across(s.colX(k)) > 0.0F && s.crestAt(s.colX(k)) > s.cube();
   }

   static float lipSize(SurfBlockWave.Shape s, float beat) {
      return WaterBlocks.lipSize(s.cube(), beat, 0.3F);
   }

   static float lipTip(float beat) {
      return WaterBlocks.lipTip(beat, 0.3F, 100.0F);
   }

   static float[] lipPivot(SurfBlockWave.Shape s, int k) {
      return new float[]{s.colX(k), -s.base() + s.crestAt(s.colX(k)) - 0.05F, s.crestZ() + 0.45F + s.lip() * s.cube()};
   }

   static void drawLips(PoseStack poseStack, VertexConsumer vc, SurfBlockWave.Shape s, WaterMesh.Paint water) {
      for (int k = 0; k < s.cols(); k++) {
         if (hasLip(s, k)) {
            float beat = lipBeat(s, k);
            float size = lipSize(s, beat);
            if (!(size < 0.03F)) {
               float[] p = lipPivot(s, k);
               WaterBlocks.tippedCube(poseStack, vc, water, water.foam(water.opacity() * 0.55F), p[0], p[1], p[2], size, lipTip(beat));
            }
         }
      }
   }

   static void drawCurtain(
      Pose pose, VertexConsumer vc, SurfBlockWave.Shape s, WaterMesh.Paint water, float top, float bottom, float fall, float trail, boolean grounded
   ) {
      float c = s.cube();
      float gap = c * 1.35F;
      float span = top - bottom;
      if (!(span <= 0.1F)) {
         int lanes = Math.max(2, Math.round(2.0F * s.halfWidth() * 0.6F / c));
         float hx = c * 0.45F;
         float hy = c * 0.6F;

         for (int l = 0; l < lanes; l++) {
            float x = ((float)l - (float)(lanes - 1) * 0.5F) * c;
            float off = WaterBlocks.flowOffset(s.time(), fall, gap, WaterMesh.noise01(s.seed(), l, 13) * gap);

            for (float d = off; d < span; d += gap) {
               float t = d / span;
               float y = top - d;
               float z = s.crestZ() - 0.225F - trail * t * t;
               float wobble = 0.06F * Mth.sin(s.time() * 0.7F + (float)l * 2.1F + d);
               float fade = grounded ? 1.0F : 1.0F - t;
               if (!(fade < 0.05F)) {
                  WaterMesh.boxCentered(pose, vc, water.alpha(water.opacity() * fade), x + wobble, y - hy, z, hx, hy, hx);
               }
            }
         }
      }
   }

   static record Shape(
      float cube,
      float height,
      float footing,
      float base,
      float crestZ,
      float front,
      float back,
      float maxBack,
      float halfWidth,
      float maxHalfWidth,
      float scroll,
      float bank,
      float boil,
      float boilRate,
      float snap,
      float lip,
      float breakPhase,
      float time,
      int seed
   ) {
      float top() {
         return this.faceFrom() + this.front + this.cube;
      }

      int rows() {
         return (int)Math.ceil((double)((this.top() - (this.crestZ - 0.45F - this.maxBack - this.cube)) / this.cube)) + 1;
      }

      private float faceFrom() {
         return Math.max(0.6F, this.crestZ + 0.45F);
      }

      boolean onShelf(float z) {
         return Math.abs(z) <= 0.6F;
      }

      boolean onCrest(float z) {
         return !this.onShelf(z) && Math.abs(z - this.crestZ) <= 0.45F;
      }

      float stepsFrom(float x, float z) {
         float c0 = this.crestZ + 0.45F;
         return c0 < -0.6F && z > c0 ? this.footing : this.crestAt(x);
      }

      int cols() {
         return 2 * (int)Math.ceil((double)(this.maxHalfWidth / this.cube)) + 1;
      }

      float colX(int k) {
         return (float)(k - this.cols() / 2) * this.cube;
      }

      float heightAt(float x, float z) {
         float crest = this.crestAt(x);
         float foot = this.footing * this.across(x);
         float c0 = this.crestZ + 0.45F;
         float c1 = this.crestZ - 0.45F;
         float from = this.faceFrom();
         if (z > from) {
            float t = (from + this.front - z) / this.front;
            return t <= 0.0F ? 0.0F : (c0 >= 0.6F ? crest : foot) * (float)Math.pow((double)Math.min(1.0F, t), 0.6);
         } else if (this.onShelf(z)) {
            return foot;
         } else if (z > c0) {
            float u = (-0.6F - z) / (-0.6F - c0);
            return foot + (crest - foot) * u * u * (3.0F - 2.0F * u);
         } else if (z >= c1) {
            return crest;
         } else {
            float t = (z - (c1 - this.back)) / this.back;
            return t <= 0.0F ? 0.0F : crest * t * t * (3.0F - 2.0F * t);
         }
      }

      float across(float x) {
         float e = Math.abs(x) / this.halfWidth;
         return e > 1.0F + 0.5F * this.cube / this.halfWidth ? 0.0F : Math.max(0.3F, 1.0F - 0.6F * e * e);
      }

      float crestAt(float x) {
         return this.height * this.across(x) * (1.0F + this.bank * Mth.clamp(x / this.halfWidth, -1.0F, 1.0F));
      }
   }
}
