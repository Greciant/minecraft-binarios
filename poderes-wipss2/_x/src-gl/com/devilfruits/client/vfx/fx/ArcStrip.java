package com.devilfruits.client.vfx.fx;

import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import net.minecraft.util.Mth;

public final class ArcStrip {
   public static final int MAX_POINTS = 64;
   private static final float[] SX = new float[64];
   private static final float[] SY = new float[64];
   private static final float[] SZ = new float[64];
   private static final float[] SW = new float[64];
   private static final float[] SH = new float[64];
   private static final float[] SA = new float[64];

   public static void draw(
      VfxFrame f,
      MeshTex tex,
      float cx,
      float cy,
      float cz,
      float ux,
      float uy,
      float uz,
      float vx,
      float vy,
      float vz,
      float radius,
      float head,
      float arc,
      float width,
      float heat,
      float alpha,
      int palette,
      float emissive,
      int rgb,
      float scroll,
      int points
   ) {
      int n = Mth.clamp(points, 2, 64);
      if (!(alpha <= 0.01F) && !(radius <= 0.01F) && f.allowMesh()) {
         for (int k = 0; k < n; k++) {
            float along = (float)k / (float)(n - 1);
            float a = head - along * arc;
            float r = radius * (1.0F - 0.1F * along);
            float c = Mth.cos(a);
            float s = Mth.sin(a);
            SX[k] = cx + (ux * c + vx * s) * r;
            SY[k] = cy + (uy * c + vy * s) * r;
            SZ[k] = cz + (uz * c + vz * s) * r;
            SW[k] = width * (1.0F - 0.72F * along);
            SH[k] = heat < 0.0F ? -1.0F : heat * (1.0F - 0.45F * along);
            float keep = 1.0F - along;
            SA[k] = alpha * keep * (float)Math.sqrt((double)keep) * VfxWorld.nearFade(SX[k], SY[k], SZ[k]);
         }

         float du = Math.abs(arc) * radius / (float)(n - 1) / Math.max(0.05F, width * 8.0F);
         MeshWriter.strip(f.mesh(tex), n, SX, SY, SZ, SW, SH, SA, scroll, du, rgb, Math.max(0, palette), emissive);
      }
   }

   public static void plane(float tilt, float azimuth, float[] out) {
      float nx = Mth.sin(tilt) * Mth.cos(azimuth);
      float ny = Mth.cos(tilt);
      float nz = Mth.sin(tilt) * Mth.sin(azimuth);
      float ux = -nz;
      float uy = 0.0F;
      float uz = nx;
      float ul = Mth.sqrt(ux * ux + nx * nx);
      if (ul < 1.0E-4F) {
         ux = 1.0F;
         uz = 0.0F;
         ul = 1.0F;
      }

      ux /= ul;
      uz /= ul;
      out[0] = ux;
      out[1] = uy;
      out[2] = uz;
      out[3] = ny * uz - nz * uy;
      out[4] = nz * ux - nx * uz;
      out[5] = nx * uy - ny * ux;
   }

   private ArcStrip() {
   }
}
