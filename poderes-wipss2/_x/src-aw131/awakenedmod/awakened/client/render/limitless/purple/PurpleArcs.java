package com.awakenedmod.awakened.client.render.limitless.purple;

import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.client.render.limitless.SpaceBend;
import org.joml.Vector3f;

public final class PurpleArcs {
   private static final int N = 13;
   private static final float PI = (float) Math.PI;
   private static final int BLUE = 2777855;
   private static final int RED = 16718390;
   private static final int BLUE_HOT = 13426687;
   private static final int RED_HOT = 16764118;
   private final Vector3f[] pts = new Vector3f[13];
   private final float[] widths = new float[13];
   private final int[] colors = new int[13];

   public PurpleArcs() {
      for (int i = 0; i < 13; i++) {
         this.pts[i] = new Vector3f();
      }
   }

   public void draw(LimitlessMesh m, Vector3f c, float r, float age, long seed, int count, float jag, PurpleArcs.Style style, float a) {
      if (!(a <= 0.003F) && !(r <= 0.01F)) {
         Vector3f d0 = new Vector3f();
         Vector3f t0 = new Vector3f();
         Vector3f ax = new Vector3f();
         Vector3f dir = new Vector3f();

         for (int k = 0; k < count; k++) {
            long sk = seed * 7349L + (long)k * 92821L;
            float period = 5.0F + 4.0F * SpaceBend.hash(sk, 1);
            float t = age / period + SpaceBend.hash(sk, 2);
            long n = (long)Math.floor((double)t);
            float u = t - (float)n;
            float env = Ease.ramp(u, 0.0F, 0.42F) * (1.0F - Ease.ramp(u, 0.62F, 0.38F));
            if (!(env <= 0.003F)) {
               long sc = sk * 31L + n * 1009L;
               unit(sc, 3, d0);
               unit(sc, 4, ax);
               t0.set(d0).cross(ax);
               if (!(t0.lengthSquared() < 1.0E-6F)) {
                  t0.normalize();
                  ax.set(d0).cross(t0);
                  float len = 0.45F + 0.55F * SpaceBend.hash(sc, 5);
                  int col = k % 2 == 0 ? style.blue() : style.red();
                  float alpha = style.alpha() * env * a * (k % 2 == 0 ? 1.0F : 0.85F);
                  this.stroke(m, c, r, d0, t0, ax, dir, len, 0.0F, jag, sc, u, alpha, style.width() * r, col);
                  if (SpaceBend.hash(sc, 6) < 0.5F) {
                     float at = len * 0.35F;
                     Vector3f fd = new Vector3f(d0).mul((float)Math.cos((double)at)).add(new Vector3f(t0).mul((float)Math.sin((double)at)));
                     float bend = (SpaceBend.hash(sc, 7) - 0.5F) * 1.6F;
                     Vector3f ft = new Vector3f(t0).mul((float)Math.cos((double)bend)).add(new Vector3f(ax).mul((float)Math.sin((double)bend)));
                     ft.sub(new Vector3f(fd).mul(ft.dot(fd))).normalize();
                     Vector3f fax = new Vector3f(fd).cross(ft);
                     this.stroke(m, c, r, fd, ft, fax, dir, len * 0.45F, 1.0F, jag, sc + 77L, u, alpha * 0.8F, style.width() * r * 0.7F, col);
                  }
               }
            }
         }
      }
   }

   private void stroke(
      LimitlessMesh m,
      Vector3f c,
      float r,
      Vector3f d0,
      Vector3f t0,
      Vector3f ax,
      Vector3f dir,
      float len,
      float tipFrom,
      float jag,
      long sc,
      float u,
      float alpha,
      float w,
      int rgb
   ) {
      Vector3f toCam = new Vector3f();

      for (int j = 0; j < 13; j++) {
         float s = (float)j / 12.0F;
         float ang = len * s;
         float jit = (SpaceBend.hash(sc, 20 + j) - 0.5F) * (1.0F - u) + (SpaceBend.hash(sc + 1L, 20 + j) - 0.5F) * u;
         float stray = 2.0F * jag * jit * (float)Math.sin((double)((float) Math.PI * s));
         dir.set(d0).mul((float)Math.cos((double)ang)).add(new Vector3f(t0).mul((float)Math.sin((double)ang))).add(new Vector3f(ax).mul(stray)).normalize();
         this.pts[j].set(dir).mul(r * 1.02F).add(c);
         toCam.set(this.pts[j]).negate().normalize();
         float front = Ease.smooth(dir.dot(toCam) * 4.0F + 0.15F);
         float body = tipFrom > 0.0F ? 1.0F - s : (float)Math.sin((double)((float) Math.PI * Math.min(1.0F, s * 1.15F)));
         this.widths[j] = w * (0.35F + 0.65F * body);
         this.colors[j] = LimitlessMesh.rgba(rgb, alpha * front * (0.3F + 0.7F * body));
      }

      m.ribbon(this.pts, this.widths, this.colors, 13);
   }

   public void glow(LimitlessMesh m, PurpleBallLook.Ball ball, float base, float jag, float alpha, float width) {
      int count = count(base, ball);
      float a = ball.a() * Math.min(1.0F, ball.glow());
      float j = jag * ball.calm();
      this.draw(m, ball.c(), ball.r(), ball.age(), ball.seed(), count, j, new PurpleArcs.Style(width, 0.55F * alpha, 2777855, 16718390), a);
      this.draw(m, ball.c(), ball.r(), ball.age(), ball.seed(), count, j, new PurpleArcs.Style(0.012F, 0.85F * alpha, 13426687, 16764118), a);
   }

   public static int count(float base, PurpleBallLook.Ball ball) {
      return Math.round(base * (ball.flying() ? Math.max(1.0F, ball.r() / 3.0F) : 0.5F));
   }

   private static void unit(long seed, int salt, Vector3f out) {
      float z = SpaceBend.hash(seed, salt) * 2.0F - 1.0F;
      float az = SpaceBend.hash(seed, salt + 100) * (float) Math.PI * 2.0F;
      float s = (float)Math.sqrt((double)Math.max(0.0F, 1.0F - z * z));
      out.set(s * (float)Math.cos((double)az), s * (float)Math.sin((double)az), z);
   }

   public static record Style(float width, float alpha, int blue, int red) {
   }
}
