package com.devilfruits.client.particle;

import com.devilfruits.core.config.CoreClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class Fx {
   private static final RandomSource RANDOM = RandomSource.create();

   public static RandomSource random() {
      return RANDOM;
   }

   public static int count(int nominal) {
      return count((float)nominal);
   }

   public static int count(float nominal) {
      Minecraft mc = Minecraft.getInstance();
      float scale = ((CoreClientConfig.VfxQuality)CoreClientConfig.VFX_QUALITY.get()).particleScale;
      ParticleStatus status = (ParticleStatus)mc.options.particles().get();
      if (status == ParticleStatus.DECREASED) {
         scale *= 0.5F;
      } else if (status == ParticleStatus.MINIMAL) {
         scale *= 0.15F;
      }

      float exact = nominal * scale;
      int whole = (int)exact;
      return whole + (RANDOM.nextFloat() < exact - (float)whole ? 1 : 0);
   }

   public static void spawn(ParticleOptions type, double x, double y, double z, double vx, double vy, double vz) {
      Level level = Minecraft.getInstance().level;
      if (level != null) {
         level.addParticle(type, x, y, z, vx, vy, vz);
      }
   }

   public static void spawnFar(ParticleOptions type, double x, double y, double z, double vx, double vy, double vz) {
      Level level = Minecraft.getInstance().level;
      if (level != null) {
         level.addAlwaysVisibleParticle(type, true, x, y, z, vx, vy, vz);
      }
   }

   public static double spread(double amount) {
      return (RANDOM.nextDouble() * 2.0 - 1.0) * amount;
   }

   public static BlockState groundAt(Level level, double x, double y, double z, MutableBlockPos out) {
      out.set(Mth.floor(x), Mth.floor(y) + 3, Mth.floor(z));

      for (int i = 0; i < 9; i++) {
         BlockState state = level.getBlockState(out);
         if (!state.isAir() && state.isSolidRender(level, out)) {
            return state;
         }

         out.move(0, -1, 0);
      }

      return null;
   }

   private Fx() {
   }
}
