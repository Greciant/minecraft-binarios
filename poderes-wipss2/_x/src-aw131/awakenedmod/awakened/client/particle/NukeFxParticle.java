package com.awakenedmod.awakened.client.particle;

import com.awakenedmod.awakened.content.particle.NukeFxOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

public final class NukeFxParticle extends TextureSheetParticle {
   private static final int FULL_BRIGHT = 15728880;
   private static final float FADE_IN = 0.15F;
   private final NukeFxParticle.Profile profile;
   private final float fullSize;
   public static final NukeFxParticle.Profile FLARE = new NukeFxParticle.Profile(
      10, 1.0F, 0.12F, 0.4F, false, 0.5F, true, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, false, 0.0F
   );
   public static final NukeFxParticle.Profile BURST = new NukeFxParticle.Profile(
      9, 1.0F, 0.55F, 0.3F, false, 0.55F, true, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, false, 0.0F
   );
   public static final NukeFxParticle.Profile BEAM = new NukeFxParticle.Profile(
      14, 1.0F, 1.0F, 0.01F, false, 0.6F, true, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, false, 0.0F
   );
   public static final NukeFxParticle.Profile SHOCK = new NukeFxParticle.Profile(
      12, 1.15F, 0.05F, 0.95F, false, 0.45F, true, 0.015F, 0.0F, 1.0F, 0.0F, 0.0F, false, 0.0F
   );
   public static final NukeFxParticle.Profile EMBER = new NukeFxParticle.Profile(
      18, 1.0F, 0.5F, 0.2F, true, 0.4F, true, 0.0F, -0.015F, 0.9F, 0.14F, 0.05F, false, 0.35F
   );
   public static final NukeFxParticle.Profile SMOKE = new NukeFxParticle.Profile(
      46, 1.4F, 0.55F, 0.85F, false, 0.62F, false, 0.01F, -0.006F, 0.94F, 0.07F, 0.04F, false, 0.35F
   );
   public static final NukeFxParticle.Profile SPARK = new NukeFxParticle.Profile(
      16, 1.0F, 0.85F, 0.1F, true, 0.3F, true, 0.2F, 0.05F, 0.94F, 0.28F, 0.16F, true, 0.2F
   );
   public static final NukeFxParticle.Profile FALLOUT = new NukeFxParticle.Profile(
      70, 1.0F, 0.4F, 0.55F, false, 0.55F, false, 0.0F, -0.002F, 0.96F, 0.05F, 0.02F, false, 0.45F
   );

   private NukeFxParticle(ClientLevel level, double x, double y, double z, float scale, SpriteSet sprites, NukeFxParticle.Profile profile) {
      super(level, x, y, z);
      this.profile = profile;
      this.fullSize = scale * profile.sizeMul;
      this.lifetime = profile.lifetime;
      this.setSprite(sprites.get(0, this.lifetime));
      this.gravity = profile.gravity;
      this.friction = profile.friction;
      this.hasPhysics = false;
      this.alpha = 0.0F;
      this.roll = this.random.nextFloat() * (float) (Math.PI * 2);
      this.oRoll = this.roll;
      this.xd = (double)((this.random.nextFloat() * 2.0F - 1.0F) * profile.driftXZ);
      this.yd = (double)(profile.driftY + (this.random.nextFloat() * 2.0F - 1.0F) * profile.driftXZ * 0.5F);
      this.zd = (double)((this.random.nextFloat() * 2.0F - 1.0F) * profile.driftXZ);
      if (profile.jitter > 0.0F) {
         float f = 1.0F - this.random.nextFloat() * profile.jitter;
         this.setColor(f, f, f);
      }
   }

   public float getQuadSize(float partialTick) {
      float lifeF = Mth.clamp(((float)this.age + partialTick) / (float)this.lifetime, 0.0F, 1.0F);
      float grow;
      if (lifeF <= this.profile.peakFrac) {
         grow = Mth.lerp(lifeF / Math.max(0.001F, this.profile.peakFrac), this.profile.startFrac, 1.0F);
      } else if (this.profile.shrink) {
         grow = Mth.lerp((lifeF - this.profile.peakFrac) / (1.0F - this.profile.peakFrac), 1.0F, 0.05F);
      } else {
         grow = 1.0F;
      }

      return this.fullSize * grow;
   }

   public void tick() {
      super.tick();
      if (!this.removed) {
         float lifeF = (float)this.age / (float)this.lifetime;
         float a;
         if (lifeF < 0.15F) {
            a = lifeF / 0.15F;
         } else if (lifeF >= this.profile.fadeFrac) {
            a = Mth.clamp(1.0F - (lifeF - this.profile.fadeFrac) / (1.0F - this.profile.fadeFrac), 0.0F, 1.0F);
         } else {
            a = 1.0F;
         }

         if (this.profile.flicker) {
            a *= 0.55F + 0.45F * Mth.abs(Mth.sin((float)this.age * 1.6F));
         }

         this.alpha = a;
         this.oRoll = this.roll;
         this.roll = this.roll + this.profile.spin;
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public int getLightColor(float partialTick) {
      return this.profile.glow ? 15728880 : super.getLightColor(partialTick);
   }

   public static record Profile(
      int lifetime,
      float sizeMul,
      float startFrac,
      float peakFrac,
      boolean shrink,
      float fadeFrac,
      boolean glow,
      float spin,
      float gravity,
      float friction,
      float driftXZ,
      float driftY,
      boolean flicker,
      float jitter
   ) {
   }

   public static final class Provider implements ParticleProvider<NukeFxOptions> {
      private final SpriteSet sprites;
      private final NukeFxParticle.Profile profile;

      public Provider(SpriteSet sprites, NukeFxParticle.Profile profile) {
         this.sprites = sprites;
         this.profile = profile;
      }

      public Particle createParticle(NukeFxOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
         return new NukeFxParticle(level, x, y, z, options.scale, this.sprites, this.profile);
      }
   }
}
