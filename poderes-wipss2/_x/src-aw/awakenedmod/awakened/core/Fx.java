package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.core.net.AwakenScreenFxPayload;
import com.awakenedmod.awakened.core.net.ScreenShakePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

public final class Fx {
   public static final DustParticleOptions OPTIC_RED = dust(16726832, 1.2F);
   public static final DustParticleOptions HAZARD_AMBER = dust(16756768, 1.0F);

   private Fx() {
   }

   public static DustParticleOptions dust(int rgb, float scale) {
      return new DustParticleOptions(new Vector3f((float)(rgb >> 16 & 0xFF) / 255.0F, (float)(rgb >> 8 & 0xFF) / 255.0F, (float)(rgb & 0xFF) / 255.0F), scale);
   }

   public static void screenShake(ServerLevel level, Vec3 center, double range, int durationTicks, int color, boolean distanceFalloff) {
      screenShake(level, center, range, durationTicks, color, distanceFalloff, false);
   }

   public static void screenShake(ServerLevel level, Vec3 center, double range, int durationTicks, int color, boolean distanceFalloff, boolean shakeOnly) {
      double rangeSqr = range * range;

      for (ServerPlayer viewer : level.players()) {
         double distSqr = viewer.distanceToSqr(center);
         if (!(distSqr > rangeSqr)) {
            int duration = durationTicks;
            if (distanceFalloff) {
               duration = Math.max(3, (int)Math.round((double)durationTicks * (1.0 - 0.6 * Math.sqrt(distSqr) / range)));
            }

            PacketDistributor.sendToPlayer(viewer, new AwakenScreenFxPayload(color, duration, shakeOnly), new CustomPacketPayload[0]);
         }
      }
   }

   public static void scanLock(ServerLevel level, Vec3 eye, SoundEvent alert, float volume, float pitch) {
      burst(level, OPTIC_RED, eye, 14, 0.25, 0.02);
      burst(level, HAZARD_AMBER, eye, 6, 0.35, 0.01);
      sound(level, eye, alert, SoundSource.HOSTILE, volume, pitch);
   }

