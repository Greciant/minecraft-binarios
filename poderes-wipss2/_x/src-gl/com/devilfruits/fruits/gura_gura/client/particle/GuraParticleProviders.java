package com.devilfruits.fruits.gura_gura.client.particle;

import com.devilfruits.fruits.gura_gura.GuraParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleType;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public final class GuraParticleProviders {
   private static final int FULL_BRIGHT = 15728880;

   public static void register(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet(
         (ParticleType)GuraParticles.GLASS_SHARD.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new GuraParticleProviders.GlassShard(level, x, y, z, vx, vy, vz, sprites)
      );
      event.registerSpriteSet(
         (ParticleType)GuraParticles.CRACK_SPARK.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new GuraParticleProviders.CrackSpark(level, x, y, z, vx, vy, vz, sprites)
      );
      event.registerSpriteSet(
         (ParticleType)GuraParticles.WATER_SPRAY.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new GuraParticleProviders.WaterSpray(level, x, y, z, vx, vy, vz, sprites)
      );
      event.registerSpriteSet(
         (ParticleType)GuraParticles.IMPACT_FLASH.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new GuraParticleProviders.ImpactFlash(level, x, y, z, (float)vx, sprites)
      );
   }

   private GuraParticleProviders() {
   }

   static final class CrackSpark extends TextureSheetParticle {
      private final SpriteSet sprites;

      CrackSpark(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
         super(level, x, y, z);
         this.sprites = sprites;
         this.xd = vx;
         this.yd = vy;
         this.zd = vz;
         this.lifetime = 6 + this.random.nextInt(8);
         this.gravity = 0.0F;
         this.friction = 0.82F;
         this.hasPhysics = false;
         this.quadSize = 0.05F + this.random.nextFloat() * 0.05F;
         this.setSpriteFromAge(sprites);
      }

      public void tick() {
         super.tick();
         this.setSpriteFromAge(this.sprites);
         this.alpha = 1.0F - (float)this.age / (float)this.lifetime * 0.6F;
      }

      public float getQuadSize(float partialTick) {
         float life = ((float)this.age + partialTick) / (float)this.lifetime;
         return this.quadSize * (1.0F - life * 0.5F);
      }

      protected int getLightColor(float partialTick) {
         return 15728880;
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   static final class GlassShard extends TextureSheetParticle {
      private final float spin;

      GlassShard(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
         super(level, x, y, z);
         this.xd = vx;
         this.yd = vy;
         this.zd = vz;
         this.pickSprite(sprites);
         this.lifetime = 22 + this.random.nextInt(22);
         this.gravity = 0.55F + this.random.nextFloat() * 0.3F;
         this.friction = 0.94F;
         this.quadSize = 0.06F + this.random.nextFloat() * 0.1F;
         this.spin = (this.random.nextFloat() - 0.5F) * 0.6F;
         this.roll = this.random.nextFloat() * (float) (Math.PI * 2);
         this.oRoll = this.roll;
         float tint = 0.85F + this.random.nextFloat() * 0.15F;
         this.rCol = tint;
         this.gCol = 0.95F + this.random.nextFloat() * 0.05F;
         this.bCol = 1.0F;
      }

      public void tick() {
         super.tick();
         this.oRoll = this.roll;
         if (!this.onGround) {
            this.roll = this.roll + this.spin;
         }

         float life = (float)this.age / (float)this.lifetime;
         this.alpha = life < 0.7F ? 1.0F : 1.0F - (life - 0.7F) / 0.3F;
      }

      protected int getLightColor(float partialTick) {
         return 15728880;
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   static final class ImpactFlash extends TextureSheetParticle {
      private final SpriteSet sprites;
      private final float size;

      ImpactFlash(ClientLevel level, double x, double y, double z, float size, SpriteSet sprites) {
         super(level, x, y, z);
         this.sprites = sprites;
         this.size = Math.max(0.3F, size);
         this.lifetime = 5;
         this.gravity = 0.0F;
         this.hasPhysics = false;
         this.xd = this.yd = this.zd = 0.0;
         this.setSpriteFromAge(sprites);
      }

      public void tick() {
         super.tick();
         this.setSpriteFromAge(this.sprites);
      }

      public float getQuadSize(float partialTick) {
         float life = ((float)this.age + partialTick) / (float)this.lifetime;
         return this.size * (0.6F + 0.6F * life);
      }

      protected int getLightColor(float partialTick) {
         return 15728880;
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   static final class WaterSpray extends TextureSheetParticle {
      WaterSpray(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
         super(level, x, y, z);
         this.xd = vx;
         this.yd = vy;
         this.zd = vz;
         this.pickSprite(sprites);
         this.lifetime = 16 + this.random.nextInt(18);
         this.gravity = 0.45F;
         this.friction = 0.96F;
         this.quadSize = 0.12F + this.random.nextFloat() * 0.18F;
      }

      public void tick() {
         super.tick();
         float life = (float)this.age / (float)this.lifetime;
         this.alpha = life < 0.6F ? 0.95F : 0.95F * (1.0F - (life - 0.6F) / 0.4F);
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }
}
