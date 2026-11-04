package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SoulCuts {
   private static final int MAX = 24;
   private static final float[] X = new float[24];
   private static final float[] Y = new float[24];
   private static final float[] Z = new float[24];
   private static final float[] W = new float[24];
   private static final float[] H = new float[24];
   private static final float[] A = new float[24];

   public static void thrust(VfxFrame f, Vec3 a, Vec3 b, float width, float heat, float alpha, float scroll) {
      line(f, a, b, width, heat * 0.85F, alpha * 0.9F, scroll, 6, true);
      line(f, a, b, width * 0.34F, heat * 1.15F, alpha, scroll * 1.3F, 7, true);
   }

   public static void soulThrust(VfxFrame f, Vec3 a, Vec3 b, float width, float heat, float alpha, float scroll) {
      line(f, a, b, width, heat * 0.85F, alpha * 0.9F, scroll, 6, true);
      line(f, a, b, width * 0.34F, heat * 1.25F, alpha, scroll * 1.3F, 6, true);
   }

   public static void seam(VfxFrame f, Vec3 a, Vec3 b, float width, float heat, float alpha, float scroll) {
      line(f, a, b, width * 2.2F, heat * 0.55F, alpha * 0.45F, scroll, 6, false);
      line(f, a, b, width, heat, alpha, scroll * 1.4F, 6, false);
   }

   private static void line(VfxFrame f, Vec3 a, Vec3 b, float width, float heat, float alpha, float scroll, int palette, boolean pointed) {
      if (!(alpha <= 0.01F) && f.allowMesh()) {
         double len = a.distanceTo(b);
         if (!(len < 0.001)) {
            int n = Mth.clamp((int)Math.ceil(len * 3.0), 3, 24);

            for (int i = 0; i < n; i++) {
               float t = (float)i / (float)(n - 1);
               X[i] = f.rx(a.x + (b.x - a.x) * (double)t);
               Y[i] = f.ry(a.y + (b.y - a.y) * (double)t);
               Z[i] = f.rz(a.z + (b.z - a.z) * (double)t);
               float profile = pointed
                  ? (float)Math.pow((double)t, 0.7) * Mth.clamp((1.0F - t) * 7.0F, 0.0F, 1.0F)
                  : Math.min(1.0F, Math.min(t, 1.0F - t) * (float)len * 3.0F + 0.3F);
               W[i] = width * Math.max(0.06F, profile);
               H[i] = heat * (0.65F + 0.35F * profile);
               A[i] = alpha * Math.min(1.0F, 0.2F + profile) * VfxWorld.nearFade(X[i], Y[i], Z[i]);
            }

            float du = (float)(len / (double)(n - 1)) / Math.max(0.1F, width * 6.0F);
            MeshWriter.strip(f.mesh(MeshTex.CUT_RIBBON), n, X, Y, Z, W, H, A, scroll, du, 16777215, palette, 2.4F);
         }
      }
   }

   private SoulCuts() {
   }
}
