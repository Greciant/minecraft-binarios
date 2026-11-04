package com.awakenedmod.awakened.content.limitless.domain;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.LoopSound;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class VoidFx {
   private static final String KEY_HUM = "limitless.void.hum";

   private VoidFx() {
   }

   public static void opened(ServerLevel level, Vec3 centre) {
      Fx.sound(level, centre, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.6F, 0.5F);
   }

   public static void hum(Entity anchor, long sinceCast) {
      float in = Math.min(1.0F, (float)sinceCast / 44.0F);
      LoopSound.keep(anchor, "limitless.void.hum", SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.25F + 0.45F * in, 0.5F);
   }

   public static void closing(ServerLevel level, Vec3 centre, Entity anchor) {
      LoopSound.stop(anchor, "limitless.void.hum");
      Fx.sound(level, centre, SoundEvents.BREEZE_INHALE, SoundSource.PLAYERS, 1.6F, 0.5F);
      Fx.sound(level, centre, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.3F, 0.5F);
   }

   public static void closed(ServerLevel level, Vec3 centre) {
      Fx.sound(level, centre, SoundEvents.ENDER_CHEST_CLOSE, SoundSource.PLAYERS, 1.0F, 0.5F);
   }

   public static void hitSparks(ServerLevel level, Vec3 at) {
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 10, 0.18, 0.18, 0.18, 0.02);
      level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 4, 0.12, 0.12, 0.12, 0.005);
   }
}
