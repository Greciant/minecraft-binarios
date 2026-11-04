package com.awakenedmod.awakened.content.phytokinetic;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Fx;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

final class PhytoFx {
   static final int LEAF = 5816906;
   static final int VINE = 4103220;
   static final int SPROUT = 9234544;
   static final int BARK = 10119731;
   static final int GOLD = 16765498;
   static final int LIFE = 14173487;
   static final DustParticleOptions BARK_DUST = Fx.dust(7029797, 0.9F);
   static final DustParticleOptions LEAF_DUST = Fx.dust(5816906, 0.8F);
   static final DustParticleOptions POLLEN_DUST = Fx.dust(16765498, 0.7F);
   static final DustParticleOptions LIFE_DUST = Fx.dust(14173487, 0.75F);
   static final BlockParticleOption MOSS_CRUMBS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSS_BLOCK.defaultBlockState());
   static final BlockParticleOption WOOD_CRUMBS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_WOOD.defaultBlockState());
   static final BlockParticleOption ROOT_DIRT_CRUMBS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState());
   static final double VINE_GROWTH_PER_TICK = 2.2;
   private static final double SEGMENT = 0.7;
   private static final double TWIST_PER_BLOCK = 3.2;
   private static final double BRAID_RADIUS = 0.13;
   private static final int VINE_LINGER = 9;
   private static final double LEAF_SPACING = 1.3;

   private PhytoFx() {
   }

   static List<Vec3> whipPath(RandomSource random, Vec3 from, Vec3 to) {
      Vec3 span = to.subtract(from);
      double length = span.length();
      List<Vec3> points = new ArrayList<>();
      if (length < 0.01) {
         points.add(from);
         points.add(to);
         return points;
      } else {
         Vec3 dir = span.scale(1.0 / length);
         Vec3 side = dir.cross(new Vec3(0.0, 1.0, 0.0));
         if (side.lengthSqr() < 1.0E-4) {
            side = new Vec3(1.0, 0.0, 0.0);
         } else {
            side = side.normalize();
         }

         double swing = (double)(random.nextBoolean() ? 1 : -1) * length * (0.16 + random.nextDouble() * 0.1);
         double sag = length * 0.06;
         Vec3 c1 = from.add(dir.scale(length * 0.33)).add(side.scale(swing)).add(0.0, sag, 0.0);
         Vec3 c2 = from.add(dir.scale(length * 0.72)).add(side.scale(-swing * 0.35)).add(0.0, -sag * 0.5, 0.0);
         int samples = Math.max(2, (int)Math.ceil(length / 0.6));

         for (int i = 0; i <= samples; i++) {
            double t = (double)i / (double)samples;
            points.add(cubic(from, c1, c2, to, t));
         }

         return points;
      }
   }

   static List<Vec3> sagPath(Vec3 from, Vec3 to, double sagFactor) {
      Vec3 span = to.subtract(from);
      double length = span.length();
      List<Vec3> points = new ArrayList<>();
      if (length < 0.01) {
         points.add(from);
         points.add(to);
         return points;
      } else {
         double sag = length * sagFactor;
         Vec3 c1 = from.add(span.scale(0.33)).add(0.0, -sag, 0.0);
         Vec3 c2 = from.add(span.scale(0.67)).add(0.0, -sag, 0.0);
         int samples = Math.max(2, (int)Math.ceil(length / 0.6));

         for (int i = 0; i <= samples; i++) {
            points.add(cubic(from, c1, c2, to, (double)i / (double)samples));
         }

         return points;
      }
   }

   private static Vec3 cubic(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
      double u = 1.0 - t;
      return p0.scale(u * u * u).add(p1.scale(3.0 * u * u * t)).add(p2.scale(3.0 * u * t * t)).add(p3.scale(t * t * t));
   }

   static double pathLength(List<Vec3> path) {
      double length = 0.0;

      for (int i = 1; i < path.size(); i++) {
         length += path.get(i).distanceTo(path.get(i - 1));
      }

      return length;
   }

   static Vec3 pointAt(List<Vec3> path, double fraction) {
      double remaining = Math.max(0.0, Math.min(1.0, fraction)) * pathLength(path);

      for (int i = 1; i < path.size(); i++) {
         double step = path.get(i).distanceTo(path.get(i - 1));
         if (remaining <= step || i == path.size() - 1) {
            double t = step < 1.0E-6 ? 0.0 : remaining / step;
            return path.get(i - 1).add(path.get(i).subtract(path.get(i - 1)).scale(t));
         }

         remaining -= step;
      }

      return path.get(path.size() - 1);
   }

   static int growVine(ServerLevel level, RandomSource random, List<Vec3> path, double halfWidth) {
      return growVine(level, random, path, halfWidth, 2.2);
   }

   static int growVine(ServerLevel level, RandomSource random, List<Vec3> path, double halfWidth, double growthPerTick) {
      double length = pathLength(path);
      if (length < 0.01) {
         return 0;
      } else {
         int travelTicks = Math.max(1, (int)Math.ceil(length / growthPerTick));
         walkSegments(path, (from, to, arcFrom, arcTo) -> {
            int delay = (int)Math.floor(arcFrom / length * (double)travelTicks);
            braidStrands(level, from, to, to.subtract(from), arcFrom, arcTo, halfWidth, delay, travelTicks - delay + 9);
         });
         double covered = 0.0;
         double sinceLeaf = 0.78;

         for (int i = 1; i < path.size() - 1; i++) {
            covered += path.get(i).distanceTo(path.get(i - 1));
            sinceLeaf += path.get(i).distanceTo(path.get(i - 1));
            if (sinceLeaf >= 1.3) {
               sinceLeaf = 0.0;
               leafFleck(level, random, path.get(i).add(0.0, 0.12, 0.0), covered / length, travelTicks);
               leafFleck(level, random, path.get(i).add(0.0, -0.12, 0.0), covered / length, travelTicks);
            }
         }

         return travelTicks;
      }
   }

   static void tautVine(ServerLevel level, List<Vec3> path, double halfWidth, int lifetime) {
      if (!(pathLength(path) < 0.01)) {
         walkSegments(path, (from, to, arcFrom, arcTo) -> braidStrands(level, from, to, to.subtract(from), arcFrom, arcTo, halfWidth, 0, lifetime));
      }
   }

   private static void walkSegments(List<Vec3> path, PhytoFx.BraidSink sink) {
      double covered = 0.0;
      double sinceSegment = 0.0;
      Vec3 segmentStart = path.get(0);
      double segmentStartLen = 0.0;

      for (int i = 1; i < path.size(); i++) {
         double step = path.get(i).distanceTo(path.get(i - 1));
         covered += step;
         sinceSegment += step;
         if (sinceSegment >= 0.7 || i == path.size() - 1) {
            sink.segment(segmentStart, path.get(i), segmentStartLen, covered);
            segmentStart = path.get(i);
            segmentStartLen = covered;
            sinceSegment = 0.0;
         }
      }
   }

   private static void braidStrands(
      ServerLevel level, Vec3 from, Vec3 to, Vec3 tangent, double arcLenFrom, double arcLenTo, double halfWidth, int delay, int lifetime
   ) {
      Vec3 perp1 = tangent.cross(new Vec3(0.0, 1.0, 0.0));
      perp1 = perp1.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : perp1.normalize();
      Vec3 perp2 = tangent.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : tangent.normalize().cross(perp1);
      Vec3 a0 = from.add(braidOffset(perp1, perp2, arcLenFrom * 3.2));
      Vec3 a1 = to.add(braidOffset(perp1, perp2, arcLenTo * 3.2));
      Vec3 b0 = from.add(braidOffset(perp1, perp2, arcLenFrom * 3.2 + Math.PI));
      Vec3 b1 = to.add(braidOffset(perp1, perp2, arcLenTo * 3.2 + Math.PI));
      strand(level, a0, a1, halfWidth, 4103220, 9234544, 0.85F, delay, lifetime);
      strand(level, b0, b1, halfWidth * 0.55, 9234544, 15728600, 0.65F, delay, lifetime);
   }

   private static Vec3 braidOffset(Vec3 perp1, Vec3 perp2, double phase) {
      return perp1.scale(Math.cos(phase) * 0.13).add(perp2.scale(Math.sin(phase) * 0.13));
   }

   private static void strand(ServerLevel level, Vec3 from, Vec3 to, double halfWidth, int body, int core, float intensity, int delay, int lifetime) {
      Vfx.effect(VfxEntity.Kind.BEAM, from)
         .scale(halfWidth)
         .secondary(to.subtract(from))
         .color(body, core)
         .lifetime(lifetime)
         .delay(delay)
         .intensity(intensity)
         .spawn(level);
   }

   private static void leafFleck(ServerLevel level, RandomSource random, Vec3 pos, double fractionAlong, int travelTicks) {
      int delay = (int)Math.floor(fractionAlong * (double)travelTicks) + 1;
      int lifetime = travelTicks - delay + 9;
      Vfx.effect(VfxEntity.Kind.SPLAT, pos.add((random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2))
         .scale(0.14 + random.nextDouble() * 0.1)
         .color(5816906, 9234544)
         .lifetime(Math.max(4, lifetime))
         .delay(delay)
         .intensity(0.75F)
         .orient(random.nextFloat() * 360.0F, 30.0F + random.nextFloat() * 60.0F)
         .spawn(level);
   }

   static void rootBurst(ServerLevel level, RandomSource random, Vec3 pos) {
      for (int i = 0; i < 2; i++) {
         double angle = random.nextDouble() * Math.PI * 2.0;
         Vec3 base = pos.add(Math.cos(angle) * 0.35, 0.0, Math.sin(angle) * 0.35);
         Vfx.blockState(
            level,
            base,
            Blocks.MANGROVE_ROOTS.defaultBlockState(),
            0.75 + random.nextDouble() * 0.2,
            0.45 + random.nextDouble() * 0.25,
            0.75 + random.nextDouble() * 0.2,
            16 + random.nextInt(5),
            random.nextFloat() * 360.0F,
            3,
            true
         );
      }

      Fx.burst(level, ROOT_DIRT_CRUMBS, pos.add(0.0, 0.15, 0.0), 14, 0.45, 0.12);
      Fx.burst(level, MOSS_CRUMBS, pos, 8, 0.4, 0.08);
      int spikes = 4 + random.nextInt(2);

      for (int i = 0; i < spikes; i++) {
         double angle = (Math.PI * 2) * (double)i / (double)spikes + random.nextDouble() * 0.6;
         double reach = 0.35 + random.nextDouble() * 0.3;
         Vec3 base = pos.add(Math.cos(angle) * reach, -0.1, Math.sin(angle) * reach);
         Vfx.effect(VfxEntity.Kind.PILLAR, base)
            .scale(0.05 + random.nextDouble() * 0.025)
            .secondary(new Vec3(0.0, 0.5 + random.nextDouble() * 0.4, 0.0))
            .color(5816906, 9234544)
            .lifetime(10 + random.nextInt(5))
            .intensity(0.75F)
            .orient((float)Math.toDegrees(angle) + 90.0F, 14.0F + random.nextFloat() * 22.0F)
            .spawn(level);
      }
   }

   static void leafBurst(ServerLevel level, Vec3 pos, int count) {
      Fx.burst(level, ParticleTypes.HAPPY_VILLAGER, pos, count, 0.5, 0.1);
      Fx.burst(level, LEAF_DUST, pos, count * 2, 0.45, 0.05);
   }

   static void sunlitDrift(ServerLevel level, Vec3 pos) {
      Fx.burst(level, ParticleTypes.HAPPY_VILLAGER, pos, 2, 0.45, 0.015);
      Fx.burst(level, LEAF_DUST, pos, 3, 0.45, 0.02);
   }

   static void gatherAtHand(ServerLevel level, Vec3 hand) {
      RandomSource random = level.getRandom();

      for (int i = 0; i < 4; i++) {
         double angle = random.nextDouble() * Math.PI * 2.0;
         double dist = 0.5 + random.nextDouble() * 0.4;
         Vec3 from = hand.add(Math.cos(angle) * dist, (random.nextDouble() - 0.5) * 0.6, Math.sin(angle) * dist);
         Vec3 vel = hand.subtract(from).scale(0.35);
         level.sendParticles(LEAF_DUST, from.x, from.y, from.z, 0, vel.x, vel.y, vel.z, 1.0);
      }
   }

   static void tipMotes(ServerLevel level, Vec3 tip, int tick) {
      Fx.burst(level, LEAF_DUST, tip, 3, 0.15, 0.02);
      if (tick % 2 == 0) {
         Fx.burst(level, ParticleTypes.HAPPY_VILLAGER, tip, 1, 0.1, 0.02);
      }
   }

   static void fallingPetals(ServerLevel level, Vec3 pos, int argb, int count) {
   }

   static Vec3 inDisc(RandomSource random, Vec3 center, double radius) {
      double angle = random.nextDouble() * Math.PI * 2.0;
      double r = radius * Math.sqrt(random.nextDouble());
      return center.add(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
   }

   @FunctionalInterface
   private interface BraidSink {
      void segment(Vec3 var1, Vec3 var2, double var3, double var5);
   }
}
