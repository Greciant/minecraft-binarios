package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.Arrays;
import net.minecraft.util.Mth;

public final class WaterCubeGrid {
   private static final float MIN_HEIGHT = 0.02F;
   private final int cols;
   private final int rows;
   private final float cube;
   private final float base;
   private final float[] xs;
   private final float[] zs;
   private final float[][] h;
   private final float[][] flash;
   private float[][] floors;

   public WaterCubeGrid(int cols, int rows, float cube, float base) {
      this.cols = Math.max(1, cols);
      this.rows = Math.max(1, rows);
      this.cube = cube;
      this.base = base;
      this.xs = new float[this.cols];
      this.zs = new float[this.rows];
      this.h = new float[this.cols][this.rows];
      this.flash = new float[this.cols][this.rows];
   }

   public static WaterCubeGrid centered(int cols, int rows, float cube, float base) {
      WaterCubeGrid g = new WaterCubeGrid(cols, rows, cube, base);

      for (int k = 0; k < g.cols; k++) {
         g.xs[k] = (float)(k - g.cols / 2) * cube;
      }

      for (int r = 0; r < g.rows; r++) {
         g.zs[r] = (float)(r - g.rows / 2) * cube;
      }

      return g;
   }

   public int cols() {
      return this.cols;
   }

   public int rows() {
      return this.rows;
   }

   public float cube() {
      return this.cube;
   }

   public float base() {
      return this.base;
   }

   public void colX(int k, float x) {
      this.xs[k] = x;
   }

   public void rowZ(int r, float z) {
      this.zs[r] = z;
   }

   public float x(int k) {
      return this.xs[k];
   }

   public float z(int r) {
      return this.zs[r];
   }

   public float height(int k, int r) {
      return k >= 0 && r >= 0 && k < this.cols && r < this.rows ? this.h[k][r] : 0.0F;
   }

   public float flash(int k, int r) {
      return this.flash[k][r];
   }

   public void floor(int k, int r, float y) {
      if (this.floors == null) {
         this.floors = new float[this.cols][this.rows];

         for (float[] col : this.floors) {
            Arrays.fill(col, this.base);
         }
      }

      this.floors[k][r] = y;
   }

   public float floor(int k, int r) {
      return this.floors == null ? this.base : this.floors[k][r];
   }

   private float coveredTo(int k, int r, float from) {
      float hn = this.height(k, r);
      return hn <= 0.0F ? from : Math.max(from, this.floor(k, r) + hn);
   }

   public void set(int k, int r, float height) {
      this.h[k][r] = height > 0.02F ? height : 0.0F;
   }

   public void flash(int k, int r, float amount) {
      this.flash[k][r] = amount;
   }

   public void setStepped(int k, int r, float target, float top, float snap) {
      float depth = (top - target) / this.cube;
      int n = Mth.floor(depth);
      float step = WaterBlocks.snapStep(depth - (float)n, snap);
      float hq = top - this.cube * ((float)n + step);
      if (hq > 0.02F) {
         this.h[k][r] = hq;
         this.flash[k][r] = WaterBlocks.slideFlash(step);
      }
   }

   public void drawWater(Pose pose, VertexConsumer vc, WaterMesh.Paint water, boolean underside) {
      this.emit(pose, vc, water, 0, 0, 0.0F, underside);
   }

   public void drawWater(Pose pose, VertexConsumer vc, WaterMesh.Paint water, int deepRgb, int crestRgb, float shadeTop, boolean underside) {
      this.emit(pose, vc, water, deepRgb, crestRgb, Math.max(0.1F, shadeTop), underside);
   }

   private void emit(Pose pose, VertexConsumer vc, WaterMesh.Paint water, int deepRgb, int crestRgb, float shadeTop, boolean underside) {
      float half = this.cube * 0.5F;
      boolean rowsUp = this.rows < 2 || this.zs[1] >= this.zs[0];
      boolean colsUp = this.cols < 2 || this.xs[1] >= this.xs[0];
      int prevRow = rowsUp ? 8 : 4;
      int nextRow = rowsUp ? 4 : 8;
      int nextCol = colsUp ? 16 : 32;
      int prevCol = colsUp ? 32 : 16;
      int caps = 1 | (underside ? 2 : 0);

      for (int k = 0; k < this.cols; k++) {
         float x = this.xs[k];

         for (int r = 0; r < this.rows; r++) {
            float hq = this.h[k][r];
            if (!(hq <= 0.0F)) {
               float z = this.zs[r];
               WaterMesh.Paint paint = shadeTop > 0.0F ? water.tint(WaterMesh.mix(deepRgb, crestRgb, hq / shadeTop)) : water;
               float floor = this.floor(k, r);
               float top = floor + hq;
               WaterMesh.box(pose, vc, paint, x - half, floor, z - half, x + half, top, z + half, caps);
               this.side(pose, vc, paint, x, z, half, top, this.coveredTo(k, r - 1, floor), prevRow);
               this.side(pose, vc, paint, x, z, half, top, this.coveredTo(k, r + 1, floor), nextRow);
               this.side(pose, vc, paint, x, z, half, top, this.coveredTo(k + 1, r, floor), nextCol);
               this.side(pose, vc, paint, x, z, half, top, this.coveredTo(k - 1, r, floor), prevCol);
            }
         }
      }
   }

   private void side(Pose pose, VertexConsumer vc, WaterMesh.Paint paint, float x, float z, float half, float top, float from, int face) {
      if (top - from > 0.01F) {
         WaterMesh.box(pose, vc, paint, x - half, from, z - half, x + half, top, z + half, face);
      }
   }

   public void drawFoam(Pose pose, VertexConsumer vc, WaterMesh.Paint foam, WaterCubeGrid.Cap cap) {
      float half = this.cube * 0.5F - 0.02F;
      float a0 = foam.opacity();

      for (int k = 0; k < this.cols; k++) {
         float x = this.xs[k];

         for (int r = 0; r < this.rows; r++) {
            float hq = this.h[k][r];
            if (!(hq <= 0.0F)) {
               float a = a0 * Math.min(1.0F, cap.at(k, r, hq) + this.flash[k][r]);
               if (!(a < 0.03F)) {
                  float z = this.zs[r];
                  float top = this.floor(k, r) + 0.01F + hq;
                  WaterMesh.box(pose, vc, foam.alpha(a), x - half, top - 0.02F, z - half, x + half, top, z + half, 1);
               }
            }
         }
      }
   }

   @FunctionalInterface
   public interface Cap {
      WaterCubeGrid.Cap NONE = (col, row, height) -> 0.0F;

      float at(int var1, int var2, float var3);
   }
}
