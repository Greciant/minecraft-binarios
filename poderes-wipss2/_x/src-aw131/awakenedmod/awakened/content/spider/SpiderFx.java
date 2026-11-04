package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class SpiderFx {
   public static final int WEB = -1512203;
   public static final DustParticleOptions SILK = Fx.dust(-1512203, 1.0F);
   public static final DustParticleOptions BLOOD = Fx.dust(-5236712, 1.0F);
   private static final int BOMB_WEB_TICKS = 80;

   private SpiderFx() {
   }

   public static void razorSlice(ServerLevel level, Vec3 pos) {
      Vfx.arc(level, pos.add(-0.55, 0.55, 0.0), pos.add(0.55, -0.55, 0.0), 0.07, -1950917, 4);
      Fx.burst(level, BLOOD, pos, 10, 0.3, 0.07);
      Fx.burst(level, ParticleTypes.CRIT, pos, 6, 0.25, 0.06);
      Fx.sound(level, pos, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.4F);
   }

   public static void impactBomb(ServerLevel level, Vec3 pos) {
      double floorY = floorBelow(level, pos);
      placeCobweb(level, pos.x, pos.y, pos.z, 1.4, floorY);
      int chunks = 7;

      for (int i = 0; i < chunks; i++) {
         double a = level.random.nextDouble() * Math.PI * 2.0;
         double r = 0.6 + level.random.nextDouble() * 1.8;
         double x = pos.x + Math.cos(a) * r;
         double z = pos.z + Math.sin(a) * r;
         double y = pos.y + level.random.nextDouble() * 1.4;
         double s = 0.7 + level.random.nextDouble() * 0.7;
         placeCobweb(level, x, y, z, s, floorY);
      }

      Fx.burst(level, SILK, pos, 16, 1.1, 0.12);
      Fx.burst(level, ParticleTypes.POOF, pos, 20, 0.9, 0.12);
      Fx.burst(level, ParticleTypes.CRIT, pos, 10, 0.5, 0.12);
      Fx.sound(level, pos, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.2F, 0.5F);
      Fx.sound(level, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 1.1F, 0.5F);
      Fx.sound(level, pos, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.9F, 0.7F);
   }

   private static void placeCobweb(ServerLevel level, double x, double y, double z, double size, double floorY) {
      if (!Double.isNaN(floorY)) {
         y = Math.max(y, floorY + size * 0.5);
      }

      Vfx.block(level, new Vec3(x, y, z), Blocks.COBWEB, size, size, size, 80, level.random.nextFloat() * 360.0F);
   }

   private static double floorBelow(ServerLevel level, Vec3 pos) {
      Vec3 from = pos.add(0.0, 0.1, 0.0);
      ClipContext ctx = new ClipContext(from, pos.subtract(0.0, 4.0, 0.0), Block.COLLIDER, Fluid.NONE, CollisionContext.empty());
      HitResult hr = level.clip(ctx);
      return hr.getType() == Type.BLOCK ? hr.getLocation().y : Double.NaN;
   }

   public static void wallWeave(ServerLevel level, Vec3 center, Vec3 right, double halfWidth, double height) {
      wallSheet(level, center, right, halfWidth, height, 0.0F);
      Fx.sound(level, center, SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.9F, 0.9F);
   }

   public static void wallSheet(ServerLevel level, Vec3 center, Vec3 right, double halfWidth, double height) {
      wallSheet(level, center, right, halfWidth, height, 1.0F);
   }

   public static void wallSheet(ServerLevel level, Vec3 center, Vec3 right, double halfWidth, double height, float progress) {
      float p = Math.max(0.08F, Math.min(1.0F, progress));
      double hw = halfWidth * (double)p;
      double h = height * (double)p;
      Vec3 base = center.subtract(0.0, h * 0.5, 0.0);
      Fx.sheet(level, SILK, base, right, hw, h, 6, 6);
   }

   public static void shotHit(ServerLevel level, Vec3 pos) {
      Fx.sound(level, pos, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.45F, 1.7F + level.random.nextFloat() * 0.3F);
   }

   public static void shotLaunch(ServerLevel level, Vec3 wrist) {
      Fx.burst(level, SILK, wrist, 2, 0.1, 0.02);
      Fx.sound(level, wrist, SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.6F, 1.4F + level.random.nextFloat() * 0.4F);
   }

   public static void webUp(ServerLevel level, Vec3 pos, double radius) {
      webStar(level, pos, radius, 7);
      Fx.sound(level, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 0.7F, 1.1F);
   }

   public static void shotKill(ServerLevel level, Vec3 pos) {
      webStar(level, pos, 1.0, 8);
      Fx.burst(level, ParticleTypes.CLOUD, pos, 4, 0.3, 0.02);
      Fx.sound(level, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 0.9F, 0.9F);
      Fx.sound(level, pos, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.7F, 0.8F);
   }

   public static void shotInFlight(ServerLevel level, WebType.Flight flight, int themeColor, Vec3 pos, Vec3 velocity) {
      DustParticleOptions tint = Fx.dust(themeColor, flight == WebType.Flight.BALL ? 1.3F : 0.9F);
      switch (flight) {
         case RAZOR:
            for (int i = 0; i < 3; i++) {
               Vec3 p = pos.subtract(velocity.scale((double)i / 3.0));
               level.sendParticles(tint, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }

            level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            break;
         case BALL:
            strand(level, pos, velocity, 3, 0.05);
            level.sendParticles(tint, pos.x, pos.y, pos.z, 2, 0.07, 0.07, 0.07, 0.0);
            break;
         default:
            strand(level, pos, velocity, 2, 0.03);
            level.sendParticles(tint, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
      }
   }

   private static void strand(ServerLevel level, Vec3 pos, Vec3 velocity, int count, double spread) {
      Vec3 back = velocity.lengthSqr() < 1.0E-6 ? Vec3.ZERO : velocity;

      for (int i = 0; i < count; i++) {
         double t = count <= 1 ? 0.0 : (double)i / (double)count;
         Vec3 p = pos.subtract(back.scale(t));
         cobweb(level, p, 1, spread);
      }
   }

   public static void splatImpact(ServerLevel level, Vec3 pos) {
      cobweb(level, pos, 3, 0.22);
      Fx.sound(level, pos, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.5F, 1.4F + level.random.nextFloat() * 0.3F);
   }

   private static void cobweb(ServerLevel level, Vec3 pos, int count, double spread) {
      Fx.burst(level, SILK, pos, count, spread, 0.0);
   }

   public static void webStar(ServerLevel level, Vec3 center, double radius, int spokes) {
      for (int i = 0; i < spokes; i++) {
         double a = (Math.PI * 2) * (double)i / (double)spokes;
         double ax = Math.cos(a);
         double az = Math.sin(a);

         for (int s = 1; s <= 3; s++) {
            double r = radius * (double)s / 3.0;
            double jy = (level.random.nextDouble() - 0.5) * 0.25;
            Vec3 p = new Vec3(center.x + ax * r, center.y + jy, center.z + az * r);
            if (s == 1) {
               level.sendParticles(SILK, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            } else {
               cobweb(level, p, 1, 0.02);
            }
         }
      }

      cobweb(level, center, 4, radius * 0.4);
   }

   public static void thwip(ServerLevel level, Vec3 wrist, Vec3 anchor) {
      Vfx.beam(level, wrist, anchor, 0.1, -1512203, 3);
      Fx.sound(level, wrist, SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.7F, 1.6F);
      level.sendParticles(ParticleTypes.CRIT, anchor.x, anchor.y, anchor.z, 4, 0.05, 0.05, 0.05, 0.0);
   }

   public static void yankWeb(ServerLevel level, ServerPlayer caster, Vec3 target) {
      Fx.soundExcept(level, caster, target, SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.8F, 1.4F);
      Fx.sound(level, target, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.6F, 1.5F);
   }

   public static void webStrand(ServerLevel level, Vec3 from, Vec3 to) {
      strandLine(level, from, to, 0.03);
      webStar(level, to, 0.5, 6);
      Fx.sound(level, from, SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.6F, 1.7F);
   }

   private static void strandLine(ServerLevel level, Vec3 from, Vec3 to, double spread) {
      Vec3 span = to.subtract(from);
      double len = span.length();
      int steps = Math.max(3, (int)Math.round(len * 2.5));
      Vec3 step = len < 1.0E-4 ? Vec3.ZERO : span.scale(1.0 / (double)steps);

      for (int i = 1; i <= steps; i++) {
         cobweb(level, from.add(step.scale((double)i)), 1, spread);
      }
   }

   public static void strikeImpact(ServerLevel level, Vec3 pos, float intensity) {
      float i = Math.max(0.0F, Math.min(1.0F, intensity));
      int silk = 12 + (int)(18.0F * i);
      Fx.burst(level, SILK, pos, silk, 0.5 + 0.5 * (double)i, 0.06 + 0.06 * (double)i);
      Fx.burst(level, ParticleTypes.CLOUD, pos, 6 + (int)(8.0F * i), 0.4 + 0.4 * (double)i, 0.02 + 0.03 * (double)i);
      Fx.burst(level, ParticleTypes.CRIT, pos, 6 + (int)(10.0F * i), 0.4, 0.08);
      Fx.sound(level, pos, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.7F - 0.2F * i);
      Fx.sound(level, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 1.0F, 0.6F);
      Fx.sound(level, pos, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.7F, 1.4F);
   }

   public static void trick(ServerLevel level, Vec3 body) {
      Fx.burst(level, SILK, body, 4, 0.35, 0.02);
      Fx.sound(level, body, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.35F, 1.5F);
   }

   public static void twang(ServerLevel level, Vec3 pos) {
      Fx.sound(level, pos, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.6F, 1.3F);
   }

   public static void cycloneSpoke(ServerLevel level, Vec3 wrist) {
      Fx.sound(level, wrist, SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.35F, 1.8F);
   }
}
