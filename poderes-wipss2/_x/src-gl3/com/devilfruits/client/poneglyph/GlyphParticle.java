package com.devilfruits.client.poneglyph;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public class GlyphParticle extends TextureSheetParticle {
   GlyphParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
      super(level, x, y, z);
      this.xd = vx + (this.random.nextDouble() - 0.5) * 0.02;
      this.yd = vy + 0.01;
      this.zd = vz + (this.random.nextDouble() - 0.5) * 0.02;
      this.friction = 0.9F;
      this.gravity = -0.01F;
      this.lifetime = 24 + this.random.nextInt(18);
      this.quadSize = 0.06F + this.random.nextFloat() * 0.04F;
      float tint = 0.8F + this.random.nextFloat() * 0.2F;
      this.setColor(0.55F * tint, 0.95F * tint, 1.0F * tint);
      this.pickSprite(sprites);
   }

   public void tick() {
      super.tick();
      this.alpha = Math.min(1.0F, (float)(this.lifetime - this.age) / 10.0F);
   }

   protected int getLightColor(float partialTick) {
      return 15728880;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }
}
