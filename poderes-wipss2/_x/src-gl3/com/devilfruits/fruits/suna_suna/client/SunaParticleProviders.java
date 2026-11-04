package com.devilfruits.fruits.suna_suna.client;

import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.devilfruits.fruits.suna_suna.client.vfx.SandCinematic;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public final class SunaParticleProviders {
   private static final SunaParticleProviders.Style GRAIN = new SunaParticleProviders.Style(
      18, 40, 0.55F, 0.96F, 0.025F, 0.045F, 1.0F, false, 0.0F, false, true
   );
   private static final SunaParticleProviders.Style CLUMP = new SunaParticleProviders.Style(22, 44, 0.9F, 0.96F, 0.055F, 0.085F, 1.0F, false, 0.3F, false, true);
   private static final SunaParticleProviders.Style DUST = new SunaParticleProviders.Style(22, 44, -0.004F, 0.9F, 0.06F, 0.12F, 1.012F, true, 0.0F, true, false);
   private static final SunaParticleProviders.Style WET_CLUMP = new SunaParticleProviders.Style(
      16, 26, 1.1F, 0.98F, 0.04F, 0.07F, 1.0F, false, 0.0F, false, true
   );
   private static final SunaParticleProviders.Style DRY_FLAKE = new SunaParticleProviders.Style(
      24, 44, 0.12F, 0.92F, 0.035F, 0.06F, 1.0F, false, 0.6F, false, true
   );

   public static void register(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet(
         (ParticleType)SunaParticles.GRAIN.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> cinematic(0, level, x, y, z, vx, vy, vz)
                  ? null
                  : new SunaParticleProviders.Sand(level, x, y, z, vx, vy, vz, sprites, GRAIN)
      );
      event.registerSpriteSet(
         (ParticleType)SunaParticles.CLUMP.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> cinematic(1, level, x, y, z, vx, vy, vz)
                  ? null
                  : new SunaParticleProviders.Sand(level, x, y, z, vx, vy, vz, sprites, CLUMP)
      );
      event.registerSpriteSet(
         (ParticleType)SunaParticles.DUST.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> cinematic(2, level, x, y, z, vx, vy, vz)
                  ? null
                  : new SunaParticleProviders.Sand(level, x, y, z, vx, vy, vz, sprites, DUST)
      );
      event.registerSpriteSet(
         (ParticleType)SunaParticles.WET_CLUMP.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> cinematic(3, level, x, y, z, vx, vy, vz)
                  ? null
                  : new SunaParticleProviders.Sand(level, x, y, z, vx, vy, vz, sprites, WET_CLUMP)
      );
      event.registerSpriteSet(
         (ParticleType)SunaParticles.DRY_FLAKE.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new SunaParticleProviders.Sand(level, x, y, z, vx, vy, vz, sprites, DRY_FLAKE)
      );
      event.registerSpriteSet(
         (ParticleType)SunaParticles.DEHYDRATED.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new SunaParticleProviders.Sand(
                  level, x, y, z, (level.random.nextDouble() - 0.5) * 0.02, 0.02, (level.random.nextDouble() - 0.5) * 0.02, sprites, DRY_FLAKE
               )
      );
   }

   private static boolean cinematic(int kind, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
      if (VfxSettings.legacy()) {
         return false;
      } else {
         RandomSource r = level.random;
         int tint = SandCinematic.lit(level, x, y, z);
         switch (kind) {
            case 0:
               if (r.nextInt(3) == 0) {
                  SandCinematic.grains(r, x, y, z, vx, vy, vz, tint).size(0.55F).life(0.7F);
               }
               break;
            case 1:
               SandCinematic.grains(r, x, y, z, vx, vy, vz, tint).size(0.8F).life(0.8F);
               break;
            case 2:
               SandCinematic.puff(r, x, y, z, vx, vy, vz, tint).size(0.55F).alpha(0.75F);
               break;
            default:
               SandCinematic.grains(r, x, y, z, vx, vy - 0.05, vz, SandCinematic.scale(tint, 0.55F)).size(0.6F).life(0.6F);
         }

         return true;
      }
   }

   private SunaParticleProviders() {
   }

   static final class Sand extends TextureSheetParticle {
      private final SpriteSet sprites;
      private final SunaParticleProviders.Style style;
      private final float spinSpeed;

      Sand(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, SunaParticleProviders.Style style) {
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
         this.hasPhysics = style.physics;
         this.spinSpeed = (this.random.nextFloat() - 0.5F) * style.spin;
         this.roll = this.random.nextFloat() * (float) (Math.PI * 2);
         this.oRoll = this.roll;
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

         if (this.style.translucent) {
            float life = (float)this.age / (float)this.lifetime;
            this.alpha = life < 0.6F ? 0.8F : 0.8F * (1.0F - (life - 0.6F) / 0.4F);
         }
      }

      public float getQuadSize(float partialTick) {
         if (this.style.translucent) {
            return this.quadSize;
         } else {
            float left = ((float)(this.lifetime - this.age) - partialTick) / 6.0F;
            return this.quadSize * Mth.clamp(left, 0.0F, 1.0F);
         }
      }

      public ParticleRenderType getRenderType() {
         return this.style.translucent ? ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT : ParticleRenderType.PARTICLE_SHEET_OPAQUE;
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
      boolean animate,
      float spin,
      boolean translucent,
      boolean physics
   ) {
   }
}
