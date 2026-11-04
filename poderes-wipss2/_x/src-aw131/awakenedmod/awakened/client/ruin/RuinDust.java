package com.awakenedmod.awakened.client.ruin;

import com.awakenedmod.awakened.core.Fx;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class RuinDust {
   private static final int PER_BODY_CAP = 7;
   private static final int PER_TICK_CAP = 56;
   private static final ParticleOptions CHUNK = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.LIGHT_GRAY_CONCRETE_POWDER.defaultBlockState());
   private static final ParticleOptions DARK_CHUNK = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.TUFF.defaultBlockState());
   private static final ParticleOptions MOTE = Fx.dust(10130831, 0.55F);
   private static int budget;

   private RuinDust() {
   }

   static void beginTick() {
      budget = 56;
   }

   static void tick(Minecraft mc, LivingEntity body, RuinDecayBodies.Body record) {
      float level = record.level;
      if (!(level < 1.0F) && mc.level != null) {
         RandomSource rng = RuinDecayBodies.random();
         AABB box = body.getBoundingBox();
         int spent = 0;
         float ashRate = RuinLook.ASH_RATE.getF() * Mth.clamp((level - 10.0F + 2.0F) / 10.0F, 0.0F, 2.0F);
         float flakeRate = RuinLook.FLAKE_RATE.getF() * Mth.clamp((level - 20.0F + 3.0F) / 20.0F, 0.0F, 2.0F);
         int n = perTick(ashRate, rng);

         for (int i = 0; i < n && spent < 7 && budget > 0; spent++) {
            ash(mc, surfacePoint(rng, box, 0.06), rng);
            i++;
         }

         n = perTick(flakeRate, rng);

         for (int i = 0; i < n && spent < 7 && budget > 0; spent++) {
            chunk(mc, surfacePoint(rng, box, 0.04), rng, 0.04, 0.02);
            i++;
         }
      }
   }

   static void payout(Minecraft mc, LivingEntity body, int stacks) {
      if (mc.level != null) {
         RandomSource rng = RuinDecayBodies.random();
         AABB box = body.getBoundingBox();
         int chunks = Math.min(28, Math.round((float)stacks * RuinLook.PAYOUT_CHUNKS.getF()) + 3);
         Vec3 centre = box.getCenter();

         for (int i = 0; i < chunks && budget > 0; i++) {
            Vec3 at = surfacePoint(rng, box, 0.05);
            Vec3 out = at.subtract(centre).normalize();
            double speed = 0.12 + rng.nextDouble() * 0.2;
            mc.level.addParticle(rng.nextFloat() < 0.75F ? CHUNK : DARK_CHUNK, at.x, at.y, at.z, out.x * speed, 0.1 + rng.nextDouble() * 0.15, out.z * speed);
            budget--;
         }

         for (int i = 0; i < 10 && budget > 0; i++) {
            ash(mc, surfacePoint(rng, box, 0.15), rng);
         }
      }
   }

   static void crumble(Minecraft mc, LivingEntity body, float progress) {
      if (mc.level != null && !(progress >= 1.0F)) {
         RandomSource rng = RuinDecayBodies.random();
         AABB box = body.getBoundingBox();
         double lineY = box.minY + (double)body.getBbHeight() * (1.0 - (double)progress);
         int ashN = RuinLook.CRUMBLE_ASH.getI();
         int chunkN = RuinLook.CRUMBLE_CHUNKS.getI();

         for (int i = 0; i < ashN && budget > 0; i++) {
            double x = Mth.lerp(rng.nextDouble(), box.minX, box.maxX);
            double z = Mth.lerp(rng.nextDouble(), box.minZ, box.maxZ);
            double y = lineY + (rng.nextDouble() - 0.3) * 0.15;
            if (rng.nextFloat() < 0.5F) {
               rise(mc, new Vec3(x, y, z), rng.nextFloat() < 0.6F ? ParticleTypes.ASH : ParticleTypes.WHITE_ASH, rng, 0.005, 0.02);
            } else {
               mc.level.addParticle(MOTE, x, y, z, (rng.nextDouble() - 0.5) * 0.03, -0.02 - rng.nextDouble() * 0.04, (rng.nextDouble() - 0.5) * 0.03);
               budget--;
            }
         }

         Vec3 centre = box.getCenter();

         for (int ix = 0; ix < chunkN && budget > 0; ix++) {
            double x = Mth.lerp(rng.nextDouble(), box.minX, box.maxX);
            double z = Mth.lerp(rng.nextDouble(), box.minZ, box.maxZ);
            Vec3 at = new Vec3(x, lineY, z);
            Vec3 out = new Vec3(x - centre.x, 0.0, z - centre.z);
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(rng.nextDouble() - 0.5, 0.0, rng.nextDouble() - 0.5) : out.normalize();
            double speed = 0.03 + rng.nextDouble() * 0.06;
            mc.level.addParticle(rng.nextFloat() < 0.7F ? CHUNK : DARK_CHUNK, at.x, at.y, at.z, out.x * speed, 0.02, out.z * speed);
            budget--;
         }
      }
   }

   private static void ash(Minecraft mc, Vec3 at, RandomSource rng) {
      float roll = rng.nextFloat();
      if (roll < 0.45F) {
         rise(mc, at, ParticleTypes.ASH, rng, 0.004, 0.015);
      } else if (roll < 0.65F) {
         rise(mc, at, ParticleTypes.WHITE_ASH, rng, 0.004, 0.012);
      } else {
         mc.level.addParticle(MOTE, at.x, at.y, at.z, (rng.nextDouble() - 0.5) * 0.02, -0.01 - rng.nextDouble() * 0.03, (rng.nextDouble() - 0.5) * 0.02);
      }

      budget--;
   }

   private static void chunk(Minecraft mc, Vec3 at, RandomSource rng, double spread, double up) {
      mc.level
         .addParticle(
            rng.nextFloat() < 0.7F ? CHUNK : DARK_CHUNK,
            at.x,
            at.y,
            at.z,
            (rng.nextDouble() - 0.5) * spread,
            up + rng.nextDouble() * up,
            (rng.nextDouble() - 0.5) * spread
         );
      budget--;
   }

   private static void rise(Minecraft mc, Vec3 at, ParticleOptions what, RandomSource rng, double upMin, double upMax) {
      mc.level
         .addParticle(what, at.x, at.y, at.z, (rng.nextDouble() - 0.5) * 0.012, upMin + rng.nextDouble() * (upMax - upMin), (rng.nextDouble() - 0.5) * 0.012);
      budget--;
   }

   private static Vec3 surfacePoint(RandomSource rng, AABB box, double out) {
      double x = Mth.lerp(rng.nextDouble(), box.minX, box.maxX);
      double y = Mth.lerp(rng.nextDouble(), box.minY, box.maxY);
      double z = Mth.lerp(rng.nextDouble(), box.minZ, box.maxZ);
      int face = rng.nextInt(6);
      switch (face) {
         case 0:
            x = box.minX - out;
            break;
         case 1:
            x = box.maxX + out;
            break;
         case 2:
            z = box.minZ - out;
            break;
         case 3:
            z = box.maxZ + out;
            break;
         case 4:
            y = box.maxY + out;
            break;
         default:
            y = box.minY + out * 0.5;
      }

      return new Vec3(x, y, z);
   }

   private static int perTick(float perSecond, RandomSource rng) {
      float perTick = perSecond / 20.0F;
      int n = (int)perTick;
      if (rng.nextFloat() < perTick - (float)n) {
         n++;
      }

      return n;
   }
}
