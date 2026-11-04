package com.awakenedmod.awakened.content.nuclear;

import com.awakenedmod.awakened.content.AwakenedParticles;
import com.awakenedmod.awakened.content.particle.NukeFxOptions;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Fx;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class NuclearFx {
   private static final List<NuclearFx.Blast> MUSHROOMS = new ArrayList<>();
   private static int lastTick = -1;
   private static final int GOLD = 16762170;

   private NuclearFx() {
   }

   public static void one(ServerLevel level, Supplier<ParticleType<NukeFxOptions>> type, Vec3 p, double scale) {
      level.sendParticles(new NukeFxOptions(type.get(), (float)scale), p.x, p.y, p.z, 0, 0.0, 0.0, 0.0, 0.0);
   }

   public static void scatter(ServerLevel level, Supplier<ParticleType<NukeFxOptions>> type, Vec3 p, double scale, int count, double spread) {
      if (count > 0) {
         level.sendParticles(new NukeFxOptions(type.get(), (float)scale), p.x, p.y, p.z, count, spread, spread, spread, 0.0);
      }
   }

   public static void gather(ServerLevel level, Vec3 p, double frac) {
      gather(level, p, frac, 1.0);
   }

   public static void gather(ServerLevel level, Vec3 p, double frac, double sizeMul) {
      one(level, AwakenedParticles.BURST, p, (0.12 + frac * 0.3) * sizeMul);
      if (frac > 0.6) {
         one(level, AwakenedParticles.SPARK, p, 0.16 * sizeMul);
      }
   }

   public static void beam(ServerLevel level, Vec3 from, Vec3 to) {
      Vfx.beam(level, from, to, 0.35, 16762170, 12);
      one(level, AwakenedParticles.BURST, from, 0.7);
   }

   public static void boom(ServerLevel level, Vec3 center, double radius, NuclearFx.Kind kind) {
      switch (kind) {
         case COMBAT:
            Vfx.sphere(level, center, radius * 0.8, 16762170, 11);
            one(level, AwakenedParticles.FLARE, center, radius);
            one(level, AwakenedParticles.SHOCK, center, radius);
            scatter(level, AwakenedParticles.EMBER, center, 0.55, 6 + (int)(radius * 1.5), radius * 0.6);
            scatter(level, AwakenedParticles.SPARK, center, 0.3, 5 + (int)radius, radius * 0.7);
            scatter(level, AwakenedParticles.SMOKE, center, radius * 0.4, 3, radius * 0.5);
            boomLayers(level, center, 1.0F, 1.0F);
            break;
         case CLUSTER:
            Vfx.sphere(level, center, radius * 0.5, 16762170, 9, 0.55F);
            one(level, AwakenedParticles.BURST, center, radius * 0.5);
            one(level, AwakenedParticles.SHOCK, center, radius * 0.7);
            scatter(level, AwakenedParticles.EMBER, center, 0.3, 3 + (int)radius, radius * 0.5);
            scatter(level, AwakenedParticles.SPARK, center, 0.2, 3, radius * 0.5);
            scatter(level, AwakenedParticles.SMOKE, center.add(0.0, radius * 0.4, 0.0), radius * 0.16, 1, radius * 0.3);
            Fx.sound(level, center, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 1.3F);
            break;
         case DUST:
            Vfx.dustDome(level, center, radius * 1.15, 12);
            scatter(level, AwakenedParticles.SMOKE, center, radius * 0.6, 10 + (int)(radius * 4.0), radius * 0.8);
            scatter(level, AwakenedParticles.SMOKE, center, radius * 0.42, 6, radius * 0.4);
            scatter(level, AwakenedParticles.SPARK, center, 0.22, 5, radius * 0.7);
            Fx.sound(level, center, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 1.5F);
            Fx.sound(level, center, SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.0F, 0.7F);
            break;
         case DASH:
            one(level, AwakenedParticles.FLARE, center, radius * 0.9);
            scatter(level, AwakenedParticles.SPARK, center, 0.28, 5, radius * 0.7);
            scatter(level, AwakenedParticles.SMOKE, center, radius * 0.35, 2, radius * 0.4);
            Fx.sound(level, center, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 1.6F);
            break;
         case MUSHROOM:
            Vfx.mushroom(level, center, radius, 110);
            NuclearFx.Blast b = new NuclearFx.Blast();
            b.level = level;
            b.center = center;
            b.radius = radius;
            b.born = level.getGameTime();
            b.life = 110;
            MUSHROOMS.add(b);
            mushroomOnset(b);
      }
   }

   public static void screenShake(ServerLevel level, Vec3 center, double range, int durationTicks) {
      Fx.screenShake(level, center, range, durationTicks, 16756768, false);
   }

   public static void tickAll(MinecraftServer server) {
      int now = server.getTickCount();
      if (now != lastTick) {
         lastTick = now;
         Iterator<NuclearFx.Blast> it = MUSHROOMS.iterator();

         while (it.hasNext()) {
            NuclearFx.Blast b = it.next();
            long age = b.level.getGameTime() - b.born;
            if (age > (long)b.life) {
               it.remove();
            } else if (age > 0L) {
               mushroomTick(b, age);
            }
         }
      }
   }

   private static void mushroomOnset(NuclearFx.Blast b) {
      ServerLevel l = b.level;
      Vec3 g = b.center;
      double R = b.radius;
      one(l, AwakenedParticles.SHOCK, g.add(0.0, 0.2, 0.0), R * 1.8);
      one(l, AwakenedParticles.FLARE, g.add(0.0, 1.0, 0.0), R * 0.9);
      scatter(l, AwakenedParticles.SMOKE, g.add(0.0, 0.4, 0.0), R * 0.6, 16, R * 0.8);
      scatter(l, AwakenedParticles.SMOKE, g.add(0.0, 0.4, 0.0), R * 0.45, 8, R * 0.4);
      scatter(l, AwakenedParticles.EMBER, g, 0.6, 12, R * 0.6);
      boomLayers(l, g, 1.5F, 0.4F);
      Fx.sound(l, g, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 7.0F, 0.4F);
      Fx.sound(l, g, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 6.0F, 0.6F);
   }

   private static void mushroomTick(NuclearFx.Blast b, long age) {
      ServerLevel l = b.level;
      Vec3 g = b.center;
      double R = b.radius;
      float f = (float)age / (float)b.life;
      double stemH = Math.min(R * 2.4, R * 0.4 + (double)f * R * 3.2);
      Vec3 top = g.add(0.0, stemH, 0.0);
      one(l, AwakenedParticles.SMOKE, top, R * 0.55);
      scatter(l, AwakenedParticles.SMOKE, top, R * 0.4, 2, R * 0.3);
      scatter(l, AwakenedParticles.SMOKE, g.add(0.0, stemH * 0.5, 0.0), R * 0.45, 2, R * 0.3);
      if (age % 4L == 0L) {
         scatter(l, AwakenedParticles.EMBER, top, 0.5, 2, R * 0.2);
      }

      if (f >= 0.3F && age % 2L == 0L) {
         double capF = Math.min(1.0, ((double)f - 0.3) / 0.5);
         Vec3 capC = g.add(0.0, stemH, 0.0);
         double capR = R * (0.6 + capF * 1.3);
         scatter(l, AwakenedParticles.SMOKE, capC, R * 0.7, 6, capR * 0.6);
         one(l, AwakenedParticles.SHOCK, capC, capR);
         if (age % 8L == 0L) {
            one(l, AwakenedParticles.FLARE, capC, R * 0.7);
         }
      }

      if (age % 40L == 0L) {
         Fx.sound(l, g, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 0.4F);
      }
   }

   private static void boomLayers(ServerLevel l, Vec3 c, float volume, float bassPitch) {
      Fx.sound(l, c, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, volume, 0.9F);
      Fx.sound(l, c, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, volume, bassPitch * 0.5F);
      Fx.sound(l, c, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, volume * 0.6F, 1.4F);
   }

   private static final class Blast {
      private ServerLevel level;
      private Vec3 center;
      private double radius;
      private long born;
      private int life;
   }

   public static enum Kind {
      COMBAT,
      CLUSTER,
      DUST,
      DASH,
      MUSHROOM;
   }
}
