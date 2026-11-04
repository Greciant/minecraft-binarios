package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

class GhostVertexConsumer implements VertexConsumer {
   protected final VertexConsumer inner;
   protected final float alpha;
   protected final float red;
   protected final float green;
   protected final float blue;

   GhostVertexConsumer(VertexConsumer inner, float alpha) {
      this(inner, alpha, 1.0F, 1.0F, 1.0F);
   }

   GhostVertexConsumer(VertexConsumer inner, float alpha, float red, float green, float blue) {
      this.inner = inner;
      this.alpha = alpha;
      this.red = red;
      this.green = green;
      this.blue = blue;
   }

   public VertexConsumer addVertex(float x, float y, float z) {
      this.inner.addVertex(x, y, z);
      return this;
   }

   public VertexConsumer setColor(int r, int g, int b, int a) {
      this.inner.setColor((int)((float)r * this.red), (int)((float)g * this.green), (int)((float)b * this.blue), (int)((float)a * this.alpha));
      return this;
   }

   public VertexConsumer setUv(float u, float v) {
      this.inner.setUv(u, v);
      return this;
   }

   public VertexConsumer setUv1(int u, int v) {
      this.inner.setUv1(u, v);
      return this;
   }

   public VertexConsumer setUv2(int u, int v) {
      this.inner.setUv2(u, v);
      return this;
   }

   public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
      this.inner.setNormal(normalX, normalY, normalZ);
      return this;
   }
}