   public static void burst(ServerLevel level, ParticleOptions particle, Vec3 pos, int count, double spread, double speed) {
      level.sendParticles(particle, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
   }

   public static void debrisFan(
      ServerLevel level, ParticleOptions particle, Vec3 origin, int count, double elevMinDeg, double elevMaxDeg, double speedMin, double speedMax
   ) {
      for (int i = 0; i < count; i++) {
         double yaw = level.random.nextDouble() * Math.PI * 2.0;
         double elev = Math.toRadians(elevMinDeg + level.random.nextDouble() * (elevMaxDeg - elevMinDeg));
         double horiz = Math.cos(elev);
         double speed = speedMin + level.random.nextDouble() * (speedMax - speedMin);
         double vx = Math.cos(yaw) * horiz * speed;
         double vz = Math.sin(yaw) * horiz * speed;
         double vy = Math.sin(elev) * speed;
         level.sendParticles(particle, origin.x, origin.y, origin.z, 0, vx, vy, vz, 1.0);
      }
   }

   public static void sphereShell(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
      double golden = Math.PI * (3.0 - Math.sqrt(5.0));

      for (int i = 0; i < points; i++) {
         double y = 1.0 - ((double)i + 0.5) * 2.0 / (double)points;
         double ring = Math.sqrt(Math.max(0.0, 1.0 - y * y));
         double theta = golden * (double)i;
         double x = Math.cos(theta) * ring;
         double z = Math.sin(theta) * ring;
         level.sendParticles(particle, center.x + x * radius, center.y + y * radius, center.z + z * radius, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   public static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, Vec3 u, Vec3 v, double radius, int points, double phase) {
      double step = (Math.PI * 2) / (double)points;

      for (int i = 0; i < points; i++) {
         double a = phase + (double)i * step;
         double cu = Math.cos(a) * radius;
         double sv = Math.sin(a) * radius;
         level.sendParticles(particle, center.x + u.x * cu + v.x * sv, center.y + u.y * cu + v.y * sv, center.z + u.z * cu + v.z * sv, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   public static void disk(ServerLevel level, ParticleOptions particle, Vec3 center, double radius) {
      int rings = Math.max(2, (int)Math.ceil(radius * 2.0));

      for (int r = 1; r <= rings; r++) {
         double rad = radius * (double)r / (double)rings;
         int pts = Math.max(6, (int)(rad * 9.0));

         for (int i = 0; i < pts; i++) {
            double a = (Math.PI * 2) * (double)i / (double)pts;
            level.sendParticles(particle, center.x + Math.cos(a) * rad, center.y, center.z + Math.sin(a) * rad, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   public static void sheet(ServerLevel level, ParticleOptions particle, Vec3 base, Vec3 right, double halfWidth, double height, int cols, int rows) {
      for (int c = 0; c <= cols; c++) {
         double w = -halfWidth + 2.0 * halfWidth * (double)c / (double)cols;

         for (int r = 0; r <= rows; r++) {
            double h = height * (double)r / (double)rows;
            Vec3 p = base.add(right.x * w, h, right.z * w);
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   public static void sound(Level level, Vec3 pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
      if (level instanceof ServerLevel server) {
         SoundMixer.play(server, pos, sound, source, volume, pitch);
      } else {
         level.playSound(null, pos.x, pos.y, pos.z, sound, source, volume, pitch);
      }
   }

   public static void soundExcept(Level level, ServerPlayer except, Vec3 pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
      if (except == null) {
         sound(level, pos, sound, source, volume, pitch);
      } else {
         level.playSound(except, pos.x, pos.y, pos.z, sound, source, volume, pitch);
      }
   }

   public static void soundForViewer(ServerLevel level, ServerPlayer viewer, Vec3 pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
      if (!viewer.isRemoved()) {
         long seed = level.getRandom().nextLong();
         level.playSound(viewer, pos.x, pos.y, pos.z, sound, source, volume, pitch);
         viewer.connection.send(new ClientboundSoundPacket(Holder.direct(sound), source, pos.x, pos.y, pos.z, volume, pitch, seed));
      }
   }

   public static void soundOnlyFor(ServerPlayer viewer, Vec3 pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
      if (!viewer.isRemoved()) {
         viewer.connection
            .send(new ClientboundSoundPacket(Holder.direct(sound), source, pos.x, pos.y, pos.z, volume, pitch, viewer.level().getRandom().nextLong()));
      }
   }

   public static void shake(ServerLevel level, Vec3 pos, double radius, float magnitude, int durationTicks, int fadeTicks) {
      ScreenShakePayload payload = new ScreenShakePayload(pos.x, pos.y, pos.z, (float)radius, magnitude, durationTicks, fadeTicks);

      for (ServerPlayer p : level.players()) {
         if (p.position().distanceToSqr(pos) <= radius * radius) {
            PacketDistributor.sendToPlayer(p, payload, new CustomPacketPayload[0]);
         }
      }
   }

   public static void shakeFor(ServerPlayer viewer, Vec3 pos, double radius, float magnitude, int durationTicks, int fadeTicks) {
      PacketDistributor.sendToPlayer(
         viewer, new ScreenShakePayload(pos.x, pos.y, pos.z, (float)radius, magnitude, durationTicks, fadeTicks), new CustomPacketPayload[0]
      );
   }

   public static Fx.Floor floorState(ServerLevel level, double x, double y, double z) {
      return floorUnder(level, x, y + 2.0, z, 7);
   }

   public static Fx.Floor floorUnder(ServerLevel level, double x, double y, double z, int blocks) {
      MutableBlockPos pos = BlockPos.containing(x, y, z).mutable();

      for (int i = 0; i < blocks; i++) {
         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && state.getFluidState().isEmpty()) {
            return new Fx.Floor(state, (double)pos.getY() + 1.0);
         }

         pos.move(0, -1, 0);
      }

      return null;
   }

   public static void blockSpall(ServerLevel level, Vec3 at, BlockState struck, int count) {
      if (!struck.isAir()) {
         level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, struck), at.x, at.y, at.z, count, 0.12, 0.12, 0.12, 0.1);
      }
   }

   public static void groundImpact(ServerLevel level, Vec3 center, double radius, boolean thud) {
      int samples = (int)Mth.clamp(radius * radius * 1.5, 6.0, 36.0);

      for (int i = 0; i < samples; i++) {
         double a = level.random.nextDouble() * Math.PI * 2.0;
         double r = radius * Math.sqrt(level.random.nextDouble());
         double x = center.x + Math.cos(a) * r;
         double z = center.z + Math.sin(a) * r;
         Fx.Floor floor = floorState(level, x, center.y, z);
         if (floor != null) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), x, floor.topY() + 0.1, z, 4, 0.15, 0.25, 0.15, 0.12);
         }
      }

      burst(level, ParticleTypes.LARGE_SMOKE, center.add(0.0, 0.3, 0.0), (int)(radius * 3.0), radius * 0.4, 0.02);
      if (thud) {
         sound(level, center, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.9F, 0.55F);
         sound(level, center, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.4F, 0.6F);
      }
   }

   public static void groundShockwave(ServerLevel level, Vec3 center, double startRadius, double endRadius, int travelTicks, boolean debrisBlocks) {
      Shockwaves.start(level, center, startRadius, endRadius, travelTicks, debrisBlocks);
   }

   public static void groundShockwave(ServerLevel level, Vec3 center, double startRadius, double endRadius, int travelTicks, boolean debrisBlocks, int color) {
      Shockwaves.start(level, center, startRadius, endRadius, travelTicks, debrisBlocks, color);
   }

   public static void groundShockwave(ServerLevel level, Vec3 center, double startRadius, double endRadius, int travelTicks, Shockwaves.Style style) {
      Shockwaves.start(level, center, startRadius, endRadius, travelTicks, style);
   }

   public static void wearVent(ServerLevel level, LivingEntity boss, int tier) {
      if (tier >= 2) {
         int interval = tier >= 3 ? 5 : 14;
         if (boss.tickCount % interval == 0) {
            Vec3 p = boss.position()
               .add(
                  (boss.getRandom().nextDouble() - 0.5) * (double)boss.getBbWidth(),
                  boss.getRandom().nextDouble() * (double)boss.getBbHeight(),
                  (boss.getRandom().nextDouble() - 0.5) * (double)boss.getBbWidth()
               );
            burst(level, ParticleTypes.SMOKE, p, 2, 0.08, 0.01);
            if (tier >= 3) {
               burst(level, ParticleTypes.ELECTRIC_SPARK, p, 3, 0.12, 0.05);
               if (boss.getRandom().nextInt(4) == 0) {
                  burst(level, ParticleTypes.LAVA, p, 1, 0.05, 0.0);
               }

               if (boss.tickCount % 40 == 0) {
                  sound(level, boss.position(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.5F, 1.6F);
               }
            }
         }
      }
   }

   public static void tierCrack(ServerLevel level, LivingEntity boss) {
      Vec3 mid = boss.position().add(0.0, (double)boss.getBbHeight() * 0.6, 0.0);
      sound(level, boss.position(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.HOSTILE, 1.8F, 0.7F);
      sound(level, boss.position(), SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 1.6F, 0.8F);
      burst(level, ParticleTypes.CRIT, mid, 24, (double)boss.getBbWidth() * 0.7, 0.15);
      burst(level, ParticleTypes.ELECTRIC_SPARK, mid, 16, (double)boss.getBbWidth() * 0.5, 0.1);
   }

   public static record Floor(BlockState state, double topY) {
   }
}
