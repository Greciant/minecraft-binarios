package com.devilfruits.fruits.uo_uo_seiryu.client.particle;

import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleType;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public final class SeiryuParticleProviders {
   private static final int FULL_BRIGHT = 15728880;
   private static final SeiryuParticleProviders.Style FLAME = new SeiryuParticleProviders.Style(
      12, 22, -0.035F, 0.9F, 0.12F, 0.24F, 1.03F, true, true, 0.0F, 0.0F, 1.0F
   );
   private static final SeiryuParticleProviders.Style EMBER = new SeiryuParticleProviders.Style(
      18, 40, -0.012F, 0.95F, 0.035F, 0.06F, 0.99F, true, false, 0.0F, 0.0F, 1.0F
   );
   private static final SeiryuParticleProviders.Style CLOUD = new SeiryuParticleProviders.Style(
      30, 60, 0.0F, 0.9F, 0.35F, 0.65F, 1.012F, false, false, 0.0F, 0.15F, 0.9F
   );
   private static final SeiryuParticleProviders.Style STORM_CLOUD = new SeiryuParticleProviders.Style(
      90, 150, 0.0F, 0.96F, 1.8F, 3.2F, 1.004F, false, false, 0.0F, 0.25F, 0.95F
   );
   private static final SeiryuParticleProviders.Style SCALE_SHARD = new SeiryuParticleProviders.Style(
      25, 45, 0.45F, 0.95F, 0.07F, 0.12F, 1.0F, false, false, 0.5F, 0.0F, 1.0F
   );
   private static final SeiryuParticleProviders.Style GOLD_SPARK = new SeiryuParticleProviders.Style(
      8, 16, 0.0F, 0.9F, 0.05F, 0.08F, 0.96F, true, true, 0.0F, 0.0F, 1.0F
   );
   private static final SeiryuParticleProviders.Style BOLT_SPARK = new SeiryuParticleProviders.Style(
      3, 7, 0.0F, 0.8F, 0.08F, 0.13F, 1.0F, true, false, 0.0F, 0.0F, 1.0F
   );
   private static final SeiryuParticleProviders.Style VIOLET_SPARK = new SeiryuParticleProviders.Style(
      4, 9, 0.0F, 0.82F, 0.07F, 0.12F, 0.97F, true, true, 0.0F, 0.0F, 1.0F
   );

   public static void register(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet(
         (ParticleType)SeiryuParticles.FLAME.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new SeiryuParticleProviders.Pixel(level, x, y, z, vx, vy, vz, sprites, FLAME)
      );
      event.registerSpriteSet(
         (ParticleType)SeiryuParticles.EMBER.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new SeiryuParticleProviders.Pixel(level, x, y, z, vx, vy, vz, sprites, EMBER)
      );
      event.registerSpriteSet(
         (ParticleType)SeiryuParticles.CLOUD.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new SeiryuParticleProviders.Pixel(level, x, y, z, vx, vy, vz, sprites, CLOUD)
      );
      event.registerSpriteSet(
         (ParticleType)SeiryuParticles.STORM_CLOUD.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new SeiryuParticleProviders.Pixel(level, x, y, z, vx, vy, vz, sprites, STORM_CLOUD)
      );
      event.registerSpriteSet(
         (ParticleType)SeiryuParticles.SCALE_SHARD.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new SeiryuParticleProviders.Pixel(level, x, y, z, vx, vy, vz, sprites, SCALE_SHARD)
      );
      event.registerSpriteSet(
         (ParticleType)SeiryuParticles.GOLD_SPARK.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new SeiryuParticleProviders.Pixel(level, x, y, z, vx, vy, vz, sprites, GOLD_SPARK)
      );
      event.registerSpriteSet(
         (ParticleType)SeiryuParticles.BOLT_SPARK.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new SeiryuParticleProviders.Pixel(level, x, y, z, vx, vy, vz, sprites, BOLT_SPARK)
      );
      event.registerSpriteSet(
         (ParticleType)SeiryuParticles.VIOLET_SPARK.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new SeiryuParticleProviders.Pixel(level, x, y, z, vx, vy, vz, sprites, VIOLET_SPARK)
      );
   }

   private SeiryuParticleProviders() {
   }

   static final class Pixel extends TextureSheetParticle {
      private final SpriteSet sprites;
      private final SeiryuParticleProviders.Style style;
      private final float spinSpeed;

      Pixel(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, SeiryuParticleProviders.Style style) {
         super(level, x, y, z);
         this.sprites = sprites;
         this.style = style;
         this.xd = vx;
         this.yd = vy;
         this.zd = vz;
         this.lifetime = style.minLife + this.random.nextInt(Math.max(1, style.maxLife - style.minLife));
         this.gravity = style.gravity;
         this.friction = style.friction;
         this.quadSize = style.minSize + this.random.nextFloat() * (style.maxSize - style.minSize);
         this.hasPhysics = style.gravity > 0.0F;
         this.spinSpeed = (this.random.nextFloat() - 0.5F) * style.spin;
         this.roll = this.random.nextFloat() * (float) (Math.PI * 2);
         this.oRoll = this.roll;
         this.alpha = style.fadeIn > 0.0F ? 0.0F : style.alpha;
         if (style.animate) {
            this.setSpriteFromAge(sprites);
         } else {
            this.pickSprite(sprites);
         }
      }

      public void tick() {
         super.tick();
         if (this.style.animate) {
            this.setSpriteFromAge(this.sprites);
         }

         this.quadSize = this.quadSize * this.style.grow;
         this.oRoll = this.roll;
         if (!this.onGround) {
            this.roll = this.roll + this.spinSpeed;
         }

         float life = (float)this.age / (float)this.lifetime;
         float in = this.style.fadeIn > 0.0F ? Math.min(1.0F, life / this.style.fadeIn) : 1.0F;
         float out = life < 0.7F ? 1.0F : 1.0F - (life - 0.7F) / 0.3F;
         this.alpha = this.style.alpha * in * out;
      }

      protected int getLightColor(float partialTick) {
         return this.style.fullBright ? 15728880 : super.getLightColor(partialTick);
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   private static record Style(
      int minLife,
      int maxLife,
      float gravity,
      float friction,
      float minSize,
      float maxSize,
      float grow,
      boolean fullBright,
      boolean animate,
      float spin,
      float fadeIn,
      float alpha
   ) {
   }
}
