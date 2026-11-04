package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class ExoFx {
   public static final int STEEL = 10475775;
   public static final int CORE = 15792895;
   public static final int LOCK = 16755251;
   public static final int SCAN_WAVE = 4247807;
   public static final int SCAN_RED = 14692909;
   public static final int FIRE_BODY = 16754728;
   public static final int FIRE_CORE = 16769120;
   public static final int FIRE_COOL = 14700556;
   public static final int FLASH_BODY = 16771488;
   public static final int EMBER = 16746512;

   private ExoFx() {
   }

   public static DustParticleOptions dust() {
      return Fx.dust(10475775, 1.0F);
   }

   public static void exhaustTrail(ServerLevel level, Vec3 pos) {
      Fx.burst(level, ParticleTypes.SMOKE, pos, 2, 0.04, 0.0);
      Fx.burst(level, dust(), pos, 1, 0.02, 0.0);
   }

   public static void dashKick(ServerLevel level, Vec3 at, Vec3 dashDir) {
      Vec3 back = dashDir.scale(-1.0);
      Vec3 plume = at.add(back.scale(0.6));
      Vfx.sphere(level, at, 0.85, 10475775, 6, 1.4F);
      Vfx.beam(level, at, at.add(back.scale(3.2)), 0.32, 10475775, 7);
      Fx.burst(level, ParticleTypes.FLAME, plume, 18, 0.3, 0.05);
      Fx.burst(level, ParticleTypes.LARGE_SMOKE, plume, 12, 0.4, 0.04);
      Fx.burst(level, dust(), at, 10, 0.3, 0.08);
      Fx.debrisFan(level, ParticleTypes.LAVA, plume, 6, -8.0, 20.0, 0.3, 0.6);
   }

   public static void sonicBoom(ServerLevel level, Vec3 at, float yaw) {
      Fx.sound(level, at, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.9F, 1.0F);
      Fx.sound(level, at, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5F, 0.6F);
      Fx.burst(level, ParticleTypes.SONIC_BOOM, at, 1, 0.0, 0.0);
      Vfx.ring(level, at, 1.0, 10475775, 8, yaw, 90.0F);
      Vfx.sphere(level, at, 0.5, 15792895, 5, 1.4F);
      Fx.burst(level, ParticleTypes.CLOUD, at, 24, 0.6, 0.05);
      Fx.shake(level, at, 30.0, 0.35F, 3, 10);
   }

   public static void dissipateUp(ServerLevel level, Vec3 base) {
      Vfx.pillar(level, base, 0.55, 3.2, 10475775, 12);
      Fx.burst(level, ParticleTypes.CLOUD, base.add(0.0, 1.2, 0.0), 18, 0.5, 0.05);
      Fx.burst(level, dust(), base.add(0.0, 1.6, 0.0), 10, 0.4, 0.08);
   }

   public static void blast(ServerLevel level, Vec3 pos, double radius) {
      Vfx.sphere(level, pos, radius, 10475775, 8, 1.4F);
      Vfx.ring(level, pos.add(0.0, 0.1, 0.0), radius * 1.2, 10475775, 12);
      Fx.burst(level, ParticleTypes.EXPLOSION, pos, 1, 0.0, 0.0);
      Fx.burst(level, ParticleTypes.LARGE_SMOKE, pos, 10, 0.4, 0.03);
      Fx.sound(level, pos, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 1.4F);
   }

   public static void rocketBlast(ServerLevel level, Vec3 pos, double radius) {
      Vfx.effect(VfxEntity.Kind.SPHERE, pos).scale(1.0).color(16771488, 16777215).lifetime(2).intensity(1.9F).spawn(level);
      Fx.burst(level, ParticleTypes.EXPLOSION, pos, 4, 0.5, 0.0);
      Vfx.effect(VfxEntity.Kind.SPHERE, pos).scale(1.4).color(16754728, 16769120).lifetime(7).intensity(1.2F).spawn(level);
      Vfx.effect(VfxEntity.Kind.DOME, pos).scale(1.6).color(16742936).lifetime(8).intensity(0.9F).delay(1).spawn(level);
      Fx.groundImpact(level, pos, 1.4, false);
      Fx.debrisFan(level, ParticleTypes.LAVA, pos, 11, -10.0, 25.0, 0.35, 0.7);
      Fx.debrisFan(level, ParticleTypes.CRIT, pos, 11, 20.0, 50.0, 0.5, 0.9);
      Fx.burst(level, ParticleTypes.POOF, pos, 6, 0.3, 0.05);

      for (int i = 0; i < 3; i++) {
         double ox = (level.random.nextDouble() - 0.5) * 0.8;
         double oz = (level.random.nextDouble() - 0.5) * 0.8;
         Vfx.sphere(level, pos.add(ox, 0.1, oz), 0.18, 16746512, 7, 1.0F);
      }

      Vfx.effect(VfxEntity.Kind.SPHERE, pos).scale(1.1).color(14700556, 13386752).lifetime(4).intensity(0.5F).delay(3).spawn(level);
      Fx.burst(level, ParticleTypes.LARGE_SMOKE, pos.add(0.0, 0.2, 0.0), 8, 0.25, 0.05);
      Fx.sound(level, pos, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.7F, 1.55F);
      Fx.sound(level, pos, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5F, 0.7F);
      Fx.sound(level, pos, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.1F, 0.5F);
      Fx.shake(level, pos, 30.0, 0.45F, 3, 13);
   }
}
