package com.awakenedmod.awakened.content.reaper;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ReaperFx {
   public static final int SOUL_BLUE = 6277344;
   public static final int VOID_BLACK = 526864;
   private static final float DIM = 0.28F;
   private static final int AURA_BODY_LOW = 8004632;
   private static final int AURA_BODY_HIGH = 13374484;
   private static final int AURA_CORE_LOW = 12734512;
   private static final int AURA_CORE_HIGH = 16728112;
   private static final int GROUND_CRACKLE_TIER = 5;
   private static final int HOVER_RENDER_LIFE = 4;
   private static final int MARK_GREEN = 4644938;

   private ReaperFx() {
   }

   public static float flameIntensityChest() {
      return 0.238F;
   }

   public static float flameIntensityBody(float tierF) {
      return 0.28F * (0.7F + tierF * 0.55F);
   }

   public static int auraBody(float f) {
      return lerpColor(8004632, 13374484, f);
   }

   public static int auraCore(float f) {
      return lerpColor(12734512, 16728112, f);
   }

   private static int lerpColor(int a, int b, float t) {
      t = t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
      int r = Math.round((float)(a >> 16 & 0xFF) + (float)((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
      int g = Math.round((float)(a >> 8 & 0xFF) + (float)((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
      int bl = Math.round((float)(a & 0xFF) + (float)((b & 0xFF) - (a & 0xFF)) * t);
      return r << 16 | g << 8 | bl;
   }

   public static void murk(ServerLevel l, Vec3 p, int count, double spread) {
      Fx.burst(l, ParticleTypes.SQUID_INK, p, count, spread, 0.02);
      Fx.burst(l, ParticleTypes.LARGE_SMOKE, p, Math.max(1, count / 2), spread * 0.8, 0.01);
      Fx.burst(l, ParticleTypes.SMOKE, p, Math.max(1, count / 2), spread, 0.01);
   }

   public static void wisps(ServerLevel l, Vec3 p, int count, double spread) {
      Fx.burst(l, ParticleTypes.SOUL, p, count, spread, 0.0);
      Fx.burst(l, ParticleTypes.SCULK_SOUL, p, Math.max(1, count / 2), spread, 0.0);
   }

   private static void glow(ServerLevel l, VfxEntity.Kind kind, Vec3 pos, double scale, Vec3 secondary, int lifetime) {
      Vfx.spawn(l, kind, pos, scale, secondary, lifetime, 6277344, 0.28F);
   }

   public static void aura(ServerLevel l, ServerPlayer player, int tier, float shown, boolean hideFromOwner) {
      if (!(shown <= 0.001F) && tier > 0) {
         long t = (long)player.tickCount;
         float tierF = (float)tier / 8.0F;
         int body = auraBody(tierF);
         int core = auraCore(tierF);
         DustParticleOptions emberDust = Fx.dust(body, 0.5F);
         double feetY = player.getY();
         double cx = player.getX();
         double cz = player.getZ();
         if (tier == 1) {
            if (t % 4L == 0L) {
               for (int i = 0; i < 2; i++) {
                  double a = rand(t + (long)i * 13L) * Math.PI * 2.0;
                  double r = 0.3 + rand(t + (long)i * 7L) * 0.3;
                  double y = feetY + 0.3 + rand(t + (long)i * 5L) * 1.3;
                  emit(l, player, hideFromOwner, emberDust, cx + Math.cos(a) * r, y, cz + Math.sin(a) * r, 1, 0.03, 0.08, 0.03, 0.02);
               }

               if (t % 16L == 0L) {
                  emit(l, player, hideFromOwner, ParticleTypes.SCULK_SOUL, cx, feetY + 0.8, cz, 1, 0.3, 0.5, 0.3, 0.0);
               }
            }
         } else if (tier == 2) {
            if (t % 2L == 0L) {
               double chestY = feetY + (double)(player.getBbHeight() * 0.55F);
               double a = rand(t) * Math.PI * 2.0;
               double r = 0.2 + rand(t + 9L) * 0.3;
               emit(
                  l, player, hideFromOwner, emberDust, cx + Math.cos(a) * r, chestY - 0.1 + rand(t + 3L) * 0.7, cz + Math.sin(a) * r, 1, 0.03, 0.1, 0.03, 0.03
               );
            }
         } else {
            float strength = tierF * shown;
            boolean fiery = tierF >= 0.5F;
            double bodyR = (double)ReaperAuraShape.bodyRadius(tier);
            double columnH = (double)ReaperAuraShape.bodyHeight(tier, player.getBbHeight(), shown);
            int updraft = 1 + (int)(strength * 8.0F);

            for (int i = 0; i < updraft; i++) {
               double ang = rand(t * 7L + (long)i * 17L) * Math.PI * 2.0;
               double r = bodyR * Math.sqrt(rand(t * 13L + (long)i * 31L));
               double x = cx + Math.cos(ang) * r;
               double z = cz + Math.sin(ang) * r;
               double y = feetY + 0.1 + rand(t * 5L + (long)i * 11L) * columnH * 0.85;
               emit(l, player, hideFromOwner, emberDust, x, y + 0.2, z, 1, 0.05, 0.16, 0.05, 0.02);
               if (fiery) {
                  emit(l, player, hideFromOwner, emberDust, x, y + 0.4, z, 1, 0.03, 0.14, 0.03, 0.03);
               }
            }

            if (t % 2L == 0L) {
               emit(l, player, hideFromOwner, ParticleTypes.SMOKE, cx, feetY + 0.1, cz, 1, bodyR * 0.5, 0.05, bodyR * 0.5, 0.01);
            }

            if (tier >= 5 && t % 3L == 0L) {
               Vec3 ground = new Vec3(cx, feetY + 0.06, cz);
               double reach = (1.0 + (double)(tier - 5) * 0.6) * (0.6 + 0.4 * (double)shown);
               int cracks = 1 + (tier - 5);

               for (int ix = 0; ix < cracks; ix++) {
                  double a = rand(t + (long)ix * 19L) * Math.PI * 2.0;
                  double rr = reach * (0.5 + 0.5 * rand(t + (long)ix * 7L));
                  Vec3 outer = ground.add(Math.cos(a) * rr, 0.05, Math.sin(a) * rr);
                  Vfx.effect(VfxEntity.Kind.ARC, ground)
                     .scale(0.08)
                     .secondary(outer.subtract(ground))
                     .color(body, core)
                     .lifetime(4)
                     .owner(player.getId())
                     .spawn(l);
               }

               for (int ix = 0; ix < cracks * 2; ix++) {
                  double a = rand(t + (long)ix * 11L) * Math.PI * 2.0;
                  double rr = reach * (0.4 + 0.6 * rand(t + (long)ix * 5L));
                  emit(l, player, hideFromOwner, emberDust, cx + Math.cos(a) * rr, feetY + 0.05, cz + Math.sin(a) * rr, 1, 0.02, 0.02, 0.02, 0.0);
               }
            }
         }
      }
   }

   private static void emit(
      ServerLevel l,
      ServerPlayer owner,
      boolean hideFromOwner,
      ParticleOptions p,
      double x,
      double y,
      double z,
      int count,
      double dx,
      double dy,
      double dz,
      double speed
   ) {
      if (!hideFromOwner) {
         l.sendParticles(p, x, y, z, count, dx, dy, dz, speed);
      } else {
         for (ServerPlayer viewer : l.players()) {
            if (viewer != owner && !(viewer.distanceToSqr(x, y, z) > 1024.0)) {
               l.sendParticles(viewer, p, false, x, y, z, count, dx, dy, dz, speed);
            }
         }
      }
   }

   private static double rand(long seed) {
      double s = Math.sin((double)seed * 12.9898) * 43758.5453;
      return s - Math.floor(s);
   }

   public static void awaken(ServerLevel l, Vec3 center, Vec3 ground) {
      glow(l, VfxEntity.Kind.DOME, ground, 3.2, Vec3.ZERO, 18);
      Vfx.sphere(l, center, 2.0, 6277344, 12, 0.28F);
      glow(l, VfxEntity.Kind.RING, ground, 4.0, Vec3.ZERO, 16);
      murk(l, center, 70, 1.6);
      wisps(l, center, 26, 1.3);
      Fx.sound(l, center, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.1F, 0.5F);
      Fx.sound(l, center, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.9F, 0.6F);
   }

   public static void harvest(ServerLevel l, Vec3 corpse, Vec3 killerChest, long value) {
      double scale = Math.min(1.0, Math.log1p((double)value) / Math.log1p(250.0));
      Vec3 from = corpse.add(0.0, 0.6, 0.0);
      wisps(l, from, (int)(6.0 + scale * 18.0), 0.4 + scale * 0.5);
      glow(l, VfxEntity.Kind.SPHERE, from, 0.6 + scale * 1.0, Vec3.ZERO, 8);
      l.sendParticles(ParticleTypes.SCULK_SOUL, from.x, from.y, from.z, (int)(4.0 + scale * 12.0), 0.2, 0.3, 0.2, 0.02);
      Vec3 mid = from.add(killerChest.subtract(from).scale(0.5)).add(0.0, 0.7, 0.0);
      int steps = (int)(8.0 + scale * 14.0);

      for (int i = 0; i <= steps; i++) {
         double t = (double)i / (double)steps;
         Vec3 p = quadBezier(from, mid, killerChest, t);
         l.sendParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
         if (i % 2 == 0) {
            l.sendParticles(ParticleTypes.SCULK_SOUL, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
         }
      }

      glow(l, VfxEntity.Kind.SPHERE, killerChest, 0.5 + scale * 0.6, Vec3.ZERO, 8);
      l.sendParticles(ParticleTypes.SOUL, killerChest.x, killerChest.y, killerChest.z, (int)(5.0 + scale * 10.0), 0.18, 0.25, 0.18, 0.01);
      Fx.sound(l, corpse, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, (float)(0.5 + scale * 0.4), (float)(1.2 - scale * 0.4));
      if (scale > 0.4) {
         Fx.sound(l, killerChest, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, (float)(0.3 + scale * 0.4), 1.4F);
      }
   }

   private static Vec3 quadBezier(Vec3 a, Vec3 b, Vec3 c, double t) {
      double u = 1.0 - t;
      return a.scale(u * u).add(b.scale(2.0 * u * t)).add(c.scale(t * t));
   }

   public static void barrageSpark(ServerLevel l, Vec3 origin) {
      l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, origin.x, origin.y, origin.z, 1, 0.05, 0.05, 0.05, 0.0);
      l.sendParticles(ParticleTypes.SOUL, origin.x, origin.y, origin.z, 1, 0.06, 0.06, 0.06, 0.0);
      Fx.sound(l, origin, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.28F, 1.5F);
   }

   public static void barrageHover(ServerLevel l, Vec3 pos) {
      Vfx.sphere(l, pos, 0.16, 6277344, 4, 0.238F);
      l.sendParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
   }

   public static void barrageVolley(ServerLevel l, Vec3 at) {
      Vfx.sphere(l, at, 0.7, 6277344, 6, 0.28F);
      Fx.sound(l, at, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.9F, 0.85F);
      Fx.sound(l, at, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.5F, 1.2F);
   }

   public static void targetSelect(ServerLevel l, LivingEntity e) {
      Vec3 c = e.getBoundingBox().getCenter();
      Vfx.ring(l, c, (double)e.getBbWidth() * 0.9 + 0.3, 4644938, 8, e.getYRot(), 0.0F);
      l.sendParticles(
         ParticleTypes.HAPPY_VILLAGER, c.x, c.y, c.z, 10, (double)e.getBbWidth() * 0.5, (double)e.getBbHeight() * 0.5, (double)e.getBbWidth() * 0.5, 0.0
      );
      Fx.sound(l, c, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.4F);
   }

   public static void targetMark(ServerLevel l, LivingEntity e) {
      Vec3 c = e.getBoundingBox().getCenter();
      Vfx.ring(l, c, (double)e.getBbWidth() * 0.85 + 0.25, 4644938, 4, e.getYRot(), 0.0F);
      l.sendParticles(
         ParticleTypes.HAPPY_VILLAGER, c.x, c.y, c.z, 1, (double)e.getBbWidth() * 0.5, (double)e.getBbHeight() * 0.5, (double)e.getBbWidth() * 0.5, 0.0
      );
   }

   public static void driftDissolve(ServerLevel l, Vec3 center) {
      murk(l, center, 40, 0.7);
      wisps(l, center, 14, 0.6);
      glow(l, VfxEntity.Kind.SPHERE, center, 1.0, Vec3.ZERO, 6);
      Fx.sound(l, center, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.7F, 0.7F);
   }

   public static void driftTrail(ServerLevel l, Vec3 p) {
      l.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y + 0.4, p.z, 2, 0.25, 0.4, 0.25, 0.0);
      l.sendParticles(ParticleTypes.SOUL, p.x, p.y + 0.4, p.z, 1, 0.2, 0.3, 0.2, 0.0);
   }

   public static void driftReform(ServerLevel l, Vec3 center) {
      murk(l, center, 30, 0.6);
      wisps(l, center, 16, 0.5);
      glow(l, VfxEntity.Kind.SPHERE, center, 1.1, Vec3.ZERO, 7);
      Fx.sound(l, center, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.7F, 1.1F);
   }

   public static void milestoneIgnite(ServerLevel l, Vec3 center, Vec3 ground) {
      glow(l, VfxEntity.Kind.DOME, ground, 4.2, Vec3.ZERO, 22);
      Vfx.sphere(l, center, 2.4, 6277344, 14, 0.28F);
      glow(l, VfxEntity.Kind.RING, ground, 5.0, Vec3.ZERO, 20);
      murk(l, center, 90, 1.9);
      wisps(l, center, 34, 1.6);
      Fx.sound(l, center, SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.8F, 0.6F);
      Fx.sound(l, center, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.7F, 0.7F);
   }

   public static void boltCast(ServerLevel l, Vec3 hand) {
      wisps(l, hand, 5, 0.25);
      l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, hand.x, hand.y, hand.z, 2, 0.1, 0.1, 0.1, 0.0);
      Fx.sound(l, hand, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
   }

   public static void boltTrail(ServerLevel l, Vec3 p, double radius) {
      Vfx.sphere(l, p, radius, 6277344, 3, 0.252F);
      l.sendParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 2, radius * 0.4, radius * 0.4, radius * 0.4, 0.0);
      l.sendParticles(ParticleTypes.SCULK_SOUL, p.x, p.y, p.z, 1, radius * 0.3, radius * 0.3, radius * 0.3, 0.0);
   }

   public static void boltImpact(ServerLevel l, Vec3 p, double radius) {
      Vfx.sphere(l, p, radius, 6277344, 8, 0.28F);
      wisps(l, p, (int)(6.0 + radius * 4.0), radius * 0.6);
      murk(l, p, (int)(8.0 + radius * 6.0), radius * 0.5);
      Fx.sound(l, p, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.7F, 0.8F);
   }

   public static void tetherBeam(ServerLevel l, Vec3 from, Vec3 to) {
      Vfx.beam(l, from, to, 0.06, 6277344, 3);
      Vfx.arc(l, from, to, 0.14, 6277344, 3);
   }

   public static void tetherDrain(ServerLevel l, Vec3 target, Vec3 caster, float phase) {
      int steps = 6;

      for (int i = 0; i <= steps; i++) {
         double t = ((double)i / (double)steps + (double)phase) % 1.0;
         Vec3 p = target.add(caster.subtract(target).scale(t));
         l.sendParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
      }

      l.sendParticles(ParticleTypes.SCULK_SOUL, target.x, target.y, target.z, 2, 0.12, 0.12, 0.12, 0.0);
   }

   public static void tetherSnap(ServerLevel l, Vec3 target, boolean cast) {
      Vfx.sphere(l, target, 0.7, 6277344, 6, 0.28F);
      wisps(l, target, 8, 0.4);
      Fx.sound(l, target, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.7F, cast ? 0.8F : 1.2F);
   }

   public static void hauntCast(ServerLevel l, Vec3 center) {
      Vfx.sphere(l, center, 0.9, 6277344, 8, 0.28F);
      wisps(l, center, 16, 0.7);
      l.sendParticles(ParticleTypes.WITCH, center.x, center.y, center.z, 10, 0.4, 0.5, 0.4, 0.02);
      Fx.sound(l, center, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.8F, 1.0F);
      Fx.sound(l, center, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 0.9F, 0.7F);
   }

   public static void hauntCling(ServerLevel l, Vec3 center, double width, long age) {
      double ang = (double)age * 0.4;
      double r = width * 0.7 + 0.35;

      for (int i = 0; i < 3; i++) {
         double a = ang + (double)i * (Math.PI * 2.0 / 3.0);
         Vec3 p = center.add(Math.cos(a) * r, Math.sin((double)age * 0.25 + (double)i) * 0.35, Math.sin(a) * r);
         l.sendParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
      }

      l.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y, center.z, 1, width * 0.5, 0.3, width * 0.5, 0.0);
      if (age % 6L == 0L) {
         l.sendParticles(ParticleTypes.WITCH, center.x, center.y, center.z, 2, width * 0.4, 0.3, width * 0.4, 0.0);
      }
   }

   public static void hauntWail(ServerLevel l, Vec3 center) {
      Fx.sound(l, center, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 0.4F, 1.3F);
   }

   public static void deathMarkCast(ServerLevel l, Vec3 chest, float yaw) {
      Vfx.ring(l, chest, 1.0, 6277344, 10, yaw, 90.0F);
      Vfx.sphere(l, chest, 0.6, 6277344, 7, 0.28F);
      wisps(l, chest, 10, 0.4);
      Fx.sound(l, chest, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.9F, 0.7F);
      Fx.sound(l, chest, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.7F, 0.8F);
   }

   public static void deathMarkBrand(ServerLevel l, Vec3 chest, float yaw) {
      Vfx.ring(l, chest, 0.85, 6277344, 4, yaw, 90.0F);
      l.sendParticles(ParticleTypes.SOUL, chest.x, chest.y, chest.z, 1, 0.15, 0.2, 0.15, 0.0);
      l.sendParticles(ParticleTypes.SCULK_SOUL, chest.x, chest.y, chest.z, 1, 0.12, 0.15, 0.12, 0.0);
   }

   public static void deathMarkLeap(ServerLevel l, Vec3 from, Vec3 to) {
      Vfx.beam(l, from, to, 0.1, 6277344, 6);
      int steps = 8;

      for (int i = 0; i <= steps; i++) {
         double t = (double)i / (double)steps;
         Vec3 p = from.add(to.subtract(from).scale(t));
         l.sendParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
      }

      Fx.sound(l, to, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 0.8F, 0.9F);
   }
}
