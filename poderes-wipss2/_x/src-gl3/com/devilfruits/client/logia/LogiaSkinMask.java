package com.devilfruits.client.logia;

import com.devilfruits.client.render.SkinPixels;
import com.mojang.blaze3d.platform.NativeImage;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

final class LogiaSkinMask implements AutoCloseable {
   private static final int GRID = 64;
   private static final int[] BAYER = new int[]{0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};
   private static final int MAX_SIZE = 512;
   final ResourceLocation skin;
   final boolean slim;
   final ResourceLocation location;
   private final DynamicTexture texture;
   private final int size;
   private final int scale;
   private final int[] source;
   private final byte[] limbOf = new byte[4096];
   private final float[] threshold = new float[4096];
   private long signature = Long.MIN_VALUE;

   private LogiaSkinMask(ResourceLocation skin, boolean slim, int size, int[] source) {
      this.skin = skin;
      this.slim = slim;
      this.size = size;
      this.scale = size / 64;
      this.source = source;
      byte[] along = new byte[4096];
      LogiaSkinLayout.classify(this.limbOf, along, slim);

      for (int y = 0; y < 64; y++) {
         for (int x = 0; x < 64; x++) {
            int i = y * 64 + x;
            float bayer = ((float)BAYER[(y & 3) * 4 + (x & 3)] + 0.5F) / 16.0F;
            float t = 0.62F * bayer + 0.38F * hash(x, y);
            int limb = this.limbOf[i];
            if (limb >= 2) {
               t += (0.5F - (float)(along[i] & 255) / 255.0F) * 0.5F;
            }

            this.threshold[i] = Math.max(0.02F, Math.min(0.98F, t));
         }
      }

      this.texture = new DynamicTexture(size, size, true);
      this.location = Minecraft.getInstance().getTextureManager().register("devilfruits_logia_skin", this.texture);
   }

   @Nullable
   static LogiaSkinMask create(ResourceLocation skin, boolean slim) {
      int[] pixels = SkinPixels.read(skin, 64, 512);
      return pixels == null ? null : new LogiaSkinMask(skin, slim, (int)Math.round(Math.sqrt((double)pixels.length)), pixels);
   }

   void update(long signature, float[] levels, float edgeBand, int edgeRgb, LogiaRenderState.Hole[] holes, int holeCount) {
      if (signature != this.signature) {
         this.signature = signature;
         NativeImage out = this.texture.getPixels();
         if (out != null) {
            int edge = 0xFF000000 | (edgeRgb & 0xFF) << 16 | edgeRgb & 0xFF00 | edgeRgb >> 16 & 0xFF;

            for (int y = 0; y < this.size; y++) {
               int gy = y / this.scale;

               for (int x = 0; x < this.size; x++) {
                  int gx = x / this.scale;
                  int i = gy * 64 + gx;
                  int c = this.source[y * this.size + x];
                  int limb = this.limbOf[i];
                  if (limb >= 0 && levels[limb] > 0.0F && c >>> 24 != 0) {
                     float thr = this.threshold[i];
                     if (levels[limb] >= thr) {
                        c = 0;
                     } else if (thr - levels[limb] < edgeBand) {
                        c = mix(c, edge);
                     }
                  }

                  out.setPixelRGBA(x, y, c);
               }
            }

            for (int h = 0; h < holeCount; h++) {
               this.cutHole(out, holes[h], edge);
            }

            this.texture.upload();
         }
      }
   }

   private void cutHole(NativeImage out, LogiaRenderState.Hole hole, int edge) {
      float r = hole.radius();
      if (!(r <= 0.0F)) {
         float ring = (r + 1.0F) * (r + 1.0F);
         float r2 = r * r;

         for (int[] face : LogiaSkinLayout.holeFaces(hole.limb, this.slim)) {
            float cx = (float)face[0] + (face[4] == 1 ? 1.0F - hole.across : hole.across) * (float)face[2];
            float cy = (float)face[1] + hole.down * (float)face[3];

            for (int gy = face[1]; gy < face[1] + face[3]; gy++) {
               for (int gx = face[0]; gx < face[0] + face[2]; gx++) {
                  float dx = (float)gx + 0.5F - cx;
                  float dy = (float)gy + 0.5F - cy;
                  float d2 = dx * dx + dy * dy;
                  if (d2 <= ring) {
                     this.fill(out, gx, gy, d2 <= r2, edge);
                  }
               }
            }
         }
      }
   }

   private void fill(NativeImage out, int gx, int gy, boolean cut, int edge) {
      for (int y = gy * this.scale; y < (gy + 1) * this.scale; y++) {
         for (int x = gx * this.scale; x < (gx + 1) * this.scale; x++) {
            int c = out.getPixelRGBA(x, y);
            if (cut) {
               out.setPixelRGBA(x, y, 0);
            } else if (c >>> 24 != 0) {
               out.setPixelRGBA(x, y, mix(c, edge));
            }
         }
      }
   }

   private static int mix(int skin, int edge) {
      int r = ((skin & 0xFF) * 2 + (edge & 0xFF) * 3) / 5;
      int g = ((skin >> 8 & 0xFF) * 2 + (edge >> 8 & 0xFF) * 3) / 5;
      int b = ((skin >> 16 & 0xFF) * 2 + (edge >> 16 & 0xFF) * 3) / 5;
      return skin & 0xFF000000 | b << 16 | g << 8 | r;
   }

   private static float hash(int x, int y) {
      int h = x * 374761393 + y * 668265263;
      h = (h ^ h >>> 13) * 1274126177;
      return (float)((h ^ h >>> 16) & 65535) / 65536.0F;
   }

   @Override
   public void close() {
      Minecraft.getInstance().getTextureManager().release(this.location);
   }
}
