package com.awakenedmod.awakened.client.particle;

import com.awakenedmod.awakened.content.particle.NukeFxOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

public final class WarpMistParticle extends TextureSheetParticle {
   private static final float FADE_IN = 0.2F;
   private static final float FADE_OUT = 0.55F;
   private static final float SPIN = 0.02F;
   private final float fullSize;

   private WarpMistParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, float scale, SpriteSet sprites) {
      super(level, x, y, z);
      this.fullSize = scale;
      this.lifetime = 18 + this.random.nextInt(10);
      this.setSprite(sprites.get(0, this.lifetime));
      this.gravity = 0.0F;
      this.friction = 0.9F;
      this.hasPhysics = false;
      this.alpha = 0.0F;
      this.xd = xd;
      this.yd = yd;
      this.zd = zd;
      this.roll = this.random.nextFloat() * (float) (Math.PI * 2);
      this.oRoll = this.roll;
      float f = 0.75F + this.random.nextFloat() * 0.25F;
      if (this.random.nextInt(4) == 0) {
         this.setColor(0.08F * f, 0.05F * f, 0.17F * f);
      } else {
         this.setColor(0.11F * f, 0.07F * f, 0.15F * f);
      }
   }

   public float getQuadSize(float partialTick) {
      float lifeF = Mth.clamp(((float)this.age + partialTick) / (float)this.lifetime, 0.0F, 1.0F);
      return this.fullSize * (0.6F + 0.4F * Math.min(1.0F, lifeF / 0.3F));
   }

   public void tick() {
      super.tick();
      if (!this.removed) {
         float peak = 0.6F;
         float lifeF = (float)this.age / (float)this.lifetime;
         if (lifeF < 0.2F) {
            this.alpha = peak * (lifeF / 0.2F);
         } else if (lifeF >= 0.55F) {
            this.alpha = peak * Mth.clamp(1.0F - (lifeF - 0.55F) / 0.45F, 0.0F, 1.0F);
         } else {
            this.alpha = peak;
         }

         this.oRoll = this.roll;
         this.roll += 0.02F;
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public static final class Provider implements ParticleProvider<NukeFxOptions> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(NukeFxOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
         return new WarpMistParticle(level, x, y, z, xd, yd, zd, options.scale, this.sprites);
      }
   }
}
