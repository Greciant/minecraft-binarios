package com.devilfruits.fruits.supa_supa.client.vfx;

import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import net.minecraft.util.Mth;

public final class Cuts {
   private static final int MAX = 24;
   private static final float[] X = new float[24];
   private static final float[] Y = new float[24];
   private static final float[] Z = new float[24];
   private static final float[] W = new float[24];
   private static final float[] H = new float[24];
   private static final float[] A = new float[24];

   public static void line(VfxFrame f, double ax, double ay, double az, double bx, double by, double bz, float width, float heat, float alpha, float scroll) {
      if (!(alpha <= 0.01F) && f.allowMesh()) {
         double len = Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
         int n = Mth.clamp((int)Math.ceil(len * 3.0), 3, 24);

         for (int i = 0; i < n; i++) {
            float t = (float)i / (float)(n - 1);
            X[i] = f.rx(ax + (bx - ax) * (double)t);
            Y[i] = f.ry(ay + (by - ay) * (double)t);
            Z[i] = f.rz(az + (bz - az) * (double)t);
            float profile = Mth.sin((float) Math.PI * t) * (0.75F + 0.25F * t);
            W[i] = width * Math.max(0.08F, profile);
            H[i] = heat * (0.7F + 0.3F * profile);
            A[i] = alpha * Math.min(1.0F, 0.25F + profile) * VfxWorld.nearFade(X[i], Y[i], Z[i]);
         }

         float du = (float)(len / (double)(n - 1)) / Math.max(0.1F, width * 6.0F);
         MeshWriter.strip(f.mesh(MeshTex.CUT_RIBBON), n, X, Y, Z, W, H, A, scroll, du, 16777215, 5, 2.2F);
      }
   }

   public static void bar(VfxFrame f, double ax, double ay, double az, double bx, double by, double bz, float width, float heat, float alpha, float scroll) {
      if (!(alpha <= 0.01F) && f.allowMesh()) {
         double len = Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
         if (!(len < 0.001)) {
            int n = Mth.clamp((int)Math.ceil(len * 2.0) + 1, 2, 24);

            for (int i = 0; i < n; i++) {
               float t = (float)i / (float)(n - 1);
               X[i] = f.rx(ax + (bx - ax) * (double)t);
               Y[i] = f.ry(ay + (by - ay) * (double)t);
               Z[i] = f.rz(az + (bz - az) * (double)t);
               float end = (float)Math.min(1.0, 0.55 + (double)Math.min(t, 1.0F - t) * len * 4.0);
               W[i] = width * end;
               H[i] = heat;
               A[i] = alpha * VfxWorld.nearFade(X[i], Y[i], Z[i]);
            }

            float du = (float)(len / (double)(n - 1)) / Math.max(0.1F, width * 6.0F);
            MeshWriter.strip(f.mesh(MeshTex.CUT_RIBBON), n, X, Y, Z, W, H, A, scroll, du, 16777215, 5, 2.2F);
         }
      }
   }

   private Cuts() {
   }
}
