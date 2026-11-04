package com.devilfruits.fruits.kage_kage.client.vfx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class RimConsumer implements VertexConsumer {
   private final float[] buf = new float[24];
   private int n;
   private VertexConsumer target;
   private float thickness;
   private int argb;

   public RimConsumer to(VertexConsumer target, float thickness, int rgb, float alpha) {
      this.target = target;
      this.thickness = thickness;
      this.argb = (int)(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) << 24 | rgb & 16777215;
      this.n = 0;
      return this;
   }

   public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
      int o = this.n * 6;
      this.buf[o] = x + nx * this.thickness;
      this.buf[o + 1] = y + ny * this.thickness;
      this.buf[o + 2] = z + nz * this.thickness;
      this.buf[o + 3] = nx;
      this.buf[o + 4] = ny;
      this.buf[o + 5] = nz;
      if (++this.n == 4) {
         for (int i = 3; i >= 0; i--) {
            int b = i * 6;
            this.target
               .addVertex(
                  this.buf[b],
                  this.buf[b + 1],
                  this.buf[b + 2],
                  this.argb,
                  0.5F,
                  0.5F,
                  OverlayTexture.NO_OVERLAY,
                  15728880,
                  -this.buf[b + 3],
                  -this.buf[b + 4],
                  -this.buf[b + 5]
               );
         }

         this.n = 0;
      }
   }

   public VertexConsumer addVertex(float x, float y, float z) {
      this.addVertex(x, y, z, -1, 0.0F, 0.0F, 0, 0, 0.0F, 1.0F, 0.0F);
      return this;
   }

   public VertexConsumer setColor(int r, int g, int b, int a) {
      return this;
   }

   public VertexConsumer setUv(float u, float v) {
      return this;
   }

   public VertexConsumer setUv1(int u, int v) {
      return this;
   }

   public VertexConsumer setUv2(int u, int v) {
      return this;
   }

   public VertexConsumer setNormal(float x, float y, float z) {
      return this;
   }
}
