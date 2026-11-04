package com.devilfruits.client.vfx.particle;

import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.mojang.blaze3d.vertex.BufferBuilder;

public final class Ribbon {
   private static int reserved;
   private final int capacity;
   private final double[] xs;
   private final double[] ys;
   private final double[] zs;
   private final float[] born;
   private int head = -1;
   private int size;
   private static final int SUB = 4;
   private final float[] cx;
   private final float[] cy;
   private final float[] cz;
   private final float[] cw;
   private final float[] ch;
   private final float[] ca;
   private final float[] px;
   private final float[] py;
   private final float[] pz;
   private final float[] width;
   private final float[] heat;
   private final float[] alpha;
   private boolean released;
   private float halfWidth = 0.4F;
   private float lifeTicks = 10.0F;
   private float heatHead = 1.2F;
   private float heatTail = 0.5F;
   private float emissive = 1.6F;
   private int palette;
   private int rgb = 16777215;
   private float scroll = 0.08F;
   private MeshTex texture = MeshTex.FLAME_RIBBON;

   private Ribbon(int capacity) {
      this.capacity = capacity;
      this.xs = new double[capacity];
      this.ys = new double[capacity];
      this.zs = new double[capacity];
      this.born = new float[capacity];
      int fine = capacity * 4 + 1;
      this.cx = new float[fine];
      this.cy = new float[fine];
      this.cz = new float[fine];
      this.cw = new float[fine];
      this.ch = new float[fine];
      this.ca = new float[fine];
      this.px = new float[capacity + 1];
      this.py = new float[capacity + 1];
      this.pz = new float[capacity + 1];
      this.width = new float[capacity + 1];
      this.heat = new float[capacity + 1];
      this.alpha = new float[capacity + 1];
   }

   public static Ribbon create(int points) {
      if (reserved + points > VfxSettings.maxRibbonPoints()) {
         return null;
      } else {
         reserved += points;
         return new Ribbon(points);
      }
   }

   public Ribbon look(float halfWidth, float lifeTicks, float heatHead, float heatTail, float emissive, int palette) {
      this.halfWidth = halfWidth;
      this.lifeTicks = lifeTicks;
      this.heatHead = heatHead;
      this.heatTail = heatTail;
      this.emissive = emissive;
      this.palette = palette;
      return this;
   }

   public Ribbon texture(MeshTex texture, float scrollPerTick) {
      this.texture = texture;
      this.scroll = scrollPerTick;
      return this;
   }

   public Ribbon tint(int rgb) {
      this.rgb = rgb;
      return this;
   }

   public void push(double x, double y, double z, float time) {
      this.head = (this.head + 1) % this.capacity;
      this.xs[this.head] = x;
      this.ys[this.head] = y;
      this.zs[this.head] = z;
      this.born[this.head] = time;
      this.size = Math.min(this.size + 1, this.capacity);
   }

   public boolean faded(float time) {
      return this.size == 0 || time - this.born[this.head] > this.lifeTicks;
   }

   public void render(VfxFrame f, double hx, double hy, double hz, float fade) {
      if (this.size != 0 && !(fade <= 0.01F)) {
         int n = 0;
         float now = f.time;
         this.px[n] = f.rx(hx);
         this.py[n] = f.ry(hy);
         this.pz[n] = f.rz(hz);
         this.width[n] = this.halfWidth;
         this.heat[n] = this.heatHead;
         this.alpha[n] = fade;
         n++;

         for (int k = 0; k < this.size; k++) {
            int i = Math.floorMod(this.head - k, this.capacity);
            float a = (now - this.born[i]) / this.lifeTicks;
            if (a >= 1.0F) {
               break;
            }

            float keep = 1.0F - a;
            this.px[n] = f.rx(this.xs[i]);
            this.py[n] = f.ry(this.ys[i]);
            this.pz[n] = f.rz(this.zs[i]);
            this.width[n] = this.halfWidth * (0.25F + 0.75F * keep);
            this.heat[n] = this.heatTail + (this.heatHead - this.heatTail) * keep;
            this.alpha[n] = fade * keep * keep;
            n++;
         }

         if (n >= 2 && f.allowMesh()) {
            int m = 0;

            for (int i = 0; i < n - 1; i++) {
               int i0 = Math.max(0, i - 1);
               int i2 = i + 1;
               int i3 = Math.min(n - 1, i + 2);

               for (int s = 0; s < 4; s++) {
                  float t = (float)s / 4.0F;
                  this.cx[m] = catmull(this.px[i0], this.px[i], this.px[i2], this.px[i3], t);
                  this.cy[m] = catmull(this.py[i0], this.py[i], this.py[i2], this.py[i3], t);
                  this.cz[m] = catmull(this.pz[i0], this.pz[i], this.pz[i2], this.pz[i3], t);
                  this.cw[m] = this.width[i] + (this.width[i2] - this.width[i]) * t;
                  this.ch[m] = this.heat[i] + (this.heat[i2] - this.heat[i]) * t;
                  this.ca[m] = this.alpha[i] + (this.alpha[i2] - this.alpha[i]) * t;
                  m++;
               }
            }

            this.cx[m] = this.px[n - 1];
            this.cy[m] = this.py[n - 1];
            this.cz[m] = this.pz[n - 1];
            this.cw[m] = this.width[n - 1];
            this.ch[m] = this.heat[n - 1];
            this.ca[m] = this.alpha[n - 1];
            m++;
            BufferBuilder b = f.mesh(this.texture);
            MeshWriter.strip(b, m, this.cx, this.cy, this.cz, this.cw, this.ch, this.ca, -now * this.scroll, 0.045F, this.rgb, this.palette, this.emissive);
         }
      }
   }

   private static float catmull(float p0, float p1, float p2, float p3, float t) {
      float t2 = t * t;
      float t3 = t2 * t;
      return 0.5F * (2.0F * p1 + (p2 - p0) * t + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t2 + (3.0F * p1 - p0 - 3.0F * p2 + p3) * t3);
   }

   public void release() {
      if (!this.released) {
         this.released = true;
         reserved = reserved - this.capacity;
      }
   }

   public static void resetBudget() {
      reserved = 0;
   }
}
