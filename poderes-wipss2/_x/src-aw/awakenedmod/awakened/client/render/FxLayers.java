package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.compat.ShaderPacks;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class FxLayers {
   private static final int PACK_ALPHA_CUTOFF = 26;
   private static final Lab.Value LIGHT_GAIN = Lab.CLIENT.def("fx.pack.light_gain", 0.7, 0.2, 1.0, 0.05);

   private FxLayers() {
   }

   public static RenderType type(FxLayer layer) {
      return ShaderPacks.active() ? layer.underPack() : layer.vanilla();
   }

   public static VertexConsumer wrap(FxLayer layer, VertexConsumer vc) {
      return (VertexConsumer)(ShaderPacks.active() ? new FxLayers.TrianglesAsQuads(vc, layer.light, layer.light ? LIGHT_GAIN.getF() : 1.0F) : vc);
   }

   public static VertexConsumer begin(MultiBufferSource buffers, FxLayer layer) {
      return wrap(layer, buffers.getBuffer(type(layer)));
   }

   public static void end(BufferSource buffers, FxLayer layer) {
      buffers.endBatch(type(layer));
   }

   public static RenderType depthStamp() {
      return ShaderPacks.active() ? ModRenderTypes.PACK_DEPTH_STAMP : null;
   }

   public static VertexConsumer pushedBack(VertexConsumer vc, float distance) {
      return new FxLayers.PushedBack(vc, distance);
   }

   private static record PushedBack(VertexConsumer out, float distance) implements VertexConsumer {
      public VertexConsumer addVertex(float x, float y, float z) {
         float len = (float)Math.sqrt((double)(x * x + y * y + z * z));
         float k = len > 1.0E-4F && len < this.distance ? this.distance / len : 1.0F;
         this.out.addVertex(x * k, y * k, z * k);
         return this;
      }

      public VertexConsumer setColor(int r, int g, int b, int a) {
         this.out.setColor(r, g, b, a);
         return this;
      }

      public VertexConsumer setColor(int argb) {
         this.out.setColor(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24);
         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         this.out.setUv(u, v);
         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         this.out.setUv1(u, v);
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         this.out.setUv2(u, v);
         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         this.out.setNormal(x, y, z);
         return this;
      }

      public VertexConsumer setLineWidth(float width) {
         return this;
      }
   }

   private static final class TrianglesAsQuads implements VertexConsumer {
      private final VertexConsumer out;
      private final boolean light;
      private final float gain;
      private final float[] xyz = new float[9];
      private final int[] argb = new int[3];
      private int count;

      TrianglesAsQuads(VertexConsumer out, boolean light, float gain) {
         this.out = out;
         this.light = light;
         this.gain = gain;
      }

      public VertexConsumer addVertex(float x, float y, float z) {
         if (this.count == 3) {
            this.count = 0;
         }

         this.xyz[this.count * 3] = x;
         this.xyz[this.count * 3 + 1] = y;
         this.xyz[this.count * 3 + 2] = z;
         this.count++;
         return this;
      }

      public VertexConsumer setColor(int r, int g, int b, int a) {
         if (this.count == 0) {
            return this;
         } else {
            if (this.light) {
               a = Math.round((float)a * this.gain);
            }

            if (a > 0 && a < 26) {
               if (this.light) {
                  r = r * a / 26;
                  g = g * a / 26;
                  b = b * a / 26;
               }

               a = 26;
            }

            this.argb[this.count - 1] = a << 24 | r << 16 | g << 8 | b;
            if (this.count == 3) {
               this.emit(0);
               this.emit(1);
               this.emit(2);
               this.emit(2);
               this.count = 0;
            }

            return this;
         }
      }

      public VertexConsumer setColor(int argb) {
         return this.setColor(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24);
      }

      private void emit(int i) {
         int c = this.argb[i];
         this.out
            .addVertex(this.xyz[i * 3], this.xyz[i * 3 + 1], this.xyz[i * 3 + 2])
            .setColor(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF, c >>> 24)
            .setUv(0.5F, 0.5F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(this.light ? 15728880 : paintLight(c))
            .setNormal(0.0F, 1.0F, 0.0F);
      }

      private static int paintLight(int argb) {
         int max = Math.max(argb >> 16 & 0xFF, Math.max(argb >> 8 & 0xFF, argb & 0xFF));
         int block = Math.round((float)max * 15.0F / 255.0F);
         return LightTexture.pack(block, 15);
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

      public VertexConsumer setLineWidth(float width) {
         return this;
      }
   }
}
