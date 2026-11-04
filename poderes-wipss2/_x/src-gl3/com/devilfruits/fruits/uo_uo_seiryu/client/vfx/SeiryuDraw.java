package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.render.VfxDraw;
import com.devilfruits.client.render.VfxRenderTypes;
import com.devilfruits.client.render.vfx.VfxContext;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class SeiryuDraw {
   public static final int BOLT_CORE = 16777215;
   public static final int BOLT_GLOW = 11955455;
   public static final int BOLT_HALO = 8011007;
   public static final int FIRE_CORE = 16776160;
   public static final int FIRE_MID = 16758574;
   public static final int FIRE_OUTER = 14041882;
   public static final int FIRE_WHITE = 16774338;
   public static final int FIRE_YELLOW = 16765503;
   public static final int FIRE_ORANGE = 16742938;
   public static final int FIRE_RED = 15217182;
   public static final int FIRE_EMBER = 8003343;
   public static final int THUNDER_WHITE = 15984895;
   public static final int VIOLET_LIGHT = 11955455;
   public static final int VIOLET = 8011007;
   public static final int VIOLET_DEEP = 2757984;
   public static final int SMOKE_BLACK = 1708080;
   public static final int AZURE = 3843811;
   public static final int AZURE_LIGHT = 8050431;
   public static final int GOLD = 15913562;
   public static final ResourceLocation FLARE = DevilFruits.id("textures/misc/uo_uo_seiryu/flare.png");
   private static final Vector3f A = new Vector3f();
   private static final Vector3f B = new Vector3f();
   private static final Vector3f W = new Vector3f();
   private static final Vector3f P = new Vector3f();
   private static final Vector3f Q = new Vector3f();

   public static long mix(long x) {
      x = (x ^ x >>> 30) * -4658895280553007687L;
      x = (x ^ x >>> 27) * -7723592293110705685L;
      return x ^ x >>> 31;
   }

   public static float unit(long hash) {
      return (float)(hash & 16777215L) / 1.6777216E7F;
   }

   public static int pointCount(int depth) {
      return VfxDraw.boltPointCount(depth);
   }

   public static void boltPath(float[] out, float dx, float dy, float dz, int depth, float jag, long seed) {
      VfxDraw.boltPath(out, dx, dy, dz, depth, jag, seed);
   }

   public static void ribbons(
      VfxContext ctx, float[] pts, int count, float ox, float oy, float oz, float width, float taper, int core, int glow, int halo, float alpha
   ) {
      if (!(alpha <= 0.003F) && count >= 2) {
         VertexConsumer vc = ctx.buffer;
         Matrix4f m = ctx.matrix;

         for (int i = 0; i + 1 < count; i++) {
            A.set(ox + pts[i * 3], oy + pts[i * 3 + 1], oz + pts[i * 3 + 2]);
            B.set(ox + pts[i * 3 + 3], oy + pts[i * 3 + 4], oz + pts[i * 3 + 5]);
            W.set(B).sub(A).cross(A.x + B.x, A.y + B.y, A.z + B.z);
            if (!(W.lengthSquared() < 1.0E-10F)) {
               W.normalize();
               float t = (float)i / (float)(count - 1);
               float w = width * (1.0F - taper * t);
               ribbon(vc, m, Math.min(w * 4.5F, w + 0.55F), halo, alpha * 0.22F);
               ribbon(vc, m, Math.min(w * 2.2F, w + 0.25F), glow, alpha * 0.55F);
               ribbon(vc, m, w, core, alpha);
            }
         }
      }
   }

   private static void ribbon(VertexConsumer vc, Matrix4f m, float half, int rgb, float alpha) {
      int a = Mth.clamp((int)(alpha * 255.0F), 0, 255);
      if (a > 0) {
         VfxDraw.ribbon(vc, m, A, B, W, half, rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, a);
      }
   }

   public static void flare(VfxContext ctx, float cx, float cy, float cz, float size, int inner, int outer, float alpha) {
      if (!(alpha <= 0.003F) && !(size <= 0.0F)) {
         VertexConsumer vc = ctx.buffers.getBuffer(VfxRenderTypes.glowTextured(FLARE));
         flareQuad(vc, ctx, cx, cy, cz, size, outer, alpha * 0.6F);
         flareQuad(vc, ctx, cx, cy, cz, size * 0.45F, inner, alpha);
      }
   }

   private static void flareQuad(VertexConsumer vc, VfxContext ctx, float cx, float cy, float cz, float s, int rgb, float alpha) {
      Vector3f l = ctx.camLeft;
      Vector3f u = ctx.camUp;
      Matrix4f m = ctx.matrix;
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      int a = Mth.clamp((int)(alpha * 255.0F), 0, 255);
      vc.addVertex(m, cx + (l.x + u.x) * s, cy + (l.y + u.y) * s, cz + (l.z + u.z) * s).setUv(0.0F, 0.0F).setColor(r, g, b, a);
      vc.addVertex(m, cx + (l.x - u.x) * s, cy + (l.y - u.y) * s, cz + (l.z - u.z) * s).setUv(0.0F, 1.0F).setColor(r, g, b, a);
      vc.addVertex(m, cx + (-l.x - u.x) * s, cy + (-l.y - u.y) * s, cz + (-l.z - u.z) * s).setUv(1.0F, 1.0F).setColor(r, g, b, a);
      vc.addVertex(m, cx + (-l.x + u.x) * s, cy + (-l.y + u.y) * s, cz + (-l.z + u.z) * s).setUv(1.0F, 0.0F).setColor(r, g, b, a);
   }

   public static void texturedBeam(
      VertexConsumer vc,
      Matrix4f m,
      float ax,
      float ay,
      float az,
      float bx,
      float by,
      float bz,
      float widthA,
      float widthB,
      float scroll,
      float repeat,
      int rgb,
      float alpha
   ) {
      P.set(ax, ay, az);
      Q.set(bx, by, bz);
      W.set(Q).sub(P).cross(P.x + Q.x, P.y + Q.y, P.z + Q.z);
      if (!(W.lengthSquared() < 1.0E-10F)) {
         W.normalize();
         int r = rgb >> 16 & 0xFF;
         int g = rgb >> 8 & 0xFF;
         int b = rgb & 0xFF;
         int a = Mth.clamp((int)(alpha * 255.0F), 0, 255);
         vc.addVertex(m, ax - W.x * widthA, ay - W.y * widthA, az - W.z * widthA).setUv(0.0F, scroll).setColor(r, g, b, a);
         vc.addVertex(m, ax + W.x * widthA, ay + W.y * widthA, az + W.z * widthA).setUv(1.0F, scroll).setColor(r, g, b, a);
         vc.addVertex(m, bx + W.x * widthB, by + W.y * widthB, bz + W.z * widthB).setUv(1.0F, scroll + repeat).setColor(r, g, b, a);
         vc.addVertex(m, bx - W.x * widthB, by - W.y * widthB, bz - W.z * widthB).setUv(0.0F, scroll + repeat).setColor(r, g, b, a);
      }
   }

   private SeiryuDraw() {
   }
}
