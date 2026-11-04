package com.devilfruits.client.vfx.fx;

import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import java.util.Arrays;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Tether {
   private static final int SEGMENTS = 24;
   private static final float[] XS = new float[25];
   private static final float[] YS = new float[25];
   private static final float[] ZS = new float[25];
   private static final float[] WS = new float[25];
   private static final float[] HS = new float[25];
   private static final float[] AS = new float[25];

   public static float strain(double length, double leash) {
      return (float)Mth.clamp((length - leash * 0.7) / (leash * 0.3), 0.0, 1.0);
   }

   public static void draw(
      VfxFrame f, Vec3 from, Vec3 to, double leash, MeshTex tex, int rgb, float emissive, float width, float alpha, int skipNear, float phase
   ) {
      if (f.allowMesh() && !(alpha <= 0.01F)) {
         Vec3 d = to.subtract(from);
         double len = d.length();
         if (!(len < 0.3)) {
            float strain = strain(len, leash);
            Vec3 side = new Vec3(-d.z, 0.0, d.x);
            side = side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
            double sag = Math.min(1.6, len * 0.08) * (1.0 - (double)strain * 0.85);
            float time = f.time + phase;
            int n = 0;

            for (int i = 0; i <= 24 - skipNear; i++) {
               float t = (float)i / 24.0F;
               float bell = Mth.sin(t * (float) Math.PI);
               double ripple = (double)Mth.sin(t * (float) Math.PI * 4.0F - time * 0.35F) * 0.07 * (double)bell * (1.0 - (double)strain * 0.7);
               Vec3 p = from.add(d.scale((double)t)).add(0.0, -sag * (double)bell, 0.0).add(side.scale(ripple));
               XS[n] = f.rx(p.x);
               YS[n] = f.ry(p.y);
               ZS[n] = f.rz(p.z);
               WS[n] = width * (0.65F + 0.35F * bell) + width * 0.3F * Mth.sin(t * 9.0F + time * 0.5F);
               AS[n] = alpha * (0.7F + 0.3F * bell) * (0.85F + 0.15F * Mth.sin(time * 0.2F + t * 6.0F));
               n++;
            }

            MeshWriter.strip(f.mesh(tex), n, XS, YS, ZS, WS, HS, AS, -time * 0.04F, 0.037499998F * (float)len, rgb, 0, emissive);
         }
      }
   }

   public static int lerpRgb(int a, int b, float t) {
      int r = Math.round(Mth.lerp(t, (float)(a >> 16 & 0xFF), (float)(b >> 16 & 0xFF)));
      int g = Math.round(Mth.lerp(t, (float)(a >> 8 & 0xFF), (float)(b >> 8 & 0xFF)));
      int bl = Math.round(Mth.lerp(t, (float)(a & 0xFF), (float)(b & 0xFF)));
      return r << 16 | g << 8 | bl;
   }

   private Tether() {
   }

   static {
      Arrays.fill(HS, -1.0F);
   }
}
