package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.sound.LoopSound;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.fruits.suna_suna.SandFunnel;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.devilfruits.fruits.suna_suna.SunaSounds;
import com.devilfruits.fruits.suna_suna.block.QuicksandBlock;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class SandFunnelEffect extends SandPaintEffect {
   private static final int ARMS = 4;
   private static final float TIGHT = 2.6F;
   private static final float SPIN = 0.045F;
   private static final int CLOSE_TICKS = 16;
   private final int casterId;
   private final float throat;
   private final float pull;
   private final int depth;
   private final int growTicks;
   private final int maxAge;
   private final float[] phase;
   private final float[] lips;
   private float shown;

   public SandFunnelEffect(ClientLevel level, int casterId, Vec3 centre, float radius, Vec3 params, int maxTicks, long seed) {
      super(level, centre, radius, seed, "funnel", 16);
      this.casterId = casterId;
      this.pull = (float)params.x;
      this.throat = (float)params.y;
      int packed = (int)Math.round(params.z);
      this.depth = packed % 10;
      this.growTicks = Math.max(0, packed / 10);
      this.maxAge = Math.max(1, maxTicks) + 40;
      this.phase = new float[this.size * this.size];

      for (int i = 0; i < this.phase.length; i++) {
         this.phase[i] = 4.0F * this.angle[i] + 2.6F * (float)Math.log((double)this.dist[i] + 0.6) + this.noise[i] * 0.07F;
      }

      this.lips = new float[this.size * this.size];

      for (int i = 0; i < this.lips.length; i++) {
         float ang = this.angle[i] * (float) (Math.PI * 2);
         this.lips[i] = 0.2F + 0.9F * Math.max(0.0F, SandNoise.lumpy(Mth.cos(ang) * 2.5F + (float)(seed & 15L), Mth.sin(ang) * 2.5F, 3.0F) * 0.5F + 0.45F);
      }

      Minecraft.getInstance()
         .getSoundManager()
         .play(new LoopSound(SunaSounds.GIRASOLE_LOOP.get(), null, centre, 1.6F, 0.8F, 0, () -> !this.isDone() && this.closing < 0));
   }

   public float shown() {
      return this.shown;
   }

   public float throat() {
      return this.throat;
   }

   @Override
   protected float shownRadius(float partialAge) {
      float r = (float)SandFunnel.currentRadius(
         new SandFunnel.Shape((double)this.radius, (double)this.throat, this.depth, (double)this.pull, Math.max(1, this.growTicks)),
         this.growTicks <= 0 ? 1000000.0F : partialAge
      );
      return this.closing < 0 ? r : r * Math.max(0.0F, 1.0F - (float)this.closing / 16.0F);
   }

   @Override
   public void tick(ClientLevel level) {
      if (this.age > this.maxAge) {
         this.finish();
      } else {
         this.shown = this.shownRadius((float)(this.age + 1));
         super.tick(level);
         if (!this.isDone()) {
            if (VfxSettings.legacy()) {
               this.particles(this.shown);
            }

            if (this.closing < 0) {
               this.pullLocalPlayer(this.shown);
            }
         }
      }
   }

   private void pullLocalPlayer(float r) {
      LocalPlayer me = Minecraft.getInstance().player;
      if (me != null
         && me.getId() != this.casterId
         && !me.isSpectator()
         && !me.getAbilities().flying
         && !QuicksandBlock.walksOn(me)
         && SandFunnel.caught(this.centre, me.position(), (double)r, this.depth)) {
         Vec3 v = SandFunnel.pull(this.centre, me.position(), (double)r, (double)this.throat, (double)this.pull);
         me.push(v.x, 0.0, v.z);
      }
   }

   @Override
   protected void paint(NativeImage img) {
      float r = this.shown;
      float spin = this.closing >= 0 ? (float)(this.age - this.closing) * 0.045F : (float)this.age * 0.045F;
      int[] p = this.palette;
      float span = Math.max(0.01F, r - this.throat);

      for (int iz = 0; iz < this.size; iz++) {
         for (int ix = 0; ix < this.size; ix++) {
            int i = iz * this.size + ix;
            float d = this.dist[i];
            int color = 0;
            if (d <= r) {
               float v = this.phase[i] + spin;
               v -= (float)Mth.floor(v);
               float b = BAYER[(iz & 3) * 4 + (ix & 3)] / 16.0F;
               float edge = r - d;
               float lip = VfxSettings.legacy() ? 0.45F : this.lips[i];
               if (edge < lip) {
                  if (b < 0.15F + edge / lip * 0.85F) {
                     color = v >= 0.5F && v < 0.6F ? p[6] : (b < 0.4F ? p[4] : p[5]);
                  }
               } else if (d < this.throat) {
                  float t = d / this.throat;
                  int base = t < 0.35F ? 0 : (t < 0.7F ? (b < (t - 0.35F) / 0.35F ? 1 : 0) : (b < (t - 0.7F) / 0.3F ? 2 : 1));
                  if (v < 0.16F) {
                     base = Math.max(0, base - 1);
                  } else if (v >= 0.5F && v < 0.56F) {
                     base = Math.min(3, base + 1);
                  }

                  color = p[base];
               } else {
                  float slope = 1.0F - (d - this.throat) / span;
                  float furrow = 0.1F + 0.08F * slope;
                  float w = v * 2.0F - (float)Mth.floor(v * 2.0F);
                  if (v < furrow) {
                     color = slope > 0.6F && b < (slope - 0.6F) * 2.5F ? p[1] : p[2];
                  } else if (v < furrow + 0.05F) {
                     color = b < 0.6F ? p[5] : p[4];
                  } else if (v < furrow + 0.08F && b < 0.4F) {
                     color = p[6];
                  } else if (w > 0.48F && w < 0.52F + 0.03F * slope && b < 0.7F) {
                     color = p[3];
                  } else if (slope > 0.45F && b < (slope - 0.45F) * 1.3F) {
                     color = p[3];
                  }
               }
            }

            img.setPixelRGBA(ix, iz, color);
         }
      }
   }

   private void particles(float r) {
      if (!(r < 0.5F)) {
         RandomSource rand = Fx.random();
         boolean open = this.closing < 0;
         int n = Fx.count(open ? r * 0.9F : r * 0.3F);

         for (int i = 0; i < n; i++) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double d = (double)this.throat + rand.nextDouble() * Math.max(0.1, (double)(r - this.throat));
            double x = this.centre.x + Math.cos(a) * d;
            double z = this.centre.z + Math.sin(a) * d;
            float y = this.surfaceAt(x, z);
            if (!Float.isNaN(y)) {
               double ix = -Math.cos(a);
               double iz = -Math.sin(a);
               double s = 0.05 + rand.nextDouble() * 0.06;
               Fx.spawn((ParticleOptions)SunaParticles.GRAIN.get(), x, (double)y + 0.06, z, (ix - iz * 0.7) * s, 0.02, (iz + ix * 0.7) * s);
            }
         }

         int dust = Fx.count(open ? r * 0.12F : r * 0.5F);

         for (int ix = 0; ix < dust; ix++) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double x = this.centre.x + Math.cos(a) * (double)r;
            double z = this.centre.z + Math.sin(a) * (double)r;
            float y = this.surfaceAt(x, z);
            if (!Float.isNaN(y)) {
               Fx.spawn((ParticleOptions)SunaParticles.DUST.get(), x, (double)y + 0.2, z, -Math.sin(a) * 0.03, 0.02, Math.cos(a) * 0.03);
            }
         }

         if (open) {
            int pour = Fx.count(this.throat * 1.2F);

            for (int ixx = 0; ixx < pour; ixx++) {
               double a = rand.nextDouble() * Math.PI * 2.0;
               double d = rand.nextDouble() * (double)this.throat;
               Fx.spawn(
                  rand.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
                  this.centre.x + Math.cos(a) * d,
                  this.centre.y + 0.15,
                  this.centre.z + Math.sin(a) * d,
                  -Math.sin(a) * 0.06,
                  -0.02,
                  Math.cos(a) * 0.06
               );
            }
         }
      }
   }
}
