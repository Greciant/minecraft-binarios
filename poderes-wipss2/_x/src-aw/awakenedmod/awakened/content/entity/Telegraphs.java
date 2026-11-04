package com.awakenedmod.awakened.content.entity;

import com.awakenedmod.awakened.Config;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class Telegraphs {
   public static final int PRE_IMPACT_LEAD = 3;
   public static final int DANGER_CLOSE_LEAD = 8;
   private static final int HALO_LIFE = 22;
   private static final int HALO_CADENCE = 16;
   private static final int TRACK_LIFE = 6;
   private static final int TRACK_CADENCE = 5;

   private Telegraphs() {
   }

   public static void commitCue(ServerLevel level, Vec3 pos, SoundEvent sound, float pitch) {
      Fx.sound(level, pos, sound, SoundSource.HOSTILE, 1.6F, pitch);
   }

   public static void dangerClose(ServerLevel level, Vec3 center, double radius) {
      if ((Boolean)Config.BOSS_DANGER_CLOSE_CUE.get()) {
         for (ServerPlayer player : level.getPlayers(p -> !p.isCreative() && !p.isSpectator() && p.position().distanceToSqr(center) <= radius * radius)) {
            ping(level, player);
         }
      }
   }

   public static void dangerCloseLine(ServerLevel level, Vec3 a, Vec3 b, double radius) {
      if ((Boolean)Config.BOSS_DANGER_CLOSE_CUE.get()) {
         for (ServerPlayer player : level.getPlayers(p -> !p.isCreative() && !p.isSpectator() && distToSegmentSq2D(p.position(), a, b) <= radius * radius)) {
            ping(level, player);
         }
      }
   }

   public static void dangerCloseAny(ServerLevel level, Iterable<Vec3> centers, double radius) {
      if ((Boolean)Config.BOSS_DANGER_CLOSE_CUE.get()) {
         for (ServerPlayer player : level.getPlayers(p -> !p.isCreative() && !p.isSpectator())) {
            for (Vec3 center : centers) {
               if (player.position().distanceToSqr(center) <= radius * radius) {
                  ping(level, player);
                  break;
               }
            }
         }
      }
   }

   private static double distToSegmentSq2D(Vec3 p, Vec3 a, Vec3 b) {
      double dx = b.x - a.x;
      double dz = b.z - a.z;
      double lenSq = dx * dx + dz * dz;
      double t = lenSq < 1.0E-6 ? 0.0 : Math.max(0.0, Math.min(1.0, ((p.x - a.x) * dx + (p.z - a.z) * dz) / lenSq));
      double ox = p.x - (a.x + dx * t);
      double oz = p.z - (a.z + dz * t);
      return ox * ox + oz * oz;
   }

   public static void dangerCloseCone(ServerLevel level, Vec3 eye, Vec3 aim, double range, double halfAngleDeg) {
      if ((Boolean)Config.BOSS_DANGER_CLOSE_CUE.get()) {
         Vec3 dir = aim.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : aim.normalize();
         double cos = Math.cos(Math.toRadians(halfAngleDeg));

         for (ServerPlayer player : level.getPlayers(p -> !p.isCreative() && !p.isSpectator())) {
            Vec3 to = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0).subtract(eye);
            double dsq = to.lengthSqr();
            if (!(dsq > range * range) && !(dsq < 1.0E-4) && to.normalize().dot(dir) >= cos) {
               ping(level, player);
            }
         }
      }
   }

   private static void ping(ServerLevel level, ServerPlayer player) {
      player.connection
         .send(
            new ClientboundSoundPacket(
               SoundEvents.NOTE_BLOCK_BELL, SoundSource.HOSTILE, player.getX(), player.getY(), player.getZ(), 0.9F, 2.0F, level.getRandom().nextLong()
            )
         );
   }

   public static void groundMarker(ServerLevel level, Vec3 center, double radius, int color, int ticksLeft, int totalTicks) {
      Vec3 c = center.add(0.0, 0.1, 0.0);
      Vfx.ring(level, c, radius, color, 3);
      double iris = radius * (double)Math.max(0, ticksLeft) / (double)Math.max(1, totalTicks);
      if (iris > 0.3) {
         Vfx.ring(level, c, iris, color, 3);
      }

      Fx.burst(level, Fx.dust(color & 16777215, 1.0F), c, 3, radius * 0.5, 0.01);
      countdownClick(level, center, ticksLeft, totalTicks);
   }

   public static void zoneMarker(ServerLevel level, Vec3 center, double radius, int color, int ticksLeft, int totalTicks) {
      zoneMarker(level, center, radius, color, ticksLeft, totalTicks, true);
   }

   public static void zoneMarker(ServerLevel level, Vec3 center, double radius, int color, int ticksLeft, int totalTicks, boolean click) {
      paintZone(level, center, radius, color, ticksLeft, totalTicks, click, 16, 22, 3);
   }

   public static void trackingZoneMarker(ServerLevel level, Vec3 center, double radius, int color, int ticksLeft, int totalTicks) {
      paintZone(level, center, radius, color, ticksLeft, totalTicks, true, 5, 6, 2);
   }

   private static void paintZone(
      ServerLevel level, Vec3 center, double radius, int color, int ticksLeft, int totalTicks, boolean click, int cadence, int life, int lifeFloor
   ) {
      Vec3 c = center.add(0.0, 0.1, 0.0);
      int elapsed = Math.max(0, totalTicks - ticksLeft);
      if (elapsed % cadence == 0) {
         Vfx.halo(level, c, radius, color, Math.min(life, Math.max(lifeFloor, ticksLeft + lifeFloor - 1)));
      }

      double iris = radius * (double)Math.max(0, ticksLeft) / (double)Math.max(1, totalTicks);
      if (iris > 0.3 && ticksLeft % 2 == 0) {
         Vfx.halo(level, c, iris, color, 3);
      }

      Fx.burst(level, Fx.dust(color & 16777215, 1.0F), c, 3, radius * 0.5, 0.01);
      if (click) {
         countdownClick(level, center, ticksLeft, totalTicks);
      }
   }

   public static void countdownClick(ServerLevel level, Vec3 pos, int ticksLeft, int totalTicks) {
      if (ticksLeft > 0 && ticksLeft % 10 == 0) {
         float pitch = 1.0F + (1.0F - (float)ticksLeft / (float)Math.max(1, totalTicks));
         Fx.sound(level, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.HOSTILE, 0.8F, pitch);
      }
   }

   public static void chargeMotes(ServerLevel level, ParticleOptions mote, Vec3 focus, int tick, int windup, double gatherRadius) {
      float t = Math.min(1.0F, (float)tick / (float)Math.max(1, windup));
      double radius = Math.max(0.15, gatherRadius * (1.0 - (double)t));
      Fx.sphereShell(level, mote, focus, radius, 2 + (int)(6.0F * t));
   }

   public static void aimTracer(ServerLevel level, Vec3 from, Vec3 to, int color) {
      Vfx.effect(VfxEntity.Kind.BEAM, from).scale(0.08).secondary(to.subtract(from)).color(color).lifetime(2).intensity(0.35F).spawn(level);
   }
}
