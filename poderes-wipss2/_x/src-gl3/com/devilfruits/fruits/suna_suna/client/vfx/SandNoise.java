package com.devilfruits.fruits.suna_suna.client.vfx;

import net.minecraft.util.Mth;

public final class SandNoise {
   private static float hash(int x, int y, int z) {
      int h = x * 374761393 + y * 668265263 + z * 1442695041;
      h = (h ^ h >>> 13) * 1274126177;
      h ^= h >>> 16;
      return (float)(h & 65535) / 32767.5F - 1.0F;
   }

   public static float noise(float x, float y, float z) {
      int ix = Mth.floor(x);
      int iy = Mth.floor(y);
      int iz = Mth.floor(z);
      float fx = x - (float)ix;
      float fy = y - (float)iy;
      float fz = z - (float)iz;
      fx = fx * fx * (3.0F - 2.0F * fx);
      fy = fy * fy * (3.0F - 2.0F * fy);
      fz = fz * fz * (3.0F - 2.0F * fz);
      float a = Mth.lerp(fx, hash(ix, iy, iz), hash(ix + 1, iy, iz));
      float b = Mth.lerp(fx, hash(ix, iy + 1, iz), hash(ix + 1, iy + 1, iz));
      float c = Mth.lerp(fx, hash(ix, iy, iz + 1), hash(ix + 1, iy, iz + 1));
      float d = Mth.lerp(fx, hash(ix, iy + 1, iz + 1), hash(ix + 1, iy + 1, iz + 1));
      return Mth.lerp(fz, Mth.lerp(fy, a, b), Mth.lerp(fy, c, d));
   }

   public static float lumpy(float x, float y, float z) {
      return noise(x, y, z) * 0.68F + noise(x * 2.3F + 17.0F, y * 2.3F, z * 2.3F) * 0.32F;
   }

   private SandNoise() {
   }
}
