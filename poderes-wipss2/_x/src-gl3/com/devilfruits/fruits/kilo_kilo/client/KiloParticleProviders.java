package com.devilfruits.fruits.kilo_kilo.client;

import com.devilfruits.fruits.kilo_kilo.KiloParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

final class KiloParticleProviders {
   static void register(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet(
         (ParticleType)KiloParticles.FEATHER.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new KiloParticleProviders.Feather(level, x, y, z, vx, vy, vz, sprites)
      );
      event.registerSpriteSet(
         (ParticleType)KiloParticles.MOTE.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new KiloParticleProviders.Mote(level, x, y, z, vx, vy, vz, sprites)
      );
   }

   private KiloParticleProviders() {
   }

   static final class Feather extends TextureSheetParticle {
      private final float phase;
      private final float sway;

      Feather(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
         super(level, x, y, z);
         this.xd = vx;
         this.yd = vy;
         this.zd = vz;
         this.lifetime = 50 + this.random.nextInt(40);
         this.gravity = 0.012F;
         this.friction = 0.94F;
         this.quadSize = 0.09F + this.random.nextFloat() * 0.05F;
         this.hasPhysics = true;
         this.phase = this.random.nextFloat() * (float) (Math.PI * 2);
         this.sway = 0.02F + this.random.nextFloat() * 0.02F;
         this.pickSprite(sprites);
      }

      public void tick() {
         super.tick();
         float t = (float)this.age * 0.18F + this.phase;
         this.xd = this.xd + (double)(Mth.cos(t) * this.sway * 0.25F);
         this.zd = this.zd + (double)(Mth.sin(t * 0.7F) * this.sway * 0.25F);
         this.yd = Math.max(this.yd, -0.05);
         this.oRoll = this.roll;
         this.roll = Mth.sin(t) * 0.7F;
         float left = (float)(this.lifetime - this.age) / 12.0F;
         this.alpha = Mth.clamp(left, 0.0F, 1.0F);
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   static final class Mote extends TextureSheetParticle {
      Mote(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
         super(level, x, y, z);
         this.xd = vx;
         this.yd = vy;
         this.zd = vz;
         this.lifetime = 20 + this.random.nextInt(20);
         this.gravity = -0.004F;
         this.friction = 0.92F;
         this.quadSize = 0.04F + this.random.nextFloat() * 0.03F;
         this.hasPhysics = false;
         this.pickSprite(sprites);
      }

      public void tick() {
         super.tick();
         this.alpha = Mth.clamp((float)(this.lifetime - this.age) / 10.0F, 0.0F, 0.9F);
      }

      protected int getLightColor(float partialTick) {
         return 15728880;
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }
}
