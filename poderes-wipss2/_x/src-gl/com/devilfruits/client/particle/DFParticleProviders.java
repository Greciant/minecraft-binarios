package com.devilfruits.client.particle;

import com.devilfruits.registry.DFParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public final class DFParticleProviders {
   public static void register(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet(
         (ParticleType)DFParticles.DUST_PUFF.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new DFParticleProviders.DustPuff(level, x, y, z, vx, vy, vz, sprites)
      );
      event.registerSpecial(DFParticles.BLOCK_DEBRIS.get(), new DFParticleProviders.DebrisProvider());
      event.registerSpriteSet(
         (ParticleType)DFParticles.EXPLOSION_FIRE.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new DFParticleProviders.Blast(level, x, y, z, vx, vy, vz, sprites, true)
      );
      event.registerSpriteSet(
         (ParticleType)DFParticles.EXPLOSION_SMOKE.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new DFParticleProviders.Blast(level, x, y, z, vx, vy, vz, sprites, false)
      );
   }

   private DFParticleProviders() {
   }

   static final class Blast extends TextureSheetParticle {
      private final SpriteSet sprites;
      private final boolean fire;
      private final float size0;

      Blast(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, boolean fire) {
         super(level, x, y, z);
         this.sprites = sprites;
         this.fire = fire;
         double whole = Math.floor(vy);
         this.size0 = (float)Math.max(0.6, whole / 4.0);
         this.xd = vx;
         this.yd = vy - whole;
         this.zd = vz;
         this.lifetime = fire ? 14 + this.random.nextInt(8) : 40 + this.random.nextInt(30);
         this.gravity = fire ? -0.02F : -0.008F;
         this.friction = 0.88F;
         this.quadSize = this.size0;
         this.hasPhysics = false;
         float shade = fire ? 1.0F : 0.55F + this.random.nextFloat() * 0.2F;
         this.rCol = shade;
         this.gCol = shade;
         this.bCol = shade;
         this.roll = this.random.nextFloat() * 6.28F;
         this.oRoll = this.roll;
         this.setSpriteFromAge(sprites);
      }

      public void tick() {
         super.tick();
         this.setSpriteFromAge(this.sprites);
         float life = (float)this.age / (float)this.lifetime;
         this.quadSize = this.size0 * (1.0F + life * (this.fire ? 0.8F : 1.4F));
         this.alpha = this.fire ? 1.0F - life * life : (life < 0.2F ? life / 0.2F : 1.0F - (life - 0.2F) / 0.8F) * 0.85F;
      }

      public int getLightColor(float partialTick) {
         return this.fire ? 15728880 : super.getLightColor(partialTick);
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   static final class BlockDebris extends TerrainParticle {
      BlockDebris(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, BlockState state) {
         super(level, x, y, z, vx, vy, vz, state);
         this.xd = vx;
         this.yd = vy;
         this.zd = vz;
         this.gravity = 1.0F;
         this.friction = 0.97F;
         this.lifetime = 30 + this.random.nextInt(25);
         this.quadSize = this.quadSize * (1.5F + this.random.nextFloat());
      }
   }

   static final class DebrisProvider implements ParticleProvider<BlockParticleOption> {
      public Particle createParticle(BlockParticleOption option, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
         BlockState state = option.getState();
         return !state.isAir() && !state.is(Blocks.MOVING_PISTON) && state.getRenderShape() != RenderShape.INVISIBLE
            ? new DFParticleProviders.BlockDebris(level, x, y, z, vx, vy, vz, state)
            : null;
      }
   }

   static final class DustPuff extends TextureSheetParticle {
      private final SpriteSet sprites;

      DustPuff(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
         super(level, x, y, z);
         this.sprites = sprites;
         this.xd = vx;
         this.yd = vy;
         this.zd = vz;
         this.lifetime = 24 + this.random.nextInt(20);
         this.gravity = -0.01F;
         this.friction = 0.9F;
         this.quadSize = 0.25F + this.random.nextFloat() * 0.25F;
         float shade = 0.75F + this.random.nextFloat() * 0.2F;
         this.rCol = shade;
         this.gCol = shade * 0.95F;
         this.bCol = shade * 0.88F;
         this.setSpriteFromAge(sprites);
      }

      public void tick() {
         super.tick();
         this.setSpriteFromAge(this.sprites);
         float life = (float)this.age / (float)this.lifetime;
         this.alpha = life < 0.5F ? 0.85F : 0.85F * (1.0F - (life - 0.5F) / 0.5F);
      }

      public float getQuadSize(float partialTick) {
         float life = ((float)this.age + partialTick) / (float)this.lifetime;
         return this.quadSize * (0.6F + 0.8F * life);
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }
}
