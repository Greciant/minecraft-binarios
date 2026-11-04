package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.render.VfxDraw;
import com.devilfruits.client.render.vfx.ShockwaveEffect;
import com.devilfruits.client.render.vfx.VfxContext;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class QuakeDraw {
   public static final int CORE_R = 255;
   public static final int CORE_G = 255;
   public static final int CORE_B = 255;
   public static final int GLOW_R = 120;
   public static final int GLOW_G = 214;
   public static final int GLOW_B = 255;
   private static final Vector3f A = new Vector3f();
   private static final Vector3f B = new Vector3f();
   private static final Vector3f D = new Vector3f();
   private static final Vector3f N = new Vector3f();
   private static final Vector3f W = new Vector3f();

   public static void cracks(
      VfxContext ctx,
      float[] seg,
      float ox,
      float oy,
      float oz,
      Vector3f ax,
      Vector3f ay,
      Vector3f az,
      QuakeDraw.Orientation orientation,
      float progress,
      float core,
      float glow,
      float alpha
   ) {
      cracks(ctx, seg, ox, oy, oz, ax, ay, az, orientation, progress, core, glow, alpha, 0.42F);
   }

   public static void cracks(
      VfxContext ctx,
      float[] seg,
      float ox,
      float oy,
      float oz,
      Vector3f ax,
      Vector3f ay,
      Vector3f az,
      QuakeDraw.Orientation orientation,
      float progress,
      float core,
      float glow,
      float alpha,
      float glowAlpha
   ) {
      if (!(alpha <= 0.003F) && !(progress <= 0.0F)) {
         VertexConsumer vc = ctx.buffer;
         Matrix4f m = ctx.matrix;
         int count = seg.length / 8;

         for (int i = 0; i < count; i++) {
            int o = i * 8;
            float birth = seg[o + 6];
            if (!(progress <= birth)) {
               float f = Math.min(1.0F, (progress - birth) / 0.12F);
               float x1 = seg[o];
               float y1 = seg[o + 1];
               float z1 = seg[o + 2];
               float x2 = Mth.lerp(f, x1, seg[o + 3]);
               float y2 = Mth.lerp(f, y1, seg[o + 4]);
               float z2 = Mth.lerp(f, z1, seg[o + 5]);
               A.set(ox + ax.x * x1 + ay.x * y1 + az.x * z1, oy + ax.y * x1 + ay.y * y1 + az.y * z1, oz + ax.z * x1 + ay.z * y1 + az.z * z1);
               B.set(ox + ax.x * x2 + ay.x * y2 + az.x * z2, oy + ax.y * x2 + ay.y * y2 + az.y * z2, oz + ax.z * x2 + ay.z * y2 + az.z * z2);
               D.set(B).sub(A);
               if (!(D.lengthSquared() < 1.0E-8F)) {
                  switch (orientation) {
                     case PLANE:
                        N.set(az);
                        break;
                     case SPHERE:
                        N.set(A).add(B).mul(0.5F).sub(ox, oy, oz);
                        break;
                     case CAMERA:
                        N.set(A).add(B).mul(0.5F);
                  }

                  W.set(D).cross(N);
                  if (!(W.lengthSquared() < 1.0E-10F)) {
                     W.normalize();
                     float width = seg[o + 7];
                     float flicker = 0.82F + 0.18F * Mth.sin(ctx.time * 1.9F + (float)i * 1.37F);
                     int ga = Mth.clamp((int)(alpha * flicker * glowAlpha * 255.0F), 0, 255);
                     int ca = Mth.clamp((int)(alpha * flicker * 255.0F), 0, 255);
                     VfxDraw.ribbon(vc, m, A, B, W, glow * width, 120, 214, 255, ga);
                     VfxDraw.ribbon(vc, m, A, B, W, core * Math.max(0.6F, width), 255, 255, 255, ca);
                  }
               }
            }
         }
      }
   }

   public static ShockwaveEffect shockwave(
      ShockwaveEffect.Mode mode, Vec3 origin, Vec3 direction, float length, float spread, int duration, int rings, float strength
   ) {
      return new ShockwaveEffect(mode, origin, direction, length, spread, duration, rings, strength).tint(120, 214, 255).shell(235, 248, 255);
   }

   private QuakeDraw() {
   }

   public static enum Orientation {
      PLANE,
      SPHERE,
      CAMERA;
   }
}
