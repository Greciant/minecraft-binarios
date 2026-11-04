package com.devilfruits.core.util;

public final class DeterministicRandom {
   private long state;
   private double nextGaussian;
   private boolean haveNextGaussian;

   public DeterministicRandom(long seed) {
      this.state = seed;
   }

   public long nextLong() {
      long z = this.state += -7046029254386353131L;
      z = (z ^ z >>> 30) * -4658895280553007687L;
      z = (z ^ z >>> 27) * -7723592293110705685L;
      return z ^ z >>> 31;
   }

   public float nextFloat() {
      return (float)(this.nextLong() >>> 40) * 5.9604645E-8F;
   }

   public float range(float min, float max) {
      return min + this.nextFloat() * (max - min);
   }

   public float signed(float amplitude) {
      return (this.nextFloat() * 2.0F - 1.0F) * amplitude;
   }

   public int nextInt(int bound) {
      return (int)((this.nextLong() >>> 33) % (long)bound);
   }

   public boolean chance(float probability) {
      return this.nextFloat() < probability;
   }

   public double nextGaussian() {
      if (this.haveNextGaussian) {
         this.haveNextGaussian = false;
         return this.nextGaussian;
      } else {
         double v1;
         double v2;
         double s;
         do {
            v1 = (double)(2.0F * this.nextFloat() - 1.0F);
            v2 = (double)(2.0F * this.nextFloat() - 1.0F);
            s = v1 * v1 + v2 * v2;
         } while (s >= 1.0 || s == 0.0);

         double multiplier = StrictMath.sqrt(-2.0 * StrictMath.log(s) / s);
         this.nextGaussian = v2 * multiplier;
         this.haveNextGaussian = true;
         return v1 * multiplier;
      }
   }
}
