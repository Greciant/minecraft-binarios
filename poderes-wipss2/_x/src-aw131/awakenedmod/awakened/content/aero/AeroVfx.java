package com.awakenedmod.awakened.content.aero;

import com.awakenedmod.awakened.content.vfx.BlockFxEntity;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.LoopSound;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class AeroVfx {
   public static final int SKY = 12577023;
   public static final int WHITE_CORE = 16055551;
   private static final float GUST_HALF_ANGLE_DEG = 30.0F;
   private static final int GUST_CLOUD_MOTES = 30;
   private static final int GUST_GUST_PUFFS = 5;
   public static final double CRESCENT_RADIUS = 0.75;
   public static final double CRESCENT_SPEED = 1.6;
   public static final double TORNADO_RADIUS = 4.0;
   public static final double TORNADO_HEIGHT = 12.0;
   public static final int TORNADO_LIFETIME = 100;
   private static final int TORNADO_DEBRIS_CHUNKS = 12;
   private static final int TORNADO_COLLAPSE_TICKS = 10;
   private static final int TORNADO_FLING_MAX_TICKS = 25;
   private static final double FLING_GRAVITY = 0.05;
   private static final double FLING_DRAG = 0.96;
   private static final int TORNADO_SKIRT_MOTES = 3;
   public static final int DUST_HAZE = 14209736;
   public static final int DUST_CREAM = 16118248;
   private static final int FUNNEL_DUST = 14077892;
   public static final double REMOVAL_RADIUS = 9.0;
   public static final double REMOVAL_WALL_HEIGHT = 5.0;
   public static final double REMOVAL_BELOW_REACH = 3.0;
   public static final double REMOVAL_BOUNDARY_SHELL = 1.5;
   public static final int REMOVAL_LIFETIME = 240;
   private static final int REMOVAL_SUCTION_TICKS = 15;
   private static final int REMOVAL_SUCTION_MOTES = 10;
   private static final int REMOVAL_WALL_MOTES = 8;
   private static final int REMOVAL_COLLAPSE_TICKS = 15;
   private static final double CLOUD_GLIDE_FACTOR = 12.0;
   public static final float FUNNEL_TAPER = 1.15F;
   private static final float CRESCENT_BANK_DEG = 35.0F;
   private static final int CRESCENT_STEEP_ONE_IN = 5;
   private static final int GASP_BUBBLES = 5;
   private static final int WISP_EVERY = 5;
   private static final int BUFFET_EVERY = 3;
   private static final int LEAP_TRAIL_TICKS = 16;
   private static final double TRAIL_MIN_SPEED = 0.12;

   private AeroVfx() {
   }

   public static float funnelFrac(float f) {
      return 0.3F + 0.7F * (float)Math.pow((double)f, 1.15F);
   }

   public static void gustCone(ServerLevel level, Vec3 origin, Vec3 dir) {
      gustCone(level, origin, dir, 1.0F);
   }

   public static void gustCone(ServerLevel level, Vec3 origin, Vec3 dir, float power) {
      Vec3 n = Vfx.safeDir(dir);
      float yaw = Vfx.yawAlong(n);
      float pitch = 90.0F + Vfx.pitchAlong(n);
      float p = Math.max(1.0F, power);
      Vfx.effect(VfxEntity.Kind.RING, origin.add(n.scale(1.0))).scale(2.2 * (double)p).color(12577023, 16055551).lifetime(6).orient(yaw, pitch).spawn(level);
      Vfx.effect(VfxEntity.Kind.RING, origin.add(n.scale(2.8)))
         .scale(3.2 * (double)p)
         .color(12577023)
         .intensity(0.75F)
         .lifetime(8)
         .delay(2)
         .orient(yaw, pitch)
         .spawn(level);
      Vfx.effect(VfxEntity.Kind.RING, origin.add(n.scale(4.6)))
         .scale(4.0 * (double)p)
         .color(12577023)
         .intensity(0.5F)
         .lifetime(9)
         .delay(4)
         .orient(yaw, pitch)
         .spawn(level);
      RandomSource rand = level.random;
      int motes = Math.round(30.0F * p);

      for (int i = 0; i < motes; i++) {
         Vec3 d = coneDir(rand, n, 30.0F);
         double speed = (0.45 + rand.nextDouble() * 0.5) * (0.7 + 0.3 * (double)p);
         mote(level, ParticleTypes.CLOUD, origin.add(d.scale(0.6)), d.scale(speed));
      }

      int puffs = Math.round(5.0F * p);

      for (int i = 0; i < puffs; i++) {
         Vec3 d = coneDir(rand, n, 21.0F);
         mote(level, ParticleTypes.GUST, origin.add(d.scale(1.5 + rand.nextDouble() * 2.5)), Vec3.ZERO);
      }

      float vol = Math.min(1.8F, p);
      Fx.sound(level, origin, SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, 1.2F * vol, 0.65F);
      Fx.sound(level, origin, SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.0F * vol, 0.7F);
   }

   public static float rollCrescentBank(RandomSource rand) {
      return rand.nextInt(5) == 0 ? (rand.nextBoolean() ? 1.0F : -1.0F) * (55.0F + rand.nextFloat() * 12.0F) : (rand.nextFloat() * 2.0F - 1.0F) * 35.0F;
   }

   public static void crescent(ServerLevel level, Vec3 origin, Vec3 dir, double range, float rollDeg) {
      Vec3 n = Vfx.safeDir(dir);
      float yaw = Vfx.yawAlong(n);
      float pitch = 90.0F + Vfx.pitchAlong(n);
      Vec3 roll = new Vec3((double)rollDeg, 0.0, 0.0);
      int steps = Math.max(1, (int)Math.ceil(range / 1.6));

      for (int i = 0; i < steps; i++) {
         Vec3 p = origin.add(n.scale(1.6 * (double)(i + 1)));
         Vfx.effect(VfxEntity.Kind.CRESCENT, p)
            .scale(0.75)
            .color(12577023, 16055551)
            .intensity(1.2F)
            .lifetime(3)
            .delay(i)
            .orient(yaw, pitch)
            .secondary(roll)
            .spawn(level);
      }

      mote(level, ParticleTypes.SWEEP_ATTACK, origin.add(n.scale(1.2)), Vec3.ZERO);
      Fx.sound(level, origin, SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, 0.9F, 1.6F);
      Fx.sound(level, origin, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.5F);
      AeroAmbience.add(new AeroVfx.CrescentTrail(level, origin, n, steps));
   }

   public static void tornado(ServerLevel level, Vec3 base) {
      Vfx.effect(VfxEntity.Kind.TORNADO, base).scale(4.0).secondary(new Vec3(0.0, 12.0, 0.0)).color(14209736, 16118248).lifetime(100).spawn(level);
      Fx.sound(level, base, SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.4F, 0.5F);
      AeroAmbience.add(new AeroVfx.TornadoFx(level, base));
   }

   public static void pullBand(ServerLevel level, Vec3 base, double inner, double outer) {
      RandomSource rand = level.random;

      for (int i = 0; i < 3; i++) {
         double a = rand.nextDouble() * Math.PI * 2.0;
         double r = inner + rand.nextDouble() * (outer - inner);
         double y = 0.2 + rand.nextDouble() * 1.8;
         Vec3 p = base.add(Math.cos(a) * r, y, Math.sin(a) * r);
         Vec3 in = new Vec3(-Math.cos(a), 0.0, -Math.sin(a)).scale(0.25 + rand.nextDouble() * 0.25);
         mote(level, (ParticleOptions)(rand.nextBoolean() ? ParticleTypes.CLOUD : Fx.dust(14209736, 0.9F)), p, in);
      }
   }

   public static void flingBurst(ServerLevel level, Vec3 at) {
      Fx.burst(level, ParticleTypes.CLOUD, at, 8, 0.5, 0.08);
      Fx.burst(level, Fx.dust(16118248, 1.1F), at, 5, 0.4, 0.05);
      Fx.sound(level, at, (SoundEvent)SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.8F, 1.15F);
   }

   public static void airRemoval(ServerLevel level, Vec3 center) {
      Vfx.effect(VfxEntity.Kind.AIR_WALL, center).scale(9.0).secondary(new Vec3(0.0, 5.0, 0.0)).color(12577023, 16055551).lifetime(240).spawn(level);
      Fx.sound(level, center, SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.PLAYERS, 1.3F, 0.9F);
      Fx.sound(level, center, SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.5F, 0.45F);
      Fx.sound(level, center, SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, 1.3F, 0.55F);
      AeroAmbience.add(new AeroVfx.AirRemovalFx(level, center));
   }

   public static void suffocationGasp(ServerLevel level, LivingEntity victim) {
      RandomSource rand = level.random;
      Vec3 mouth = victim.getEyePosition().add(0.0, -0.1, 0.0);
      double w = (double)victim.getBbWidth() * 0.5;

      for (int i = 0; i < 5; i++) {
         Vec3 p = mouth.add(rand.nextGaussian() * w * 0.4, rand.nextDouble() * 0.15, rand.nextGaussian() * w * 0.4);
         mote(level, ParticleTypes.BUBBLE_POP, p, new Vec3(rand.nextGaussian() * 0.04, 0.1 + rand.nextDouble() * 0.08, rand.nextGaussian() * 0.04));
      }

      Fx.burst(level, ParticleTypes.CLOUD, mouth, 2, 0.2, 0.01);

      for (int i = 0; i < 3; i++) {
         double a = rand.nextDouble() * Math.PI * 2.0;
         Vec3 p = victim.position().add(Math.cos(a) * w * 1.2, rand.nextDouble() * (double)victim.getBbHeight(), Math.sin(a) * w * 1.2);
         mote(level, Fx.dust(12577023, 0.8F), p, new Vec3(0.0, 0.04, 0.0));
      }
   }

   public static void suffocationWisp(ServerLevel level, LivingEntity victim) {
      if (victim.tickCount % 5 == 0) {
         RandomSource rand = level.random;
         double w = (double)victim.getBbWidth() * 0.5;
         Vec3 mouth = victim.getEyePosition().add(rand.nextGaussian() * w * 0.3, -0.05, rand.nextGaussian() * w * 0.3);
         mote(level, ParticleTypes.BUBBLE_POP, mouth, new Vec3(rand.nextGaussian() * 0.03, 0.08 + rand.nextDouble() * 0.05, rand.nextGaussian() * 0.03));
         double a = rand.nextDouble() * Math.PI * 2.0;
         Vec3 p = victim.position().add(Math.cos(a) * w * 1.1, rand.nextDouble() * (double)victim.getBbHeight() * 0.8, Math.sin(a) * w * 1.1);
         mote(level, Fx.dust(16055551, 0.7F), p, new Vec3(0.0, 0.03, 0.0));
      }
   }

   public static void boundaryBuffet(ServerLevel level, LivingEntity victim) {
      if (victim.tickCount % 3 == 0) {
         RandomSource rand = level.random;
         Vec3 push = victim.getDeltaMovement();
         Vec3 against = push.lengthSqr() > 1.0E-6 ? push.normalize().scale(-1.0) : new Vec3(0.0, 0.0, 1.0);
         double w = (double)victim.getBbWidth() * 0.5 + 0.3;
         Fx.burst(level, ParticleTypes.CLOUD, victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0), 5, w, 0.06);

         for (int i = 0; i < 3; i++) {
            Vec3 off = new Vec3(rand.nextGaussian(), rand.nextGaussian() * 0.5, rand.nextGaussian()).scale(w * 0.6);
            Vec3 p = victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0).add(off);
            mote(level, Fx.dust(12577023, 0.9F), p, against.scale(0.12 + rand.nextDouble() * 0.08));
         }
      }
   }

   public static void leapRing(ServerLevel level, Vec3 feet) {
      Vfx.ring(level, feet.add(0.0, 0.1, 0.0), 1.7, 12577023, 8);
      Fx.burst(level, ParticleTypes.CLOUD, feet.add(0.0, 0.2, 0.0), 12, 0.7, 0.04);
      mote(level, ParticleTypes.GUST, feet.add(0.0, 0.4, 0.0), Vec3.ZERO);
      Fx.sound(level, feet, SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.1F, 1.3F);
   }

   public static void windTrail(ServerLevel level, Vec3 from, Vec3 to) {
      Vec3 seg = to.subtract(from);
      double len = seg.length();
      if (!(len < 0.5)) {
         Vec3 n = seg.scale(1.0 / len);
         RandomSource rand = level.random;
         int wisps = Mth.clamp((int)(len * 3.0), 10, 28);

         for (int i = 0; i < wisps; i++) {
            Vec3 p = from.add(n.scale(rand.nextDouble() * len)).add(new Vec3(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).scale(0.18));
            mote(level, ParticleTypes.CLOUD, p, n.scale(0.18 + rand.nextDouble() * 0.1));
         }

         mote(level, ParticleTypes.GUST, from.add(n.scale(len * 0.5)), n.scale(0.1));
      }
   }

   public static void leapTrail(ServerPlayer player) {
      AeroAmbience.add(new AeroVfx.LeapTrailFx(player));
   }

   public static void flightBurst(ServerLevel level, Vec3 center) {
      Vfx.sphere(level, center, 1.2, 12577023, 7, 0.8F);
      Vfx.ring(level, center, 2.0, 12577023, 8);
      Fx.burst(level, ParticleTypes.CLOUD, center, 10, 0.8, 0.06);
      mote(level, ParticleTypes.GUST, center, Vec3.ZERO);
      Fx.sound(level, center, (SoundEvent)SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
   }

   public static void flightTrail(ServerLevel level, Vec3 feet, Vec3 move, long gameTime) {
      double speed = move.length();
      RandomSource rand = level.random;
      if (speed < 0.12) {
         if (gameTime % 2L == 0L) {
            double a = (double)(gameTime % 40L) / 40.0 * Math.PI * 2.0 + rand.nextDouble() * 0.6;
            double r = 0.55 + rand.nextDouble() * 0.25;
            Vec3 p = feet.add(Math.cos(a) * r, -0.15 + rand.nextDouble() * 0.2, Math.sin(a) * r);
            Vec3 tangent = new Vec3(-Math.sin(a), -0.02, Math.cos(a)).scale(0.12);
            mote(level, ParticleTypes.CLOUD, p, tangent);
         }
      } else {
         Vec3 center = feet.add(0.0, 0.6, 0.0);
         Vec3 back = move.scale(-1.0 / speed);

         for (int i = 0; i < 4; i++) {
            Vec3 off = new Vec3(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).scale(0.25);
            Vec3 p = center.add(back.scale(0.6 + rand.nextDouble() * 1.6)).add(off);
            mote(level, ParticleTypes.CLOUD, p, back.scale(0.06));
         }
      }
   }

   public static void glideCatch(ServerLevel level, Vec3 feet) {
      Vfx.ring(level, feet.add(0.0, -0.1, 0.0), 1.3, 12577023, 7);
      Fx.burst(level, ParticleTypes.CLOUD, feet.add(0.0, 0.1, 0.0), 8, 0.6, 0.05);
      mote(level, ParticleTypes.GUST, feet, Vec3.ZERO);
      Fx.sound(level, feet, SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.9F, 1.5F);
   }

   private static void mote(ServerLevel level, ParticleOptions particle, Vec3 pos, Vec3 vel) {
      level.sendParticles(particle, pos.x, pos.y, pos.z, 0, vel.x, vel.y, vel.z, 1.0);
   }

   private static Vec3 coneDir(RandomSource rand, Vec3 axis, float halfAngleDeg) {
      Vec3 u = Math.abs(axis.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0).cross(axis) : new Vec3(0.0, 1.0, 0.0).cross(axis);
      u = u.normalize();
      Vec3 v = axis.cross(u).normalize();
      double ang = rand.nextDouble() * Math.PI * 2.0;
      double spread = Math.tan(Math.toRadians((double)halfAngleDeg)) * Math.sqrt(rand.nextDouble());
      return axis.add(u.scale(Math.cos(ang) * spread)).add(v.scale(Math.sin(ang) * spread)).normalize();
   }

   private static Block debrisMaterial(ServerLevel level, Vec3 pos) {
      Fx.Floor floor = Fx.floorState(level, pos.x, pos.y, pos.z);
      return floor != null ? floor.state().getBlock() : Blocks.DIRT;
   }

   private static final class AirRemovalFx implements AeroAmbience.TickFx {
      private static final ParticleOptions WALL_BODY = Fx.dust(16055551, 1.4F);
      private static final ParticleOptions STILL_MOTE = Fx.dust(11518668, 0.7F);
      private final ServerLevel level;
      private final Vec3 center;
      private int age;

      AirRemovalFx(ServerLevel level, Vec3 center) {
         this.level = level;
         this.center = center;
      }

      @Override
      public boolean tick() {
         this.age++;
         if (this.age > 240) {
            return false;
         } else {
            RandomSource rand = this.level.random;
            int collapseStart = 225;
            if (this.age <= 15) {
               this.suctionTick(rand);
            } else if (this.age < collapseStart) {
               this.wallTick(rand);
            } else {
               this.collapseTick(rand);
            }

            if (this.age == collapseStart) {
               Fx.sound(this.level, this.center, SoundEvents.AMBIENT_UNDERWATER_EXIT, SoundSource.PLAYERS, 1.2F, 1.0F);
               Fx.sound(this.level, this.center, SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, 1.3F, 0.5F);
            }

            if (this.age == 238) {
               Fx.sound(this.level, this.center, SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.4F, 0.4F);
               Fx.burst(this.level, ParticleTypes.CLOUD, this.center.add(0.0, 0.8, 0.0), 20, 1.2, 0.1);
               AeroVfx.mote(this.level, ParticleTypes.GUST, this.center.add(0.0, 1.0, 0.0), Vec3.ZERO);
            }

            return true;
         }
      }

      private void suctionTick(RandomSource rand) {
         for (int i = 0; i < 10; i++) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double r = 9.0 * (0.05 + rand.nextDouble() * 0.6);
            double y = 0.2 + rand.nextDouble() * 4.6;
            Vec3 p = this.center.add(Math.cos(a) * r, y, Math.sin(a) * r);
            double speed = (9.0 - r) / 12.0;
            Vec3 out = new Vec3(Math.cos(a), 0.0, Math.sin(a)).scale(speed);
            AeroVfx.mote(this.level, ParticleTypes.CLOUD, p, out);
            if (i % 4 == 0) {
               for (int k = 1; k <= 3; k++) {
                  double rr = r + (9.0 - r) * (double)k / 4.0;
                  AeroVfx.mote(this.level, WALL_BODY, this.center.add(Math.cos(a) * rr, y, Math.sin(a) * rr), Vec3.ZERO);
               }
            }
         }
      }

      private void wallTick(RandomSource rand) {
         for (int i = 0; i < 8; i++) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double r = 9.0 * (0.94 + rand.nextDouble() * 0.12);
            double y = rand.nextDouble() * 5.0;
            Vec3 p = this.center.add(Math.cos(a) * r, y, Math.sin(a) * r);
            Vec3 churn = new Vec3(-Math.sin(a), 0.0, Math.cos(a)).scale(0.1 + rand.nextDouble() * 0.08).add(0.0, (rand.nextDouble() - 0.4) * 0.1, 0.0);
            AeroVfx.mote(this.level, (ParticleOptions)(rand.nextInt(3) == 0 ? WALL_BODY : ParticleTypes.CLOUD), p, churn);
         }

         if (this.age % 2 == 0) {
            for (int i = 0; i < 2; i++) {
               boolean top = i == 0;
               double a = rand.nextDouble() * Math.PI * 2.0;
               double r = 9.0 * Math.sqrt(rand.nextDouble()) * 0.92;
               double y = top ? 4.85 : 0.25;
               Vec3 p = this.center.add(Math.cos(a) * r, y, Math.sin(a) * r);
               Vec3 slide = new Vec3(-Math.sin(a), 0.0, Math.cos(a)).scale((0.06 + rand.nextDouble() * 0.05) * (r / 9.0));
               AeroVfx.mote(this.level, (ParticleOptions)(rand.nextInt(3) == 0 ? WALL_BODY : ParticleTypes.CLOUD), p, slide);
            }
         }

         if (this.age % 5 == 0) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double r = 9.0 * rand.nextDouble() * 0.7;
            AeroVfx.mote(this.level, STILL_MOTE, this.center.add(Math.cos(a) * r, 0.4 + rand.nextDouble() * 3.5, Math.sin(a) * r), Vec3.ZERO);
         }

         if (this.age % 30 == 0) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            Fx.sound(this.level, this.center.add(Math.cos(a) * 9.0, 1.0, Math.sin(a) * 9.0), SoundEvents.BREEZE_IDLE_AIR, SoundSource.PLAYERS, 0.8F, 0.7F);
         }
      }

      private void collapseTick(RandomSource rand) {
         for (int i = 0; i < 14; i++) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double r = 9.0 * (0.9 + rand.nextDouble() * 0.15);
            double y = 0.2 + rand.nextDouble() * 5.0;
            Vec3 p = this.center.add(Math.cos(a) * r, y, Math.sin(a) * r);
            Vec3 in = new Vec3(-Math.cos(a), 0.0, -Math.sin(a)).scale(r / 12.0 * 1.15);
            AeroVfx.mote(this.level, (ParticleOptions)(rand.nextInt(3) == 0 ? WALL_BODY : ParticleTypes.CLOUD), p, in);
         }
      }
   }

   private static final class CrescentTrail implements AeroAmbience.TickFx {
      private final ServerLevel level;
      private final Vec3 origin;
      private final Vec3 dir;
      private final int steps;
      private int age;

      CrescentTrail(ServerLevel level, Vec3 origin, Vec3 dir, int steps) {
         this.level = level;
         this.origin = origin;
         this.dir = dir;
         this.steps = steps;
      }

      @Override
      public boolean tick() {
         this.age++;
         if (this.age > this.steps) {
            Fx.sound(this.level, this.origin.add(this.dir.scale(1.6 * (double)this.steps)), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.4F, 1.7F);
            return false;
         } else {
            Vec3 p = this.origin.add(this.dir.scale(1.6 * (double)this.age));
            RandomSource rand = this.level.random;

            for (int i = 0; i < 2; i++) {
               Vec3 off = new Vec3(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).scale(0.18);
               AeroVfx.mote(this.level, Fx.dust(12577023, 0.7F), p.add(off), Vec3.ZERO);
            }

            return true;
         }
      }
   }

   private static final class LeapTrailFx implements AeroAmbience.TickFx {
      private final ServerPlayer player;
      private Vec3 lastPos;
      private int age;

      LeapTrailFx(ServerPlayer player) {
         this.player = player;
         this.lastPos = player.position();
      }

      @Override
      public boolean tick() {
         if (this.player.isAlive() && this.age < 16) {
            this.age++;
            Vec3 pos = this.player.position();
            Vec3 move = pos.subtract(this.lastPos);
            this.lastPos = pos;
            AeroVfx.flightTrail(this.player.serverLevel(), pos, move, this.player.serverLevel().getGameTime());
            return true;
         } else {
            return false;
         }
      }
   }

   private static final class TornadoFx implements AeroAmbience.TickFx {
      private static final ParticleOptions FUNNEL_BODY = Fx.dust(14077892, 1.3F);
      private final ServerLevel level;
      private final Vec3 base;
      private final Block material;
      private final BlockFxEntity[] chunks = new BlockFxEntity[12];
      private final double[] angle = new double[12];
      private final double[] angVel = new double[12];
      private final double[] heightFrac = new double[12];
      private final double[] radFrac = new double[12];
      private final int[] releaseAt = new int[12];
      private final int[] flingEnd = new int[12];
      private final Vec3[] flingPos = new Vec3[12];
      private final Vec3[] flingVel = new Vec3[12];
      private int age;

      TornadoFx(ServerLevel level, Vec3 base) {
         this.level = level;
         this.base = base;
         this.material = AeroVfx.debrisMaterial(level, base);
         RandomSource rand = level.random;

         for (int i = 0; i < 12; i++) {
            this.angle[i] = rand.nextDouble() * Math.PI * 2.0;
            this.angVel[i] = 0.22 + rand.nextDouble() * 0.2;
            this.heightFrac[i] = 0.1 + rand.nextDouble() * 0.62;
            this.radFrac[i] = this.heightFrac[i] > 0.45 ? 1.05 + rand.nextDouble() * 0.2 : 0.65 + rand.nextDouble() * 0.45;
            double size = (0.45 + rand.nextDouble() * 0.35) * (1.0 + this.heightFrac[i] * 0.35);
            this.releaseAt[i] = 90 + (int)Math.round(this.heightFrac[i] / 0.72 * 7.0) + rand.nextInt(3);
            this.flingEnd[i] = this.releaseAt[i] + 15 + rand.nextInt(11);
            this.chunks[i] = Vfx.blockTracked(level, base, this.material, size, size, size, 145, rand.nextFloat() * 360.0F);
         }
      }

      @Override
      public boolean tick() {
         this.age++;
         if (this.age > 100) {
            return this.collapseTick();
         } else {
            RandomSource rand = this.level.random;

            for (int i = 0; i < 3; i++) {
               double a = rand.nextDouble() * Math.PI * 2.0;
               double f = rand.nextDouble() * rand.nextDouble() * 0.18;
               double r = 4.0 * (0.35 + rand.nextDouble() * 0.95);
               Vec3 p = this.base.add(Math.cos(a) * r, 0.15 + 12.0 * f, Math.sin(a) * r);
               Vec3 tangent = new Vec3(-Math.sin(a), 0.0, Math.cos(a)).scale(0.5 + rand.nextDouble() * 0.25).add(0.0, 0.05 + rand.nextDouble() * 0.1, 0.0);
               AeroVfx.mote(this.level, (ParticleOptions)(rand.nextBoolean() ? ParticleTypes.CLOUD : FUNNEL_BODY), p, tangent);
            }

            if (this.age % 2 == 0) {
               double a = rand.nextDouble() * Math.PI * 2.0;
               double r = 1.0 + rand.nextDouble() * 3.5;
               double x = this.base.x + Math.cos(a) * r;
               double z = this.base.z + Math.sin(a) * r;
               this.level
                  .sendParticles(
                     new BlockParticleOption(ParticleTypes.BLOCK, this.material.defaultBlockState()), x, this.base.y + 0.15, z, 3, 0.25, 0.15, 0.25, 0.12
                  );
            }

            if (this.age % 15 == 0) {
               float f = rand.nextFloat();
               AeroVfx.mote(this.level, ParticleTypes.GUST, this.base.add(0.0, 12.0 * (double)f, 0.0), Vec3.ZERO);
            }

            this.debrisStep(rand);
            LoopSound.keepAt(
               this.level,
               "tornado@" + Integer.toHexString(System.identityHashCode(this)),
               this.base.add(0.0, 2.0, 0.0),
               SoundEvents.BREEZE_IDLE_AIR,
               SoundSource.PLAYERS,
               1.6F,
               0.55F
            );
            if (this.age % 24 == 0) {
               Fx.sound(this.level, this.base.add(0.0, 6.0, 0.0), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8F, 0.55F);
            }

            return true;
         }
      }

      private boolean collapseTick() {
         if (this.age > 140) {
            for (BlockFxEntity chunk : this.chunks) {
               if (chunk != null && !chunk.isRemoved()) {
                  chunk.discard();
               }
            }

            return false;
         } else {
            return this.debrisStep(this.level.random);
         }
      }

      private boolean debrisStep(RandomSource rand) {
         boolean anyLive = false;

         for (int i = 0; i < this.chunks.length; i++) {
            if (this.chunks[i] != null && !this.chunks[i].isRemoved()) {
               anyLive = true;
               if (this.flingVel[i] != null) {
                  this.flyChunk(i, rand);
               } else if (this.age < this.releaseAt[i]) {
                  this.orbitChunk(i);
               } else {
                  this.releaseChunk(i, rand);
               }
            }
         }

         return anyLive;
      }

      private Vec3 orbitChunk(int i) {
         this.angle[i] = this.angle[i] + this.angVel[i];
         double f = Mth.clamp(this.heightFrac[i] + 0.25 * Math.sin((double)(this.age + i * 17) * 0.05), 0.05, 0.9);
         double r = 4.0 * (double)AeroVfx.funnelFrac((float)f) * this.radFrac[i];
         Vec3 p = new Vec3(this.base.x + Math.cos(this.angle[i]) * r, this.base.y + 12.0 * f, this.base.z + Math.sin(this.angle[i]) * r);
         this.chunks[i].setPos(p.x, p.y, p.z);
         return p;
      }

      private void releaseChunk(int i, RandomSource rand) {
         Vec3 p = this.orbitChunk(i);
         double dx = p.x - this.base.x;
         double dz = p.z - this.base.z;
         double r = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
         double cos = dx / r;
         double sin = dz / r;
         this.flingPos[i] = p;
         this.flingVel[i] = new Vec3(-sin, 0.0, cos).scale(this.angVel[i] * r).add(cos * 0.25, 0.15 + rand.nextDouble() * 0.2, sin * 0.25);
      }

      private void flyChunk(int i, RandomSource rand) {
         Vec3 v = this.flingVel[i];
         this.flingVel[i] = new Vec3(v.x * 0.96, v.y - 0.05, v.z * 0.96);
         this.flingPos[i] = this.flingPos[i].add(this.flingVel[i]);
         Vec3 p = this.flingPos[i];
         boolean grounded = p.y <= this.base.y + 0.2;
         if (!grounded && this.age < this.flingEnd[i]) {
            this.chunks[i].setPos(p.x, p.y, p.z);
         } else {
            this.expireChunk(i, grounded ? new Vec3(p.x, this.base.y + 0.2, p.z) : p, grounded, rand);
         }
      }

      private void expireChunk(int i, Vec3 p, boolean grounded, RandomSource rand) {
         this.level
            .sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, this.material.defaultBlockState()), p.x, p.y + 0.15, p.z, 6, 0.22, 0.12, 0.22, 0.08);
         if (grounded) {
            Fx.sound(
               this.level,
               p,
               this.material.defaultBlockState().getSoundType(this.level, BlockPos.containing(p), null).getBreakSound(),
               SoundSource.PLAYERS,
               0.45F,
               0.8F + rand.nextFloat() * 0.4F
            );
         }

         this.chunks[i].discard();
      }
   }
}
