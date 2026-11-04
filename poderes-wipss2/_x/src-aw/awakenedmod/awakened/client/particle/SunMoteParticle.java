package com.awakenedmod.awakened.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class SunMoteParticle extends TextureSheetParticle {
   private static final int FULL_BRIGHT = 15728880;

   private SunMoteParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
      super(level, x, y, z);
      this.lifetime = 12 + this.random.nextInt(8);
      this.setSprite(sprites.get(0, this.lifetime));
      this.hasPhysics = false;
      this.gravity = 0.0F;
      this.friction = 0.9F;
      this.quadSize = this.quadSize * (0.45F + this.random.nextFloat() * 0.35F);
      this.xd = xd * 0.2;
      this.yd = 0.015 + yd * 0.2;
      this.zd = zd * 0.2;
      this.alpha = 0.0F;
      float warm = 0.62F + this.random.nextFloat() * 0.16F;
      this.setColor(1.0F, warm, 0.28F);
   }

   public void tick() {
      super.tick();
      if (!this.removed) {
         float f = (float)this.age / (float)this.lifetime;
         this.alpha = f < 0.2F ? f / 0.2F : Mth.clamp(1.0F - (f - 0.2F) / 0.8F, 0.0F, 1.0F);
      }
   }

   public float getQuadSize(float partialTick) {
      return this.quadSize;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public int getLightColor(float partialTick) {
      return 15728880;
   }

   public static final class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
         return new SunMoteParticle(level, x, y, z, xd, yd, zd, this.sprites);
      }
   }
}
