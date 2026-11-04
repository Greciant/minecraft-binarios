package com.awakenedmod.awakened.content.shadow;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class ShadowFx {
   static final int VOID = 5386362;
   static final int SHADOW_BLACK = 788500;
   private static final float DIM = 0.3F;

   private ShadowFx() {
   }

   static void murk(ServerLevel l, Vec3 p, int count, double spread) {
      Fx.burst(l, ParticleTypes.SQUID_INK, p, count, spread, 0.02);
      Fx.burst(l, ParticleTypes.LARGE_SMOKE, p, Math.max(1, count / 2), spread * 0.8, 0.01);
      Fx.burst(l, ParticleTypes.SMOKE, p, Math.max(1, count / 2), spread, 0.01);
   }

   static void wisps(ServerLevel l, Vec3 p, int count, double spread) {
      Fx.burst(l, ParticleTypes.WITCH, p, count, spread, 0.0);
      Fx.burst(l, ParticleTypes.SCULK_SOUL, p, Math.max(1, count / 2), spread, 0.0);
   }

   private static void glow(ServerLevel l, VfxEntity.Kind kind, Vec3 pos, double scale, Vec3 secondary, int lifetime) {
      Vfx.spawn(l, kind, pos, scale, secondary, lifetime, 5386362, 0.3F);
   }

   static void awaken(ServerLevel l, Vec3 center, Vec3 ground) {
      glow(l, VfxEntity.Kind.DOME, ground, 3.4, Vec3.ZERO, 18);
      Vfx.sphere(l, center, 2.2, 5386362, 12, 0.3F);
      glow(l, VfxEntity.Kind.RING, ground, 4.0, Vec3.ZERO, 16);
      murk(l, center, 80, 1.6);
      wisps(l, center, 30, 1.3);
      Fx.sound(l, center, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.7F, 1.4F);
      Fx.sound(l, center, SoundEvents.ENDERMAN_SCREAM, SoundSource.PLAYERS, 0.6F, 0.7F);
   }

   static void slashLaunch(ServerLevel l, Vec3 hand) {
      Fx.sound(l, hand, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 0.7F);
   }

   static void slashImpact(ServerLevel l, Vec3 p) {
      Fx.sound(l, p, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.9F, 0.75F);
   }

   static void boltTrail(ServerLevel l, Vec3 p, double radius) {
      Vfx.sphere(l, p, radius, 5386362, 3, 0.24000001F);
      l.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y, p.z, 2, radius * 0.4, radius * 0.4, radius * 0.4, 0.0);
      l.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 1, radius * 0.3, radius * 0.3, radius * 0.3, 0.0);
   }

   static void orbBurst(ServerLevel l, Vec3 p, double radius) {
      Vfx.sphere(l, p, radius * 1.25, 5386362, 14, 0.3F);
      Vfx.spawn(l, VfxEntity.Kind.DOME, p, radius * 1.7, Vec3.ZERO, 16, 5386362, 0.3F);
      Vfx.spawn(l, VfxEntity.Kind.RING, p, radius * 2.2, Vec3.ZERO, 18, 5386362, 0.3F);
      Vfx.spawn(l, VfxEntity.Kind.PILLAR, p, radius * 0.6, new Vec3(0.0, radius * 2.2, 0.0), 20, 5386362, 0.3F);
      murk(l, p, (int)(44.0 + radius * 16.0), radius * 0.9);
      wisps(l, p, (int)(radius * 10.0), radius * 0.9);
      Fx.sound(l, p, SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.4F, 0.5F);
      Fx.sound(l, p, SoundEvents.ENDERMAN_SCREAM, SoundSource.PLAYERS, 1.0F, 0.55F);
      Fx.sound(l, p, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.7F, 1.2F);
   }

   static void chokeGrip(ServerLevel l, Vec3 center, double height) {
      if (l.random.nextInt(3) == 0) {
         double a = l.random.nextDouble() * Math.PI * 2.0;
         double r = 0.55 + l.random.nextDouble() * 0.3;
         Vec3 wisp = center.add(Math.cos(a) * r, (l.random.nextDouble() - 0.5) * height * 0.6, Math.sin(a) * r);
         l.sendParticles(ParticleTypes.SCULK_SOUL, wisp.x, wisp.y, wisp.z, 1, 0.0, 0.02, 0.0, 0.0);
      }
   }

   static void chokeSeize(ServerLevel l, Vec3 center) {
      Fx.burst(l, ParticleTypes.SQUID_INK, center, 6, 0.35, 0.02);
      wisps(l, center, 4, 0.4);
      Fx.sound(l, center, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.6F);
      Fx.sound(l, center, SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.6F, 0.7F);
   }

   static void devourOpen(ServerLevel l, Vec3 center, double radius) {
      Vfx.spawn(l, VfxEntity.Kind.RING, center, radius, Vec3.ZERO, 20, 5386362, 0.3F);
      Vfx.spawn(l, VfxEntity.Kind.PILLAR, center, radius * 0.5, new Vec3(0.0, radius * 2.0, 0.0), 24, 5386362, 0.3F);
      murk(l, center, (int)(40.0 + radius * 12.0), radius * 0.7);
      wisps(l, center.add(0.0, 1.0, 0.0), (int)(radius * 6.0), radius * 0.7);
      Fx.sound(l, center, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.2F, 0.6F);
      Fx.sound(l, center, SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.0F, 0.5F);
   }

   static void devourDrag(ServerLevel l, Vec3 p, float width) {
      double r = 0.3 + (double)width * 0.5;
      l.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y + 0.4, p.z, 3, r, 0.3, r, 0.04);
      l.sendParticles(ParticleTypes.SMOKE, p.x, p.y + 0.2, p.z, 2, r * 0.6, 0.2, r * 0.6, 0.0);
      if (l.random.nextInt(3) == 0) {
         l.sendParticles(ParticleTypes.SCULK_SOUL, p.x, p.y + 0.6, p.z, 1, r * 0.4, 0.1, r * 0.4, 0.0);
      }
   }

   static void devourTick(ServerLevel l, Vec3 center, double radius, long age) {
      for (int i = 0; i < 12; i++) {
         double a = (double)i / 12.0 * Math.PI * 2.0 + (double)age * 0.22;
         double r = radius * (0.3 + 0.7 * ((double)(i % 4) / 3.0));
         double y = 0.2 + (double)(i % 5) * 0.25;
         Vec3 p = center.add(Math.cos(a) * r, y, Math.sin(a) * r);
         Vec3 inward = center.add(0.0, 0.1, 0.0).subtract(p).normalize().scale(0.12);
         l.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y, p.z, 0, inward.x, inward.y, inward.z, 1.0);
         if (i % 3 == 0) {
            l.sendParticles(ParticleTypes.SCULK_SOUL, p.x, p.y + 0.3, p.z, 0, inward.x, 0.05, inward.z, 1.0);
         }
      }

      if (age % 6L == 0L) {
         Vfx.spawn(l, VfxEntity.Kind.RING, center, radius, Vec3.ZERO, 8, 5386362, 0.24000001F);
      }

      if (age % 80L == 0L) {
         Fx.sound(l, center, SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 0.7F, 0.5F);
      }
   }
}
