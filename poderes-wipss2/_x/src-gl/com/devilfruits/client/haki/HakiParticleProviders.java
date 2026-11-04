package com.devilfruits.client.haki;

import com.devilfruits.registry.DFParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleType;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public final class HakiParticleProviders {
   private static final HakiParticleProviders.Style SHARD = new HakiParticleProviders.Style(14, 26, 0.7F, 0.94F, 0.05F, 0.085F, 0.5F, false, true);
   private static final HakiParticleProviders.Style EMBER = new HakiParticleProviders.Style(16, 30, -0.02F, 0.92F, 0.03F, 0.05F, 0.0F, false, false);
   private static final HakiParticleProviders.Style SPARK = new HakiParticleProviders.Style(6, 12, 0.1F, 0.86F, 0.035F, 0.06F, 0.0F, true, false);

   public static void register(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet(
         (ParticleType)DFParticles.HAKI_SHARD.get(),
         s -> (t, level, x, y, z, vx, vy, vz) -> new HakiParticleProviders.Bit(level, x, y, z, vx, vy, vz, s, SHARD)
      );
      event.registerSpriteSet(
         (ParticleType)DFParticles.HAKI_EMBER.get(),
         s -> (t, level, x, y, z, vx, vy, vz) -> new HakiParticleProviders.Bit(level, x, y, z, vx, vy, vz, s, EMBER)
      );
      event.registerSpriteSet(
         (ParticleType)DFParticles.HAKI_SPARK.get(),
         s -> (t, level, x, y, z, vx, vy, vz) -> new HakiParticleProviders.Bit(level, x, y, z, vx, vy, vz, s, SPARK)
      );
   }

   private HakiParticleProviders() {
   }

   static final class Bit extends TextureSheetParticle {
      private final SpriteSet sprites;
      private final HakiParticleProviders.Style style;
      private final float spin;

      Bit(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, HakiParticleProviders.Style style) {
         super(level, x, y, z);
         this.sprites = sprites;
         this.style = style;
         this.xd = vx;
         this.yd = vy;
         this.zd = vz;
         this.gravity = style.gravity();
         this.friction = style.friction();
         this.lifetime = style.minLife() + this.random.nextInt(Math.max(1, style.maxLife() - style.minLife()));
         this.quadSize = style.minSize() + this.random.nextFloat() * (style.maxSize() - style.minSize());
         this.spin = (this.random.nextFloat() - 0.5F) * style.spin();
         this.hasPhysics = style.gravity() > 0.3F;
         if (style.pick()) {
            this.pickSprite(sprites);
         } else {
            this.setSpriteFromAge(sprites);
         }
      }

      public void tick() {
         super.tick();
         if (!this.style.pick()) {
            this.setSpriteFromAge(this.sprites);
         }

         this.oRoll = this.roll;
         this.roll = this.roll + this.spin;
      }

      protected int getLightColor(float partialTick) {
         return this.style.bright() ? 15728880 : super.getLightColor(partialTick);
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
      }
   }

   private static record Style(int minLife, int maxLife, float gravity, float friction, float minSize, float maxSize, float spin, boolean bright, boolean pick) {
   }
}
