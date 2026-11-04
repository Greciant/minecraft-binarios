package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Ballistics;
import com.awakenedmod.awakened.core.caster.Caster;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class BarrageEngine {
   public static final String BALANCE_KEY = BarrageAbility.class.getSimpleName();
   public static final float UPFRONT_COST = 6.0F;
   public static final float DRAIN_PER_TICK = 0.6F;
   private static final int REFUSAL_GRACE = 20;
   public static final String FAIL_NO_ROOM = "awakened.pyrokinetic.barrage.no_room";
   private static final String FAIL_NO_GROUND = "awakened.pyrokinetic.barrage.no_ground";
   private static final String FAIL_TOO_CLOSE = "awakened.pyrokinetic.barrage.too_close";
   private static final String FAIL_OUT_OF_REACH = "awakened.pyrokinetic.barrage.out_of_reach";
   private static final double MAX_RANGE = 30.0;
   private static final double MIN_RANGE = 5.0;
   private static final int FLIGHT_TICKS = 28;
   private static final int FLIGHT_TICKS_MIN = 9;
   private static final double CEILING_MARGIN = 0.4;
   private static final double CEILING_PROBE = 8.4;
   private static final double SHELL_GRAVITY = 0.08;
   private static final float MAX_LAUNCH_SPEED = 2.2F;
   private static final double DISPERSION = 1.2;
   private static final double DISPERSION_MIN_SCALE = 0.35;
   private static final double CLIP_ARM_PITCH = -46.0;
   private static final double SPLIT_FRACTION = 0.65;
   private static final int SPLIT_MIN_TICK = 5;
   private static final int BOMBLETS = 6;
   private static final double BOMBLET_KICK = 0.26;
   private static final int BOMBLET_WAKE_INTERVAL = 4;
   public static final float DAMAGE = 3.0F;
   private static final int IGNITE_SECONDS = 2;
   private static final double BURST_RADIUS = 1.0;
   private static final int MAX_FLIGHT_TICKS = 90;
   private static final int TELEGRAPH_INTERVAL = 4;
   private static final double TELEGRAPH_RADIUS = 3.2;
   private static final int TELEGRAPH_LIFETIME = 8;
   private static final PyroFireball.Shot SHELL = new PyroFireball.Shot(3.0F, 2, 1.0, 1.0, 1.0F, 0.08, 0.0F, 90);
   private static final PyroFireball.Shot BOMBLET = SHELL.withWakeInterval(4);
   private static final float PITCH_JITTER = 0.18F;

   private BarrageEngine() {
   }

   public static boolean hasRoom(Caster caster) {
      ServerLevel level = caster.level();
      Vec3 origin = launchOrigin(caster, false);
      BarrageEngine.Aim aim = aim(caster, level);
      Vec3 target = aim != null ? aim.point() : origin.add(AimUtil.lookFlat(caster.body()).scale(5.0));
      return flightTicks(caster, level, origin, target) != 0;
   }

   public static void open(Caster caster) {
      PyroFlames.sound(caster, caster.body().position(), SoundEvents.FIRECHARGE_USE, 0.7F, 0.7F, FlameTone.of(caster));
   }

   public static BarrageEngine.Step tick(Caster caster, BarrageEngine.Volley volley) {
      int tick = volley.tick++;
      boolean firing = tick % 6 == 0;
      boolean telegraphing = tick % 4 == 0;
      if (firing || telegraphing) {
         ServerLevel level = caster.level();
         BarrageEngine.Aim aim = aim(caster, level);
         if (aim == null) {
            if (firing) {
               tellOnce(caster, volley, 2, "awakened.pyrokinetic.barrage.no_ground");
               volley.refuse();
            }
         } else {
            if (telegraphing) {
               Vfx.ring(level, aim.point().add(0.0, 0.05, 0.0), 3.2, FlameTone.of(caster).uiColor(), 8);
            }

            if (firing) {
               if (aim.deadZone()) {
                  tellOnce(caster, volley, 1, "awakened.pyrokinetic.barrage.too_close");
               }

               if (!fire(caster, level, volley, aim)) {
                  return BarrageEngine.Step.END;
               }
            }
         }
      }

      if (volley.refusing()) {
         return volley.refusedTooLong() ? BarrageEngine.Step.END : BarrageEngine.Step.SKIP;
      } else {
         return BarrageEngine.Step.BILL;
      }
   }

   public static void close(Caster caster) {
      PyroFlames.sound(caster, caster.body().position(), SoundEvents.FIRE_EXTINGUISH, 0.6F, 0.8F, FlameTone.of(caster));
   }

   private static boolean fire(Caster caster, ServerLevel level, BarrageEngine.Volley volley, BarrageEngine.Aim aim) {
      boolean offHand = volley.offHand;
      boolean rightArm = caster.rightArm(offHand);
      Vec3 origin = launchOrigin(caster, offHand);
      Vec3 target = scatter(caster, aim);
      int ticks = flightTicks(caster, level, origin, target);
      if (ticks == 0) {
         caster.fail("awakened.pyrokinetic.barrage.no_room");
         return false;
      } else {
         Vec3 velocity = Ballistics.arc(origin, target, ticks, 0.08);
         double speed = velocity.length();
         if (speed < 1.0E-4) {
            volley.refuse();
            return true;
         } else if (speed > 2.2F) {
            tellOnce(caster, volley, 4, "awakened.pyrokinetic.barrage.out_of_reach");
            volley.refuse();
            return true;
         } else {
            volley.fired();
            volley.offHand = !offHand;
            PyroHeatState.gainBarrageShot(caster);
            FlameTone tone = FlameTone.of(caster);
            float bonus = PyroHeatState.heatDamageBonus(caster);
            float damage = caster.scaledDamage(3.0F, BALANCE_KEY, bonus);
            float burn = caster.scaledDamage(2.0F, BALANCE_KEY, bonus);
            int splitTick = Math.max(5, (int)Math.round((double)ticks * 0.65));
            double kickLift = (double)(ticks - 9) / 19.0;
            PyroFireball.launch(
               caster,
               SHELL.withDamage(damage).withVelocity((float)speed),
               origin,
               velocity.scale(1.0 / speed),
               rightArm,
               tone,
               new PyroFireball.Extras(
                  shell -> split(caster, shell, splitTick, damage, burn, rightArm, tone, kickLift),
                  (pos, normal) -> PyroBurningGround.plant(caster, level, pos, tone, burn)
               )
            );
            PyroFlames.impactFlare(level, origin, tone);
            PyroFlames.sound(caster, origin, SoundEvents.FIRECHARGE_USE, 0.55F, 0.62F + jitter(caster), tone);
            return true;
         }
      }
   }

   private static void split(Caster caster, KineticProjectile shell, int splitTick, float damage, float burn, boolean rightArm, FlameTone tone, double kickLift) {
      if (shell.tickCount >= 5 && (!(shell.getDeltaMovement().y >= 0.0) || shell.tickCount >= splitTick)) {
         if (shell.level() instanceof ServerLevel level && level == caster.body().level()) {
            Vec3 pos = shell.position();
            Vec3 flight = shell.getDeltaMovement();
            double speed = flight.length();
            Vec3 heading = speed < 1.0E-4 ? new Vec3(0.0, -1.0, 0.0) : flight.scale(1.0 / speed);
            Vec3 pole = Math.abs(heading.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
            Vec3 side = heading.cross(pole).normalize();
            Vec3 lift = side.cross(heading).normalize();
            RandomSource rng = level.random;
            double phase = rng.nextDouble() * Math.PI * 2.0;
            boolean[] cue = new boolean[]{true};

            for (int i = 0; i < 6; i++) {
               double angle = phase + (Math.PI * 2) * (double)i / 6.0;
               double reach = 0.26 * Math.sqrt(rng.nextDouble());
               Vec3 kick = side.scale(Math.cos(angle)).add(lift.scale(Math.sin(angle))).scale(reach);
               if (kickLift < 1.0) {
                  kick = new Vec3(kick.x, kick.y * kickLift, kick.z);
               }

               Vec3 child = flight.add(kick);
               double childSpeed = child.length();
               if (!(childSpeed < 1.0E-4)) {
                  PyroFireball.launch(
                     caster,
                     BOMBLET.withDamage(damage).withVelocity((float)childSpeed),
                     pos,
                     child.scale(1.0 / childSpeed),
                     rightArm,
                     tone,
                     new PyroFireball.Extras(null, (hit, normal) -> {
                        PyroBurningGround.plant(caster, level, hit, tone, burn);
                        if (cue[0]) {
                           cue[0] = false;
                           PyroFlames.sound(caster, hit, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), 0.5F, 1.05F, tone);
                        }
                     }, true)
                  );
               }
            }

            PyroFlames.impactFlare(level, pos, tone);
            PyroFlames.knot(level, pos, 0.7, tone);
            PyroFlames.sound(caster, pos, SoundEvents.FIREWORK_ROCKET_BLAST, 0.5F, 1.4F, tone);
            shell.discard();
            return;
         }

         shell.discard();
      }
   }

   private static Vec3 launchOrigin(Caster caster, boolean offHand) {
      double armPitch = Mth.clamp((double)caster.pitch() + -46.0, -90.0, 90.0);
      return caster.hand(offHand, armPitch, 0.34, 0.46, 0.6);
   }

   @Nullable
   private static BarrageEngine.Aim aim(Caster caster, ServerLevel level) {
      Vec3 eye = caster.eyePos();
      Vec3 end = eye.add(caster.aimDir().scale(30.0));
      Vec3 hit = caster.aimPoint(30.0);
      LivingEntity foe = caster.aimTarget(30.0);
      Vec3 point;
      if (foe != null && eye.distanceToSqr(foe.getBoundingBox().getCenter()) < eye.distanceToSqr(hit)) {
         point = foe.position();
      } else if (hit.distanceToSqr(end) > 1.0E-4) {
         point = hit;
      } else {
         Vec3 floor = AimUtil.groundedPositionIfFound(level, end).orElse(null);
         if (floor == null) {
            if (flat(eye, end) < 5.0) {
               return null;
            }

            floor = end;
         }

         point = floor;
      }

      double distance = flat(eye, point);
      boolean deadZone = distance < 5.0;
      if (deadZone) {
         Vec3 out = distance < 0.001 ? AimUtil.lookFlat(caster.body()) : AimUtil.horizontalDir(eye, point);
         point = new Vec3(eye.x + out.x * 5.0, point.y, eye.z + out.z * 5.0);
         distance = 5.0;
      }

      return new BarrageEngine.Aim(point, distance, deadZone);
   }

   private static Vec3 scatter(Caster caster, BarrageEngine.Aim aim) {
      double radius = 1.2 * Mth.clamp(aim.flat() / 30.0, 0.35, 1.0);
      RandomSource rng = caster.body().getRandom();
      double angle = rng.nextDouble() * Math.PI * 2.0;
      double r = Math.sqrt(rng.nextDouble()) * radius;
      return aim.point().add(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
   }

   private static int flightTicks(Caster caster, ServerLevel level, Vec3 origin, Vec3 target) {
      double room = headroom(caster, level, origin) - 0.4;

      for (int ticks = 28; ticks >= 9; ticks--) {
         if (apex(Ballistics.arc(origin, target, ticks, 0.08).y, ticks) <= room) {
            return ticks;
         }
      }

      return 0;
   }

   private static double headroom(Caster caster, ServerLevel level, Vec3 origin) {
      Vec3 top = origin.add(0.0, 8.4, 0.0);
      HitResult roof = level.clip(new ClipContext(origin, top, Block.COLLIDER, Fluid.NONE, caster.body()));
      return roof.getType() == Type.MISS ? 8.4 : roof.getLocation().y - origin.y;
   }

   private static double apex(double lift, int ticks) {
      double peak = 0.0;

      for (int n = 1; n <= ticks; n++) {
         double y = lift * Ballistics.travel(n) - 0.08 * Ballistics.drop(n);
         if (y <= peak) {
            return peak;
         }

         peak = y;
      }

      return peak;
   }

   private static double flat(Vec3 from, Vec3 to) {
      double dx = to.x - from.x;
      double dz = to.z - from.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static float jitter(Caster caster) {
      return (caster.body().getRandom().nextFloat() * 2.0F - 1.0F) * 0.18F;
   }

   private static void tellOnce(Caster caster, BarrageEngine.Volley volley, int flag, String langKey) {
      if ((volley.told & flag) == 0) {
         volley.told |= flag;
         caster.fail(langKey);
      }
   }

   private static record Aim(Vec3 point, double flat, boolean deadZone) {
   }

   public static enum Step {
      BILL,
      SKIP,
      END;
   }

   public static final class Volley {
      private static final int TOLD_CLOSE = 1;
      private static final int TOLD_SKY = 2;
      private static final int TOLD_REACH = 4;
      private int tick;
      private boolean offHand;
      private int told;
      private int refusedSince = -1;

      private void refuse() {
         if (this.refusedSince < 0) {
            this.refusedSince = this.tick;
         }
      }

      private void fired() {
         this.refusedSince = -1;
      }

      private boolean refusing() {
         return this.refusedSince >= 0;
      }

      private boolean refusedTooLong() {
         return this.refusing() && this.tick - this.refusedSince >= 20;
      }
   }
}
