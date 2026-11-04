package com.awakenedmod.awakened.content.teleporter;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class TeleFx {
   static final int VOID = 11565823;
   private static final int GHOST_TICKS = 14;

   private TeleFx() {
   }

   static void ghost(ServerLevel l, ServerPlayer player) {
      AfterimageEntity ghost = new AfterimageEntity(AwakenedEntities.AFTERIMAGE.get(), l);
      Vec3 feet = player.position();
      ghost.setPos(feet.x, feet.y, feet.z);
      ghost.configure(player.getUUID(), player.yBodyRot, 14);
      l.addFreshEntity(ghost);
      Fx.burst(l, ParticleTypes.PORTAL, feet.add(0.0, (double)player.getBbHeight() * 0.5, 0.0), 26, 0.5, 0.3);
   }

   static void rift(ServerLevel l, Vec3 pos) {
      Vfx.ring(l, pos, 0.8, 11565823, 8);
      Fx.burst(l, ParticleTypes.PORTAL, pos, 24, 0.4, 0.35);
      Fx.burst(l, ParticleTypes.WITCH, pos, 8, 0.3, 0.1);
   }

   public static void gate(ServerLevel l, Vec3 center, double radius, int lifetime, float yawDeg) {
      Vfx.portal(l, center, radius, 11565823, lifetime, yawDeg);
      gateDressing(l, center, radius);
   }

   public static void gateFlat(ServerLevel l, Vec3 center, double radius, int lifetime) {
      Vfx.portalFlat(l, center, radius, 11565823, lifetime);
      gateDressing(l, center, radius);
   }

   private static void gateDressing(ServerLevel l, Vec3 center, double radius) {
      Fx.sound(l, center, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.55F);
   }

   static void trail(ServerLevel l, Vec3 from, Vec3 to) {
      if (!(from.distanceToSqr(to) < 1.0E-4)) {
         Vfx.beam(l, from, to, 0.1, 11565823, 5);
         Fx.burst(l, ParticleTypes.PORTAL, from.lerp(to, 0.5), 16, from.distanceTo(to) * 0.2, 0.1);
      }
   }
}
