package com.awakenedmod.awakened.content.sun;

import com.awakenedmod.awakened.content.AwakenedParticles;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class SunFx {
   public static final int GOLD = 16761390;
   public static final int ORANGE = 16742936;
   public static final int RED = 16720912;
   public static final int PALE = 16770688;

   private SunFx() {
   }

   public static void detonation(ServerLevel level, Vec3 pos, double radius) {
      Vfx.sphere(level, pos, radius, 16761390, 12);
      Vfx.sphere(level, pos, radius * 0.55, 16770688, 10, 1.5F);
      Vfx.ring(level, pos, radius * 1.7, 16761390, 14);
      Vfx.pillar(level, pos.subtract(0.0, radius * 0.3, 0.0), radius * 0.28, radius * 1.8, 16770688, 10);
      Fx.burst(level, (ParticleOptions)AwakenedParticles.SUN_MOTE.get(), pos, 44, radius * 0.6, 0.22);
      Fx.sound(level, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.7F);
      Fx.sound(level, pos, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 1.5F);
   }
}
