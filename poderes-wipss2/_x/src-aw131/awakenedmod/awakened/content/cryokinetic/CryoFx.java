package com.awakenedmod.awakened.content.cryokinetic;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class CryoFx {
   static final int FROST = 6742271;
   private static final BlockParticleOption ICE_CRUMBS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState());
   private static final int BURST_TICKS = 22;

   private CryoFx() {
   }

   static void crystals(ServerLevel l, Vec3 p, int count, double spread) {
      Fx.burst(l, ParticleTypes.SNOWFLAKE, p, count, spread, 0.03);
      Fx.burst(l, ParticleTypes.ITEM_SNOWBALL, p, Math.max(1, count / 3), spread * 0.7, 0.05);
   }

   static void glints(ServerLevel l, Vec3 p, int count, double spread) {
      Fx.burst(l, ParticleTypes.END_ROD, p, count, spread, 0.0);
      Fx.burst(l, ParticleTypes.SNOWFLAKE, p, count, spread, 0.0);
   }

   static void frostBurst(ServerLevel l, Vec3 center, double radius, Vec3 groundAt) {
      Vfx.sphere(l, center, radius * 0.7, 6742271, 9);
      Vfx.ring(l, groundAt, radius, 6742271, 10);
      crystals(l, center, (int)(radius * 6.0), radius * 0.5);
      glints(l, center, (int)(radius * 3.0), radius * 0.5);
      Fx.sound(l, center, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.7F);
      Fx.sound(l, center, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.7F, 1.2F);
   }

   static void frostBreath(ServerLevel l, Vec3 eye, Vec3 look, double range) {
      Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
      if (right.lengthSqr() < 1.0E-4) {
         right = new Vec3(1.0, 0.0, 0.0);
      }

      right = right.normalize();
      Vec3 up = right.cross(look).normalize();
      Vec3 src = eye.subtract(up.scale(0.28));

      for (int i = 0; i < 16; i++) {
         double t = l.random.nextDouble();
         double dist = 0.4 + t * range;
         double spread = 0.12 + t * 0.55;
         Vec3 off = right.scale((l.random.nextDouble() * 2.0 - 1.0) * spread).add(up.scale((l.random.nextDouble() * 2.0 - 1.0) * spread));
         Vec3 p = src.add(look.scale(dist)).add(off);
         Vec3 vel = look.scale(0.28).add(off.scale(0.04));
         l.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 0, vel.x, vel.y, vel.z, 1.0);
         if (i % 3 == 0) {
            l.sendParticles(ParticleTypes.WHITE_ASH, p.x, p.y, p.z, 0, vel.x * 0.6, vel.y * 0.6, vel.z * 0.6, 1.0);
         }

         if (i % 5 == 0) {
            l.sendParticles(ParticleTypes.ITEM_SNOWBALL, p.x, p.y, p.z, 0, vel.x, vel.y, vel.z, 1.0);
         }
      }
   }

   static void encase(ServerLevel l, Vec3 feet, double width, double height, int lifetime) {
      double w = width + 0.35;
      double h = height + 0.3;
      Vec3 center = feet.add(0.0, height / 2.0, 0.0);
      Vfx.block(l, center, Blocks.ICE, w, h, w, lifetime, 0.0F);
      crystals(l, center, 28, w * 0.6);
      glints(l, center, 10, w * 0.6);
      Fx.sound(l, center, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.3F, 0.6F);
      Fx.sound(l, center, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0F, 0.8F);
   }

   static void shardLaunch(ServerLevel l, Vec3 hand) {
      Fx.burst(l, ICE_CRUMBS, hand, 8, 0.15, 0.1);
      Fx.sound(l, hand, (SoundEvent)SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.25F);
      Fx.sound(l, hand, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.5F, 1.9F);
   }

   static void icicleTrail(ServerLevel l, Vec3 at) {
      for (int i = 0; i < 2; i++) {
         double s = 0.15 + l.random.nextDouble() * 0.15;
         Vec3 p = at.add((l.random.nextDouble() - 0.5) * 0.3, (l.random.nextDouble() - 0.5) * 0.3, (l.random.nextDouble() - 0.5) * 0.3);
         Vfx.block(l, p, l.random.nextBoolean() ? Blocks.ICE : Blocks.PACKED_ICE, s, s, s, 5 + l.random.nextInt(4), l.random.nextFloat() * 90.0F);
      }

      Fx.burst(l, ICE_CRUMBS, at, 4, 0.08, 0.0);
   }

   static void shardImpact(ServerLevel l, Vec3 p) {
      Fx.burst(l, ICE_CRUMBS, p, 14, 0.25, 0.1);
      iceChunks(l, p, 3, 0.12, 0.2, 0.18);
      Fx.sound(l, p, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.5F);
   }

   static void iceTrail(ServerLevel l, Vec3 feet) {
      Vfx.block(l, feet.add(0.0, -0.12, 0.0), Blocks.PACKED_ICE, 1.5, 0.3, 1.5, 18, 0.0F);
      Fx.burst(l, ParticleTypes.SNOWFLAKE, feet.add(0.0, 0.05, 0.0), 2, 0.35, 0.0);
   }

   static void javelinHit(ServerLevel l, LivingEntity target) {
      Vec3 center = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
      Fx.sound(l, center, SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.2F, 0.8F);
      Fx.burst(l, ICE_CRUMBS, center, 20, (double)target.getBbWidth() * 0.4, 0.15);
      iceChunks(l, center, 3, 0.16, 0.28, 0.24);
      frostSplash(l, target.position(), 1, 0.7);
      crystalBurst(l, center, 3, 0.9, 0.18);
      Fx.sound(l, center, SoundEvents.GLASS_HIT, SoundSource.PLAYERS, 1.0F, 1.3F);
   }

   private static void frostSplash(ServerLevel l, Vec3 feet, int count, double radius) {
      double floor = AimUtil.groundedPosition(l, feet.add(0.0, 0.5, 0.0)).y;

      for (int i = 0; i < count; i++) {
         double a = ((double)i + l.random.nextDouble() * 0.6) / (double)count * Math.PI * 2.0;
         double r = radius * (0.35 + l.random.nextDouble() * 0.65);
         double s = 0.4 + l.random.nextDouble() * 0.5;
         Vec3 at = new Vec3(feet.x + Math.cos(a) * r, floor + 0.06, feet.z + Math.sin(a) * r);
         Vfx.block(l, at, Blocks.PACKED_ICE, s, 0.12, s * (1.2 + l.random.nextDouble()), 30 + l.random.nextInt(15), (float)Math.toDegrees(-a) + 90.0F);
      }
   }

   private static void iceChunks(ServerLevel l, Vec3 from, int count, double minSize, double maxSize, double speed) {
      double floor = AimUtil.groundedPosition(l, from).y;

      for (int i = 0; i < count; i++) {
         double a = l.random.nextDouble() * Math.PI * 2.0;
         double s = minSize + l.random.nextDouble() * (maxSize - minSize);
         double v = speed * (0.6 + l.random.nextDouble() * 0.4);
         Vec3 vel = new Vec3(Math.cos(a) * v, 0.12 + l.random.nextDouble() * 0.18, Math.sin(a) * v);
         Block block = l.random.nextInt(3) == 0 ? Blocks.ICE : Blocks.PACKED_ICE;
         Vfx.blockThrown(
            l,
            from,
            block.defaultBlockState(),
            s,
            s,
            s,
            28 + l.random.nextInt(10),
            (float)Math.toDegrees(a),
            vel,
            floor + s / 2.0,
            18.0F + l.random.nextFloat() * 20.0F
         );
      }
   }

   private static void crystalBurst(ServerLevel l, Vec3 center, int count, double length, double radius) {
      GlacierEntity.spawn(l, GlacierShape.burst(center, count, length, radius, l.random), 22);
   }

   static void rampartOnset(ServerLevel l, Vec3 ground) {
      crystals(l, ground.add(0.0, 0.3, 0.0), 30, 1.2);
      Fx.burst(l, ParticleTypes.SNOWFLAKE, ground.add(0.0, 0.1, 0.0), 30, 1.6, 0.08);
      Fx.sound(l, ground, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.2F, 0.6F);
      Fx.sound(l, ground, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.4F, 0.5F);
   }

   static void rampartColumn(ServerLevel l, Vec3 feet, double width, double height) {
      Fx.burst(l, ParticleTypes.ITEM_SNOWBALL, feet.add(0.0, 0.3, 0.0), 4, width * 0.6, 0.12);
      Fx.burst(l, ParticleTypes.SNOWFLAKE, feet.add(0.0, height * 0.5, 0.0), 5, width * 0.6, 0.04);
   }

   static void rampartFront(ServerLevel l, Vec3 front) {
      Fx.sound(l, front, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.5F, 0.45F);
      Fx.sound(l, front, SoundEvents.POWDER_SNOW_PLACE, SoundSource.PLAYERS, 1.2F, 0.6F);
   }

   static void rampartHaze(ServerLevel l, Vec3 top, double width) {
      Fx.burst(l, ParticleTypes.SNOWFLAKE, top, 3, width * 0.5, 0.01);
      Fx.burst(l, ParticleTypes.WHITE_ASH, top, 2, width * 0.5, 0.0);
      Fx.burst(l, ICE_CRUMBS, top, 1, width * 0.4, 0.0);
   }

   static void rampartShatter(ServerLevel l, Vec3 at, boolean loud) {
      iceChunks(l, at, 2, 0.3, 0.55, 0.22);
      Fx.burst(l, ICE_CRUMBS, at, 8, 0.5, 0.15);
      crystals(l, at, 4, 0.5);
      if (loud) {
         Fx.sound(l, at, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5F, 0.55F);
      }
   }
}
