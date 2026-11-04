package com.devilfruits.mobs.pirate.client;

import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.render.vfx.ShockwaveEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.mobs.pirate.PirateFx;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.registry.DFParticles;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public final class PirateClientFx {
   static void register() {
      ClientFxRegistry.register(PirateFx.SLAM, PirateClientFx::slam);
      ClientFxRegistry.register(PirateFx.RALLY, PirateClientFx::rally);
      ClientFxRegistry.register(PirateFx.ENRAGE, PirateClientFx::enrage);
   }

   static void registerParticles(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet(
         (ParticleType)Pirates.MUZZLE_FLASH.get(),
         sprites -> (type, level, x, y, z, vx, vy, vz) -> new PirateClientFx.MuzzleFlash(level, x, y, z, (float)vx, sprites)
      );
   }

   private static void slam(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = p.pos();
      float radius = Math.max(1.5F, p.power());
      VfxManager.add(new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at, Vec3.ZERO, radius, 0.0F, 14, 3, 0.85F));
      RandomSource r = RandomSource.create(p.seed());
      int n = 28;

      for (int i = 0; i < n; i++) {
         double a = (Math.PI * 2) * (double)i / (double)n + r.nextDouble() * 0.2;
         double c = Math.cos(a);
         double s = Math.sin(a);
         double d = 0.6 + r.nextDouble() * 0.5;
         level.addParticle(
            (ParticleOptions)DFParticles.DUST_PUFF.get(), at.x + c * d, at.y + 0.1, at.z + s * d, c * 0.25, 0.04 + r.nextDouble() * 0.05, s * 0.25
         );
      }
   }

   private static void rally(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = entity != null ? entity.position() : p.pos();
      RandomSource r = RandomSource.create(p.seed());

      for (int i = 0; i < 36; i++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         double d = 0.8 + r.nextDouble() * 1.4;
         level.addParticle(
            ParticleTypes.CRIT,
            at.x + Math.cos(a) * d,
            at.y + 0.2 + r.nextDouble() * 0.6,
            at.z + Math.sin(a) * d,
            Math.cos(a) * 0.15,
            0.35 + r.nextDouble() * 0.2,
            Math.sin(a) * 0.15
         );
      }

      level.addParticle(ParticleTypes.ANGRY_VILLAGER, at.x, at.y + (entity != null ? (double)entity.getBbHeight() + 0.4 : 3.0), at.z, 0.0, 0.0, 0.0);
   }

   private static void enrage(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = entity != null ? entity.position() : p.pos();
      float h = entity != null ? entity.getBbHeight() : 2.6F;
      RandomSource r = RandomSource.create(p.seed());

      for (int i = 0; i < 24; i++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         level.addParticle(
            ParticleTypes.LAVA, at.x + Math.cos(a) * 0.5, at.y + (double)h * (0.3 + r.nextDouble() * 0.5), at.z + Math.sin(a) * 0.5, 0.0, 0.0, 0.0
         );
      }

      VfxManager.add(new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at, Vec3.ZERO, 3.0F, 0.0F, 10, 2, 0.6F));
   }

   private PirateClientFx() {
   }

   static final class MuzzleFlash extends TextureSheetParticle {
      private final SpriteSet sprites;
      private final float size;

      MuzzleFlash(ClientLevel level, double x, double y, double z, float size, SpriteSet sprites) {
         super(level, x, y, z);
         this.sprites = sprites;
         this.size = Math.max(0.3F, size) * 0.5F;
         this.lifetime = 3;
         this.gravity = 0.0F;
         this.hasPhysics = false;
         this.xd = this.yd = this.zd = 0.0;
         this.roll = this.oRoll = level.random.nextFloat() * (float) (Math.PI * 2);
         this.setSpriteFromAge(sprites);
      }

      public void tick() {
         super.tick();
         this.setSpriteFromAge(this.sprites);
      }

      public float getQuadSize(float partialTick) {
         return this.size * (0.7F + 0.5F * ((float)this.age + partialTick) / (float)this.lifetime);
      }

      protected int getLightColor(float partialTick) {
         return 15728880;
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }
   }
}
