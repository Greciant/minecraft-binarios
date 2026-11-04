package com.devilfruits.fruits.gura_gura.quake;

import com.devilfruits.core.util.DeterministicRandom;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;

public final class FissurePattern {
   public static final float STEP = 0.75F;

   public static List<FissurePattern.Line> generate(long seed, int lineCount, float length) {
      DeterministicRandom random = new DeterministicRandom(seed);
      List<FissurePattern.Line> lines = new ArrayList<>();
      float base = random.range(0.0F, (float) (Math.PI * 2));

      for (int i = 0; i < lineCount; i++) {
         float angle = base + (float)i * (float) (Math.PI * 2) / (float)lineCount + random.signed((float) Math.PI / (float)lineCount * 0.55F);
         float len = length * random.range(0.7F, 1.0F);
         walk(random, lines, 0.0F, 0.0F, 0.0F, angle, len, 1.0F, 0);
      }

      return lines;
   }

   private static void walk(
      DeterministicRandom random, List<FissurePattern.Line> out, float sx, float sz, float startDist, float angle, float length, float width, int depth
   ) {
      int steps = Math.max(2, Mth.ceil(length / 0.75F));
      float[] xs = new float[steps + 1];
      float[] zs = new float[steps + 1];
      float[] ds = new float[steps + 1];
      xs[0] = sx;
      zs[0] = sz;
      ds[0] = startDist;
      float x = sx;
      float z = sz;
      float a = angle;

      for (int s = 1; s <= steps; s++) {
         float var18 = a + (float)random.nextGaussian() * 0.32F;
         a = Mth.rotLerp(0.25F, var18 * (180.0F / (float)Math.PI), angle * (180.0F / (float)Math.PI)) * (float) (Math.PI / 180.0);
         x += Mth.cos(a) * 0.75F;
         z += Mth.sin(a) * 0.75F;
         xs[s] = x;
         zs[s] = z;
         ds[s] = startDist + (float)s * 0.75F;
         if (depth < 2 && s > 2 && s < steps - 1 && random.chance(depth == 0 ? 0.16F : 0.08F)) {
            float side = random.chance(0.5F) ? 1.0F : -1.0F;
            walk(
               random, out, x, z, ds[s], a + side * random.range(0.45F, 0.9F), (float)(steps - s) * 0.75F * random.range(0.35F, 0.6F), width * 0.6F, depth + 1
            );
         }
      }

      out.add(new FissurePattern.Line(xs, zs, ds, width));
   }

   private FissurePattern() {
   }

   public static record Line(float[] x, float[] z, float[] dist, float width) {
      public int size() {
         return this.x.length;
      }
   }
}
