package com.devilfruits.client.haki;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public final class CoatGeometry implements VertexConsumer {
   private float[] pos = new float[768];
   private float[] uv = new float[512];
   private float[] nrm = new float[768];
   private float[] corner = new float[768];
   private int count;
   private boolean cornersDone;
   private final Long2IntOpenHashMap slots = new Long2IntOpenHashMap();
   private float[] sums = new float[768];
   private int[] slotScratch = new int[256];
   private static final Map<TextureAtlasSprite, int[]> RIMS = new WeakHashMap<>();
   private float ox;
   private float oy;
   private float oz;
   private float ax;
   private float ay;
   private float az;
   private float bx;
   private float by;
   private float bz;

   public void reset() {
      this.count = 0;
      this.cornersDone = false;
   }

   public boolean isEmpty() {
      return this.count < 4;
   }

   public int vertexCount() {
      return this.count;
   }

   public VertexConsumer addVertex(float x, float y, float z) {
      if (this.count * 3 + 3 > this.pos.length) {
         int n = this.pos.length * 2;
         this.pos = Arrays.copyOf(this.pos, n);
         this.nrm = Arrays.copyOf(this.nrm, n);
         this.corner = Arrays.copyOf(this.corner, n);
         this.sums = Arrays.copyOf(this.sums, n);
         this.uv = Arrays.copyOf(this.uv, n / 3 * 2);
      }

      int i = this.count++;
      this.pos[i * 3] = x;
      this.pos[i * 3 + 1] = y;
      this.pos[i * 3 + 2] = z;
      this.uv[i * 2] = 0.0F;
      this.uv[i * 2 + 1] = 0.0F;
      this.nrm[i * 3] = 0.0F;
      this.nrm[i * 3 + 1] = 1.0F;
      this.nrm[i * 3 + 2] = 0.0F;
      this.cornersDone = false;
      return this;
   }

   public VertexConsumer setColor(int r, int g, int b, int a) {
      return this;
   }

   public VertexConsumer setUv(float u, float v) {
      if (this.count > 0) {
         this.uv[(this.count - 1) * 2] = u;
         this.uv[(this.count - 1) * 2 + 1] = v;
      }

      return this;
   }

   public VertexConsumer setUv1(int u, int v) {
      return this;
   }

   public VertexConsumer setUv2(int u, int v) {
      return this;
   }

   public VertexConsumer setNormal(float x, float y, float z) {
      if (this.count > 0) {
         int i = this.count - 1;
         this.nrm[i * 3] = x;
         this.nrm[i * 3 + 1] = y;
         this.nrm[i * 3 + 2] = z;
      }

      return this;
   }

   private void corners() {
      if (!this.cornersDone) {
         this.cornersDone = true;
         this.slots.clear();
         int used = 0;
         int[] slotOf = this.scratch(this.count);

         for (int i = 0; i < this.count; i++) {
            long key = key(this.pos[i * 3], this.pos[i * 3 + 1], this.pos[i * 3 + 2]);
            int slot = this.slots.getOrDefault(key, -1);
            if (slot < 0) {
               slot = used++;
               this.slots.put(key, slot);
               this.sums[slot * 3] = 0.0F;
               this.sums[slot * 3 + 1] = 0.0F;
               this.sums[slot * 3 + 2] = 0.0F;
            }

            slotOf[i] = slot;
            this.sums[slot * 3] = this.sums[slot * 3] + this.nrm[i * 3];
            this.sums[slot * 3 + 1] = this.sums[slot * 3 + 1] + this.nrm[i * 3 + 1];
            this.sums[slot * 3 + 2] = this.sums[slot * 3 + 2] + this.nrm[i * 3 + 2];
         }

         for (int i = 0; i < this.count; i++) {
            int s = slotOf[i];
            float x = this.sums[s * 3];
            float y = this.sums[s * 3 + 1];
            float z = this.sums[s * 3 + 2];
            float len = (float)Math.sqrt((double)(x * x + y * y + z * z));
            if (len < 1.0E-4F) {
               x = this.nrm[i * 3];
               y = this.nrm[i * 3 + 1];
               z = this.nrm[i * 3 + 2];
               len = 1.0F;
            }

            float scale = Math.min(1.8F, Math.max(1.0F, len)) / len;
            this.corner[i * 3] = x * scale;
            this.corner[i * 3 + 1] = y * scale;
            this.corner[i * 3 + 2] = z * scale;
         }
      }
   }

   private int[] scratch(int n) {
      if (this.slotScratch.length < n) {
         this.slotScratch = new int[Math.max(n, this.slotScratch.length * 2)];
      }

      return this.slotScratch;
   }

   private static long key(float x, float y, float z) {
      long kx = (long)Math.round(x * 1024.0F);
      long ky = (long)Math.round(y * 1024.0F);
      long kz = (long)Math.round(z * 1024.0F);
      return kx * 73856093L ^ ky * 19349663L ^ kz * 83492791L;
   }

   public void emit(VertexConsumer out, float inflate, boolean reverse, int argb, int light, int overlay, boolean upNormals) {
      this.corners();

      for (int q = 0; q + 3 < this.count; q += 4) {
         boolean flip = reverse && !this.mirrored(q);

         for (int k = 0; k < 4; k++) {
            int i = q + (flip ? (4 - k) % 4 : k);
            float x = this.pos[i * 3] + this.corner[i * 3] * inflate;
            float y = this.pos[i * 3 + 1] + this.corner[i * 3 + 1] * inflate;
            float z = this.pos[i * 3 + 2] + this.corner[i * 3 + 2] * inflate;
            if (upNormals) {
               out.addVertex(x, y, z, argb, this.uv[i * 2], this.uv[i * 2 + 1], overlay, light, 0.0F, 1.0F, 0.0F);
            } else {
               out.addVertex(x, y, z, argb, this.uv[i * 2], this.uv[i * 2 + 1], overlay, light, this.nrm[i * 3], this.nrm[i * 3 + 1], this.nrm[i * 3 + 2]);
            }
         }
      }
   }

   public boolean emitSpriteRim(VertexConsumer out, TextureAtlasSprite sprite, float width, int argb, int light, int overlay) {
      float su0 = sprite.getU0();
      float su1 = sprite.getU1();
      float sv0 = sprite.getV0();
      float sv1 = sprite.getV1();
      float eu = Math.abs(su1 - su0) * 0.02F;
      float ev = Math.abs(sv1 - sv0) * 0.02F;
      int front = -1;
      int back = -1;
      float best = 0.0F;

      for (int b = 0; b + 3 < this.count; b += 4) {
         if (this.spans(b, Math.min(su0, su1), Math.max(su0, su1), Math.min(sv0, sv1), Math.max(sv0, sv1), eu, ev)) {
            float a = this.area(b);
            if (a > best * 1.01F) {
               best = a;
               front = b;
            }
         }
      }

      if (front < 0) {
         return false;
      } else {
         for (int bx = 0; bx + 3 < this.count; bx += 4) {
            if (bx != front
               && this.area(bx) > best * 0.9F
               && this.nrm[bx * 3] * this.nrm[front * 3] + this.nrm[bx * 3 + 1] * this.nrm[front * 3 + 1] + this.nrm[bx * 3 + 2] * this.nrm[front * 3 + 2]
                  < -0.5F) {
               back = bx;
               break;
            }
         }

         int w = sprite.contents().width();
         int h = sprite.contents().height();
         float s0 = (this.uv[front * 2] - su0) / (su1 - su0) * (float)w;
         float t0 = (this.uv[front * 2 + 1] - sv0) / (sv1 - sv0) * (float)h;
         float s1 = (this.uv[(front + 1) * 2] - su0) / (su1 - su0) * (float)w - s0;
         float t1 = (this.uv[(front + 1) * 2 + 1] - sv0) / (sv1 - sv0) * (float)h - t0;
         float s3 = (this.uv[(front + 3) * 2] - su0) / (su1 - su0) * (float)w - s0;
         float t3 = (this.uv[(front + 3) * 2 + 1] - sv0) / (sv1 - sv0) * (float)h - t0;
         float det = s1 * t3 - s3 * t1;
         if (Math.abs(det) < 1.0E-6F) {
            return false;
         } else {
            int p0 = front * 3;
            int p1 = (front + 1) * 3;
            int p3 = (front + 3) * 3;
            float e1x = this.pos[p1] - this.pos[p0];
            float e1y = this.pos[p1 + 1] - this.pos[p0 + 1];
            float e1z = this.pos[p1 + 2] - this.pos[p0 + 2];
            float e3x = this.pos[p3] - this.pos[p0];
            float e3y = this.pos[p3 + 1] - this.pos[p0 + 1];
            float e3z = this.pos[p3 + 2] - this.pos[p0 + 2];
            this.ax = (e1x * t3 - e3x * t1) / det;
            this.ay = (e1y * t3 - e3y * t1) / det;
            this.az = (e1z * t3 - e3z * t1) / det;
            this.bx = (e3x * s1 - e1x * s3) / det;
            this.by = (e3y * s1 - e1y * s3) / det;
            this.bz = (e3z * s1 - e1z * s3) / det;
            float dx;
            float dy;
            float dz;
            if (back >= 0) {
               dx = (this.centre(back, 0) - this.centre(front, 0)) * 0.5F;
               dy = (this.centre(back, 1) - this.centre(front, 1)) * 0.5F;
               dz = (this.centre(back, 2) - this.centre(front, 2)) * 0.5F;
            } else {
               float half = (float)Math.sqrt((double)(this.ax * this.ax + this.ay * this.ay + this.az * this.az)) * 0.5F;
               dx = -this.nrm[p0] * half;
               dy = -this.nrm[p0 + 1] * half;
               dz = -this.nrm[p0 + 2] * half;
            }

            this.ox = this.pos[p0] - this.ax * s0 - this.bx * t0 + dx;
            this.oy = this.pos[p0 + 1] - this.ay * s0 - this.by * t0 + dy;
            this.oz = this.pos[p0 + 2] - this.az * s0 - this.bz * t0 + dz;

            for (int edge : rim(sprite)) {
               int x = edge & 1023;
               int y = edge >> 10 & 1023;
               switch (edge >> 20) {
                  case 0:
                     this.rect(out, (float)x - width, (float)y, (float)x, (float)(y + 1), argb, light, overlay);
                     break;
                  case 1:
                     this.rect(out, (float)(x + 1), (float)y, (float)(x + 1) + width, (float)(y + 1), argb, light, overlay);
                     break;
                  case 2:
                     this.rect(out, (float)x, (float)y - width, (float)(x + 1), (float)y, argb, light, overlay);
                     break;
                  case 3:
                     this.rect(out, (float)x, (float)(y + 1), (float)(x + 1), (float)(y + 1) + width, argb, light, overlay);
                     break;
                  case 4:
                     this.rect(out, (float)x - width, (float)y - width, (float)x, (float)y, argb, light, overlay);
                     break;
                  case 5:
                     this.rect(out, (float)(x + 1), (float)y - width, (float)(x + 1) + width, (float)y, argb, light, overlay);
                     break;
                  case 6:
                     this.rect(out, (float)x - width, (float)(y + 1), (float)x, (float)(y + 1) + width, argb, light, overlay);
                     break;
                  default:
                     this.rect(out, (float)(x + 1), (float)(y + 1), (float)(x + 1) + width, (float)(y + 1) + width, argb, light, overlay);
               }
            }

            return true;
         }
      }
   }

   private void rect(VertexConsumer out, float s0, float t0, float s1, float t1, int argb, int light, int overlay) {
      this.corner(out, s0, t0, argb, light, overlay);
      this.corner(out, s1, t0, argb, light, overlay);
      this.corner(out, s1, t1, argb, light, overlay);
      this.corner(out, s0, t1, argb, light, overlay);
   }

   private void corner(VertexConsumer out, float s, float t, int argb, int light, int overlay) {
      out.addVertex(
         this.ox + this.ax * s + this.bx * t,
         this.oy + this.ay * s + this.by * t,
         this.oz + this.az * s + this.bz * t,
         argb,
         0.5F,
         0.5F,
         overlay,
         light,
         0.0F,
         1.0F,
         0.0F
      );
   }

   private boolean spans(int b, float u0, float u1, float v0, float v1, float eu, float ev) {
      float umin = Float.MAX_VALUE;
      float umax = -Float.MAX_VALUE;
      float vmin = Float.MAX_VALUE;
      float vmax = -Float.MAX_VALUE;

      for (int k = 0; k < 4; k++) {
         float u = this.uv[(b + k) * 2];
         float v = this.uv[(b + k) * 2 + 1];
         umin = Math.min(umin, u);
         umax = Math.max(umax, u);
         vmin = Math.min(vmin, v);
         vmax = Math.max(vmax, v);
      }

      return Math.abs(umin - u0) < eu && Math.abs(umax - u1) < eu && Math.abs(vmin - v0) < ev && Math.abs(vmax - v1) < ev;
   }

   private float centre(int b, int axis) {
      return (this.pos[b * 3 + axis] + this.pos[(b + 1) * 3 + axis] + this.pos[(b + 2) * 3 + axis] + this.pos[(b + 3) * 3 + axis]) * 0.25F;
   }

   private static int[] rim(TextureAtlasSprite sprite) {
      return RIMS.computeIfAbsent(sprite, sp -> {
         SpriteContents c = sp.contents();
         int w = c.width();
         int h = c.height();
         IntArrayList edges = new IntArrayList();

         for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
               if (!clear(c, x, y, w, h)) {
                  boolean l = clear(c, x - 1, y, w, h);
                  boolean r = clear(c, x + 1, y, w, h);
                  boolean u = clear(c, x, y - 1, w, h);
                  boolean d = clear(c, x, y + 1, w, h);
                  int at = x | y << 10;
                  if (l) {
                     edges.add(at);
                  }

                  if (r) {
                     edges.add(at | 1048576);
                  }

                  if (u) {
                     edges.add(at | 2097152);
                  }

                  if (d) {
                     edges.add(at | 3145728);
                  }

                  if (l && u && clear(c, x - 1, y - 1, w, h)) {
                     edges.add(at | 4194304);
                  }

                  if (r && u && clear(c, x + 1, y - 1, w, h)) {
                     edges.add(at | 5242880);
                  }

                  if (l && d && clear(c, x - 1, y + 1, w, h)) {
                     edges.add(at | 6291456);
                  }

                  if (r && d && clear(c, x + 1, y + 1, w, h)) {
                     edges.add(at | 7340032);
                  }
               }
            }
         }

         return edges.toIntArray();
      });
   }

   private static boolean clear(SpriteContents c, int x, int y, int w, int h) {
      return x < 0 || y < 0 || x >= w || y >= h || c.isTransparent(0, x, y);
   }

   private boolean mirrored(int b) {
      float ax = this.pos[(b + 1) * 3] - this.pos[b * 3];
      float ay = this.pos[(b + 1) * 3 + 1] - this.pos[b * 3 + 1];
      float az = this.pos[(b + 1) * 3 + 2] - this.pos[b * 3 + 2];
      float bx = this.pos[(b + 3) * 3] - this.pos[b * 3];
      float by = this.pos[(b + 3) * 3 + 1] - this.pos[b * 3 + 1];
      float bz = this.pos[(b + 3) * 3 + 2] - this.pos[b * 3 + 2];
      float cx = ay * bz - az * by;
      float cy = az * bx - ax * bz;
      float cz = ax * by - ay * bx;
      return cx * this.nrm[b * 3] + cy * this.nrm[b * 3 + 1] + cz * this.nrm[b * 3 + 2] < 0.0F;
   }

   private float area(int b) {
      float ax = this.pos[(b + 1) * 3] - this.pos[b * 3];
      float ay = this.pos[(b + 1) * 3 + 1] - this.pos[b * 3 + 1];
      float az = this.pos[(b + 1) * 3 + 2] - this.pos[b * 3 + 2];
      float bx = this.pos[(b + 3) * 3] - this.pos[b * 3];
      float by = this.pos[(b + 3) * 3 + 1] - this.pos[b * 3 + 1];
      float bz = this.pos[(b + 3) * 3 + 2] - this.pos[b * 3 + 2];
      float cx = ay * bz - az * by;
      float cy = az * bx - ax * bz;
      float cz = ax * by - ay * bx;
      return (float)Math.sqrt((double)(cx * cx + cy * cy + cz * cz));
   }
}
