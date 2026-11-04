package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.sound.LoopSound;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.devilfruits.fruits.suna_suna.SunaSounds;
import com.devilfruits.fruits.suna_suna.ability.GroundSeccoAbility;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class DryGroundEffect extends SandPaintEffect {
   private static final float CELL = 1.7F;
   private static final int CLOSE_TICKS = 40;
   private final int growTicks;
   private final int maxAge;
   private final float[] edge;
   private float shown;

   public DryGroundEffect(ClientLevel level, Vec3 centre, float radius, int growTicks, int maxTicks, long seed) {
      super(level, centre, radius, seed, "dry", 40);
      this.growTicks = Math.max(1, growTicks);
      this.maxAge = Math.max(1, maxTicks) + 60;
      this.edge = new float[this.size * this.size];
      int half = Mth.ceil(this.radius) + 1;
      int minX = Mth.floor(centre.x) - half;
      int minZ = Mth.floor(centre.z) - half;

      for (int iz = 0; iz < this.size; iz++) {
         for (int ix = 0; ix < this.size; ix++) {
            double wx = ((double)minX + ((double)ix + 0.5) / (double)this.ppb) / 1.7F;
            double wz = ((double)minZ + ((double)iz + 0.5) / (double)this.ppb) / 1.7F;
            int gx = Mth.floor(wx);
            int gz = Mth.floor(wz);
            double f1 = 9.0;
            double f2 = 9.0;

            for (int ox = -1; ox <= 1; ox++) {
               for (int oz = -1; oz <= 1; oz++) {
                  long h = hash(gx + ox, gz + oz);
                  double cx = (double)(gx + ox) + (double)(h & 65535L) / 65536.0 * 0.8 + 0.1;
                  double cz = (double)(gz + oz) + (double)(h >>> 16 & 65535L) / 65536.0 * 0.8 + 0.1;
                  double d = Math.sqrt((wx - cx) * (wx - cx) + (wz - cz) * (wz - cz));
                  if (d < f1) {
                     f2 = f1;
                     f1 = d;
                  } else if (d < f2) {
                     f2 = d;
                  }
               }
            }

            this.edge[iz * this.size + ix] = (float)((f2 - f1) * 1.7F);
         }
      }

      Minecraft.getInstance()
         .getSoundManager()
         .play(new LoopSound(SunaSounds.SECCO_LOOP.get(), null, centre, 1.0F, 0.85F, 0, () -> !this.isDone() && this.closing < 0));
   }

   private static long hash(int x, int z) {
      long h = (long)x * 3129871L ^ (long)z * 116129781L;
      h = h * h * 42317861L + h * 11L;
      return h >> 16;
   }

   public float shown() {
      return this.shown;
   }

   @Override
   protected float shownRadius(float partialAge) {
      return (float)GroundSeccoAbility.radius((double)this.radius, (int)partialAge, this.growTicks);
   }

   @Override
   public void tick(ClientLevel level) {
      if (this.age > this.maxAge) {
         this.close();
      }

      if (this.closing < 0) {
         this.shown = this.shownRadius((float)(this.age + 1));
      }

      super.tick(level);
      if (!this.isDone() && this.closing < 0 && VfxSettings.legacy()) {
         this.particles();
      }
   }

   @Override
   protected void paint(NativeImage img) {
      float r = this.shown;
      int[] p = this.palette;

      for (int iz = 0; iz < this.size; iz++) {
         for (int ix = 0; ix < this.size; ix++) {
            int i = iz * this.size + ix;
            float d = this.dist[i];
            int color = 0;
            if (d <= r + this.noise[i] * 0.5F) {
               float e = this.edge[i];
               float b = BAYER[(iz & 3) * 4 + (ix & 3)] / 16.0F;
               float width = 0.07F + 0.05F * Mth.clamp((r - d) / 2.0F, 0.0F, 1.0F);
               if (e < width) {
                  color = e < width * 0.5F ? p[0] : p[1];
               } else if (e < width + 0.07F) {
                  color = b < 0.55F ? p[2] : p[3];
               } else if (e < width + 0.13F && b < 0.35F) {
                  color = p[5];
               }
            }

            img.setPixelRGBA(ix, iz, color);
         }
      }
   }

   private void particles() {
      RandomSource r = Fx.random();
      int n = Fx.count(this.shown * 0.5F);

      for (int i = 0; i < n; i++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         double d = Math.sqrt(r.nextDouble()) * (double)this.shown;
         double x = this.centre.x + Math.cos(a) * d;
         double z = this.centre.z + Math.sin(a) * d;
         float y = this.surfaceAt(x, z);
         if (!Float.isNaN(y)) {
            Fx.spawn(
               r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.DRY_FLAKE.get() : (ParticleOptions)SunaParticles.DUST.get(),
               x,
               (double)y + 0.08,
               z,
               Fx.spread(0.015),
               0.03 + r.nextDouble() * 0.03,
               Fx.spread(0.015)
            );
         }
      }
   }
}
